package de.leisetv;
import android.content.*;
import android.content.pm.*;
import android.widget.Toast;
import java.io.IOException;
import java.util.*;
import org.json.*;
/** Explicit app favorites; no installation, account handling or arbitrary intents. */
final class AppShortcuts {
 static final String[] SLOTS={"ard","zdf","app3","app4"};
 private static final class Entry {final String id,name;final Intent intent;Entry(String id,String name,Intent intent){this.id=id;this.name=name;this.intent=intent;}}
 private static List<Entry> cached;private static long cachedAt;
 private static synchronized List<Entry> catalog(Context c,boolean fresh){
  if(!fresh&&cached!=null&&android.os.SystemClock.elapsedRealtime()-cachedAt<2000)return cached;
  PackageManager pm=c.getPackageManager();Map<String,Entry> entries=new LinkedHashMap<>();Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
  List<ResolveInfo> all=pm.queryIntentActivities(query,0);all.sort(Comparator.comparing(i->i.activityInfo.packageName));
  for(ResolveInfo info:all){String pkg=info.activityInfo.packageName;if(pkg.equals(c.getPackageName()))continue;Intent intent=pm.getLeanbackLaunchIntentForPackage(pkg);if(intent==null)continue;String name=info.loadLabel(pm).toString().replaceAll("[\\p{Cntrl}]","").trim();if(name.isEmpty())name=pkg;if(name.length()>80)name=name.substring(0,80);String flat=name.toLowerCase(Locale.ROOT).replaceAll("\\s+","");String id=Arrays.asList("de.swr.avp.ard.tv","de.swr.avp.ard").contains(pkg)||flat.equals("ardmediathek")?"ard":pkg.equals("com.zdf.android.mediathek")||Arrays.asList("zdf","zdfmediathek").contains(flat)?"zdf":"pkg:"+pkg;
   Entry prior=entries.get(id);boolean official=pkg.equals("de.swr.avp.ard.tv")||pkg.equals("com.zdf.android.mediathek");if(prior==null||official)entries.put(id,new Entry(id,name,intent));
  }
  List<Entry> result=new ArrayList<>(entries.values());result.sort(Comparator.comparing((Entry e)->e.name,String.CASE_INSENSITIVE_ORDER).thenComparing(e->e.id));if(result.size()>128)result=new ArrayList<>(result.subList(0,128));cached=result;cachedAt=android.os.SystemClock.elapsedRealtime();return result;
 }
 static JSONObject config(Context c)throws Exception{
  SharedPreferences p=AdminServer.preferences(c);if(p.contains("favorite_apps"))return AppSelection.normalize(new JSONObject().put("enabled",p.getBoolean("show_media_apps",false)).put("apps",new JSONArray(p.getString("favorite_apps","[]"))));
  return AppSelection.normalize(new JSONObject().put("ard",p.getBoolean("app_shortcut_ard",false)).put("zdf",p.getBoolean("app_shortcut_zdf",false)));
 }
 private static Entry find(List<Entry> entries,String id){for(Entry entry:entries)if(entry.id.equals(id))return entry;return null;}
 static int count(Context c){try{JSONObject cfg=config(c);return cfg.getBoolean("enabled")?cfg.getJSONArray("apps").length():0;}catch(Exception e){return 0;}}
 static boolean enabled(Context c,String slot){return Arrays.asList(SLOTS).indexOf(slot)>=0&&Arrays.asList(SLOTS).indexOf(slot)<count(c);}
 private static String selected(Context c,String slot)throws Exception{int at=Arrays.asList(SLOTS).indexOf(slot);return at>=0&&at<count(c)?config(c).getJSONArray("apps").getString(at):"";}
 static String label(Context c,String slot){try{String id=selected(c,slot);Entry entry=find(catalog(c,false),id);if(entry!=null)return entry.name;if(id.equals("ard"))return "ARD Mediathek";if(id.equals("zdf"))return "ZDF Mediathek";JSONObject labels=new JSONObject(AdminServer.preferences(c).getString("favorite_app_labels","{}"));return labels.optString(id,UiText.t("App nicht verfügbar"));}catch(Exception e){return UiText.t("App nicht verfügbar");}}
 static JSONObject view(Context c)throws Exception{
  JSONObject cfg=config(c),labels=new JSONObject(AdminServer.preferences(c).getString("favorite_app_labels","{}"));JSONArray inventory=new JSONArray(),ids=cfg.getJSONArray("apps");Set<String> seen=new HashSet<>();List<Entry> entries=catalog(c,true);
  for(Entry entry:entries){inventory.put(new JSONObject().put("id",entry.id).put("name",entry.name).put("available",true));seen.add(entry.id);}
  for(int n=0;n<ids.length();n++){String id=ids.getString(n);if(!seen.contains(id))inventory.put(new JSONObject().put("id",id).put("name",labels.optString(id,id.equals("ard")?"ARD Mediathek":id.equals("zdf")?"ZDF Mediathek":UiText.t("App nicht verfügbar"))).put("available",false));}
  boolean ard=false,zdf=false;for(int n=0;n<ids.length();n++){ard|=ids.getString(n).equals("ard");zdf|=ids.getString(n).equals("zdf");}
  return new JSONObject(cfg.toString()).put("inventory",inventory).put("limit",AppSelection.LIMIT).put("ard",cfg.getBoolean("enabled")&&ard).put("zdf",cfg.getBoolean("enabled")&&zdf).put("ardAvailable",find(entries,"ard")!=null).put("zdfAvailable",find(entries,"zdf")!=null);
 }
 static JSONObject save(Context c,JSONObject input)throws Exception{
  JSONObject cfg=AppSelection.normalize(input),old=config(c);if(input.has("ard")){JSONArray legacy=cfg.getJSONArray("apps"),prior=old.getJSONArray("apps");List<String> keep=new ArrayList<>();for(int n=0;n<prior.length();n++){String id=prior.getString(n);if(!id.equals("ard")&&!id.equals("zdf"))keep.add(id);}boolean on=ProfileConfig.flag(input,"ard")||ProfileConfig.flag(input,"zdf");if(on){for(int n=0;n<legacy.length();n++)keep.add(legacy.getString(n));}if(keep.isEmpty())keep.addAll(Arrays.asList("ard","zdf"));cfg=AppSelection.normalize(new JSONObject().put("apps",new JSONArray(keep)).put("enabled",on||old.getBoolean("enabled")&&keep.stream().anyMatch(id->!id.equals("ard")&&!id.equals("zdf"))));}JSONArray ids=cfg.getJSONArray("apps"),prior=old.getJSONArray("apps");List<Entry> entries=catalog(c,true);JSONObject labels=new JSONObject(AdminServer.preferences(c).getString("favorite_app_labels","{}"));
  for(int n=0;n<ids.length();n++){String id=ids.getString(n);Entry entry=find(entries,id);boolean retained=false;for(int j=0;j<prior.length();j++)retained|=prior.getString(j).equals(id);if(entry==null&&!retained)throw new IOException("App is not installed");if(entry!=null)labels.put(id,entry.name);}
  SharedPreferences p=AdminServer.preferences(c);if(!p.edit().putString("favorite_apps",ids.toString()).putBoolean("show_media_apps",cfg.getBoolean("enabled")).putString("favorite_app_labels",labels.toString()).putLong("revision",Math.max(System.currentTimeMillis(),p.getLong("revision",0)+1)).commit())throw new IOException("Settings not saved");return view(c);
 }
 static void open(Context c,String slot){try{String id=selected(c,slot);if(id.isEmpty())return;Entry entry=find(catalog(c,true),id);if(entry!=null){c.startActivity(new Intent(entry.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;}}catch(Exception ignored){}Toast.makeText(c,UiText.t("App nicht gefunden. Bitte über Apps installieren."),Toast.LENGTH_LONG).show();}
}
