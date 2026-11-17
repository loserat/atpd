package de.leisetv;
import java.util.*;
public final class HomeLayoutChecks {
    private static int configurations;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void verify(List<String> order){
        for(boolean camera:new boolean[]{true,false})for(boolean hue:new boolean[]{true,false})for(boolean ard:new boolean[]{true,false})for(boolean zdf:new boolean[]{true,false})for(String mode:new String[]{"focus","grid"}){
            List<HomeLayout.Slot> slots=HomeLayout.positions(order,camera,hue,ard,zdf,mode);List<String> expected=HomeLayout.normalizedOrder(order);expected.removeAll(Arrays.asList("apple-tv","camera1","camera2","camera3"));expected.remove("app3");expected.remove("app4");if(!camera)expected.remove("cameras");if(!hue)expected.remove("hue");if(!ard)expected.remove("ard");if(!zdf)expected.remove("zdf");
            require(slots.size()==expected.size(),"Visible tile count");
            for(int i=0;i<slots.size();i++){
                HomeLayout.Slot slot=slots.get(i);require(slot.id.equals(expected.get(i)),"Stored order is respected");
                require(slot.x>=96&&slot.y>=300&&slot.x+slot.w<=1824&&slot.y+slot.h<=856,"Tile inside TV content region");
                for(int j=i+1;j<slots.size();j++){HomeLayout.Slot other=slots.get(j);require(slot.x+slot.w<=other.x||other.x+other.w<=slot.x||slot.y+slot.h<=other.y||other.y+other.h<=slot.y,"No overlapping tiles");}
            }
            Set<Integer> reached=new HashSet<>();ArrayDeque<Integer> todo=new ArrayDeque<>();todo.add(0);
            while(!todo.isEmpty()){int at=todo.remove();if(!reached.add(at))continue;for(int direction=0;direction<4;direction++){int next=HomeLayout.neighbor(slots,at,direction);require(next>=0&&next<slots.size(),"Every neighbor exists");todo.add(next);}}
            require(reached.size()==slots.size(),"D-pad reaches every visible destination");configurations++;
        }
    }
    private static void permutations(List<String> order,int start){if(start==order.size()){verify(order);return;}for(int i=start;i<order.size();i++){Collections.swap(order,i,start);permutations(order,start+1);Collections.swap(order,i,start);}}
    public static void main(String[] args){
        List<String> defaults=Arrays.asList(HomeLayout.DEFAULT_ORDER);
        require(HomeLayout.validOrder(defaults),"Default order valid");
        require(defaults.equals(Arrays.asList("ambilight","cameras","hue","tv","inputs","system","ard","zdf","app3","app4","apple-tv","camera1","camera2","camera3")),"Primary-TV standard with optional media positions");
        require(!HomeLayout.validOrder(Arrays.asList("cameras","cameras","hue","tv","inputs","system")),"Duplicate rejected");
        require(!HomeLayout.validOrder(Arrays.asList("unknown","ambilight","hue","tv","inputs","system")),"Unknown rejected");
        require(!HomeLayout.validOrder(defaults.subList(0,5)),"Missing destination rejected");
        List<HomeLayout.Slot> standard=HomeLayout.positions(defaults,true,"focus");require(standard.get(0).x==96&&standard.get(0).w==560&&standard.get(0).h==392&&standard.get(3).y==724&&standard.get(3).h==132,"Original layout preserved");
        List<HomeLayout.Slot> hidden=HomeLayout.positions(defaults,false,"focus");require(hidden.get(0).w==852&&hidden.get(2).id.equals("tv")&&hidden.get(2).h==132,"Hidden camera leaves two large and three small tiles");
        List<String> moved=new ArrayList<>(defaults);Collections.swap(moved,moved.indexOf("cameras"),5);require(HomeLayout.positions(moved,true,"focus").get(5).id.equals("cameras")&&HomeLayout.positions(moved,true,"focus").get(5).h==132,"Camera can move to small row");
        boolean invalid=false;try{HomeLayout.positions(defaults,true,"unknown");}catch(IllegalArgumentException e){invalid=true;}require(invalid,"Unknown geometry rejected");
        List<String> legacy=new ArrayList<>(Arrays.asList(HomeLayout.LEGACY_ORDER));Collections.reverse(legacy);require(HomeLayout.validOrder(legacy)&&HomeLayout.normalizedOrder(legacy).subList(0,6).equals(legacy),"Legacy custom order is preserved before appending shortcuts");
        permutations(new ArrayList<>(Arrays.asList(HomeLayout.LEGACY_EIGHT_ORDER)),0);require(configurations==1290240,"All eight-item permutations, visibility states and modes tested");
        int expanded=0;Random random=new Random(130);
        for(int n=0;n<4096;n++){List<String> order=new ArrayList<>(Arrays.asList(HomeLayout.LEGACY_TEN_ORDER));if(n>0)Collections.shuffle(order,random);for(boolean camera:new boolean[]{false,true})for(boolean hue:new boolean[]{false,true})for(int count=0;count<=4;count++)for(String mode:new String[]{"focus","grid"}){
            List<HomeLayout.Slot> slots=HomeLayout.positions(order,camera,hue,count,mode);Set<Integer> reached=new HashSet<>();ArrayDeque<Integer> todo=new ArrayDeque<>();todo.add(0);for(int i=0;i<slots.size();i++){HomeLayout.Slot a=slots.get(i);require(a.x>=96&&a.y>=300&&a.x+a.w<=1824&&a.y+a.h<=856,"Expanded TV-safe bounds");for(int j=i+1;j<slots.size();j++){HomeLayout.Slot b=slots.get(j);require(a.x+a.w<=b.x||b.x+b.w<=a.x||a.y+a.h<=b.y||b.y+b.h<=a.y,"Expanded no overlap");}}while(!todo.isEmpty()){int at=todo.remove();if(!reached.add(at))continue;for(int d=0;d<4;d++)todo.add(HomeLayout.neighbor(slots,at,d));}require(reached.size()==slots.size(),"Expanded D-pad reachability");expanded++;
        }}require(expanded==163840,"Expanded seeded configurations");
        int favoriteCases=0;for(int n=0;n<512;n++){List<String> order=new ArrayList<>(defaults);if(n>0)Collections.shuffle(order,random);for(boolean camera:new boolean[]{false,true})for(boolean hue:new boolean[]{false,true})for(int apps=0;apps<=4;apps++)for(int cameras=0;cameras<=3;cameras++)for(boolean input:new boolean[]{false,true})for(String mode:new String[]{"focus","grid"}){
            List<HomeLayout.Slot> slots=HomeLayout.positions(order,camera,hue,apps,cameras,input,mode);require(slots.size()==4+(camera?1:0)+(hue?1:0)+apps+(camera?cameras:0)+(input?1:0),"Favorite visibility count");Set<Integer> reached=new HashSet<>();ArrayDeque<Integer> todo=new ArrayDeque<>();todo.add(0);
            for(int i=0;i<slots.size();i++){HomeLayout.Slot a=slots.get(i);require(a.x>=96&&a.y>=300&&a.x+a.w<=1824&&a.y+a.h<=856,"Favorite TV-safe bounds");require(a.w>=326,"Favorite cards remain readable");for(int j=i+1;j<slots.size();j++){HomeLayout.Slot b=slots.get(j);require(a.x+a.w<=b.x||b.x+b.w<=a.x||a.y+a.h<=b.y||b.y+b.h<=a.y,"Favorite no overlap");}for(int d=2;d<4;d++){int next=HomeLayout.neighbor(slots,i,d);require(next==i||Math.abs(slots.get(next).row-a.row)==1,"Vertical navigation never skips a row");}}
            while(!todo.isEmpty()){int at=todo.remove();if(!reached.add(at))continue;for(int d=0;d<4;d++)todo.add(HomeLayout.neighbor(slots,at,d));}require(reached.size()==slots.size(),"Favorite D-pad reachability");favoriteCases++;
        }}require(favoriteCases==163840,"Seeded fourteen-slot favorites cases");
        System.out.println("Home layout checks passed: 1290240 retained eight-slot configurations + 163840 seeded ten-slot + 163840 seeded fourteen-slot configurations; bounds, overlap and D-pad verified.");
    }
}
