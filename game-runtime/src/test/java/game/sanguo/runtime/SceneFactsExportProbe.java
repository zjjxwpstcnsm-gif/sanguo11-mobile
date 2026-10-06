package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.query.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

/** Export only a fresh test-authored installed save; the supplied token was recorded live. */
public final class SceneFactsExportProbe {
    private static String quote(String value){
        StringBuilder b=new StringBuilder("\"");
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(c=='"'||c=='\\')b.append('\\').append(c);
            else if(c<32)b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));
            else b.append(c);
        }
        return b.append('"').toString();
    }
    private static String json(Object value)throws Exception{
        if(value==null)return "null";
        if(value instanceof String)return quote((String)value);
        if(value instanceof Number||value instanceof Boolean)return value.toString();
        StringJoiner entries=new StringJoiner(",");
        if(value instanceof Map){
            for(var e:((Map<?,?>)value).entrySet())entries.add(quote(e.getKey().toString())+":"+json(e.getValue()));
            return "{"+entries+"}";
        }
        if(value instanceof Iterable){for(Object element:(Iterable<?>)value)entries.add(json(element));return "["+entries+"]";}
        Field[] fields=value.getClass().getFields();Arrays.sort(fields,Comparator.comparing(Field::getName));
        for(Field f:fields)if(!Modifier.isStatic(f.getModifiers()))entries.add(quote(f.getName())+":"+json(f.get(value)));
        return "{"+entries+"}";
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=4)throw new IllegalArgumentException("fresh save path, recorded sessionId/generation/revision");
        byte[] raw=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(raw);byte[] before=SaveCodec.encode(w);
        Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("byteIdenticalHostEncoding",Arrays.equals(raw,before));
        if(!Arrays.equals(raw,before)){
            if(raw.length!=before.length||!Arrays.equals(Arrays.copyOf(raw,12),Arrays.copyOf(before,12)))throw new AssertionError("Installed save length/header changed");
            int different=-1;
            for(int i=20;i<raw.length;i++)if(raw[i]!=before[i]){if(different>=0)throw new AssertionError("Multiple payload differences");different=i;}
            int start=different-9;
            if(start<24||raw[different]!=0||before[different]!=(byte)255||raw[start]!=(byte)31||raw[start+1]!=(byte)139||raw[start+2]!=8||raw[start+3]!=0)
                throw new AssertionError("Unexplained installed save payload difference");
            int length=ByteBuffer.wrap(raw,start-4,4).getInt();
            if(length<18||start+length+4>raw.length||ByteBuffer.wrap(raw,start+length,4).getInt()!=0x47563637)
                throw new AssertionError("Difference is not the typed report envelope");
            byte[] left,right;
            try(InputStream in=new GZIPInputStream(new ByteArrayInputStream(raw,start,length))){left=in.readAllBytes();}
            try(InputStream in=new GZIPInputStream(new ByteArrayInputStream(before,start,length))){right=in.readAllBytes();}
            if(!Arrays.equals(left,right))throw new AssertionError("Report content changed");
            envelope.put("scope","report gzip OS byte only; outer save CRC changes accordingly; raw installed bytes retained");
            envelope.put("offset",different);envelope.put("installedGzipOS",0);envelope.put("hostGzipOS",255);envelope.put("decompressedReportByteIdentical",true);
        }
        StateToken token=new StateToken(args[1],Long.parseLong(args[2]),Long.parseLong(args[3]));
        SceneFactsSnapshot scene=SceneFactsQuery.capture(w,token);OfficerSnapshot officers=OfficerQuery.capture(w,token);
        List<War.Structure> active=new ArrayList<>();for(War.Structure s:w.war.structures())if(s.builder>=0&&!s.complete)active.add(s);
        if(active.size()!=1||w.unit(active.get(0).builder)==null)throw new AssertionError("Unique actual construction builder required");
        OfficerSnapshot.Officer builder=officers.officer(w.unit(active.get(0).builder).officerId);
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Export changed World/RNG");
        StringJoiner hash=new StringJoiner("");for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))hash.add(String.format(Locale.ROOT,"%02x",b&255));
        Map<String,Object> result=new LinkedHashMap<>();result.put("origin","decoded exact own installed normal construction save; token recorded by live serial API, not a new live session");result.put("saveSha256",hash.toString());result.put("encodingEnvelope",envelope);result.put("fullWorldBothRngPure",true);result.put("scene",scene);result.put("builderOfficer",builder);result.put("originalSpeechCaller","unknown");
        System.out.println(json(result));
    }
}
