package game.sanguo.mobile;

import java.io.*;
import java.nio.*;

/** Original SENV states. Display data only: no rule state, RNG or save access. */
final class PcEnvironment {
    private final float[][] directions=new float[24][3],ambient=new float[24][3];
    private final int[] fogColor=new int[24],fogStart=new int[24],fogEnd=new int[24],fogDensity=new int[24],fadeStart=new int[24];
    PcEnvironment(InputStream source)throws IOException {
        byte[] raw;
        try(InputStream in=source;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[256];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>968)throw new IOException("PC SENV budget");out.write(block,0,n);}
            raw=out.toByteArray();
        }
        if(raw.length!=968)throw new IOException("PC SENV length");
        ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        if(b.getLong()!=0x32303030564e4553L)throw new IOException("PC SENV header");
        for(int state=0;state<24;state++){
            for(int i=0;i<3;i++)directions[state][i]=b.getFloat();
            double length=0;
            for(float v:directions[state]){if(!Float.isFinite(v))throw new IOException("PC SENV direction");length+=(double)v*v;}
            if(length<1e-8f)throw new IOException("PC SENV zero direction");
            length=Math.sqrt(length);
            // 442e50 normalizes/negates into light column0. Double intermediates
            // match observed autumn/winter c18; a float length changes1ULP.
            for(int i=0;i<3;i++)directions[state][i]=(float)(-directions[state][i]/length);
            for(int i=0;i<3;i++){ambient[state][i]=b.getFloat();if(!Float.isFinite(ambient[state][i]))throw new IOException("PC SENV ambient");}
            fogColor[state]=b.getInt();fogStart[state]=b.get()&255;fogEnd[state]=b.get()&255;fogDensity[state]=b.get()&255;fadeStart[state]=b.get()&255;
            if(fogStart[state]>=fogEnd[state]||fogEnd[state]>100||fadeStart[state]>=100)throw new IOException("PC SENV distances");
            b.position(b.position()+8); // Source tail colors remain in the immutable asset.
        }
    }
    private int baseState(int month){
        if(month<1||month>12)throw new IllegalArgumentException("PC SENV month");
        return ((month-1)/3)*6;
    }
    float[] baseAmbient(int month){return ambient[baseState(month)].clone();}
    float[] baseFogColor(int month){int c=fogColor[baseState(month)];return new float[]{((c>>>16)&255)/255f,((c>>>8)&255)/255f,(c&255)/255f};}
    /** Float intermediates reproduce observed x87 control0x007f. Original
     * 5a2530 multiplies percentage after delta; 441d30 multiplies before delta.
     * Returned c26.xyz,c27.xy use source units. No camera/rule/save writes. */
    float[] groundFog(int month,float near,float far){
        int s=baseState(month);if(!Float.isFinite(near)||!Float.isFinite(far)||far<=near)throw new IllegalArgumentException("PC SENV lens");
        float delta=far-near;
        float start=near+(fogStart[s]*delta)*.01f,end=near+(fogEnd[s]*delta)*.01f;
        float fogReciprocal=1f/(end-start);
        int density=Math.min(255,(int)(fogDensity[s]*2.55f));
        float fade=near+(fadeStart[s]*.01f)*delta,fadeReciprocal=1f/(far-fade);
        return new float[]{fogReciprocal,fogReciprocal*end,(255-density)*(1f/255f),fadeReciprocal,fadeReciprocal*far};
    }
    float[] baseDirection(int month){
        if(month<1||month>12)throw new IllegalArgumentException("PC SENV month");
        return directions[baseState(month)].clone();
    }
}
