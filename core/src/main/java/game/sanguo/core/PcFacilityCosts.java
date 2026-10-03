package game.sanguo.core;

/** Generated from the installed Scenario.s11 by export_pc_facility_costs.py.
 * Native construction5bc462..5bc49b debits actor+c4; normal menu600e15 has
 * base facilities only. These are base costs, not battlefield discount rules. */
final class PcFacilityCosts {
    private PcFacilityCosts() {}
    static final int BUILD_ACTION_POINTS=20;
    private static final int[] VALUES={65535,65535,65535,500,500,500,600,600,300,300,800,500,800,1200,65535,65535,200,200,200,200,200,200,200,65535,65535,65535,65535,65535,65535,65535,1500,200,200,300,300,300,300,300,400,400,200,200,200,200,200,200,400,400,50,400,100,100,100,100,100,100,100,100,100,100,100,100,100,100};
    static int at(int nativeId) { return VALUES[nativeId]; }
}
