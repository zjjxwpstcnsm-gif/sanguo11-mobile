package game.sanguo.mobile;

import android.content.Context;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Worker-only original voice sources and exact saved source identities; no action/speaker inference. */
final class PcVoiceCatalog {
    static final class Voice extends PcAudioSource {
        final int resourceId,candidateIndex;
        final long referenceFrames;
        final String timelineStatus;
        Voice(JSONObject row)throws Exception {
            // Voice timeline differences are recorded. Every emitted PCM byte remains intact.
            super(row.getString("asset"),row.getString("oggSha256"),row.getString("referencePcmSha256"),row.getInt("sampleRate"),row.getInt("channels"),row.getLong("frames"),false);
            resourceId=row.getInt("resourceId");candidateIndex=row.getInt("voiceCandidateIndex");referenceFrames=row.getLong("referenceDecodedFrames");timelineStatus=row.getString("timelineStatus");
            if(candidateIndex<0||candidateIndex>=1997||resourceId!=2287+candidateIndex||!asset.equals("audio/pc/voices/"+resourceId+".ogg")
                ||referenceFrames<1||Math.abs(referenceFrames-frames)>2048||row.getLong("referenceFrameDifference")!=referenceFrames-frames)
                throw new IOException("Invalid original voice timeline/index");
            if(!timelineStatus.equals(referenceFrames==frames?"REFERENCE_EQUALS_SOURCE_EOS":"REFERENCE_DIFFERS_SOURCE_EOS_ORIGINAL_DECODER_PENDING"))throw new IOException("Voice timeline status mismatch");
        }
    }
    private static final class Person {
        final PortraitMediaIdentity identity;
        final int voiceType;
        Person(JSONObject row)throws Exception {
            identity=new PortraitMediaIdentity(row.getInt("officerId"),row.getInt("nativeId"),row.getString("sourceVariant"),row.getString("sourcePath"),row.getString("sourceSha256"),row.getString("recordSha256"));
            voiceType=row.getInt("voiceTypeRaw");if(voiceType<0||voiceType>=8)throw new IOException("Unexamined original voice type");
        }
    }
    private final Map<Integer,Voice> voices;
    private final Map<String,Person> people;
    private static JSONObject read(Context context,String asset)throws Exception {
        try(var input=context.getAssets().open(asset)) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
            for(int n;(n=input.read(buffer))!=-1;){if(bytes.size()+n>16*1024*1024)throw new IOException("Voice manifest too large");bytes.write(buffer,0,n);}
            return new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        }
    }
    PcVoiceCatalog(Context context)throws IOException {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Voice manifest parsed on UI thread");
        try {
            JSONObject root=read(context,"audio/pc/voice-manifest.json");
            if(root.getInt("schema")!=1||!root.getString("sourceArchiveSha256").equals("e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a")
                ||!root.getString("sourceExecutableSha256").equals(PcVoicePolicy.SOURCE_EXECUTABLE_SHA256)||root.getInt("alternateOffset")!=996||root.getInt("alternateThreshold")!=5)
                throw new IOException("Unexamined original voice source/selector");
            Map<Integer,Voice> found=new LinkedHashMap<>();JSONArray rows=root.getJSONArray("voices");
            for(int i=0;i<rows.length();i++){Voice voice=new Voice(rows.getJSONObject(i));if(found.put(voice.candidateIndex,voice)!=null)throw new IOException("Duplicate original voice resource");}
            if(found.size()!=1997)throw new IOException("Incomplete original voice resources");voices=Collections.unmodifiableMap(found);
            root=read(context,"audio/pc/voice-identities.json");
            if(root.getInt("schema")!=1||!root.getString("sourceExecutableSha256").equals(PcVoicePolicy.SOURCE_EXECUTABLE_SHA256)||!root.getString("voiceTypeFieldOffsetHex").equals("0x100"))throw new IOException("Unexamined actor voice field");
            Map<String,Person> identities=new LinkedHashMap<>();rows=root.getJSONArray("identities");
            for(int i=0;i<rows.length();i++){Person p=new Person(rows.getJSONObject(i));if(identities.put(p.identity.key(),p)!=null)throw new IOException("Duplicate voice identity");}
            if(identities.size()!=10656)throw new IOException("Incomplete approved voice identities");people=Collections.unmodifiableMap(identities);
        }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Original voice catalog",e);}
    }
    Voice voice(int nativeVoiceId,boolean alternateRaw){if(nativeVoiceId<0||nativeVoiceId>1000)return null;return voices.get(nativeVoiceId+(alternateRaw&&nativeVoiceId>=5?996:0));}
    int voiceType(PortraitMediaIdentity actualSavedIdentity) {
        if(actualSavedIdentity==null)return -1;Person p=people.get(actualSavedIdentity.key());
        if(p==null||!p.identity.sourcePath.equals(actualSavedIdentity.sourcePath)||!p.identity.sourceSha.equals(actualSavedIdentity.sourceSha)||!p.identity.recordSha.equals(actualSavedIdentity.recordSha))return -1;
        return p.voiceType;
    }
    Map<Integer,Voice> voices(){return voices;}
    int identityCount(){return people.size();}
}
