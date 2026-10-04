package game.sanguo.mobile;

import android.content.Context;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;

/** Bounded original HUD33 PCM voices, prepared off UI; no rule/state/RNG access. */
final class PcPcmEffectPlayer implements AutoCloseable {
    private static final int VOICES=6,FRAMES=45203;
    private static final class Slot {final AudioTrack track;boolean active;Slot(AudioTrack track){this.track=track;}}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayList<Slot> slots=new ArrayList<>();
    private volatile boolean closed;private int cursor,serial;
    PcPcmEffectPlayer(Context context){
        Context app=context.getApplicationContext();
        new Thread(()->{ArrayList<Slot> prepared=new ArrayList<>();boolean posted=false;
            try{
                byte[] wave=Files.readAllBytes(PcEffectSourceFile.prepare(app).toPath());
                ByteBuffer header=ByteBuffer.wrap(wave).order(ByteOrder.LITTLE_ENDIAN);
                if(wave.length!=90450||header.getInt(20)!=0x10001||header.getInt(24)!=44100||header.getInt(40)!=FRAMES*2)
                    throw new java.io.IOException("Unexamined original HUD33 WAV format");
                for(int i=0;i<VOICES;i++){
                    if(closed)return;
                    AudioTrack track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        .setAudioFormat(new AudioFormat.Builder().setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                        .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(FRAMES*2).build();
                    prepared.add(new Slot(track));
                    if(track.getState()!=AudioTrack.STATE_NO_STATIC_DATA||track.write(wave,44,FRAMES*2,AudioTrack.WRITE_BLOCKING)!=FRAMES*2)
                        throw new java.io.IOException("Original HUD33 static PCM preparation failed");
                }
                main.post(()->{if(closed)release(prepared);else slots.addAll(prepared);});posted=true;
            }catch(Exception e){android.util.Log.e("PcAudio","Original HUD33 PCM preparation failed",e);}
            finally{if(!posted)release(prepared);}
        },"pc-effect-pcm").start();
    }
    private static void ui(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Serial effect owner required");}
    boolean ready(){ui();return !closed&&slots.size()==VOICES;}
    boolean active(){ui();for(Slot slot:slots)if(slot.active&&Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition())<FRAMES)return true;return false;}
    long playedFrames(){ui();long frames=0;for(Slot slot:slots)if(slot.active)frames+=Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition());return frames;}
    int play(float gain){ui();if(!ready())return 0;Slot selected=null;
        for(Slot slot:slots)if(!slot.active||Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition())>=FRAMES){selected=slot;break;}
        if(selected==null)selected=slots.get(cursor++%VOICES);
        selected.track.stop();selected.track.setPlaybackHeadPosition(0);selected.track.setVolume(gain);selected.track.play();selected.active=true;
        return ++serial;
    }
    void paused(boolean value){ui();for(Slot slot:slots)if(slot.active){if(value)slot.track.pause();else if(Integer.toUnsignedLong(slot.track.getPlaybackHeadPosition())<FRAMES)slot.track.play();}}
    void volume(float gain){ui();for(Slot slot:slots)if(slot.active)slot.track.setVolume(gain);}
    void stop(){ui();for(Slot slot:slots)if(slot.active){slot.track.stop();slot.track.setPlaybackHeadPosition(0);slot.active=false;}}
    private static void release(ArrayList<Slot> slots){for(Slot slot:slots)slot.track.release();slots.clear();}
    @Override public void close(){ui();if(closed)return;closed=true;stop();release(slots);}
}
