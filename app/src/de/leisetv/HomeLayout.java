package de.leisetv;

import java.util.*;

/** Pure geometry for fourteen stored Home destinations, with deterministic D-pad neighbors. */
final class HomeLayout {
    static final String[] LEGACY_ORDER={"cameras","ambilight","hue","tv","inputs","system"};
    // Shared project baseline: the primary TV layout. Saved orders remain authoritative.
    static final String[] LEGACY_EIGHT_ORDER={"ambilight","cameras","hue","tv","inputs","system","ard","zdf"};
    static final String[] LEGACY_TEN_ORDER={"ambilight","cameras","hue","tv","inputs","system","ard","zdf","app3","app4"};
    static final String[] DEFAULT_ORDER={"ambilight","cameras","hue","tv","inputs","system","ard","zdf","app3","app4","apple-tv","camera1","camera2","camera3"};
    static boolean validOrder(List<String> order){for(String[] allowed:new String[][]{DEFAULT_ORDER,LEGACY_TEN_ORDER,LEGACY_EIGHT_ORDER,LEGACY_ORDER})if(order.size()==allowed.length&&new HashSet<>(order).equals(new HashSet<>(Arrays.asList(allowed))))return true;return false;}
    static List<String> normalizedOrder(List<String> order){if(!validOrder(order))throw new IllegalArgumentException("Invalid Home order");List<String> result=new ArrayList<>(order);for(String id:DEFAULT_ORDER)if(!result.contains(id))result.add(id);return result;}
    static final class Slot {
        final String id;final int x,y,w,h,row;
        Slot(String id,int x,int y,int w,int h,int row){this.id=id;this.x=x;this.y=y;this.w=w;this.h=h;this.row=row;}
        int center(){return x+w/2;}
    }
    static List<Slot> positions(List<String> order,boolean camera,String layout){
        return positions(order,camera,true,layout);
    }
    static List<Slot> positions(List<String> order,boolean camera,boolean hue,String layout){
        return positions(order,camera,hue,false,false,layout);
    }
    static List<Slot> positions(List<String> order,boolean camera,boolean hue,boolean ard,boolean zdf,String layout){
        List<String> apps=new ArrayList<>();if(ard)apps.add("ard");if(zdf)apps.add("zdf");return positions(order,camera,hue,apps,0,false,layout);
    }
    static List<Slot> positions(List<String> order,boolean camera,boolean hue,int appCount,String layout){if(appCount<0||appCount>4)throw new IllegalArgumentException("Invalid app count");return positions(order,camera,hue,Arrays.asList("ard","zdf","app3","app4").subList(0,appCount),0,false,layout);}
    static List<Slot> positions(List<String> order,boolean camera,boolean hue,int appCount,int cameraCount,boolean input,String layout){if(appCount<0||appCount>4||cameraCount<0||cameraCount>3)throw new IllegalArgumentException("Invalid favorites count");return positions(order,camera,hue,Arrays.asList("ard","zdf","app3","app4").subList(0,appCount),cameraCount,input,layout);}
    private static List<Slot> positions(List<String> order,boolean camera,boolean hue,List<String> apps,int cameraCount,boolean input,String layout){
        if(!validOrder(order)||!("focus".equals(layout)||"grid".equals(layout)))throw new IllegalArgumentException("Invalid Home layout");
        List<String> visible=normalizedOrder(order);if(!camera)visible.remove("cameras");if(!hue)visible.remove("hue");for(String id:Arrays.asList("ard","zdf","app3","app4"))if(!apps.contains(id))visible.remove(id);
        if(!input)visible.remove("apple-tv");for(int i=0;i<3;i++)if(!camera||i>=cameraCount)visible.remove("camera"+(i+1));
        int top=visible.size()>6?(visible.size()+1)/2:visible.size()==4?2:"focus".equals(layout)?visible.size()-3:3;List<Slot> slots=new ArrayList<>();
        for(int i=0;i<visible.size();i++){
            int row=i<top?0:1,count=row==0?top:visible.size()-top,col=row==0?i:i-top;
            if(visible.size()>10){int first=(visible.size()+2)/3,second=(visible.size()-first+1)/2;row=i<first?0:i<first+second?1:2;count=row==0?first:row==1?second:visible.size()-first-second;col=i-(row==0?0:row==1?first:first+second);}
            int w=(1728-(count-1)*24)/count,y=row==0?300:"focus".equals(layout)?724:584;
            int h="grid".equals(layout)?260:row==0?392:132;
            if(visible.size()>10){y=300+row*196;h=164;}
            slots.add(new Slot(visible.get(i),96+col*(w+24),y,w,h,row));
        }
        return slots;
    }
    // 0 left, 1 right, 2 up, 3 down. Edges stay put; Main connects the lower row to its network badge.
    static int neighbor(List<Slot> slots,int index,int direction){
        Slot from=slots.get(index);int best=index,distance=Integer.MAX_VALUE;
        for(int i=0;i<slots.size();i++){
            Slot to=slots.get(i);boolean valid=direction==0?to.row==from.row&&to.x<from.x:direction==1?to.row==from.row&&to.x>from.x:direction==2?to.row==from.row-1:direction==3&&to.row==from.row+1;
            if(valid){int d=Math.abs(to.center()-from.center());if(d<distance){best=i;distance=d;}}
        }
        return best;
    }
}
