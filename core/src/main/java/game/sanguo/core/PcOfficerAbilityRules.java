package game.sanguo.core;

/** Original numeric ability calculation, consumed by saved OfficerAbilities state. */
final class PcOfficerAbilityRules {
    private PcOfficerAbilityRules() {}

    /** Original 360-day calendar and source fixed-age flag. Elapsed turns are
     * ten days; the fixed branch uses the scenario's start year indefinitely.
     * Retains native unsigned division and signed32 field arithmetic. */
    static int age(int startYear,int startMonth,int startDay,int elapsedTurns,int birth,boolean fixedAge) {
        int year=startYear;
        if(!fixedAge) {
            int days=(((startYear-1)*12+startMonth-1)*3+elapsedTurns)*10+startDay-1;
            year=Integer.divideUnsigned(days,360)+1;
        }
        return year-birth+1;
    }

    /** The original startup forces ability-change off for fixed-age scenarios.
     * This setting bypasses age curves only, not experience/rank/spouse/injury. */
    static boolean growthDisabled(boolean requestedDisabled,boolean fixedAge) {
        return requestedDisabled||fixedAge;
    }

    /** Original signed32 arithmetic, including overflow before integer division.
     * Numeric curve identities come from native source records; no guessed labels. */
    static int growthPercent(int curve,int age) {
        switch(curve) {
            case 0:return age<=25?(age+175)/2:100;
            case 1:return age<=25?(age+175)/2:age<50?100:(1050-age)/10;
            case 2:return age<=18?age+82:age<35?100:(485-age)*2/9;
            case 3:return age<=18?age+82:age<40?100:(840-age)/8;
            case 4:return age<=30?(age+170)/2:age<45?100:(395-age)*2/7;
            case 5:return age<=30?(age+170)/2:age<50?100:(650-age)/6;
            case 6:return age<=40?(age+160)/2:100;
            case 7:return age<=50?(age*3+650)/8:100;
            case 8:return age<=25?(age+175)/2:age<40?100:age<55?age*2+20:130;
            default:return 100;
        }
    }

    /** Inputs describe original resolved references, not project IDs or UI data.
     * ignoreModifiers corresponds only to original special actors700..799;
     * growthDisabled bypasses growth alone. Rank and spouse bonuses apply after
     * injury, then the final value is clamped again. */
    static int current(int base,int curve,int age,int experience,int stat,int injury,
                       int rankStat,int rankBonus,boolean spouseBonus,
                       boolean ignoreModifiers,boolean growthDisabled) {
        if(base<0||base>255||experience<0||experience>65535||stat<0||stat>4||rankBonus<0||rankBonus>255)
            throw new IllegalArgumentException("Native ability field domain required");
        if(ignoreModifiers)return clamp(base);
        int value=clamp((growthDisabled?base:base*growthPercent(curve,age)/100)+experience/100);
        if(stat<4&&injury>=1&&injury<=3) {
            int percent=injury==1?80:injury==2?50:30;
            value=clamp(value*percent/100);
        }
        if(rankStat==stat)value+=rankBonus;
        if(spouseBonus)value++;
        return clamp(value);
    }
    private static int clamp(int value){return Math.max(1,Math.min(100,value));}
}
