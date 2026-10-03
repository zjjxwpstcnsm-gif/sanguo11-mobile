package game.sanguo.mobile;

import android.content.Context;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Visual source worker transport. Contains no rule/state/save/RNG access.
 * Creation and frame requests belong on a background thread. Source code runs
 * in a separate child; failures/limits stop that child instead of the UI VM.
 * Renderer/event bindings are separate from this binary transport.
 */
final class PcEffectProcess implements AutoCloseable {
    static final String KERNEL_SHA="2668d82c3f835b4dc043a3d28f1902b472627570c1c25ba081425c950f4f8207";
    static final String SCENE_SHA="98d2626d2842f272474cc7a3c2437e29b946d3827c8b52089741155cbc79e4ae";
    static final int CAMERA_FLOATS=51,RECORD_BYTES=184,MAX_PACKETS=32768;
    // xyz3, source RH view16(+40), view*projection16(+c0/+140), projection16(+180).
    private static final int[] VERTEX_FLOAT_OFFSETS={0,4,8,16,20};
    private static final ScheduledThreadPoolExecutor DEADLINES=new ScheduledThreadPoolExecutor(1,r->{
        Thread t=new Thread(r,"PC visual child deadline");t.setDaemon(true);return t;
    });
    static {DEADLINES.setRemoveOnCancelPolicy(true);}
    private final Process process;
    private final InputStream output;
    private final OutputStream input;
    private final AtomicBoolean closed=new AtomicBoolean();
    private final StringBuilder error=new StringBuilder();
    private int serial;
    private volatile String deadlineFailure="";
    // Bounded visual-input evidence for a failed source call. Contains only
    // camera matrices and visual time, never authority, gameplay RNG or saves.
    private final java.util.ArrayDeque<byte[]> commandTrace=new java.util.ArrayDeque<>();
    private boolean commandTraceTruncated;

