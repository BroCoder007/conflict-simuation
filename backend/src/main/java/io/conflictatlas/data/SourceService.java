package io.conflictatlas.data;

import io.conflictatlas.domain.Models.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;




@Service
public final class SourceService {
    public static final String NEWS_URL="https://news.un.org/feed/subscribe/en/news/all/rss.xml";
    private static final Map<String,String> INDICATORS=Map.of("spending","MS.MIL.XPND.CD","personnel","MS.MIL.TOTL.P1","gdp","NY.GDP.MKTP.CD","population","SP.POP.TOTL");
    private final DataCatalog catalog;
    private final JdbcTemplate db;
    private final boolean enabled;
    private final JsonMapper json=JsonMapper.builder().build();
    private volatile HttpClient http;
    private final AtomicBoolean refreshing=new AtomicBoolean();
    private volatile NewsSnapshot news;
    private volatile String lastAttempt="",resourceError="",newsError="";
    public SourceService(DataCatalog catalog,JdbcTemplate db,@Value("${atlas.live-data:true}")boolean enabled){
        this.catalog=catalog;this.db=db;this.enabled=enabled;news=catalog.seedNews();
        db.execute("CREATE TABLE IF NOT EXISTS source_cache(id VARCHAR(30) PRIMARY KEY,payload TEXT NOT NULL)");
        var cachedResources=db.query("SELECT payload FROM source_cache WHERE id='resources'",(row,n)->json.readValue(row.getString(1),ResourceSnapshot.class));
        if(!cachedResources.isEmpty()&&cachedResources.getFirst().fetchedAt().compareTo(catalog.resources().fetchedAt())>0)catalog.updateResources(cachedResources.getFirst());
        var cachedNews=db.query("SELECT payload FROM source_cache WHERE id='news'",(row,n)->json.readValue(row.getString(1),NewsSnapshot.class));
        if(!cachedNews.isEmpty())news=cachedNews.getFirst();
    }
    public NewsSnapshot news(){
        boolean stale=news.stale()||Instant.parse(news.fetchedAt()).isBefore(Instant.now().minus(Duration.ofHours(1)));
        return new NewsSnapshot(news.fetchedAt(),news.items(),stale,newsError.isBlank()?news.lastError():newsError);
    }
    public Map<String,Object> status(){return Map.of("enabled",enabled,"refreshing",refreshing.get(),"lastAttempt",lastAttempt,"resourcesFetchedAt",catalog.resources().fetchedAt(),"resourceError",resourceError,"newsError",newsError,"newsFetchedAt",news.fetchedAt(),"conflict",catalog.manifest());}
    @Scheduled(initialDelay=15000,fixedDelay=900000)
    public void scheduledRefresh(){refresh(false);}
    public void refresh(boolean manual){
        if(!enabled)return;
        if(!lastAttempt.isBlank()&&Instant.parse(lastAttempt).isAfter(Instant.now().minus(Duration.ofMinutes(10))))return;
        if(!refreshing.compareAndSet(false,true))return;
        lastAttempt=Instant.now().toString();
        Thread.startVirtualThread(()->{
            try {
                try {refreshNews();newsError="";}catch(Exception ex){newsError="UN reporting is unavailable. Showing the last successful snapshot.";}
                if(manual||Instant.parse(catalog.resources().fetchedAt()).isBefore(Instant.now().minus(Duration.ofHours(24)))) {
                    try{refreshResources();resourceError="";}catch(Exception ex){resourceError="World Bank refresh failed. Published observations from the last successful snapshot remain available.";}
                }
            }finally{refreshing.set(false);}
        });
    }
    private byte[] fetch(String url,int limit)throws Exception{
        if(http==null)http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        var response=http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("User-Agent","ConflictAtlas-Educational/1.0").GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        try(var input=response.body()){
            if(response.statusCode()!=200)throw new IOException("Source HTTP "+response.statusCode());
            InputStream decoded=response.headers().firstValue("Content-Encoding").orElse("").equalsIgnoreCase("gzip")?new java.util.zip.GZIPInputStream(input):input;
            byte[] body=decoded.readNBytes(limit+1);if(body.length>limit)throw new IOException("Source exceeds size limit.");return body;
        }
    }
    private void refreshResources()throws Exception{
        Map<String,Map<String,Metric>> result=new HashMap<>();
        Set<String> allowed=new HashSet<>(catalog.countries().stream().map(CountryInfo::id).toList());
        for(var pair:INDICATORS.entrySet()){
            String url="https://api.worldbank.org/v2/country/all/indicator/"+pair.getValue()+"?format=json&mrnev=1&per_page=1000";
            JsonNode tree=json.readTree(fetch(url,5_000_000));
            if(!tree.isArray()||tree.size()<2||!tree.get(1).isArray())throw new IOException("Unexpected indicator schema.");
            int accepted=0;
            for(JsonNode row:tree.get(1)){
                String id=row.path("countryiso3code").asText();
                if(allowed.contains(id)&&row.path("value").isNumber()){
                    double value=row.path("value").asDouble();int year=row.path("date").asInt();
                    if(Double.isFinite(value)&&value>=0&&year>=1960&&year<=Year.now().getValue()){
                        result.computeIfAbsent(id,k->new HashMap<>()).put(pair.getKey(),new Metric(value,year,pair.getValue()));accepted++;
                    }
                }
            }
            if(accepted<50)throw new IOException("Incomplete indicator response.");
        }
        var snapshot=new ResourceSnapshot(Instant.now().toString(),result);
        persist("resources",snapshot);catalog.updateResources(snapshot);
    }
    private void refreshNews()throws Exception{
        var entries=OfficialNewsParser.parse(fetch(NEWS_URL,2_000_000));
        var snapshot=new NewsSnapshot(Instant.now().toString(),entries,false,"");persist("news",snapshot);news=snapshot;
    }
    private void persist(String key,Object value){String payload=json.writeValueAsString(value);if(db.update("UPDATE source_cache SET payload=? WHERE id=?",payload,key)==0)db.update("INSERT INTO source_cache(id,payload) VALUES(?,?)",key,payload);}
}
