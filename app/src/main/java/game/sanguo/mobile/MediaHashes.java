package game.sanguo.mobile;

/** Shared immutable digest syntax. Avoid recompiling the same pattern per original identity. */
final class MediaHashes {
    private static final java.util.regex.Pattern SHA256=java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private MediaHashes(){}
    static boolean sha256(String value){return value!=null&&SHA256.matcher(value).matches();}
}
