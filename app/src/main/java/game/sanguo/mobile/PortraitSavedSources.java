package game.sanguo.mobile;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.Pattern;

/** Media-only projection of immutable pc-officer-source-v1 bytes, matching completed PcOfficerInfo validation. */
final class PortraitSavedSources {
    private static final Pattern SHA=Pattern.compile("[0-9a-f]{64}"),HEX=Pattern.compile("(?:[0-9a-f]{2})*");
    private PortraitSavedSources(){}
    static Map<Integer,PortraitMediaIdentity> read(byte[] raw,Map<Integer,String> currentNames)throws IOException{
        if(raw==null)return Collections.emptyMap();
        if(raw.length>16*1024*1024)throw new IOException("Portrait saved source extent");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
        if(in.readInt()!=0x50434f31)throw new IOException("Unknown portrait source namespace version");
        int count=bounded(in.readInt(),0,10000);Map<Integer,PortraitMediaIdentity> result=new HashMap<>();Set<Integer> seen=new HashSet<>();
        for(int i=0;i<count;i++){
            int id=bounded(in.readInt(),0,1000000),nativeId=bounded(in.readInt(),0,1099);String[] s=new String[10];
            for(int j=0;j<10;j++)s[j]=text(in,j==7?32768:1024);
            if(s[0].isEmpty()||s[1].isEmpty()||s[2].isEmpty()||!SHA.matcher(s[3]).matches()||!SHA.matcher(s[4]).matches()||!HEX.matcher(s[6]).matches()||!SHA.matcher(s[8]).matches()||!s[9].isEmpty()&&!SHA.matcher(s[9]).matches()||!seen.add(id))throw new IOException("Saved portrait source identity/duplicate rejected");
            int unknown=bounded(in.readInt(),0,100);for(int j=0;j<unknown;j++)text(in,256);
            // Same approved saved-identity invalidation as OfficerQuery, never infer native IDs from a name.
            if(s[0].equals(currentNames.get(id)))result.put(id,new PortraitMediaIdentity(id,nativeId,s[1],s[2],s[3],s[4]));
        }
        if(in.available()!=0)throw new IOException("Unknown saved portrait source tail");
        return Collections.unmodifiableMap(result);
    }
    private static int bounded(int n,int min,int max)throws IOException{if(n<min||n>max)throw new IOException("Saved portrait field extent");return n;}
    private static String text(DataInputStream in,int max)throws IOException{
        int size=bounded(in.readInt(),0,max);if(size>in.available())throw new IOException("Truncated saved portrait text");byte[] raw=new byte[size];in.readFully(raw);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(raw)).toString();
    }
}
