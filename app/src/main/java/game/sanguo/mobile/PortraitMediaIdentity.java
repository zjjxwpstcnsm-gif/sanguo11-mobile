package game.sanguo.mobile;

/** Values copied from authoritative readonly OfficerSnapshot.SourceInfo. Never use a name as the source key. */
final class PortraitMediaIdentity {
    final int officerId,nativeId;
    final String sourceVariant,sourcePath,sourceSha,recordSha;
    final Integer canonicalOfficerId,originalVoiceProfile;
    final java.util.Map<Integer,Integer> originalFields;
    PortraitMediaIdentity(int officerId,int nativeId,String variant,String path,String sourceSha,String recordSha){
        this(officerId,nativeId,variant,path,sourceSha,recordSha,null,null,java.util.Collections.emptyMap());
    }
    PortraitMediaIdentity(int officerId,int nativeId,String variant,String path,String sourceSha,String recordSha,Integer canonicalOfficerId,Integer originalVoiceProfile,java.util.Map<Integer,Integer> originalFields){
        if(!java.util.Objects.equals(originalVoiceProfile,originalFields.get(48)))throw new IllegalArgumentException("Original voice metadata differs");
        this.canonicalOfficerId=canonicalOfficerId;this.originalVoiceProfile=originalVoiceProfile;
        this.originalFields=java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(originalFields));
        if(officerId<0||nativeId<0||nativeId>1099||variant==null||variant.isEmpty()||path==null||!MediaHashes.sha256(sourceSha)||!MediaHashes.sha256(recordSha))throw new IllegalArgumentException("Unverified portrait identity");
        this.officerId=officerId;this.nativeId=nativeId;sourceVariant=variant;sourcePath=path;this.sourceSha=sourceSha;this.recordSha=recordSha;
    }
    String key(){return officerId+":"+nativeId+":"+sourceVariant;}
}
