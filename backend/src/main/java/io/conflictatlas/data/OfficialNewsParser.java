package io.conflictatlas.data;
import io.conflictatlas.domain.Models.NewsItem;
import java.io.*;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public final class OfficialNewsParser {
    private OfficialNewsParser() {}
    private static final Pattern TOPIC=Pattern.compile("\\b(?:wars?|gaza|sudan|ukraine|lebanon|syria|militar\\w*|peace\\w*|humanitarian|ceasefire|displac\\w*|conflict\\w*|attack\\w*|violence|aid|rafah|myanmar|armed|drone\\w*|flee)\\b",Pattern.CASE_INSENSITIVE);
    public static List<NewsItem> parse(byte[] bytes)throws Exception{
        var factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
        var document=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        var items=document.getElementsByTagName("item");List<NewsItem> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int i=0;i<Math.min(items.getLength(),100)&&result.size()<15;i++){
            Element item=(Element)items.item(i);String title=text(item,"title"),url=text(item,"link"),date=text(item,"pubDate");
            try{
                URI uri=URI.create(url);
                if("https".equals(uri.getScheme())&&"news.un.org".equals(uri.getHost())&&title.length()<=500&&TOPIC.matcher(title).find()&&seen.add(url)){
                    String normalized=ZonedDateTime.parse(date.replace(" CET"," +0100").replace(" CEST"," +0200"),DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toString();
                    result.add(new NewsItem(title,url,normalized,"UN News"));
                }
            }catch(IllegalArgumentException|java.time.DateTimeException ignored){/* Skip malformed provider entries; keep other dated reporting. */}
        }
        if(result.isEmpty())throw new IOException("No valid dated reporting entries.");return List.copyOf(result);
    }
    private static String text(Element element,String tag){var nodes=element.getElementsByTagName(tag);return nodes.getLength()>0?nodes.item(0).getTextContent().strip():"";}
}
