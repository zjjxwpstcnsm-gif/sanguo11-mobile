package game.sanguo.core;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Actual new-source loader versus independent original executable receipts. */
public final class PcDuelKinshipOpeningTest {
    private static long checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static String hash(byte[] raw)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));}
    private static Object field(byte[]raw,String key)throws IOException {
        // Receipt timing is fractional; the production data-only parser rightly
        // rejects it. Parse only a uniquely named original evidence field.
        String text=new String(raw,java.nio.charset.StandardCharsets.UTF_8);
        var match=java.util.regex.Pattern.compile("\""+key+"\"\\s*:").matcher(text);
        if(!match.find())throw new IOException("Original receipt field missing: "+key);
        int start=match.end();if(match.find())throw new IOException("Original receipt field duplicated: "+key);
        while(Character.isWhitespace(text.charAt(start)))start++;
        int depth=0,end=start;boolean quoted=false,escaped=false;
        for(;end<text.length();end++){
            char c=text.charAt(end);
            if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='\"')quoted=false;continue;}
            if(c=='\"'){quoted=true;continue;}
            if(c=='{'||c=='[')depth++;
            else if(c=='}'||c==']'){if(depth==0)break;if(--depth==0){end++;break;}}
            else if(c==','&&depth==0)break;
        }
        return MapJson.parse(text.substring(start,end).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public static void main(String[] args)throws Exception {
        Path folder=Path.of(args[0]);var sources=PcScenarioCatalog.all();check(sources.size()==16,"all distinct installed sources");
        for(int index=0;index<16;index++){
            byte[]original=Files.readAllBytes(folder.resolve(String.format(Locale.ROOT,"source-%02d.json",index)));
            var receipt=MapJson.obj("source",field(original,"source"),"exeSha",field(original,"exeSha"),"wholeWorldAndRngPure",field(original,"wholeWorldAndRngPure"),"completeGoal",field(original,"completeGoal"),"rows",field(original,"rows"),"columns",field(original,"columns"),"rowHex",field(original,"rowHex"));var source=MapJson.object(receipt.get("source"));var identity=sources.get(index).identity;
            check(MapJson.string(receipt.get("exeSha"),64).equals(PcScenarioIdentity.EXE_SHA),"original executable provenance");
            check(MapJson.bool(receipt.get("wholeWorldAndRngPure"))&&!MapJson.bool(receipt.get("completeGoal")),"completed source numeric receipt with bounded claims");
            check(MapJson.integer(receipt.get("rows"),670,670)==670&&MapJson.integer(receipt.get("columns"),670,670)==670,"source matrix dimensions");
            check(MapJson.string(source.get("scenarioId"),80).equals(identity.scenarioId)&&MapJson.string(source.get("sourceSha256"),64).equals(identity.sha)&&MapJson.string(source.get("sourceVariant"),300).equals(identity.sourceVariant),"distinct original source identity");
            World w=PcScenarioCatalog.load(identity.scenarioId,-1,23);check(PcDuelKinship.enabled(w),"actual new-source creation initializes source strategy");
            check(!PcDuelCampaignPolicy.enabled(w),"kinship data alone does not enable unfinished normal duel settlement");
            byte[]saved=SaveCodec.encode(w);World cold=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(cold)),"whole new World and both RNG cold save");
            var in=new DataInputStream(new ByteArrayInputStream(w.extensions.get(PcDuelKinship.NAMESPACE)));in.readInt();in.readInt();for(int n=0;n<4;n++)in.readUTF();check(in.readUTF().equals(hash(original)),"saved original receipt SHA");
            var rows=MapJson.array(receipt.get("rowHex"));check(rows.size()==670,"all original source rows");
            for(int own=0;own<670;own++){
                byte[]row=HexFormat.of().parseHex(MapJson.string(rows.get(own),1340));check(row.length==670,"original row bound");
                for(int target=0;target<670;target++)check(PcDuelKinship.classifier(cold,own,target)==row[target],"exact original classifier byte");
            }
            check(Arrays.equals(saved,SaveCodec.encode(cold)),"queries preserve complete World and both RNG");
            cold.extensions.put(PcDuelKinship.NAMESPACE,null);byte[]absent=SaveCodec.encode(cold);World old=SaveCodec.decode(absent);check(!PcDuelKinship.enabled(old)&&Arrays.equals(absent,SaveCodec.encode(old)),"absent old strategy remains absent with no resource lookup/backfill");
            old.extensions.put(PcDuelKinship.NAMESPACE,new byte[]{1,2,3,4});byte[]opaque=SaveCodec.encode(old);check(Arrays.equals(opaque,SaveCodec.encode(SaveCodec.decode(opaque))),"unknown future strategy remains opaque");
            System.out.println("PASS actual source opening kinship "+index+" "+identity.scenarioId);
        }
        System.out.println("PASS actual16 opening kinship "+checks+" checks; normal native duel/APK remains pending");
    }
}
