package game.sanguo.mobile;
import game.sanguo.core.*;
public final class PaletteAudit {
 public static void main(String[]args)throws Exception {
  int index=0;for(World w:ScenarioCatalog.all())report("engineering-"+(index++),w);
  for(PcScenarioCatalog.Source s:PcScenarioCatalog.all()){World w=PcScenarioCatalog.preview(s.identity.scenarioId);report(s.identity.path,w);}
 }
 static void report(String source,World w){int[] colors=FactionColors.build(w);int zero=0,liveZero=0;String names="";for(int i=0;i<colors.length;i++)if(colors[i]==0){zero++;if(w.alive(i)){liveZero++;names+=w.faction(i)+"|";}}System.out.println(source+" slots="+colors.length+" zero="+zero+" activeZero="+liveZero+" names="+names);}
}