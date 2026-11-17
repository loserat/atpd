package de.leisetv;
import android.content.*;
import org.json.*;
import java.util.*;

/** Production persistence/configuration, with in-memory Android and snapshot adapters. */
public final class ProfileChecks {
 static int count;static final Context C=new Context();
 static void check(String name,boolean ok){if(!ok)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
 interface Action {void run()throws Exception;}
 static void rejects(String name,Action action)throws Exception{String before=AdminServer.P.values.toString();int commits=AdminServer.P.commits;boolean rejected=false;try{action.run();}catch(Exception expected){rejected=true;}check(name,rejected&&commits==AdminServer.P.commits&&before.equals(AdminServer.P.values.toString()));}
 static JSONObject copy(JSONObject o)throws Exception{return new JSONObject(o.toString());}
 static JSONObject request(String id)throws Exception{return new JSONObject().put("id",id);}
 static JSONObject firstCustom(JSONObject view)throws Exception{return view.getJSONArray("profiles").getJSONObject(2);}
 public static void main(String[] args)throws Exception{
  JSONObject standard=ProfileConfig.builtin("standard"),media=ProfileConfig.builtin("media");
  check("Standard retains primary-TV destinations and enabled integrations",standard.getJSONObject("display").getBoolean("camera")&&standard.getJSONObject("integrations").getBoolean("hue")&&standard.getJSONObject("integrations").getBoolean("unifi")&&!standard.getJSONObject("appShortcuts").getBoolean("enabled")&&standard.getJSONObject("appearance").getJSONArray("order").getString(0).equals("ambilight"));
  check("Mediathek selects optional apps and disables absent providers",!media.getJSONObject("display").getBoolean("camera")&&!media.getJSONObject("integrations").getBoolean("hue")&&!media.getJSONObject("integrations").getBoolean("unifi")&&media.getJSONObject("appShortcuts").getBoolean("enabled")&&media.getJSONObject("appShortcuts").getJSONArray("apps").length()==2&&media.getJSONObject("appearance").getJSONArray("order").getString(1).equals("ard"));
  rejects("Unknown builtin rejected",()->ProfileConfig.builtin("missing"));
  JSONObject extra=copy(standard).put("host","secret");rejects("Provider fields cannot enter a profile",()->ProfileConfig.normalize(extra));
  JSONObject nested=copy(standard);nested.getJSONObject("display").put("homeTitle","private");rejects("Personal headings cannot enter a profile",()->ProfileConfig.normalize(nested));
  JSONObject badBool=copy(standard);badBool.getJSONObject("integrations").put("hue","false");rejects("String flags rejected",()->ProfileConfig.normalize(badBool));
  JSONObject badOrder=copy(standard);badOrder.getJSONObject("appearance").put("order",new JSONArray(Arrays.asList("ambilight","cameras")));rejects("Partial layout rejected",()->ProfileConfig.normalize(badOrder));
  JSONObject badFont=copy(standard);badFont.getJSONObject("interface").put("textFont","external-font");rejects("External font rejected",()->ProfileConfig.normalize(badFont));
  JSONObject badKey=copy(standard);badKey.getJSONObject("remote").put("red","intent:anything");rejects("Arbitrary intents rejected",()->ProfileConfig.normalize(badKey));
  JSONObject legacy=copy(standard);legacy.getJSONObject("appearance").put("order",new JSONArray(Arrays.asList("system","inputs","tv","hue","cameras","ambilight")));check("Legacy profile order normalizes without reordering",ProfileConfig.normalize(legacy).getJSONObject("appearance").getJSONArray("order").getString(0).equals("system")&&ProfileConfig.normalize(legacy).getJSONObject("appearance").getJSONArray("order").length()==14);
  for(Object value:new Object[]{"","  ","x\ny",new String(new char[41]).replace('\0','x'),42})rejects("Invalid profile name rejected "+count,()->ProfileConfig.name(value));
  check("Names trim and preserve Unicode",ProfileConfig.name("  Kino 🌙  ").equals("Kino 🌙"));
  JSONObject initial=DashboardProfiles.view(C);check("Fresh store offers two presets and recognizes Standard",initial.getJSONArray("profiles").length()==2&&initial.getString("activeId").equals("standard")&&initial.getInt("limit")==8);
  AdminServer.P.values.put("home_title","Personal title");AdminServer.P.values.put("device_display_name","Private TV");AdminServer.P.values.put("device_product_model","Sales model");AdminServer.P.values.put("admin_pin","local-only");AdminServer.P.values.put("hue_standby_off",true);AdminServer.P.values.put("saver_style","color");AdminServer.P.values.put("provider_secret","retained");
  int before=AdminServer.P.commits;JSONObject saved=DashboardProfiles.save(C,new JSONObject().put("name","Zuhause"));String id=firstCustom(saved).getString("id");
  check("Creating snapshots changes only the profile store",AdminServer.P.commits==before+1&&AdminServer.P.lastKeys.equals(Collections.singleton("dashboard_profiles_v1"))&&saved.getJSONArray("profiles").length()==3);
  check("Snapshots exclude private names keys and automation",!firstCustom(saved).getJSONObject("config").toString().contains("Personal title")&&!firstCustom(saved).getJSONObject("config").toString().contains("retained")&&!firstCustom(saved).getJSONObject("config").has("saver"));
  rejects("Duplicate names rejected without writes",()->DashboardProfiles.save(C,new JSONObject().put("name"," zuhause ")));
  rejects("Builtin names reserved",()->DashboardProfiles.save(C,new JSONObject().put("name","Standard")));
  rejects("Builtin overwrite rejected",()->DashboardProfiles.save(C,new JSONObject().put("name","Copy").put("id","media")));
  rejects("Unknown save fields rejected",()->DashboardProfiles.save(C,new JSONObject().put("name","Copy").put("config",standard)));
  JSONObject snap=copy(firstCustom(saved).getJSONObject("config"));JSONObject renamed=DashboardProfiles.rename(C,new JSONObject().put("id",id).put("name","Mein Profil"));check("Rename retains exact saved settings",firstCustom(renamed).getString("name").equals("Mein Profil")&&ProfileConfig.equal(snap,firstCustom(renamed).getJSONObject("config")));
  before=AdminServer.P.commits;JSONObject applied=DashboardProfiles.apply(C,request("media"));check("Applying is one configuration commit",AdminServer.P.commits==before+1&&AdminServer.P.lastKeys.contains("integration_hue")&&AdminServer.P.lastKeys.contains("home_order")&&AdminServer.P.lastKeys.contains("revision"));
  check("Applied Mediathek is recognized and stored",applied.getString("activeId").equals("media")&&AdminServer.P.getBoolean("show_media_apps",false)&&!AdminServer.P.getBoolean("show_camera",true));
  check("Application retains personal fields and unrelated settings",AdminServer.P.getString("home_title","").equals("Personal title")&&AdminServer.P.getString("device_display_name","").equals("Private TV")&&AdminServer.P.getString("device_product_model","").equals("Sales model")&&AdminServer.P.getString("admin_pin","").equals("local-only")&&AdminServer.P.getBoolean("hue_standby_off",false)&&AdminServer.P.getString("saver_style","").equals("color")&&AdminServer.P.getString("provider_secret","").equals("retained"));
  check("Stored custom snapshot can restore the original setup",ProfileConfig.equal(DashboardProfiles.apply(C,request(id)).getJSONObject("current"),snap));
  rejects("Missing apply target rejected atomically",()->DashboardProfiles.apply(C,request("custom-00000000-0000-0000-0000-000000000000")));
  rejects("Apply cannot carry extra configuration",()->DashboardProfiles.apply(C,request(id).put("config",media)));
  rejects("Builtin deletion rejected",()->DashboardProfiles.delete(C,request("standard")));
  rejects("Builtin rename rejected",()->DashboardProfiles.rename(C,request("media").put("name","X")));
  AdminServer.P.values.put("home_theme","wald");check("Manual adjustments stop matching an old profile",DashboardProfiles.view(C).getString("activeId").isEmpty());
  DashboardProfiles.save(C,request(id).put("name","Wald"));check("Replacing captures the latest saved configuration",firstCustom(DashboardProfiles.view(C)).getJSONObject("config").getJSONObject("appearance").getString("theme").equals("wald"));
  before=AdminServer.P.commits;DashboardProfiles.delete(C,request(id));check("Deleting a profile never applies another setup",AdminServer.P.getString("home_theme","").equals("wald")&&AdminServer.P.commits==before+1&&AdminServer.P.lastKeys.equals(Collections.singleton("dashboard_profiles_v1")));
  for(int n=0;n<8;n++)DashboardProfiles.save(C,new JSONObject().put("name","Profile "+n));check("Eight custom profiles can coexist",DashboardProfiles.view(C).getJSONArray("profiles").length()==10);
  rejects("Ninth profile rejected without changes",()->DashboardProfiles.save(C,new JSONObject().put("name","Ninth")));
  String one=firstCustom(DashboardProfiles.view(C)).getString("id");DashboardProfiles.save(C,request(one).put("name","Updated at limit"));check("Existing profile can update at the limit",DashboardProfiles.view(C).getJSONArray("profiles").length()==10);
  JSONObject mediaCustom=copy(media);mediaCustom.put("type","standard");check("Profile type is explicit and independent from visibility",ProfileConfig.normalize(mediaCustom).getString("type").equals("standard"));
  JSONObject old=copy(media);old.remove("type");old.put("appShortcuts",new JSONObject().put("ard",true).put("zdf",true));check("Legacy Mediathek type is inferred without discarding apps",ProfileConfig.normalize(old).getString("type").equals("media"));
  JSONObject four=new JSONObject().put("enabled",true).put("apps",new JSONArray(Arrays.asList("ard","zdf","pkg:tv.sample.video","pkg:tv.sample.music")));check("Four bounded application choices accepted",AppSelection.normalize(four).getJSONArray("apps").length()==4);
  rejects("Duplicate favorite identifiers rejected",()->AppSelection.normalize(new JSONObject().put("enabled",true).put("apps",new JSONArray(Arrays.asList("ard","ard")))));
  rejects("Fifth favorite rejected",()->AppSelection.normalize(new JSONObject().put("enabled",true).put("apps",new JSONArray(Arrays.asList("ard","zdf","pkg:tv.a.app","pkg:tv.b.app","pkg:tv.c.app")))));
  rejects("Intent URL cannot be an application identifier",()->AppSelection.normalize(new JSONObject().put("enabled",true).put("apps",new JSONArray(Arrays.asList("https://anything.test")))));
  rejects("Numeric application identifier rejected",()->AppSelection.normalize(new JSONObject().put("enabled",true).put("apps",new JSONArray(Arrays.asList(42)))));
  rejects("Unknown profile type rejected",()->ProfileConfig.normalize(copy(media).put("type","remote-command")));
  JSONObject mediaApps=copy(media);mediaApps.put("appShortcuts",four);check("A profile snapshots every favorite and its type",ProfileConfig.normalize(mediaApps).getJSONObject("appShortcuts").getJSONArray("apps").length()==4&&ProfileConfig.normalize(mediaApps).getString("type").equals("media"));
  DashboardProfiles.apply(C,request("standard"));check("Standard hides but preserves the current favorite choice",!AppShortcuts.config(C).getBoolean("enabled"));
  JSONObject same=DashboardProfiles.save(C,request(one).put("name","Named standard").put("type","standard"));DashboardProfiles.apply(C,request(one));check("Explicit custom selection wins over an identical builtin",DashboardProfiles.view(C).getString("selectedId").equals(one)&&DashboardProfiles.view(C).getString("activeId").equals(one));
  AdminServer.P.values.put("home_theme","abendrot");JSONObject changed=DashboardProfiles.view(C);check("Edited custom profile retains its selected name and reports modified",changed.getString("selectedId").equals(one)&&changed.getString("activeId").isEmpty()&&changed.getBoolean("modified"));
  DashboardProfiles.rename(C,request(one).put("name","Renamed selected"));check("Renaming selected profile keeps its type",firstCustom(DashboardProfiles.view(C)).getJSONObject("config").getString("type").equals("standard"));
  JSONObject favorites=FavoriteConfig.empty();favorites.getJSONObject("input").put("enabled",true).put("id","test.pkg/.Input/HW4");favorites.put("cameras",new JSONArray(Arrays.asList("front","garden","garage")));
  check("Favorite local references normalize without provider fields",FavoriteConfig.normalize(favorites).getJSONArray("cameras").length()==3);
  JSONObject tooMany=copy(favorites).put("cameras",new JSONArray(Arrays.asList("a","b","c","d")));rejects("Fourth camera favorite rejected",()->FavoriteConfig.normalize(tooMany));
  JSONObject duplicateFavorite=copy(favorites).put("cameras",new JSONArray(Arrays.asList("a","a")));rejects("Duplicate camera favorites rejected",()->FavoriteConfig.normalize(duplicateFavorite));
  JSONObject noInput=copy(favorites);noInput.getJSONObject("input").put("id","");rejects("Enabled HDMI requires a target",()->FavoriteConfig.normalize(noInput));
  JSONObject providerFavorite=copy(favorites).put("url","rtsps://example.invalid");rejects("Favorite provider fields rejected",()->FavoriteConfig.normalize(providerFavorite));
  JSONObject withFavorites=copy(standard).put("favorites",favorites);check("Favorite config belongs to profile snapshots",ProfileConfig.normalize(withFavorites).getJSONObject("favorites").toString().equals(FavoriteConfig.normalize(favorites).toString()));
  check("Legacy snapshots normalize favorites disabled",ProfileConfig.normalize(standard).getJSONObject("favorites").getJSONArray("cameras").length()==0);
  System.out.println(count+" profile configuration/persistence checks passed; in-memory Android adapters only.");
 }
}
class AdminServer {
 static final Prefs P=new Prefs();static SharedPreferences preferences(Context c){return P;}
 static class Prefs implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>();int commits;Set<String> lastKeys;
  public boolean contains(String key){return values.containsKey(key);}public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}public long getLong(String k,long d){return values.containsKey(k)?(Long)values.get(k):d;}
  public Editor edit(){return new Editor(){final Map<String,Object> changes=new HashMap<>();public Editor putString(String k,String v){changes.put(k,v);return this;}public Editor putBoolean(String k,boolean v){changes.put(k,v);return this;}public Editor putLong(String k,long v){changes.put(k,v);return this;}public Editor remove(String k){changes.put(k,null);return this;}public boolean commit(){values.putAll(changes);lastKeys=new HashSet<>(changes.keySet());commits++;return true;}};}
 }
}
class AppShortcuts {static JSONObject config(Context c)throws Exception{if(AdminServer.P.contains("favorite_apps"))return AppSelection.normalize(new JSONObject().put("enabled",AdminServer.P.getBoolean("show_media_apps",false)).put("apps",new JSONArray(AdminServer.P.getString("favorite_apps","[]"))));return AppSelection.normalize(new JSONObject().put("ard",AdminServer.P.getBoolean("app_shortcut_ard",false)).put("zdf",AdminServer.P.getBoolean("app_shortcut_zdf",false)));}}
class UiSettings {static JSONObject view(Context c)throws Exception{return new JSONObject().put("language",AdminServer.P.getString("ui_language","de")).put("textFont",AdminServer.P.getString("ui_text_font","sans")).put("clockFont",AdminServer.P.getString("ui_clock_font","sans"));}}
class RemoteShortcuts {static JSONObject view(Context c)throws Exception{JSONObject o=new JSONObject();String[] keys={"red","green","yellow","blue"},defaults={"cameras","ambilight","hue","inputs"};for(int n=0;n<4;n++)o.put(keys[n],AdminServer.P.getString("remote_"+keys[n],defaults[n]));return o;}}

class HomeFavorites {static JSONObject config(Context c)throws Exception{String raw=AdminServer.P.getString("home_favorites","");return raw.isEmpty()?FavoriteConfig.empty():FavoriteConfig.normalize(new JSONObject(raw));}}
