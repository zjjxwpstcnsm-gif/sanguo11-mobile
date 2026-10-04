package game.sanguo.core;

/** Original4a75a0 raw loyalty. UI loyalty is a separate native getter.
 * The native sibling test requires the same valid father ID, not all ancestors. */
final class PcOfficerJoinRules {
    static int loyalty(int current,int gap,int nativeHonor,int nativeAmbition,int rulerCharm,
                       boolean weighted,boolean ruler,boolean spouse,boolean sworn,
                       boolean liked,boolean disliked,boolean sameFather,
                       boolean sameHan,boolean sameBirthplace){
        if(current<0||current>255||gap<0||gap>75||nativeHonor<0||nativeHonor>4||nativeAmbition<0||nativeAmbition>4||rulerCharm<0||rulerCharm>255)
            throw new IllegalArgumentException("Original loyalty inputs outside verified range");
        if(ruler)return Math.max(current,250);
        if(spouse||sworn)return Math.max(current,150);
        if(liked)return Math.max(current,120);
        gap=Math.min(gap,50);
        int denominator=20+nativeHonor;
        if(weighted){denominator+=nativeHonor;gap=gap*(20+nativeAmbition)/20;}
        int value=100+rulerCharm/14-gap*10/denominator;
        if(sameHan)value+=5;
        if(disliked)value-=20;else if(sameFather)value+=5;
        if(sameBirthplace)value+=5;
        return Math.min(value,100);
    }
    private PcOfficerJoinRules(){}
}
