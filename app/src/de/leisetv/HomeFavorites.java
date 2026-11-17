package de.leisetv;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputManager;
import android.widget.Toast;
import java.io.IOException;
import java.util.*;
import org.json.*;
/** Opt-in Home destinations. Configuration/discovery never opens a source. */
final class HomeFavorites {
 static final String[] CAMERA_SLOTS={"camera1","camera2","camera3"};
 static JSONObject config(Context c)throws Exception{String raw=AdminServer.preferences(c).getString("home_favorites","");return raw.isEmpty()?FavoriteConfig.empty():FavoriteConfig.normalize(new JSONObject(raw));}
 static boolean inputEnabled(Context c){try{return config(c).getJSONObject("input").getBoolean("enabled");}catch(Exception e){return false;}}
 static int cameraCount(Context c){try{return config(c).getJSONArray("cameras").length();}catch(Exception e){return 0;}}
 static InputCatalog.Entry input(Context c,String id){for(InputCatalog.Entry e:InputCatalog.read(c))if(e.id.equals(id))return e;return null;}
 static JSONObject camera(Context c,String id){JSONArray all=CameraLibrary.all(c);for(int n=0;n<all.length();n++){JSONObject value=all.optJSONObject(n);if(value!=null&&id.equals(value.optString("id")))return value;}return null;}
 static String cameraId(Context c,String slot)throws Exception{int at=Arrays.asList(CAMERA_SLOTS).indexOf(slot);JSONArray ids=config(c).getJSONArray("cameras");return at>=0&&at<ids.length()?ids.getString(at):"";}
 static String label(Context c,String slot){try{if(slot.equals("apple-tv"))return config(c).getJSONObject("input").getString("name");String id=cameraId(c,slot);JSONObject camera=camera(c,id);if(camera!=null)return camera.getString("name");return new JSONObject(AdminServer.preferences(c).getString("favorite_camera_labels","{}")).optString(id,UiText.t("Kamera-Favorit"));}catch(Exception e){return UiText.t("Favorit");}}
 static String detail(Context c,String slot){try{if(slot.equals("apple-tv")){InputCatalog.Entry e=input(c,config(c).getJSONObject("input").getString("id"));return e==null?UiText.t("Eingang nicht verfügbar"):e.port+" · "+e.status();}JSONObject camera=camera(c,cameraId(c,slot));return camera==null?UiText.t("Kamera nicht verfügbar"):CameraLibrary.ready(camera)?UiText.t("Kamera öffnen"):UiText.t("Stream fehlt");}catch(Exception e){return UiText.t("Nicht verfügbar");}}
 static JSONObject view(Context c)throws Exception{
  JSONObject cfg=config(c);JSONArray inputs=InputCatalog.view(c),cameras=new JSONArray();Set<String> seen=new HashSet<>();
  for(JSONObject item:publicCameras(c)){cameras.put(item);seen.add(item.getString("id"));}
  JSONArray selected=cfg.getJSONArray("cameras");JSONObject labels=new JSONObject(AdminServer.preferences(c).getString("favorite_camera_labels","{}"));for(int n=0;n<selected.length();n++){String id=selected.getString(n);if(!seen.contains(id))cameras.put(new JSONObject().put("id",id).put("name",labels.optString(id,UiText.t("Kamera-Favorit"))).put("available",false).put("missing",true));}
  return new JSONObject().put("config",cfg).put("inputs",inputs).put("cameras",cameras).put("cameraLimit",FavoriteConfig.CAMERA_LIMIT);
 }
 private static List<JSONObject> publicCameras(Context c)throws Exception{List<JSONObject> result=new ArrayList<>();JSONArray all=CameraLibrary.all(c);for(int n=0;n<all.length();n++){JSONObject item=all.getJSONObject(n);result.add(new JSONObject().put("id",item.getString("id")).put("name",item.getString("name")).put("available",CameraLibrary.ready(item)).put("missing",false));}return result;}
 static synchronized JSONObject save(Context c,JSONObject raw)throws Exception{
  JSONObject cfg=FavoriteConfig.normalize(raw),old=config(c),input=cfg.getJSONObject("input"),previous=old.getJSONObject("input");String id=input.getString("id");if(!id.isEmpty()&&input(c,id)==null&&!id.equals(previous.getString("id")))throw new IOException("Input no longer present");
  JSONArray ids=cfg.getJSONArray("cameras"),prior=old.getJSONArray("cameras");JSONObject labels=new JSONObject(AdminServer.preferences(c).getString("favorite_camera_labels","{}"));for(int n=0;n<ids.length();n++){String cameraId=ids.getString(n);JSONObject entry=camera(c,cameraId);boolean retained=false;for(int j=0;j<prior.length();j++)retained|=cameraId.equals(prior.getString(j));if(entry==null&&!retained)throw new IOException("Camera no longer present");if(entry!=null)labels.put(cameraId,entry.getString("name"));}
  SharedPreferences p=AdminServer.preferences(c);if(!p.edit().putString("home_favorites",cfg.toString()).putString("favorite_camera_labels",labels.toString()).putLong("revision",Math.max(System.currentTimeMillis(),p.getLong("revision",0)+1)).commit())throw new IOException("Favorites not saved");return view(c);
 }
 static void open(Context c,String slot){try{
  if("apple-tv".equals(slot)){if(!inputEnabled(c))return;String id=config(c).getJSONObject("input").getString("id");InputCatalog.Entry e=input(c,id);if(e==null||e.state==TvInputManager.INPUT_STATE_DISCONNECTED)throw new IOException("Input unavailable");Intent intent=InputCatalog.intent(c,id);ResolveInfo target=c.getPackageManager().resolveActivity(intent,0);if(target==null||target.activityInfo==null||target.activityInfo.packageName.equals(c.getPackageName()))throw new IOException("Input unavailable");c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));AdminServer.preferences(c).edit().putString("last_input",id).commit();return;}
  if(!AdminServer.preferences(c).getBoolean("show_camera",true))return;String id=cameraId(c,slot);JSONObject selected=camera(c,id);if(selected==null||!CameraLibrary.ready(selected))throw new IOException("Camera unavailable");c.startActivity(new Intent(c,CameraActivity.class).putExtra("cameraId",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;
 }catch(Exception e){Toast.makeText(c,UiText.t(slot.equals("apple-tv")?"Eingang nicht verfügbar. Bitte Favorit prüfen.":"Kamera nicht verfügbar. Bitte Favorit prüfen."),Toast.LENGTH_LONG).show();}}
}
