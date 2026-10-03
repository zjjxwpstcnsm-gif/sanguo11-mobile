package game.sanguo.mobile;

import java.nio.*;

/** Lossless GPU index encoding. CPU mesh/topology and draw offsets stay unchanged. */
final class MeshIndexBuffer {
    static boolean compact(int vertices){return vertices>0&&vertices<=65536;}
    static long bytes(int vertices,int indices){return (long)indices*(compact(vertices)?2:4);}
    static Buffer encode(int vertices,int[] indices){
        for(int index:indices)if(index<0||index>=vertices)throw new IllegalArgumentException("mesh index outside vertex buffer");
        ByteBuffer bytes=ByteBuffer.allocateDirect(Math.toIntExact(bytes(vertices,indices.length))).order(ByteOrder.nativeOrder());
        if(compact(vertices)){
            ShortBuffer out=bytes.asShortBuffer();for(int index:indices)out.put((short)index);out.flip();return out;
        }
        IntBuffer out=bytes.asIntBuffer();out.put(indices);out.flip();return out;
    }
}
