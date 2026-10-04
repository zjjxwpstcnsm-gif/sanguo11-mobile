package game.sanguo.mobile;

/** Generated from unchanged PC selector execution. Raw inputs only; no event binding or playback. */
final class PcVoicePolicy {
    static final String SOURCE_EXECUTABLE_SHA256 = "30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb";
    static final String NATIVE_REPORT_SHA256 = "ade70a620a30e752a6d7c903c5fa93f6d09a82432baffeb3b72ba4729b948052";
    private static final int[] TYPES = {0,0,-1,1,0,-1,2,0,-1,3,0,-1,1,1,-1,2,1,-1,3,0,12,1,0,13};
    private static final int[] BASES = {5,19,33,47,61,75,89,103,117,131,145,159,173,187,201,215,229,243,257,271,285,299,313,327,341,355,369,383,397,411,425,439,453,467,481,495,509,523,537,551,565,579,593,607,621,635,649,663,677,691,705,719,733,747,761,775,789,803,817,831,845,859,873,887,901,915,929,943,957,971,985};
    private static final int[] ABILITY = {6,7,4,5,2,3,1,0,10,11,10,11,9,8,9,8};
    private static final int[] FEEDBACK_A = {6,7,4,5,2,3,0,1,10,11,10,11,8,9,8,9};
    private static final int[] FEEDBACK_B = {6,7,4,5,2,3,0,1,10,11,10,11,8,9,8,9};
    private PcVoicePolicy() {}
    private static boolean valid(int profile, int voiceTypeRaw, boolean actorValid) {
        return actorValid && profile >= 0 && profile < BASES.length && voiceTypeRaw >= 0 && voiceTypeRaw < 8;
    }
    /** Original4d1290: current unsigned-byte values already calculated by the committed state. */
    static int abilityVoice(int nativeProfile, int voiceTypeRaw, boolean actorValid,
                            int ability0, int ability1, int ability2, int ability3) {
        if (!valid(nativeProfile, voiceTypeRaw, actorValid) || ability0 < 0 || ability0 > 255
            || ability1 < 0 || ability1 > 255 || ability2 < 0 || ability2 > 255 || ability3 < 0 || ability3 > 255) return -1;
        int kind = voiceTypeRaw * 3;
        int superior = ability1 > ability0 && ability1 > ability2 && ability1 > ability3 ? 1 : 0;
        return BASES[nativeProfile] + ABILITY[2 * (TYPES[kind] + 4 * TYPES[kind + 1]) + superior];
    }
    /** Original4d13b0. feedbackRaw remains raw until each caller proves its semantics. */
    static int feedbackAVoice(int nativeProfile, int voiceTypeRaw, boolean actorValid, int feedbackRaw) {
        return feedback(nativeProfile, voiceTypeRaw, actorValid, feedbackRaw, FEEDBACK_A);
    }
    /** Original4d1490. Kept distinct even though this executable has equal feedback tables. */
    static int feedbackBVoice(int nativeProfile, int voiceTypeRaw, boolean actorValid, int feedbackRaw) {
        return feedback(nativeProfile, voiceTypeRaw, actorValid, feedbackRaw, FEEDBACK_B);
    }
    private static int feedback(int profile, int voiceTypeRaw, boolean actorValid, int feedbackRaw, int[] table) {
        if (!valid(profile, voiceTypeRaw, actorValid) || feedbackRaw < 0 || feedbackRaw > 1) return -1;
        int kind = voiceTypeRaw * 3;
        int override = TYPES[kind + 2];
        int variant = override >= 0 && override < 14 ? override : table[2 * (TYPES[kind] + 4 * TYPES[kind + 1]) + feedbackRaw];
        return BASES[profile] + variant;
    }
}
