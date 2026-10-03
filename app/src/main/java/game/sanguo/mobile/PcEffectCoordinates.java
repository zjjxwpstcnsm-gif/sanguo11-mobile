package game.sanguo.mobile;

/** Source D3DX row-vector camera <-> Filament column-vector camera boundary.
 * Source x/z origin is the inherited PC half-grid origin; source units use .05.
 * No authority or RNG input. This is a coordinate mapping, not PC lens calibration.
 */
final class PcEffectCoordinates {
    static final double SCALE=.05;
    private PcEffectCoordinates(){}

    static float[] camera(float[] glView,double[] glProjection,double originX,double originZ) {
        return camera(glView,glProjection,originX,originZ,1);
    }
    static float[] camera(float[] glView,double[] glProjection,double originX,double originZ,double homogeneousScale) {
        if(!Double.isFinite(homogeneousScale)||homogeneousScale<=0)throw new IllegalArgumentException("Positive homogeneous projection scale");
        if(glView.length!=16||glProjection.length!=16)throw new IllegalArgumentException("Camera matrices16");
        double tx=-28.5-originX,tz=-28.5-originZ;
        double[] view=new double[16];
        for(int col=0;col<3;col++)for(int row=0;row<3;row++)
            view[col*4+row]=glView[col*4+row];
        // Original441ab0 builds an RH view (eye-target), as does GL lookAt.
        for(int row=0;row<3;row++)view[12+row]=
            (glView[row]*tx+glView[8+row]*tz+glView[12+row])/SCALE;
        view[15]=1;
        double[] inverse=new double[16];
        for(int col=0;col<3;col++)for(int row=0;row<3;row++)inverse[col*4+row]=view[row*4+col];
        for(int row=0;row<3;row++)inverse[12+row]=-
            (inverse[row]*view[12]+inverse[4+row]*view[13]+inverse[8+row]*view[14]);
        inverse[15]=1;
        double[] projection=new double[16];
        for(int col=0;col<4;col++) {
            double factor=(col==3?1:SCALE)*homogeneousScale;
            projection[col*4]=glProjection[col*4]*factor;
            projection[col*4+1]=glProjection[col*4+1]*factor;
            projection[col*4+2]=(glProjection[col*4+2]+glProjection[col*4+3])*.5*factor;
            projection[col*4+3]=glProjection[col*4+3]*factor;
        }
        float[] result=new float[51];
        for(int row=0;row<3;row++)result[row]=(float)inverse[12+row];
        // Original441ab0/441b80: camera+c0 is view*projection in D3DX
        // row-vector memory, NOT inverse view.45a590 copies this to+140.
        double[] viewProjection=new double[16];
        for(int col=0;col<4;col++)for(int row=0;row<4;row++)for(int k=0;k<4;k++)
            viewProjection[col*4+row]+=projection[k*4+row]*view[col*4+k];
        for(int i=0;i<16;i++){result[3+i]=(float)view[i];result[19+i]=(float)viewProjection[i];result[35+i]=(float)projection[i];}
        for(float value:result)if(!Float.isFinite(value))throw new IllegalArgumentException("Finite source camera");
        return result;
    }

    /** Native 44d560 input precedes the D3DX transpose for shader upload.
     * Its row-vector world transform has the same flat memory as GL column-major.
     */
    static void vertex(java.nio.ByteBuffer packet,int offset,int vertex,double originX,double originZ,float[] output,int target) {
        int v=offset+24+vertex*24,m=offset+120;
        float x=packet.getFloat(v),y=packet.getFloat(v+4),z=packet.getFloat(v+8);
        for(int row=0;row<3;row++) {
            float world=x*packet.getFloat(m+row*4)+y*packet.getFloat(m+16+row*4)
                +z*packet.getFloat(m+32+row*4)+packet.getFloat(m+48+row*4);
            output[target+row]=(float)(world*SCALE-(row==0?28.5+originX:row==2?28.5+originZ:0));
        }
        int color=packet.getInt(v+12);
        output[target+3]=((color>>>16)&255)/255f;
        output[target+4]=((color>>>8)&255)/255f;
        output[target+5]=(color&255)/255f;
        output[target+6]=(color>>>24)/255f;
        output[target+7]=packet.getFloat(v+16);output[target+8]=packet.getFloat(v+20);
    }
}
