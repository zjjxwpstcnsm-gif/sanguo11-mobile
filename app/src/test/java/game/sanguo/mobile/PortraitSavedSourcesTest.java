package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;
import java.io.*;

/** Independent completed metadata reader comparison; no production save or rules changes. */
public final class PortraitSavedSourcesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static Map<Integer,String> names(World world){Map<Integer,String> names=new HashMap<>();for(var officer:world.officers)names.put(officer.id,officer.name);return names;}
    private static void rejects(byte[] raw,Map<Integer,String> names,String label){boolean failed=false;try{PortraitSavedSources.read(raw,names);}catch(Exception expected){failed=true;}check(failed,label);}
    public static void main(String[] args)throws Exception{
        World initial=ScenarioCatalog.load("coalition-190",0,20260923L);byte[] clean=SaveCodec.encode(initial);int sources=0,rows=0;
        check(PortraitSavedSources.read(null,names(initial)).isEmpty(),"missing source stays unknown");
        for(var source:PcOfficerSources.all()){
            World world=SaveCodec.decode(clean);PcOfficerSources.attachOpening(world,source.id);byte[] before=SaveCodec.encode(world),raw=world.extensions.get(PcOfficerInfo.NAMESPACE);Map<Integer,String> current=names(world);
            var independent=PcOfficerInfo.saved(world);var projected=PortraitSavedSources.read(raw,current);check(projected.size()==independent.size(),"all exact source rows "+source.path);
            for(var person:independent.values()){
                var media=projected.get(person.officerId);check(media!=null&&media.officerId==person.officerId&&media.nativeId==person.nativeId&&media.sourceVariant.equals(person.sourceVariant)&&media.sourcePath.equals(person.sourcePath)&&media.sourceSha.equals(person.sourceSha)&&media.recordSha.equals(person.recordSha),"completed reader exact media identity "+person.officerId);rows++;
            }
            var first=independent.values().iterator().next();Map<Integer,String> changed=new HashMap<>(current);changed.put(first.officerId,"unapproved changed identity");check(!PortraitSavedSources.read(raw,changed).containsKey(first.officerId),"same identity-change rejection as OfficerQuery");changed.remove(first.officerId);check(!PortraitSavedSources.read(raw,changed).containsKey(first.officerId),"missing current officer rejected");
            rejects(Arrays.copyOf(raw,raw.length-1),current,"truncated namespace rejected");byte[] tail=Arrays.copyOf(raw,raw.length+1);rejects(tail,current,"unknown tail rejected");byte[] version=raw.clone();version[3]^=1;rejects(version,current,"unknown namespace version rejected");byte[] id=raw.clone();Arrays.fill(id,12,16,(byte)0xff);rejects(id,current,"invalid native ID rejected");byte[] extent=raw.clone();Arrays.fill(extent,16,20,(byte)0x7f);rejects(extent,current,"oversize UTF field rejected");byte[] utf=raw.clone();utf[20]=(byte)0xff;rejects(utf,current,"malformed UTF rejected");
            check(Arrays.equals(before,SaveCodec.encode(world)),"all projection checks preserve complete save/RNG");sources++;
        }
        check(sources==16,"all16 completed source namespaces");System.out.println("PASS saved media projection checks="+checks+" sources="+sources+" rows="+rows+"; exact completed metadata reader/identity invalidation/full Save/RNG");
    }
}
