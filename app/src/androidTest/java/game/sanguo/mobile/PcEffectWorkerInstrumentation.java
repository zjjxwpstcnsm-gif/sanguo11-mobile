package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;

/** Installed original-child transport proof. No activity, autosave or rule mutation.
 * The independent source fixture is memory evidence, not PC image acceptance.
 */
public final class PcEffectWorkerInstrumentation extends Instrumentation {
    private int checks;
    private void check(boolean pass,String message){if(!pass)throw new AssertionError(message);checks++;}
    @Override public void onCreate(Bundle arguments) {super.onCreate(arguments);start();}
    @Override public void onStart() {
        Bundle result=new Bundle();PcEffectProcess worker=null;byte[] save=null;boolean existed=false;
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");
        try {
            existed=auto.isFile();if(existed)save=Files.readAllBytes(auto.toPath());
            byte[] cameraBytes=read(getContext().getAssets().open("pc-effects/worker-camera.bin"));
            check(cameraBytes.length==204,"independent source camera51 floats");
            float[] camera=new float[51];ByteBuffer.wrap(cameraBytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(camera);
            // AAPT expands the source .gz asset and drops that suffix in APK.
            byte[] reference=read(getContext().getAssets().open("pc-effects/worker-reference.bin"));
            ByteBuffer expected=ByteBuffer.wrap(reference).order(ByteOrder.LITTLE_ENDIAN);
            expected.position(16);int records=0;
            worker=PcEffectProcess.open(getTargetContext(),camera);
            float[] times={.0333333333f,.5f,1f,0f,0f};
            for(int i=0;i<times.length;i++) {
                PcEffectProcess.Frame frame=worker.frame(times[i],camera);
                byte[] magic=new byte[8];expected.get(magic);
                check(Arrays.equals(magic,new byte[]{'P','C','F','X','F','R','0','1'}),"independent frame fixture");
                check(frame.serial==expected.getInt(),"persistent serial order");
                int count=expected.getInt();check(frame.count==count,"accepted original queue count");
                check(Float.floatToRawIntBits(frame.sourceElapsed)==Float.floatToRawIntBits(expected.getFloat()),"original clock and paused redraw");
                expected.position(expected.position()+12);
                byte[] bytes=new byte[count*184];expected.get(bytes);byte[] actual=new byte[bytes.length];frame.records.duplicate().get(actual);
                check(Arrays.equals(bytes,actual),"whole ordered source packet/VB/matrix/texture/blend frame");
                records+=count;
            }
            check(!expected.hasRemaining(),"complete independent fixture consumed");
            worker.close();
            boolean rejected=false;try{worker.frame(0,camera);}catch(java.io.IOException closed){rejected=true;}
            check(rejected,"closed child transport rejects further frames");worker=null;
            // Rebuild a separate visual VM; no old source clock/RNG/queue reuse.
            worker=PcEffectProcess.open(getTargetContext(),camera);
            PcEffectProcess.Frame fresh=worker.frame(times[0],camera);check(fresh.serial==1,"new scene source serial starts1");
            check(Float.floatToRawIntBits(fresh.sourceElapsed)==Float.floatToRawIntBits(times[0]),"new scene source clock reset");
            expected.position(32);int firstCount=ByteBuffer.wrap(reference).order(ByteOrder.LITTLE_ENDIAN).getInt(28);
            expected.position(48);byte[] first=new byte[firstCount*184];expected.get(first);byte[] actual=new byte[first.length];fresh.records.duplicate().get(actual);
            check(fresh.count==firstCount&&Arrays.equals(first,actual),"new scene original visual RNG/geometry exact");
            result.putString("stream","PASS PC SOURCE WORKER installed checks="+checks+" packets="+records+"; persistent3 updates/2 paused redraws, original ordered vertices/matrices/texture/blend, rebuild and close; renderer/events/PC/ARM performance pending\n");
        } catch(Throwable failure) {result.putString("stream","FAIL PC SOURCE WORKER "+android.util.Log.getStackTraceString(failure));}
        finally {
            if(worker!=null)worker.close();
            try {
                if(existed)check(auto.isFile()&&Arrays.equals(save,Files.readAllBytes(auto.toPath())),"actual user save unchanged");
                else check(!auto.exists(),"transport does not create a gameplay save");
            }catch(Throwable failure){result.putString("stream",result.getString("stream")+"FAIL save boundary "+failure);}
        }
        finish(Activity.RESULT_OK,result);
    }
    private static byte[] read(InputStream input)throws java.io.IOException {
        try(InputStream in=input;ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))>=0)if(n>0){if(output.size()+n>8*1024*1024)throw new java.io.IOException("fixture limit");output.write(bytes,0,n);}return output.toByteArray();
        }
    }
}
