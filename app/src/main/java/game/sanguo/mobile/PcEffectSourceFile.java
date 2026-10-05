package game.sanguo.mobile;

import android.content.Context;
import java.io.*;
import java.security.MessageDigest;

/** Immutable original WAV copy for SoundPool. Call on a worker, never the UI thread. */
final class PcEffectSourceFile {
    private static final String HASH="1cf7d3edbf967ac7f7123fdb2f91a54714827520abce2ced13c4a70610909f78";
    static File prepare(Context context)throws Exception{
        File directory=new File(context.getCacheDir(),"pc-effect-sources");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create original effect cache");
        File target=new File(directory,HASH+".wav");
        if(target.isFile()&&verified(target))return target;
        File partial=File.createTempFile("pc33-",".partial",directory);
        try{
            try(InputStream in=context.getAssets().open("audio/pc/technique-33.wav");OutputStream out=new FileOutputStream(partial)){
                byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            }
            if(!verified(partial))throw new IOException("Original PC33 source hash/size mismatch");
            if(!partial.renameTo(target))throw new IOException("Cannot publish original effect cache");
            return target;
        }finally{if(partial.exists()&&!partial.delete())android.util.Log.w("PcAudio","Own incomplete effect cache retained");}
    }
    private static boolean verified(File file)throws Exception{
        if(file.length()!=90450)return false;MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)digest.update(bytes,0,n);}
        StringBuilder hash=new StringBuilder(64);for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return HASH.contentEquals(hash);
    }
    private PcEffectSourceFile(){}
}
