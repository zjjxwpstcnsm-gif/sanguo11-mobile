package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.runtime.GameSession;

/** All packaged source voices/identity joins/actual Android PCM; no normal event or playback claim. */
public final class VoiceSourceInstrumentation extends Instrumentation {
    private int checks;
    private Bundle arguments;
    private final JSONArray resources=new JSONArray();
    private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    @Override public void onCreate(Bundle args){super.onCreate(args);arguments=args==null?new Bundle():args;start();}
    private JSONObject asset(String name)throws Exception {
        try(var input=getTargetContext().getAssets().open(name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];for(int n;(n=input.read(block))!=-1;)out.write(block,0,n);
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    private int[] sampleResources(int index)throws Exception {
        android.os.Debug.MemoryInfo memory=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(memory);
        String[] fds=new File("/proc/self/fd").list(),tasks=new File("/proc/self/task").list();
        if(fds==null||tasks==null)throw new IllegalStateException("Own process resource inventory unavailable");
        resources.put(new JSONObject().put("index",index).put("pssKb",memory.getTotalPss()).put("fdCount",fds.length).put("nativeThreadCount",tasks.length)
            .put("javaUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
        return new int[]{fds.length,tasks.length};
    }
    @Override public void onStart(){
        Bundle result=new Bundle();JSONArray decodedRows=new JSONArray(),errors=new JSONArray();AtomicReference<GameSession> session=new AtomicReference<>();byte[][] before={null};
        File directory=new File(getTargetContext().getCacheDir(),"voice-source-probe-"+android.os.SystemClock.elapsedRealtimeNanos());long began=android.os.SystemClock.elapsedRealtime();
        int sourceDifferences=0,referenceDifferences=0;
        try {
            check(directory.mkdir(),"fresh own voice source cache");
            runOnMainSync(()->{try{session.set(new GameSession(ScenarioCatalog.load("coalition-190",0,20260923L)));before[0]=session.get().captureSave();}catch(Exception e){throw new IllegalStateException(e);}});
            PcVoiceCatalog catalog=new PcVoiceCatalog(getTargetContext());check(catalog.voices().size()==1997,"all1997 packaged sources");
            JSONArray approved=asset("portraits/pc/media-manifest.json").getJSONArray("identities");
            for(int i=0;i<approved.length();i++) {
                JSONObject row=approved.getJSONObject(i);PortraitMediaIdentity identity=new PortraitMediaIdentity(row.getInt("officerId"),row.getInt("nativeId"),row.getString("sourceVariant"),row.getString("sourcePath"),row.getString("sourceSha256"),row.getString("recordSha256"));
                check(catalog.voiceType(identity)==row.getInt("voiceTypeRaw"),"exact completed saved identity voice type "+i);
                PortraitMediaIdentity wrong=new PortraitMediaIdentity(identity.officerId,identity.nativeId,identity.sourceVariant,identity.sourcePath,identity.sourceSha,"0000000000000000000000000000000000000000000000000000000000000000");
                check(catalog.voiceType(wrong)==-1,"record mismatch never borrows source voice "+i);
            }
            check(catalog.identityCount()==10656&&approved.length()==10656&&catalog.voiceType(null)==-1,"all approved joins and unknown source");
            for(int id=0;id<=1000;id++)for(boolean alternate:new boolean[]{false,true}) {
                PcVoiceCatalog.Voice voice=catalog.voice(id,alternate);
                check(voice!=null&&voice.resourceId==2287+id+(alternate&&id>=5?996:0),"original native resource IO "+id+"/"+alternate);
            }
            check(catalog.voice(-1,false)==null&&catalog.voice(1001,true)==null,"invalid source voice rejects");
            int start=Integer.parseInt(arguments.getString("start","0")),count=Integer.parseInt(arguments.getString("count","1997"));
            check(start>=0&&count>0&&start+count<=1997,"bounded explicit voice sample range");
            long maximumDecodeMillis=0;int[] initialResources=null;
            for(int index=start;index<start+count;index++) {
                PcVoiceCatalog.Voice voice=catalog.voices().get(index);File pcm=new File(directory,voice.resourceId+".pcm");
                try {
                    PcVorbisDecoder.Result decoded=PcVorbisDecoder.decode(getTargetContext(),voice,pcm,new AtomicBoolean());
                    check(decoded.frames>0&&decoded.bytes==decoded.frames*voice.channels*2&&pcm.length()==decoded.bytes,"all actual decoder bytes preserved "+voice.resourceId);
                    if(!decoded.sourceFrameEqual)sourceDifferences++;if(!decoded.referenceByteEqual)referenceDifferences++;
                    maximumDecodeMillis=Math.max(maximumDecodeMillis,decoded.elapsedMillis);
                    decodedRows.put(new JSONObject().put("resourceId",voice.resourceId).put("oggSha256",voice.oggSha256).put("sourceEndGranuleFrames",voice.frames)
                        .put("referenceDecodedFrames",voice.referenceFrames).put("actualDecodedFrames",decoded.frames).put("sourceFrameEqual",decoded.sourceFrameEqual)
                        .put("referencePcmSha256",voice.referencePcmSha256).put("actualPcmSha256",decoded.pcmSha256).put("referenceByteEqual",decoded.referenceByteEqual)
                        .put("actualBytes",decoded.bytes).put("channels",voice.channels).put("sampleRate",voice.sampleRate).put("codec",decoded.codec).put("decodeMillis",decoded.elapsedMillis)
                        .put("timelineStatus",decoded.sourceFrameEqual?"ANDROID_EQUALS_ORIGINAL_EOS_NATIVE_PCM_PENDING":"ANDROID_DIFFERS_ORIGINAL_EOS_NATIVE_TIMELINE_PENDING"));
                    if(arguments.containsKey("retainResource")&&Integer.parseInt(arguments.getString("retainResource"))==voice.resourceId)
                        Files.copy(pcm.toPath(),new File(getTargetContext().getFilesDir(),"voice-source-"+voice.resourceId+".pcm").toPath());
                    check(pcm.delete(),"release current voice PCM "+voice.resourceId);
                }catch(Exception error) {
                    errors.put(new JSONObject().put("resourceId",voice.resourceId).put("error",error.toString()));
                    if(pcm.exists()&&!pcm.delete())throw new IllegalStateException("Own voice cache retained",error);
                }
                if(index==start||index%100==0){int[] sample=sampleResources(index);if(initialResources==null)initialResources=sample;android.util.Log.i("PcVoiceSource","DECODED index="+index+" errors="+errors.length());}
            }
            int[] finalResources=sampleResources(start+count);
            check(initialResources!=null&&finalResources[0]<=initialResources[0]+16&&finalResources[1]<=initialResources[1]+16,"no accumulated per-decoder FD/native-thread leak before process exits");
            runOnMainSync(()->{try{check(Arrays.equals(before[0],session.get().captureSave()),"entire voice catalog/decode preserves complete Save/RNG");}catch(Exception e){throw new IllegalStateException(e);}});
            check(directory.delete(),"no own voice cache directory remains");
            check(errors.length()==0&&decodedRows.length()==count,"all requested original samples decoded");
            result.putString("voiceSource","VOICE_SOURCE PASS checks="+checks+" samples="+decodedRows.length()+"; source/codec evidence, no normal playback/timeline parity claim");
            result.putLong("maximumDecodeMillis",maximumDecodeMillis);
        }catch(Throwable error){result.putString("voiceSource","FAIL "+android.util.Log.getStackTraceString(error));}
        finally {
            try{Files.write(new File(getTargetContext().getFilesDir(),"voice-source.json").toPath(),new JSONObject().put("scope","Actual packaged voices/identity/PCM with unresolved native timeline; no normal voice playback")
                .put("checks",checks).put("sourceFrameDifferences",sourceDifferences).put("referenceByteDifferences",referenceDifferences).put("decoded",decodedRows).put("errors",errors).put("resources",resources)
                .put("elapsedMillis",android.os.SystemClock.elapsedRealtime()-began).put("maximumDecodeMillis",result.getLong("maximumDecodeMillis",-1)).put("result",result.getString("voiceSource")).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            catch(Exception e){result.putString("voiceSource","FAIL evidence "+e);}
            runOnMainSync(()->{if(session.get()!=null)session.get().close();});
            File[] own=directory.listFiles();if(own!=null)for(File f:own)if(!f.delete())android.util.Log.w("PcAudio","Own voice probe cache retained "+f);if(directory.exists()&&!directory.delete())android.util.Log.w("PcAudio","Own voice probe directory retained");
        }
        finish(result.getString("voiceSource").startsWith("VOICE_SOURCE PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
}
