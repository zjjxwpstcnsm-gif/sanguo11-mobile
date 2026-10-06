package game.sanguo.mobile;

import java.io.*;
import java.security.*;
import java.util.*;

/** Pinned, bounded material input before native parsing; not a driver compatibility claim. */
final class VerifiedMaterial {
    private static final Map<String,String> EXPECTED = new HashMap<>();
    static {
        EXPECTED.put("3d/pc-effects/quad-add.filamat", "0adff6b5f46acacce7e5466448c288ef3d5b3359e059e8e59dd30664b5777a2c");
        EXPECTED.put("3d/pc-presentations/add-encoded.filamat", "cf52313ca0a54d9bbdd9b2d854af2226a1981b6d864c2b37b168019ead82953a");
        EXPECTED.put("3d/pc-presentations/over-encoded.filamat", "900d277b48345eb0a543f0d1f53150ec1e7d66e4d881b57d9387766186cbaef4");
        EXPECTED.put("3d/pc-presentations/backdrop.filamat", "822ddf30722f0cdbb96414e5bb6d654eb458859d3448a3266f3f664f255e8be5");
        EXPECTED.put("3d/pc-presentations/add.filamat", "b017e12af9075704304a2ef23767b1a96e92599db7ee64767fd2198f67cfb540");
        EXPECTED.put("3d/pc-presentations/over.filamat", "dd0c32816dca1a02644639561c29f8d9393fe33235facd104e86daccc7b2d87e");
        EXPECTED.put("3d/pc-map/ground-outline.filamat", "f90fb6009c3741338a9ed2d4d5b950f850f0f9787495bd1bf66029a5a4a60403");
        EXPECTED.put("3d/pc-effects/quad.filamat", "350ee8ad1ea3378ee237b6d194aff1c3253cc85a4f7421f3e2193d6027e8c5bf");
        EXPECTED.put("3d/pc-map/water.filamat", "98e39a51a8f3fdb32dedccfc4c0177a5be584770df179c9319c9cda448577b2c");
        EXPECTED.put("3d/pc-units/unit-alpha.filamat", "5cd8f2bc8a346f23312be0f8735e204767e6a5eb9522288b5ba2d46af5b78074");
        EXPECTED.put("3d/pc-units/unit.filamat", "f2d22306ef6e96a54ac28f0ef91c5b26b9b3f9cc78d51ef6efb7089ffcf9c558");
        EXPECTED.put("3d/pc-scenery/scenery.filamat", "1e1a914990de53cdc485b742cfc92b35a36be7ffdc86dd21ca659902382dbfb3");
        EXPECTED.put("3d/pc-map/ground.filamat", "8c831a3d7053c13f271e542a18ba0ab8837b2f3c800db6d9b2f41a529656dc8b");
        EXPECTED.put("3d/terrain/v129/ground-overview.filamat", "6d25524f55f25f03ba5d0fc4869095f879efd7987042711a93f20240ac4bc875");
        EXPECTED.put("3d/terrain/v129/ground.filamat", "3f78685dc70edc4fc9233136547156d51a53079bf18d1a3c974358dc1ee14f45");
        EXPECTED.put("3d/field/v128/scenery.filamat", "ba63ac82fbe0d146af851bfbf355154146510ad66d6c0fafe6b7375fe8aac572");
        EXPECTED.put("3d/field/v127/scenery.filamat", "e4dc2c510a2519379fc068d51821cba85fb45d6d2a338624f20dcc2e7d635caa");
        EXPECTED.put("3d/field/v126/scenery.filamat", "ae89f2d8d75ade2603526208acdce2d64be38510c61099ed9363996c607b8ff0");
        EXPECTED.put("3d/field/v125/scenery.filamat", "3c04cfc1161d41878ead57c499d2df76a9b1eb0cd72b082b5eadd4f6eefc6ce1");
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
