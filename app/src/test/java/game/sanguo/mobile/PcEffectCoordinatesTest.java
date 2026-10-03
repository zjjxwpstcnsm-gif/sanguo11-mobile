package game.sanguo.mobile;
import java.nio.*;
import java.nio.file.*;

/** Independent look-at and analytic screen/depth reference, plus original
 * D3DX source-machine vertex fixture supplied by the Python checker. */
public final class PcEffectCoordinatesTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static double[] transform(double[] m,double[] v){double[] result=new double[4];for(int row=0;row<4;row++)for(int col=0;col<4;col++)result[row]+=m[col*4+row]*v[col];return result;}
    static double[] transform(float[] m,int offset,double[] v){double[] d=new double[16];for(int i=0;i<16;i++)d[i]=m[offset+i];return transform(d,v);}
    static void near(double a,double b,double tolerance,String message){check(Math.abs(a-b)<=tolerance,message+" "+a+"/"+b);}
    public static void main(String[] args)throws Exception {
        for(double origin:new double[]{0,63,-42})for(int tilt:new int[]{40,55,70})for(int yaw:new int[]{0,35,90,180,265,359})for(int facing:new int[]{-1,1}) {
            double right=Math.cos(Math.toRadians(yaw))*facing,back=Math.sin(Math.toRadians(yaw))*facing;
            double s=Math.sin(Math.toRadians(tilt)),c=Math.cos(Math.toRadians(tilt)),focusX=43,focusZ=51;
            // Standard independent orthonormal look-at: right, up, backwards.
            float[] glView={(float)right,(float)(-back*s),(float)(back*c),0,
                0,(float)c,(float)s,0,(float)-back,(float)(-right*s),(float)(right*c),0,
                (float)(-right*focusX+back*focusZ),(float)((back*focusX+right*focusZ)*s),
                (float)(-(back*focusX+right*focusZ)*c-300),1};
            double span=17,aspect=1080.0/1232,near=.1,far=1000;
            double[] projection={1/(span*aspect),0,0,0,0,1/span,0,0,0,0,-2/(far-near),0,0,0,-(far+near)/(far-near),1};
            float[] camera=PcEffectCoordinates.camera(glView,projection,origin,origin*.7);
            for(int i=0;i<100;i++) {
                double x=focusX+(i%10-5)*1.7,z=focusZ+(i/10-5)*2.1,y=i%7*.4;
                double[] source={(x+28.5+origin)/.05,y/.05,(z+28.5+origin*.7)/.05,1};
                double[] inView=transform(camera,3,source),clip=transform(camera,35,inView);
                near(clip[0]/clip[3],((x-focusX)*right-(z-focusZ)*back)/(span*aspect),3e-5,"source screen X");
                near(clip[1]/clip[3],(-((x-focusX)*back+(z-focusZ)*right)*s+y*c)/span,3e-5,"source screen Y");
                double forward=300-((x-focusX)*back+(z-focusZ)*right)*c-y*s;
                near(clip[2]/clip[3],(forward-near)/(far-near),3e-6,"source D3D depth0..1");
                double[] direct=transform(camera,19,source);
                for(int axis=0;axis<3;axis++)near(direct[axis]/direct[3],clip[axis]/clip[3],3e-5,"source+c0 combined view projection");
            }
        }
        ByteBuffer fixture=ByteBuffer.wrap(Files.readAllBytes(Path.of(args[0]))).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer expected=ByteBuffer.wrap(Files.readAllBytes(Path.of(args[1]))).order(ByteOrder.LITTLE_ENDIAN);
        fixture.position(16);
        while(fixture.hasRemaining()) {
            int count=fixture.getInt(fixture.position()+12);fixture.position(fixture.position()+32);
            for(int i=0;i<count;i++)for(int v=0;v<4;v++) {
                float[] actual=new float[9];PcEffectCoordinates.vertex(fixture,fixture.position()+i*184,v,63,44.1,actual,0);
                for(int axis=0;axis<3;axis++)near(actual[axis],expected.getFloat(),.00005,"original D3DX transformed source vertex");
                int color=fixture.getInt(fixture.position()+i*184+24+v*24+12);
                for(int channel=0;channel<4;channel++){int shift=new int[]{16,8,0,24}[channel];near(actual[3+channel],((color>>>shift)&255)/255f,0,"exact BGRA color");}
                near(actual[7],fixture.getFloat(fixture.position()+i*184+24+v*24+16),0,"exact U");
                near(actual[8],fixture.getFloat(fixture.position()+i*184+24+v*24+20),0,"exact V");
            }
            fixture.position(fixture.position()+count*184);
        }
        check(!expected.hasRemaining(),"complete source vertex fixture consumed");
        ByteBuffer nativeCameras=ByteBuffer.wrap(Files.readAllBytes(Path.of(args[2]))).order(ByteOrder.LITTLE_ENDIAN);
        int cameras=0;
        while(nativeCameras.hasRemaining()) {
            double originX=nativeCameras.getDouble(),originZ=nativeCameras.getDouble(),homogeneousScale=nativeCameras.getDouble();float[] view=new float[16];double[] projection=new double[16];
            for(int i=0;i<16;i++)view[i]=nativeCameras.getFloat();for(int i=0;i<16;i++)projection[i]=nativeCameras.getDouble();
            float[] mapped=PcEffectCoordinates.camera(view,projection,originX,originZ,homogeneousScale);
            for(int i=0;i<51;i++) {
                float original=nativeCameras.getFloat();
                // Original source eye/target inputs are float32 before its
                // normalization. GL view is rounded after its independent
                // look-at; source translation comparison allows three source
                // units (.00015 scene units), rotation2e-6 and clip4e-5.
                double tolerance=i<3?.006:i>=15&&i<=17?.003:i<19?2e-6:i<35?4e-5:1e-6;
                near(mapped[i],original,tolerance,"source441ab0/441b80 camera field"+i);
            }
            cameras++;
        }
        check(cameras==1188,"all independent original camera construction cases");
        System.out.println("PASS PC effect coordinates "+checks+" checks; RH camera origin/rotation/combined view projection/depth, original D3DX vertices, BGRA/UV; HOST ONLY, no PC visual acceptance");
    }
}
