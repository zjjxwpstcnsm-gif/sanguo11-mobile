package game.sanguo.core;
import java.util.*;
import java.io.*;

/** Actual original held items, ordinary confiscate/reward and full persistence. */
public final class PcNativeItemPolicyTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        int sources=0;for(var source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);var runtime=PcDuelRuntimeFacts.saved(w);var ids=PcNativeItemPolicy.identities(w);check(ids.size()==43,"all original native identities");Map<Integer,Integer>people=new HashMap<>();for(var p:PcDuelSourceFacts.saved(w).values())people.put(p.nativeId,p.officerId);
            for(var item:runtime.items.values())if(item.valid&&item.nativeId<43&&item.initialOwner>=0){int id=people.get(item.initialOwner);int[][]held=PcNativeItemPolicy.held(w,id);check(w.life.present(id)?Arrays.stream(held).anyMatch(v->v[0]==item.nativeId&&v[1]==item.kind):held.length==0,"actualsource holder join without inactive activation");}
            byte[]before=SaveCodec.encode(w);World copy=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(copy)),"complete sourceitems/World/allRNG cold persistence");
            w.extensions.put(PcNativeItemPolicy.NAMESPACE,null);byte[]old=SaveCodec.encode(w);World legacy=SaveCodec.decode(old);check(!PcNativeItemPolicy.enabled(legacy)&&Arrays.equals(old,SaveCodec.encode(legacy)),"old no namespace retains ownership");
            legacy.extensions.put(PcNativeItemPolicy.NAMESPACE,new byte[]{1,2,3,4});byte[]future=SaveCodec.encode(legacy);World opaque=SaveCodec.decode(future);check(!PcNativeItemPolicy.enabled(opaque)&&Arrays.equals(future,SaveCodec.encode(opaque)),"unknown no adoption");sources++;
        }
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var held=w.treasures.items().stream().filter(i->i.definition.id.equals("item-030")).findFirst().orElseThrow();int ownerId=held.holder;World.Officer owner=w.officer(ownerId);int city=owner.cityId;
        final World initial=w;World.Officer actor=w.officers.stream().filter(o->o.owner==initial.player&&o.cityId==city&&o.unitId==-1&&!o.acted&&!initial.government.captive(o.id)&&!initial.strategy.busy(o.id)&&!initial.domestic.busy(o.id)).findFirst().orElseThrow();
        check(w.treasures.confiscate(city,actor.id,held.definition.id).ok,"ordinary original heldbook confiscation");check(PcNativeItemPolicy.held(w,ownerId).length==0,"source initial book not refilled");byte[]save=SaveCodec.encode(w);w=SaveCodec.decode(save);check(Arrays.equals(save,SaveCodec.encode(w)),"ordinary confiscation fullWorld/RNG reload");
        final World current=w;actor=w.officers.stream().filter(o->o.owner==current.player&&o.cityId==city&&o.unitId==-1&&!o.acted&&!current.government.captive(o.id)&&!current.strategy.busy(o.id)&&!current.domestic.busy(o.id)).findFirst().orElseThrow();check(w.treasures.award(city,actor.id,held.definition.id,ownerId).ok,"ordinary reward source book from treasury");boolean unknownRaw=false;try{PcDuelRawLoyalty.current(w,ownerId);}catch(IOException expected){unknownRaw=true;}check(unknownRaw,"ordinary capped item reward marks unproven raw mutation unknown");check(Arrays.stream(PcNativeItemPolicy.held(w,ownerId)).anyMatch(v->v[0]==30&&v[1]==5),"current exactnative heldbook afterreward");save=SaveCodec.encode(w);check(Arrays.equals(save,SaveCodec.encode(SaveCodec.decode(save))),"wholeWorld/allRNG reward persistence");check(sources==16,"all16distinctsources");System.out.println("PASS original item identity/current ownership "+checks+" checks; normal original duel admission and hiddenitems still pending");
    }
}
