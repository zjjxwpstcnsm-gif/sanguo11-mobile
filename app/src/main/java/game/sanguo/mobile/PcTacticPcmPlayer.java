package game.sanguo.mobile;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;

/** Nine worker-prepared original49/78/58 PCM tracks; immutable sources, no rules or RNG. */
final class PcTacticPcmPlayer implements AutoCloseable {
    private static final int[] IDS={49,78,58},FRAMES={45350,45644,33553},RATES={44100,44100,22050};
    private static final String[] HASHES={"1d13dcc14f9f40c74062c6715d9bf245a299573882769ebc2c128e854a86a705","d62f28032fce9e132a9930565770d4e5a7a2a607623354c8c568a45cb616c638","e51aa76c832a0c2e61b31fa36b8435517774745fb5d90e206453a6c5379d9c81"};
    private static final class Slot {
        final int sound,frames;final AudioTrack track;boolean active;
        Slot(int sound,int frames,AudioTrack track){this.sound=sound;this.frames=frames;this.track=track;}
    }
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayList<Slot> slots=new ArrayList<>();
    private volatile boolean closed;
    private volatile String error="";
    private int cursor,serial;
    PcTacticPcmPlayer(Context context){Context app=context.getApplicationContext();
        new Thread(()->{ArrayList<Slot> prepared=new ArrayList<>();boolean posted=false;
            try{
                for(int source=0;source<IDS.length;source++){
                    byte[] wave;try(var input=app.getAssets().open("audio/pc/tactic-"+IDS[source]+".wav")){
                        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
                        for(int n;(n=input.read(buffer))!=-1;){if(bytes.size()+n>100000)throw new java.io.IOException("Original tactic PCM extent exceeded");bytes.write(buffer,0,n);}wave=bytes.toByteArray();
                    }
                    StringBuilder digest=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(wave))digest.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
                    ByteBuffer header=ByteBuffer.wrap(wave).order(ByteOrder.LITTLE_ENDIAN);
                    if(!HASHES[source].contentEquals(digest)||wave.length!=44+FRAMES[source]*2||header.getInt(20)!=0x10001||header.getInt(24)!=RATES[source]||header.getInt(40)!=FRAMES[source]*2)
                        throw new java.io.IOException("Original tactic WAV/hash differs");
                    for(int i=0;i<3;i++){
                        if(closed)return;
                        AudioTrack track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                            .setAudioFormat(new AudioFormat.Builder().setSampleRate(RATES[source]).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(FRAMES[source]*2).build();
                        prepared.add(new Slot(IDS[source],FRAMES[source],track));
                        if(track.getState()!=AudioTrack.STATE_NO_STATIC_DATA||track.write(wave,44,FRAMES[source]*2,AudioTrack.WRITE_BLOCKING)!=FRAMES[source]*2)throw new java.io.IOException("Original tactic PCM preparation failed");
                    }
                }
                main.post(()->{if(closed)release(prepared);else slots.addAll(prepared);});posted=true;
            }catch(Exception failure){error=failure.toString();android.util.Log.e("PcAudio","Original tactic PCM rejected",failure);}
            finally{if(!posted)release(prepared);}
        },"pc-tactic-pcm").start();
    }
    private static void ui(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Serial source effect owner required");}
    boolean ready(){ui();return !closed&&slots.size()==IDS.length*3;}
    String error(){return error;}
    boolean active(){ui();for(Slot slot:slots)if(slot.active&&Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition())<slot.frames)return true;return false;}
    int play(int sound,float gain){ui();if(sound!=49&&sound!=78&&sound!=58)throw new IllegalArgumentException("Unverified native tactic sound");if(!ready())return 0;
        Slot selected=null;for(Slot slot:slots)if(slot.sound==sound&&(!slot.active||Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition())>=slot.frames)){selected=slot;break;}
        if(selected==null){int nth=cursor++%3;for(Slot slot:slots)if(slot.sound==sound&&nth--==0){selected=slot;break;}}
        selected.track.stop();selected.track.setPlaybackHeadPosition(0);selected.track.setVolume(gain);selected.track.play();selected.active=true;return ++serial;
    }
    void paused(boolean paused){ui();for(Slot slot:slots)if(slot.active){if(paused)slot.track.pause();else if(Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition())<slot.frames)slot.track.play();}}
    void volume(float gain){ui();for(Slot slot:slots)if(slot.active)slot.track.setVolume(gain);}
    void stop(){ui();for(Slot slot:slots)if(slot.active){slot.track.stop();slot.track.setPlaybackHeadPosition(0);slot.active=false;}}
    private static void release(ArrayList<Slot> slots){for(Slot slot:slots)slot.track.release();slots.clear();}
    @Override public void close(){ui();if(closed)return;closed=true;stop();release(slots);}
}
