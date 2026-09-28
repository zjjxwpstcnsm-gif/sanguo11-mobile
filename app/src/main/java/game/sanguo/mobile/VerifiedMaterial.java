package game.sanguo.mobile;

import java.io.*;
import java.security.*;
import java.util.*;

/** Pinned, bounded material input before native parsing; not a driver compatibility claim. */
final class VerifiedMaterial {
    private static final Map<String,String> EXPECTED = new HashMap<>();
    static {
        EXPECTED.put("3d/field/unit.filamat", "5e4330b35069f7e21a2bfad2c456d6af6b8dfced35440e7c56ccaf6bf497034a");
        EXPECTED.put("3d/sites/site.filamat", "b4489c980aa59f8094b951874dc6550345f98a263db9fd64c100aefb05977b11");
        EXPECTED.put("3d/terrain/ground-overview.filamat", "6a1c2b3aa616aa30778e5397ef25f3e919e3937e408e623c633d843ba374eb5e");
        EXPECTED.put("3d/terrain/ground.filamat", "f42aa786ea9fff5c9f8f43c7eb103671b11506ff5890fcd05aa3d52038c959bb");
        EXPECTED.put("3d/terrain/water-overview.filamat", "d05bd27fe805db7adf13c1fe74ec9305f18773258a98530dcb5e25283e110d79");
        EXPECTED.put("3d/terrain/water.filamat", "f0c27cebf48137e12c7f50d6e26a4355ea533c64abde06d0a99f5d143b74c21c");
        EXPECTED.put("3d/terrain.filamat", "ec5a3a545265a4a9caf6c6de0ef378e6e98aaf64bae96de1746035f4087714d7");
    }
    static byte[] read(String path, InputStream input) throws IOException {
        try (InputStream in=input; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            if(in==null || !EXPECTED.containsKey(path)) throw new IOException("Unknown/missing 3D material: "+path);
            byte[] block=new byte[8192]; int n,total=0;
            while((n=in.read(block))!=-1){
                total+=n;
                if(total>4*1024*1024)throw new IOException("3D material exceeds 4 MiB: "+path);
                out.write(block,0,n);
            }
            byte[] bytes=out.toByteArray();
            try {
                byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
                StringBuilder hex=new StringBuilder(64);
                for(byte b:digest)hex.append(String.format(Locale.ROOT,"%02x",b&255));
                if(!EXPECTED.get(path).equals(hex.toString()))throw new IOException("3D material checksum mismatch: "+path);
            } catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
            return bytes;
        }
    }
    private VerifiedMaterial() {}
}
