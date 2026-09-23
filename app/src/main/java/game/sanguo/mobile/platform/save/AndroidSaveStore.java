package game.sanguo.mobile.platform.save;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;

/** Platform-only atomic persistence. It never chooses, reads or mutates a game world. */
public final class AndroidSaveStore {
    private final File directory;
    public AndroidSaveStore(Context context){directory=context.getApplicationContext().getFilesDir();}
    public AtomicFile file(String slot){
        if(!slot.matches("auto|manual[23]?"))throw new IllegalArgumentException("Unknown save slot");
        return new AtomicFile(new File(directory,slot+".sg11"));
    }
    public void write(String slot,byte[] captured)throws IOException{
        AtomicFile file=file(slot);FileOutputStream output=null;
        try{output=file.startWrite();output.write(captured);file.finishWrite(output);}
        catch(IOException failure){if(output!=null)file.failWrite(output);throw failure;}
    }
}
