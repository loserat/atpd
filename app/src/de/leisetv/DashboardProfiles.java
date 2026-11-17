package de.leisetv;

import android.content.*;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Per-TV snapshots; never stores TV identity, provider setup, light state or standby automation. */
final class DashboardProfiles {
    private static final String STORE="dashboard_profiles_v1";
    static final int LIMIT=8;
    private static JSONObject capture(Context c)throws Exception{
        SharedPreferences p=AdminServer.preferences(c);
        JSONObject apps=AppShortcuts.config(c);
        JSONObject raw=new JSONObject().put("display",new JSONObject().put("camera",p.getBoolean("show_camera",true)).put("clock",p.getBoolean("show_clock",true)).put("date",p.getBoolean("show_date",true)))
            .put("favorites",HomeFavorites.config(c)).put("appearance",HomeAppearance.view(c)).put("integrations",IntegrationSettings.view(c)).put("appShortcuts",apps).put("interface",UiSettings.view(c)).put("remote",RemoteShortcuts.view(c));if(p.contains("profile_type"))raw.put("type",p.getString("profile_type","standard"));return ProfileConfig.normalize(raw);
    }
    private static JSONArray custom(Context c)throws Exception{
        JSONArray entries=new JSONArray(AdminServer.preferences(c).getString(STORE,"[]"));if(entries.length()>LIMIT)throw new IOException("Too many profiles");Set<String> ids=new HashSet<>();
        for(int n=0;n<entries.length();n++){JSONObject entry=entries.getJSONObject(n);ProfileConfig.fields(entry,"id","name","config");String id=entry.getString("id");if(!id.matches("custom-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")||!ids.add(id))throw new IOException("Invalid saved profile");ProfileConfig.name(entry.get("name"));ProfileConfig.normalize(entry.getJSONObject("config"));}return entries;
    }
    private static void persist(Context c,JSONArray entries)throws Exception{if(!AdminServer.preferences(c).edit().putString(STORE,entries.toString()).commit())throw new IOException("Profile not saved");}
    private static int index(JSONArray entries,String id)throws Exception{for(int n=0;n<entries.length();n++)if(id.equals(entries.getJSONObject(n).getString("id")))return n;throw new IOException("Unknown profile");}
    private static void unique(JSONArray entries,String name,String except)throws Exception{
        if(name.equalsIgnoreCase("Standard")||name.equalsIgnoreCase("Mediathek"))throw new IOException("Reserved profile name");
        for(int n=0;n<entries.length();n++){JSONObject entry=entries.getJSONObject(n);if(!entry.getString("id").equals(except)&&name.equalsIgnoreCase(entry.getString("name")))throw new IOException("Duplicate profile name");}
    }
    private static JSONObject normalized(Context c,JSONObject raw)throws Exception{JSONObject copy=new JSONObject(raw.toString());if(!copy.has("favorites"))copy.put("favorites",HomeFavorites.config(c));return ProfileConfig.normalize(copy);}
    private static JSONObject builtin(Context c,String id)throws Exception{JSONObject result=ProfileConfig.builtin(id);result.put("favorites",HomeFavorites.config(c));result.getJSONObject("appShortcuts").put("apps",AppShortcuts.config(c).getJSONArray("apps")).put("enabled","media".equals(id));return ProfileConfig.normalize(result);}
    static synchronized JSONObject view(Context c)throws Exception{
        JSONArray entries=new JSONArray();JSONObject current=capture(c);String active="";
        for(String id:new String[]{"standard","media"}){JSONObject config=builtin(c,id);entries.put(new JSONObject().put("id",id).put("name","standard".equals(id)?"Standard":"Mediathek").put("builtin",true).put("config",config));if(active.isEmpty()&&ProfileConfig.equal(config,current))active=id;}
        JSONArray custom=custom(c);for(int n=0;n<custom.length();n++){JSONObject entry=new JSONObject(custom.getJSONObject(n).toString());entry.put("config",normalized(c,entry.getJSONObject("config")));entry.put("builtin",false);entries.put(entry);if(active.isEmpty()&&ProfileConfig.equal(entry.getJSONObject("config"),current))active=entry.getString("id");}
        String selected=AdminServer.preferences(c).getString("dashboard_profile_id","");boolean selectedExists=false;for(int n=0;n<entries.length();n++){JSONObject entry=entries.getJSONObject(n);if(entry.getString("id").equals(selected)){selectedExists=true;active=ProfileConfig.equal(entry.getJSONObject("config"),current)?selected:"";}}
        if(!selectedExists)selected=active.isEmpty()?current.getString("type"):active;
        return new JSONObject().put("profiles",entries).put("activeId",active).put("selectedId",selected).put("modified",!selected.equals(active)).put("current",current).put("limit",LIMIT);
    }
    static synchronized JSONObject save(Context c,JSONObject input)throws Exception{
        boolean replace=input.has("id");if(input.has("type"))ProfileConfig.fields(input,replace?new String[]{"id","name","type"}:new String[]{"name","type"});else ProfileConfig.fields(input,replace?new String[]{"id","name"}:new String[]{"name"});String name=ProfileConfig.name(input.get("name"));JSONArray entries=custom(c);String id=replace?input.getString("id"):"custom-"+UUID.randomUUID();int at=replace?index(entries,id):entries.length();
        if(!replace&&entries.length()>=LIMIT)throw new IOException("Profile limit reached");unique(entries,name,id);
        JSONObject config=capture(c);if(input.has("type"))config.put("type",ProfileConfig.choice(input,"type","standard","media"));entries.put(at,new JSONObject().put("id",id).put("name",name).put("config",config));persist(c,entries);return view(c);
    }
    static synchronized JSONObject rename(Context c,JSONObject input)throws Exception{
        ProfileConfig.fields(input,"id","name");String id=input.getString("id"),name=ProfileConfig.name(input.get("name"));JSONArray entries=custom(c);int at=index(entries,id);unique(entries,name,id);entries.getJSONObject(at).put("name",name);persist(c,entries);return view(c);
    }
    static synchronized JSONObject delete(Context c,JSONObject input)throws Exception{
        ProfileConfig.fields(input,"id");JSONArray entries=custom(c);int at=index(entries,input.getString("id"));entries.remove(at);persist(c,entries);return view(c);
    }
    static synchronized JSONObject apply(Context c,JSONObject input)throws Exception{
        ProfileConfig.fields(input,"id");String id=input.getString("id");JSONObject config;
        if("standard".equals(id)||"media".equals(id))config=builtin(c,id);else{JSONArray entries=custom(c);config=entries.getJSONObject(index(entries,id)).getJSONObject("config");}
        config=normalized(c,config);JSONObject d=config.getJSONObject("display"),a=config.getJSONObject("appearance"),i=config.getJSONObject("integrations"),apps=config.getJSONObject("appShortcuts"),ui=config.getJSONObject("interface"),keys=config.getJSONObject("remote");SharedPreferences p=AdminServer.preferences(c);
        SharedPreferences.Editor edit=p.edit().putString("home_favorites",config.getJSONObject("favorites").toString());for(String key:new String[]{"camera","clock","date"})edit.putBoolean("show_"+key,d.getBoolean(key));for(String key:new String[]{"hue","unifi"})edit.putBoolean("integration_"+key,i.getBoolean(key));edit.putBoolean("show_media_apps",apps.getBoolean("enabled")).putString("favorite_apps",apps.getJSONArray("apps").toString()).putString("profile_type",config.getString("type")).putString("dashboard_profile_id",id);
        edit.putString("home_theme",a.getString("theme")).putString("home_layout",a.getString("layout")).putString("home_order",a.getJSONArray("order").toString()).putString("ui_language",ui.getString("language")).putString("ui_text_font",ui.getString("textFont")).putString("ui_clock_font",ui.getString("clockFont"));for(String key:new String[]{"red","green","yellow","blue"})edit.putString("remote_"+key,keys.getString(key));
        if(!edit.putLong("revision",Math.max(System.currentTimeMillis(),p.getLong("revision",0)+1)).commit())throw new IOException("Profile not applied");return view(c);
    }
}
