package game.sanguo.mobile;

/** Values copied from authoritative readonly OfficerSnapshot.SourceInfo. Never use a name as the source key. */
final class PortraitMediaIdentity {
    final int officerId,nativeId;
    final String sourceVariant,sourcePath,sourceSha,recordSha;
    PortraitMediaIdentity(int officerId,int nativeId,String variant,String path,String sourceSha,String recordSha){
        if(officerId<0||nativeId<0||nativeId>1099||variant==null||variant.isEmpty()||path==null||!sourceSha.matches("[0-9a-f]{64}")||!recordSha.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Unverified portrait identity");
        this.officerId=officerId;this.nativeId=nativeId;sourceVariant=variant;sourcePath=path;this.sourceSha=sourceSha;this.recordSha=recordSha;
    }
    String key(){return officerId+":"+nativeId+":"+sourceVariant;}
}
