package game.sanguo.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Resource readNBytes semantics using stream methods available on minSdk26.
 * The caller retains its original limit+1 check, SHA validation and ownership. */
final class PcResourceBytes {
    static byte[] readUpTo(InputStream input,int count)throws IOException {
        if(input==null)throw new NullPointerException("input");
        if(count<0)throw new IllegalArgumentException("negative byte count");
        ByteArrayOutputStream out=new ByteArrayOutputStream(Math.min(count,8192));
        byte[] block=new byte[Math.min(count,8192)];
        while(out.size()<count){
            int n=input.read(block,0,Math.min(block.length,count-out.size()));
            if(n<0)break;
            if(n==0){int one=input.read();if(one<0)break;out.write(one);}
            else out.write(block,0,n);
        }
        return out.toByteArray();
    }
    private PcResourceBytes(){}
}
