package game.sanguo.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** Raw Android mix capture of only the installed game's UID; no microphone or display capture. */
public final class AndroidMixCaptureService extends Service {
    private static final String TARGET="game.sanguo.mobile.dev",CHANNEL="game-mix-measurement";
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile AudioRecord recorder;
    private MediaProjection projection;
    private boolean started;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(started)throw new IllegalStateException("Only one fresh measurement per service");
        started=true;
        try {
            String run=intent.getStringExtra("run");int seconds=intent.getIntExtra("seconds",0);
            if(run==null||!run.matches("[A-Za-z0-9_-]{1,64}")||seconds<95||seconds>180)
                throw new IllegalArgumentException("Invalid diagnostic interval");
            NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel=new NotificationChannel(CHANNEL,"Game mix measurement",NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null,null);notifications.createNotificationChannel(channel);
            startForeground(91,new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Game mix measurement").setOngoing(true).build());
            int uid=getPackageManager().getApplicationInfo(TARGET,0).uid;
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=manager.getMediaProjection(intent.getIntExtra("projectionResult",0),intent.getParcelableExtra("projectionData"));
            if(projection==null)throw new IllegalStateException("No actual projection token");
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){cancel();}},main);
            File directory=new File(getExternalFilesDir("android-game-mix"),run);
            if(!directory.mkdirs())throw new IllegalStateException("Fresh own capture directory required");
            new Thread(()->measure(directory,uid,seconds),"AndroidMixMeasurement").start();
        } catch(Exception error) {android.util.Log.e("AndroidMixCapture","Startup failed",error);cancel();stopSelf();}
        return START_NOT_STICKY;
    }
    private void cancel(){cancelled.set(true);AudioRecord active=recorder;if(active!=null)try{active.stop();}catch(IllegalStateException ignored){}}
    private static void json(File path,JSONObject value)throws Exception {
        if(path.exists())throw new IllegalStateException("No overwriting prior measurement");
        try(FileOutputStream out=new FileOutputStream(path)){out.write(value.toString(2).getBytes("UTF-8"));out.getFD().sync();}
    }
    private static byte[] header(long bytes) {
        if(bytes<0||bytes>180L*44100*4+65536||bytes%4!=0)throw new IllegalArgumentException("Unexamined capture extent");
        ByteBuffer b=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R','I','F','F'}).putInt((int)bytes+36).put(new byte[]{'W','A','V','E','f','m','t',' '})
            .putInt(16).putShort((short)1).putShort((short)2).putInt(44100).putInt(176400)
            .putShort((short)4).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt((int)bytes);
        return b.array();
    }
    private static String sha(File path)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];
        try(FileInputStream in=new FileInputStream(path)){for(int n;(n=in.read(buffer))!=-1;)digest.update(buffer,0,n);}
        StringBuilder text=new StringBuilder();for(byte v:digest.digest())text.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return text.toString();
    }
    private void measure(File directory,int uid,int seconds) {
        AudioRecord audio=null;long bytes=0,began=SystemClock.elapsedRealtimeNanos(),lastStamp=0;String failure="";
        JSONArray timestamps=new JSONArray();File wave=new File(directory,"android-mix.wav");
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            AudioPlaybackCaptureConfiguration capture=new AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUid(uid).addMatchingUsage(AudioAttributes.USAGE_GAME).build();
            int minimum=AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT);
            if(minimum<=0)throw new IllegalStateException("Unsupported capture format");
            audio=new AudioRecord.Builder().setAudioFormat(new AudioFormat.Builder().setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setAudioPlaybackCaptureConfig(capture).setBufferSizeInBytes(Math.max(minimum,65536)).build();
            if(audio.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Record not initialized");
            recorder=audio;audio.startRecording();
            if(audio.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("Record not started");
            json(new File(directory,"ready.json"),new JSONObject().put("targetPackage",TARGET).put("targetUid",uid)
                .put("sampleRate",44100).put("channels",2).put("seconds",seconds).put("beganElapsedNanos",began)
                .put("onlyUsageGame",true).put("microphoneCapture",false).put("displayCapture",false));
            byte[] buffer=new byte[8192];
            try(RandomAccessFile out=new RandomAccessFile(wave,"rw")) {
                out.write(header(0));
                while(!cancelled.get()&&SystemClock.elapsedRealtimeNanos()-began<seconds*1000000000L) {
                    int n=audio.read(buffer,0,buffer.length,AudioRecord.READ_BLOCKING);
                    if(n<0){if(cancelled.get())break;throw new IllegalStateException("AudioRecord read error "+n);}
                    if(n%4!=0)throw new IllegalStateException("Incomplete actual stereo frame");
                    out.write(buffer,0,n);bytes+=n;
                    long now=SystemClock.elapsedRealtimeNanos();
                    if(now-lastStamp>=1000000000L) {
                        AudioTimestamp stamp=new AudioTimestamp();int status=audio.getTimestamp(stamp,AudioTimestamp.TIMEBASE_MONOTONIC);
                        timestamps.put(new JSONObject().put("observedElapsedNanos",now).put("capturedFrames",bytes/4)
                            .put("status",status).put("framePosition",stamp.framePosition).put("nanoTime",stamp.nanoTime));lastStamp=now;
                    }
                }
                out.seek(0);out.write(header(bytes));out.getFD().sync();
            }
        } catch(Exception error){failure=error.toString();android.util.Log.e("AndroidMixCapture","Measurement failed",error);}
        finally {
            boolean interrupted=cancelled.get();
            recorder=null;if(audio!=null){try{audio.stop();}catch(IllegalStateException ignored){}audio.release();}
            if(projection!=null){projection.stop();projection=null;}
            try {json(new File(directory,"result.json"),new JSONObject().put("status","MEASURED_NOT_WAVEFORM_ACCEPTED")
                .put("targetPackage",TARGET).put("targetUid",uid).put("bytes",bytes).put("frames",bytes/4)
                .put("failure",failure).put("projectionStoppedOrCancelled",interrupted)
                .put("elapsedNanos",SystemClock.elapsedRealtimeNanos()-began).put("timestamps",timestamps)
                .put("wavSha256",wave.exists()?sha(wave):"").put("speakerOrArmEvidence",false));}
            catch(Exception error){android.util.Log.e("AndroidMixCapture","Retain result failed",error);}
            stopForeground(true);stopSelf();
        }
    }
    @Override public void onDestroy(){cancel();if(projection!=null){projection.stop();projection=null;}super.onDestroy();}
}
