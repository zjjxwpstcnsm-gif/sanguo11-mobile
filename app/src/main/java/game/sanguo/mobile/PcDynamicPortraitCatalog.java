package game.sanguo.mobile;

import java.io.*;
import java.util.*;

/** Compact, immutable original fullscreen lookup. No names, mutable rules or RNG. */
final class PcDynamicPortraitCatalog {
    private static final class Row {
        final PortraitMediaIdentity identity;
        final int face,birth,threshold,current,older;
        Row(PortraitMediaIdentity identity,int face,int birth,int threshold,int current,int older){
            this.identity=identity;this.face=face;this.birth=birth;this.threshold=threshold;this.current=current;this.older=older;
        }
    }
    private final Map<String,Row> rows;
    private PcDynamicPortraitCatalog(Map<String,Row> rows){this.rows=Collections.unmodifiableMap(rows);}
    static PcDynamicPortraitCatalog read(InputStream input)throws IOException {
        java.security.MessageDigest digest;
        try{digest=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException error){throw new IOException(error);}
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new java.security.DigestInputStream(input,digest)))){
            byte[] magic=new byte[8];in.readFully(magic);
            if(!Arrays.equals(magic,new byte[]{'P','C','D','Y','0','0','0','1'}))throw new IOException("Dynamic portrait schema");
            int variants=in.readInt(),count=in.readInt();
            if(variants!=16||count!=10656)throw new IOException("Dynamic portrait source coverage");
            String[][] sources=new String[variants][3];
            for(int i=0;i<variants;i++){sources[i][0]=text(in);sources[i][1]=text(in);sources[i][2]=hash(in);}
            Map<String,Row> rows=new HashMap<>();
            for(int i=0;i<count;i++){
                int id=in.readInt(),nativeId=in.readInt(),variant=in.readUnsignedShort(),face=in.readInt(),birth=in.readShort(),threshold=in.readUnsignedByte(),current=in.readUnsignedShort(),older=in.readUnsignedShort();
                String record=hash(in);
                if(variant>=variants||face<0||face>=2200||current<131||current>192||older<131||older>192)throw new IOException("Invalid original dynamic row");
                String[] source=sources[variant];PortraitMediaIdentity identity;
                try{identity=new PortraitMediaIdentity(id,nativeId,source[0],source[1],source[2],record);}catch(IllegalArgumentException error){throw new IOException("Invalid dynamic identity",error);}
                if(rows.put(identity.key(),new Row(identity,face,birth,threshold,current,older))!=null)throw new IOException("Duplicate dynamic identity");
            }
            if(in.read()!=-1)throw new IOException("Trailing dynamic portrait bytes");
            if(!hash(new DataInputStream(new ByteArrayInputStream(digest.digest()))).equals("d39442ce2b53d00eea2be9f32e5b02fb4a11d7107edd394daefb8ba5d54fc141"))throw new IOException("Unexamined dynamic portrait source");
            return new PcDynamicPortraitCatalog(rows);
        }
    }
    private static String text(DataInputStream in)throws IOException {int length=in.readUnsignedShort();if(length==0||length>4096)throw new IOException("Dynamic source string extent");byte[] raw=new byte[length];in.readFully(raw);return new String(raw,java.nio.charset.StandardCharsets.UTF_8);}
    private static String hash(DataInputStream in)throws IOException {byte[] raw=new byte[32];in.readFully(raw);char[] text=new char[64];char[] digits="0123456789abcdef".toCharArray();for(int i=0;i<32;i++){text[2*i]=digits[(raw[i]&255)>>>4];text[2*i+1]=digits[raw[i]&15];}return new String(text);}
    int selector(PortraitMediaIdentity identity,int appliedYear){
        if(identity==null)return -1;Row row=rows.get(identity.key());
        if(row==null||!row.identity.sourcePath.equals(identity.sourcePath)||!row.identity.sourceSha.equals(identity.sourceSha)||!row.identity.recordSha.equals(identity.recordSha))return -1;
        // Original native age output changes only faces in the young bank.
        return row.face<1000&&(long)appliedYear-row.birth+1>=row.threshold?row.older:row.current;
    }
    int size(){return rows.size();}
}