    static final class Frame {
        final int serial,count;
        final float sourceElapsed,updateMillis,drawMillis,geometryMillis;
        final ByteBuffer records;
        private Frame(int serial,int count,float elapsed,float update,float draw,float geometry,byte[] bytes) {
            this.serial=serial;this.count=count;sourceElapsed=elapsed;updateMillis=update;drawMillis=draw;geometryMillis=geometry;
            records=ByteBuffer.wrap(bytes).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    static PcEffectProcess open(Context context,float[] sourceCamera) throws IOException {
        if(sourceCamera!=null)sourceCamera=sourceCamera.clone();
        checkCamera(sourceCamera);
        File directory=new File(context.getCacheDir(),"pc-source-effects");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("PC visual cache directory");
        File kernel=verifiedAsset(context,directory,"source-kernel.bin",KERNEL_SHA,5779520);
        File scene=verifiedAsset(context,directory,"source-scene.bin",SCENE_SHA,138072);
        File libraries=new File(context.getApplicationInfo().nativeLibraryDir);
        File binary=new File(libraries,"libpc_effect_worker.so");
        if(!binary.isFile())throw new IOException("Original visual worker unavailable for installed ABI");
        ProcessBuilder builder=new ProcessBuilder(binary.getAbsolutePath(),kernel.getAbsolutePath(),scene.getAbsolutePath(),
            Float.toString(sourceCamera[0]),Float.toString(sourceCamera[1]),Float.toString(sourceCamera[2]),"--stream");
        builder.environment().put("LD_LIBRARY_PATH",libraries.getAbsolutePath());
        for(String key:Arrays.asList("PC_VM_PROBE_TRACE","PC_VM_PROBE_BLOCK_BUDGET","PC_VM_PROBE_HEAP_PATH","PC_VM_PROBE_VISUAL_RNG_PATH"))
            builder.environment().remove(key);
        // Every x86 instruction occupies >=1 byte. The existing block-byte
        // guard conservatively retains the5M instruction cap, avoiding the
        // per-instruction accounting cost. Native5s/Java6s deadlines remain.
        builder.environment().put("PC_VM_PROBE_BLOCK_BUDGET","1");
        PcEffectProcess worker=new PcEffectProcess(builder.start());
        ScheduledFuture<?> deadline=worker.deadline(15000);
        try {
            worker.command(0,0,0,sourceCamera);
            byte[] ready=worker.read(16);
            if(!Arrays.equals(Arrays.copyOf(ready,8),new byte[]{'P','C','F','X','R','D','Y','1'})
                ||ByteBuffer.wrap(ready).order(ByteOrder.LITTLE_ENDIAN).getInt(8)!=8
                ||ByteBuffer.wrap(ready).order(ByteOrder.LITTLE_ENDIAN).getInt(12)!=126)
                throw new IOException("Original visual worker readiness contract");
            return worker;
        } catch(IOException|RuntimeException failure) {worker.close();throw failure;}
        finally {deadline.cancel(false);}
    }

    private PcEffectProcess(Process process) {
        this.process=process;input=process.getOutputStream();output=process.getInputStream();
        Thread diagnostics=new Thread(()->{
            try(InputStream stream=process.getErrorStream()) {
                byte[] bytes=new byte[1024];int n;
                while((n=stream.read(bytes))>=0)if(n>0)synchronized(error) {
                    error.append(new String(bytes,0,n,java.nio.charset.StandardCharsets.UTF_8));
                    if(error.length()>8192)error.delete(0,error.length()-8192);
                }
            } catch(IOException ignored) { /* Pipe closure is owned by close(). */ }
        },"PC visual child diagnostics");
        diagnostics.setDaemon(true);diagnostics.start();
    }

    /** dt==0 is a paused redraw: no source update or visual RNG advance. */
    synchronized Frame frame(float dt,float[] sourceCamera) throws IOException {
        if(sourceCamera!=null)sourceCamera=sourceCamera.clone();
        checkCamera(sourceCamera);
        if(!Float.isFinite(dt)||dt<0||dt>30)throw new IllegalArgumentException("Source visual dt");
        if(closed.get())throw new IOException("PC visual child closed");
        if(serial==Integer.MAX_VALUE){close();throw new IOException("PC visual serial boundary");}
        int request=++serial;
        ScheduledFuture<?> deadline=deadline(6000);
        try {
            command(dt==0?2:1,request,dt,sourceCamera);
            byte[] bytes=read(32);
            if(!Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{'P','C','F','X','F','R','0','1'}))
                throw new IOException("PC visual frame header");
            ByteBuffer header=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            int response=header.getInt(8),count=header.getInt(12);
            if(response!=request||count<0||count>MAX_PACKETS)throw new IOException("PC visual frame extent/order");
            float elapsed=header.getFloat(16),update=header.getFloat(20),draw=header.getFloat(24),geometry=header.getFloat(28);
            if(!Float.isFinite(elapsed)||!Float.isFinite(update)||!Float.isFinite(draw)||!Float.isFinite(geometry)
                ||elapsed<0||update<0||draw<0||geometry<0)throw new IOException("PC visual frame timing");
            byte[] records=read(count*RECORD_BYTES);
            ByteBuffer decoded=ByteBuffer.wrap(records).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=0;i<count;i++) {
                int offset=i*RECORD_BYTES,primitive=decoded.getInt(offset),texture=decoded.getInt(offset+4);
                if(primitive<0||primitive>3||texture<0||texture>=33)throw new IOException("PC visual source primitive/image");
                float depth=decoded.getFloat(offset+20);
                if(!Float.isFinite(depth)||depth<0||depth>1)throw new IOException("PC visual source queue depth");
                for(int v=0;v<4;v++)for(int component:VERTEX_FLOAT_OFFSETS)
                    if(!Float.isFinite(decoded.getFloat(offset+24+v*24+component)))throw new IOException("PC visual vertex");
                for(int v=0;v<16;v++)if(!Float.isFinite(decoded.getFloat(offset+120+v*4)))throw new IOException("PC visual matrix");
            }
            return new Frame(response,count,elapsed,update,draw,geometry,records);
        } catch(IOException|RuntimeException failure) {close();throw failure;}
        finally {deadline.cancel(false);}
    }

