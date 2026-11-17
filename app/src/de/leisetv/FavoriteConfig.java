package de.leisetv;
import org.json.*;
import java.io.IOException;
import java.util.*;
/** Strict local references only: no stream URLs, credentials or launch intents. */
final class FavoriteConfig {
 static final int CAMERA_LIMIT=3;
 static JSONObject empty()throws Exception{return new JSONObject().put("input",new JSONObject().put("enabled",false).put("id","").put("name","Apple TV")).put("cameras",new JSONArray());}
 static String reference(Object value,int limit,boolean empty)throws Exception{if(!(value instanceof String))throw new IOException("Invalid favorite reference");String s=(String)value;if((!empty&&s.isEmpty())||s.length()>limit||s.chars().anyMatch(Character::isISOControl))throw new IOException("Invalid favorite reference");return s;}
 static JSONObject normalize(JSONObject raw)throws Exception{
  ProfileConfig.fields(raw,"input","cameras");JSONObject input=raw.getJSONObject("input");ProfileConfig.fields(input,"enabled","id","name");boolean enabled=ProfileConfig.flag(input,"enabled");String id=reference(input.get("id"),512,!enabled),name=reference(input.get("name"),40,false).trim();if(name.isEmpty())throw new IOException("Favorite name required");
  JSONArray ids=raw.getJSONArray("cameras");if(ids.length()>CAMERA_LIMIT)throw new IOException("Too many camera favorites");List<String> cameras=new ArrayList<>();for(int n=0;n<ids.length();n++){String item=reference(ids.get(n),100,false);if(cameras.contains(item))throw new IOException("Duplicate camera favorite");cameras.add(item);}
  return new JSONObject().put("input",new JSONObject().put("enabled",enabled).put("id",id).put("name",name)).put("cameras",new JSONArray(cameras));
 }
}
