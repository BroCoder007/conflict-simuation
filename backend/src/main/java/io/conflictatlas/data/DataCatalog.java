package io.conflictatlas.data;

import io.conflictatlas.domain.Models.*;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.*;

@Service
public final class DataCatalog {
    private final JsonMapper json=JsonMapper.builder().build();
    private final List<CountryInfo> countries;
    private final List<HistoryRow> history;
    private final List<CandidateRow> candidate;
    private final JsonNode manifest;
    private volatile ResourceSnapshot resources;
    private final NewsSnapshot seedNews;
    public DataCatalog() throws IOException {
        countries=List.of(read("countries.json",CountryInfo[].class));
        history=List.of(read("history.json",HistoryRow[].class));
        candidate=List.of(read("candidate.json",CandidateRow[].class));
        resources=read("metrics.json",ResourceSnapshot.class);
        manifest=read("manifest.json",JsonNode.class);
        JsonNode news=read("news.json",JsonNode.class);
        seedNews=new NewsSnapshot(news.path("fetchedAt").asText(),List.of(json.treeToValue(news.path("items"),NewsItem[].class)),true,"Bundled snapshot; awaiting a successful feed refresh.");
    }
    private <T>T read(String file,Class<T> type) throws IOException {
        try(var input=new ClassPathResource("data/"+file).getInputStream()){return json.readValue(input,type);}
    }
    public List<CountryInfo> countries(){return countries;}
    public List<HistoryRow> history(){return history;}
    public List<CandidateRow> candidate(){return candidate;}
    public JsonNode manifest(){return manifest;}
    public ResourceSnapshot resources(){return resources;}
    public void updateResources(ResourceSnapshot snapshot){resources=snapshot;}
    public NewsSnapshot seedNews(){return seedNews;}
    public Country country(String id){
        CountryInfo info=countries.stream().filter(c->c.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Country not found."));
        return new Country(info,resources.countries().getOrDefault(id,Map.of()),resources.fetchedAt());
    }
    public List<HistoryRow> history(String id){return history.stream().filter(r->r.country().equals(id)).toList();}
    public double variability(String id){
        double[] values=history(id).stream().sorted(Comparator.comparingInt(HistoryRow::year).reversed()).limit(10).mapToDouble(r->Math.log1p(r.best())).toArray();
        if(values.length<3)return .5; // Documented neutral model assumption when history is sparse.
        double mean=Arrays.stream(values).average().orElse(0),sum=0;
        for(double x:values)sum+=(x-mean)*(x-mean);
        return Math.min(1,Math.sqrt(sum/values.length)/5.0);
    }
}
