package game.sanguo.mobile;

/** Immutable, display-only art constants. Months follow the existing three-month quarters.
 * No weather simulation, geographic snow assumption or authoritative state is stored here. */
final class SeasonStyle {
    static final String ID="ink-landscape-r14-v1";
    static final SeasonStyle SPRING=new SeasonStyle("spring",.90f,1.04f,.85f,1f,.98f,.94f,.95f,1.02f,.98f,47000,17000);
    static final SeasonStyle SUMMER=new SeasonStyle("summer",.82f,.98f,.76f,.98f,1f,.98f,.88f,1.02f,1.04f,50000,18000);
    static final SeasonStyle AUTUMN=new SeasonStyle("autumn",1.25f,.83f,.52f,1.02f,.96f,.88f,.98f,.97f,.93f,45000,17000);
    static final SeasonStyle WINTER=new SeasonStyle("winter",.85f,.88f,.78f,.93f,.98f,1.03f,.87f,.98f,1.08f,42000,18000);
    final String name;
    final float plantR,plantG,plantB,artR,artG,artB,waterR,waterG,waterB,sunLux,skyLux;
    private SeasonStyle(String name,float pr,float pg,float pb,float ar,float ag,float ab,float wr,float wg,float wb,float sun,float sky){
        this.name=name;plantR=pr;plantG=pg;plantB=pb;artR=ar;artG=ag;artB=ab;waterR=wr;waterG=wg;waterB=wb;sunLux=sun;skyLux=sky;
        for(float v:new float[]{pr,pg,pb,ar,ag,ab,wr,wg,wb})if(!Float.isFinite(v)||v<.4f||v>1.3f)throw new IllegalArgumentException("art palette");
        if(sun<35000||sun>55000||sky<14000||sky>22000)throw new IllegalArgumentException("light budget");
    }
    static int month(int startMonth,int turn){
        if(startMonth<1||startMonth>12||turn<0)throw new IllegalArgumentException("calendar projection");
        return (startMonth-1+(turn/3)%12)%12+1;
    }
    static SeasonStyle fromDate(int startMonth,int turn){return forMonth(month(startMonth,turn));}
    static SeasonStyle forMonth(int month){
        if(month<1||month>12)throw new IllegalArgumentException("month");
        return month<=3?SPRING:month<=6?SUMMER:month<=9?AUTUMN:WINTER;
    }
}
