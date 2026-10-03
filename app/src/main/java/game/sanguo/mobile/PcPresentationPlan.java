package game.sanguo.mobile;

import game.sanguo.core.TurnJournal;
import game.sanguo.core.War;
import java.util.*;

/** Presentation decisions use immutable, already applied rule facts only. */
final class PcPresentationPlan {
    static final int CUE_MILLIS=750; // Default within the user-approved 500–1000ms range; not measured PC timing.
    static final class Cue {
        final int selector,template;
        Cue(int selector,int template){this.selector=selector;this.template=template;}
    }
    static final int[] TEMPLATES={115,121,122},SELECTORS={126,127,131,132,133,134,136,143,151,152,153,154,156,163};
    private static final Cue SORCERY=new Cue(126,121),LIGHTNING=new Cue(127,122);
    // Installed sixteen scenarios -> unchanged48b760 serializer -> FCE ->
    // original48a5b0 lookup. Age thresholds verified by original4a66c0/488a20.
    // Each row: canonical project ID, birth year, transition age, young, old.
    private static final int[][] PORTRAITS={{1000,161,48,136,156},{1001,162,47,132,152},
        {1002,167,42,143,163},{1003,168,58,131,151},{1004,181,43,134,154},{2000,155,54,133,153}};
    private static final String[] NAMES={"刘备","关羽","张飞","赵云","诸葛亮","曹操"};
    private static final Cue[][] PORTRAIT_CUES=new Cue[PORTRAITS.length][2];
    static {for(int i=0;i<PORTRAITS.length;i++){PORTRAIT_CUES[i][0]=new Cue(PORTRAITS[i][3],115);PORTRAIT_CUES[i][1]=new Cue(PORTRAITS[i][4],115);}}
    private static Cue portrait(int id,String name,int year){
        for(int i=0;i<PORTRAITS.length;i++)if(PORTRAITS[i][0]==id&&NAMES[i].equals(name))
            return PORTRAIT_CUES[i][year-PORTRAITS[i][1]+1>=PORTRAITS[i][2]?1:0];
        return null;
    }
    static List<Cue> cues(TurnJournal.Event event){
        if(event==null)return Collections.emptyList();
        List<Cue> out=new ArrayList<>();
        if(event.critical!=null){Cue actor=portrait(event.critical.officerId,event.critical.name,event.critical.year);if(actor!=null)out.add(actor);}
        for(TurnJournal.PlotOutcome result:event.plotOutcomes)if(result.success&&result.critical){
            // Original5933a0 dispatch + the original8aecb4 name table:
            // plot7 (妖術) calls592050 -> selector126; plot8 (落雷) calls
            // 592ed0 -> selector127. Ordinary扰乱 must not borrow妖術 art.
            if(result.plot==War.Plot.SORCERY)out.add(SORCERY);
            else if(result.plot==War.Plot.LIGHTNING)out.add(LIGHTNING);
        }
        return Collections.unmodifiableList(out);
    }
    static int duration(TurnJournal.Event event){return cues(event).size()*CUE_MILLIS;}
    private PcPresentationPlan(){}
}
