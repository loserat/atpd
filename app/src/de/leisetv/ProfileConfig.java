package de.leisetv;

import org.json.*;
import java.io.IOException;
import java.util.*;

/** Strict, provider-free profile payload. Safe to validate without Android. */
final class ProfileConfig {
    static void fields(JSONObject value,String... names)throws Exception{
        if(value.length()!=names.length)throw new IOException("Invalid profile fields");
        for(String name:names)if(!value.has(name))throw new IOException("Missing profile field");
    }
    static boolean flag(JSONObject value,String key)throws Exception{Object item=value.get(key);if(!(item instanceof Boolean))throw new IOException("Invalid profile flag");return (Boolean)item;}
    static String choice(JSONObject value,String key,String... choices)throws Exception{Object item=value.get(key);if(!(item instanceof String)||!Arrays.asList(choices).contains(item))throw new IOException("Invalid profile choice");return (String)item;}
    static JSONObject normalize(JSONObject input)throws Exception{
        fields(input,"display","appearance","integrations","appShortcuts","interface","remote");
        JSONObject d=input.getJSONObject("display"),a=input.getJSONObject("appearance"),i=input.getJSONObject("integrations"),apps=input.getJSONObject("appShortcuts"),ui=input.getJSONObject("interface"),remote=input.getJSONObject("remote");
        fields(d,"camera","clock","date");fields(a,"theme","layout","order");fields(i,"hue","unifi");fields(apps,"ard","zdf");fields(ui,"language","textFont","clockFont");fields(remote,"red","green","yellow","blue");
        List<String> order=new ArrayList<>();JSONArray raw=a.getJSONArray("order");for(int n=0;n<raw.length();n++){Object id=raw.get(n);if(!(id instanceof String))throw new IOException("Invalid order");order.add((String)id);}if(!HomeLayout.validOrder(order))throw new IOException("Invalid order");
        JSONObject keys=new JSONObject();for(String key:new String[]{"red","green","yellow","blue"})keys.put(key,choice(remote,key,"cameras","ambilight","hue","inputs","network","system","apps","none"));
        return new JSONObject().put("display",new JSONObject().put("camera",flag(d,"camera")).put("clock",flag(d,"clock")).put("date",flag(d,"date")))
            .put("appearance",new JSONObject().put("theme",choice(a,"theme","nordlicht","mitternacht","abendrot","wald")).put("layout",choice(a,"layout","focus","grid")).put("order",new JSONArray(HomeLayout.normalizedOrder(order))))
            .put("integrations",new JSONObject().put("hue",flag(i,"hue")).put("unifi",flag(i,"unifi")))
            .put("appShortcuts",new JSONObject().put("ard",flag(apps,"ard")).put("zdf",flag(apps,"zdf")))
            .put("interface",new JSONObject().put("language",choice(ui,"language","de","en")).put("textFont",choice(ui,"textFont","sans","condensed","serif","mono")).put("clockFont",choice(ui,"clockFont","sans","condensed","serif","mono"))).put("remote",keys);
    }
    static JSONObject builtin(String id)throws Exception{
        if(!"standard".equals(id)&&!"media".equals(id))throw new IOException("Unknown profile");boolean media="media".equals(id);
        return normalize(new JSONObject().put("display",new JSONObject().put("camera",!media).put("clock",true).put("date",true))
            .put("appearance",new JSONObject().put("theme","nordlicht").put("layout","focus").put("order",new JSONArray(media?Arrays.asList("ambilight","ard","zdf","cameras","hue","tv","inputs","system"):Arrays.asList(HomeLayout.DEFAULT_ORDER))))
            .put("integrations",new JSONObject().put("hue",!media).put("unifi",!media)).put("appShortcuts",new JSONObject().put("ard",media).put("zdf",media))
            .put("interface",new JSONObject().put("language","de").put("textFont","sans").put("clockFont","sans"))
            .put("remote",new JSONObject().put("red","cameras").put("green","ambilight").put("yellow","hue").put("blue","inputs")));
    }
    static boolean equal(JSONObject a,JSONObject b)throws Exception{return normalize(a).toString().equals(normalize(b).toString());}
    static String name(Object input)throws Exception{
        if(!(input instanceof String))throw new IOException("Invalid profile name");String value=((String)input).trim();
        if(value.isEmpty()||value.length()>40||value.chars().anyMatch(Character::isISOControl))throw new IOException("Invalid profile name");return value;
    }
}
