package game.sanguo.core;

/** Original51dd10 numeric rewards only. Recruitment/diplomacy callback and
 * injury recovery are separate original policies, not inferred here. */
final class PcDebateSettlement {
    static final class Result {
        final int[] merit,intelligenceExperience,injury;
        final int[] techniquePoints;
        Result(int[] merit,int[] xp,int[] injury,int[] points){this.merit=merit;intelligenceExperience=xp;this.injury=injury;techniquePoints=points;}
    }
    static Result calculate(int winner,int outcome,int[] merit,int[] xp,int[] injury,int[] owner,int[] points,boolean[] guided,boolean[] eligible){
        if(winner<0||winner>1||outcome<0||outcome>3||merit.length!=2||xp.length!=2||injury.length!=2||owner.length!=2||guided.length!=2||eligible.length!=2)throw new IllegalArgumentException("Original settlement inputs required");
        int[] nextMerit=merit.clone(),nextXp=xp.clone(),nextInjury=injury.clone(),nextPoints=points.clone();
        for(int i=0;i<2;i++)if(merit[i]<0||merit[i]>60000||xp[i]<0||xp[i]>3000||injury[i]<0||injury[i]>3||owner[i]<-1||owner[i]>=points.length)throw new IllegalArgumentException("Original settlement values outside verified range");
        for(int p:points)if(p<0||p>10000)throw new IllegalArgumentException("Original force technique points invalid");
        int loser=1-winner;boolean terminalChoice=outcome==1||outcome==2;
        int winnerXp=outcome==1?30:10;
        if(eligible[winner]){nextMerit[winner]=Math.min(60000,merit[winner]+(terminalChoice?200:100));nextXp[winner]=Math.min(3000,xp[winner]+winnerXp*(guided[winner]?2:1));}
        if(eligible[loser]){nextMerit[loser]=Math.min(60000,merit[loser]+10);nextXp[loser]=Math.min(3000,xp[loser]+(guided[loser]?2:1));if(terminalChoice)nextInjury[loser]=Math.min(3,injury[loser]+1);}
        if(outcome==2&&eligible[winner]&&owner[winner]>=0)nextPoints[owner[winner]]=PcTechniquePoints.after(points[owner[winner]],50);
        return new Result(nextMerit,nextXp,nextInjury,nextPoints);
    }
    private PcDebateSettlement(){}
}
