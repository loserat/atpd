package de.leisetv;
import org.json.*;
import java.io.IOException;
import java.util.*;
/** Provider-free bounded application identifiers, including retained legacy aliases. */
final class AppSelection {
 static final int LIMIT=4;
 static boolean id(String id){return "ard".equals(id)||"zdf".equals(id)||id.matches("pkg:[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+");}
 static JSONObject normalize(JSONObject input)throws Exception{
  boolean legacy=input.has("ard");List<String> ids=new ArrayList<>();boolean enabled;
  if(legacy){ProfileConfig.fields(input,"ard","zdf");boolean ard=ProfileConfig.flag(input,"ard"),zdf=ProfileConfig.flag(input,"zdf");enabled=ard||zdf;if(ard)ids.add("ard");if(zdf)ids.add("zdf");if(!enabled)ids.addAll(Arrays.asList("ard","zdf"));}
  else{ProfileConfig.fields(input,"enabled","apps");enabled=ProfileConfig.flag(input,"enabled");JSONArray array=input.getJSONArray("apps");if(array.length()>LIMIT)throw new IOException("Too many apps");for(int n=0;n<array.length();n++){Object item=array.get(n);if(!(item instanceof String)||!id((String)item)||ids.contains(item))throw new IOException("Invalid app selection");ids.add((String)item);}}
  return new JSONObject().put("enabled",enabled).put("apps",new JSONArray(ids));
 }
}