    private void command(int type,int serial,float dt,float[] camera) throws IOException {
        ByteBuffer bytes=ByteBuffer.allocate(216).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(type).putInt(serial).putFloat(dt);for(float value:camera)bytes.putFloat(value);
        synchronized(commandTrace){if(commandTrace.size()==65){commandTrace.removeFirst();commandTraceTruncated=true;}commandTrace.addLast(bytes.array());}
        input.write(bytes.array());input.flush();
    }
    byte[] sourceCommands() {
        synchronized(commandTrace) {
            if(commandTraceTruncated)return null; // Never label a partial trace replayable.
            ByteArrayOutputStream data=new ByteArrayOutputStream(commandTrace.size()*216);
            for(byte[] command:commandTrace)data.write(command,0,command.length);
            return data.toByteArray();
        }
    }
    private byte[] read(int length) throws IOException {
        byte[] bytes=new byte[length];int offset=0;
        while(offset<length) {
            int n;
            try{n=output.read(bytes,offset,length-offset);}
            catch(java.io.InterruptedIOException interrupted) {
                // Signals can interrupt a live Android pipe read without
                // cancelling this visual scene. Retain any transferred bytes.
                if(closed.get()||Thread.currentThread().isInterrupted())
                    throw new IOException("PC visual read cancelled "+deadlineFailure+": "+diagnostics(),interrupted);
                int transferred=interrupted.bytesTransferred;
                if(transferred<0||transferred>length-offset)throw new IOException("PC visual interrupted pipe extent",interrupted);
                offset+=transferred;continue;
            }
            if(n<0)throw new IOException("PC visual child EOF: "+diagnostics());
            if(n==0)continue;offset+=n;
        }
        return bytes;
    }
    private ScheduledFuture<?> deadline(long millis) {return DEADLINES.schedule(()->{deadlineFailure="deadline"+millis+"ms";close();},millis,TimeUnit.MILLISECONDS);}
    String diagnostics(){synchronized(error){return error.toString();}}
    @Override public void close() {
        if(!closed.compareAndSet(false,true))return;
        // Never wait for a worker holding the frame monitor. Destroy wakes pipe
        // readers; the OS reclaims this child's VM/arena/JIT independently.
        process.destroy();
        try{input.close();}catch(IOException ignored){}
        try{output.close();}catch(IOException ignored){}
    }
    private static void checkCamera(float[] camera) {
        if(camera==null||camera.length!=CAMERA_FLOATS)throw new IllegalArgumentException("Source camera51 floats");
        for(float value:camera)if(!Float.isFinite(value))throw new IllegalArgumentException("Finite source camera");
    }
    private static File verifiedAsset(Context context,File directory,String name,String expected,int bytes) throws IOException {
        File destination=new File(directory,expected+"-"+name);
        if(destination.isFile()&&destination.length()==bytes&&digest(java.nio.file.Files.readAllBytes(destination.toPath())).equals(expected))return destination;
        byte[] data;
        try(InputStream input=context.getAssets().open("3d/pc-effects/"+name)) {
            ByteArrayOutputStream buffer=new ByteArrayOutputStream(bytes);byte[] scratch=new byte[32768];int n;
            while((n=input.read(scratch))>=0)if(n>0) {
                if(buffer.size()+n>bytes)throw new IOException("PC source asset extent: "+name);
                buffer.write(scratch,0,n);
            }
            data=buffer.toByteArray();
        }
        if(data.length!=bytes||!digest(data).equals(expected))throw new IOException("Unverified original visual input: "+name);
        File temporary=File.createTempFile("pc-visual-",".tmp",directory);
        try {
            try(FileOutputStream output=new FileOutputStream(temporary)){output.write(data);output.getFD().sync();}
            java.nio.file.Files.move(temporary.toPath(),destination.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {temporary.delete();}
        return destination;
    }
    private static String digest(byte[] bytes) throws IOException {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder hex=new StringBuilder(64);
            for(byte value:hash)hex.append(Character.forDigit((value>>4)&15,16)).append(Character.forDigit(value&15,16));return hex.toString();
        } catch(NoSuchAlgorithmException failure){throw new IOException("SHA-256 unavailable",failure);}
    }
}
