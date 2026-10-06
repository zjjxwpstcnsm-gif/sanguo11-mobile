package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class PaletteRegression {
 static int checked;
 static void require(boolean ok,String message){checked++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  int i=0;for(World w:ScenarioCatalog.all())report("engineering-"+(i++),w);
  for(PcScenarioCatalog.Source s:PcScenarioCatalog.all())report(s.identity.path,PcScenarioCatalog.preview(s.identity.scenarioId));
  for(int count:new int[]{2,33,47,128,1024,4096}){
   String[] names=new String[count];for(int n=0;n<count;n++)names[n]="faction-"+n;
   int[] colors=FactionColors.build(names);require(new HashSet<Integer>(){{for(int c:colors)add(c);}}.size()==count,"unique "+count);
   for(int c:colors){require((c>>>24)==255,"opaque "+count);require(FactionColors.contrast(FactionColors.textColor(c),FactionColors.LABEL_BACKGROUND)>=4.5,"contrast "+count);}
   require(Arrays.equals(colors,FactionColors.build(names)),"deterministic "+count);
  }
  for(int c:new int[]{0,0xff000000,0x123456,0xff00ff00,0xff0000ff,0xffff0000,0xffffffff}){
   int ink=FactionColors.textColor(c);require((ink>>>24)==255,"ink alpha");require(FactionColors.contrast(ink,FactionColors.LABEL_BACKGROUND)>=4.5,"ink contrast");
  }
  System.out.println("PASS PALETTE checks="+checked+" sources=16 engineering=9; host evidence, Android pixels/install pending");
 }
 static void report(String source,World w){
  int[] colors=FactionColors.build(w);Set<Integer> seen=new HashSet<>();int active=0;
  for(int n=0;n<colors.length;n++){
   int c=colors[n];require(c!=0&&(c>>>24)==255,source+" opaque slot "+n);require(seen.add(c),source+" unique slot "+n);
   require(FactionColors.contrast(FactionColors.textColor(c),FactionColors.LABEL_BACKGROUND)>=4.5,source+" readable slot "+n);
   if(w.alive(n))active++;
  }
  w.player=(w.player+1)%w.factions.length;require(Arrays.equals(colors,FactionColors.build(w)),source+" selection stable");
  System.out.println(source+" slots="+colors.length+" active="+active+" zero=0 alpha=255 minContrast>=4.5");
 }
}
