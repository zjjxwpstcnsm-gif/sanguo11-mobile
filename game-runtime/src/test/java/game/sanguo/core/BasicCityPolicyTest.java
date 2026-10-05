package game.sanguo.core;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Real saved policy/preview/ordinary command checks; does not replace APK QA. */
public final class BasicCityPolicyTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        World legacy;
        try(InputStream in=BasicCityPolicyTest.class.getResourceAsStream("/architecture/prepared-9548bb35-v33.sg11")){
            if(in==null)throw new AssertionError("genuine historical fixture missing");legacy=SaveCodec.read(in);
        }
        byte[] before=SaveCodec.encode(legacy);check(ByteBuffer.wrap(before).getInt(4)==33&&legacy.extensions.get(BasicCityPolicy.NAMESPACE)==null&&!legacy.officerAbilities.enabled(),"legacy v33 keeps no marker/base inference");
        World.City home=legacy.home();World.Officer actor=legacy.idle(home).get(0);
        CityActionPlan preview=legacy.strategy.previewCityAction(CityActionPlan.Operation.RECRUIT,home.id,actor.id,new int[0]);
        check(preview.allowed()&&preview.actionPointsCost==10,"old recruit admission and preview are10AP");
        check(Arrays.equals(before,SaveCodec.encode(legacy)),"old preview does not backfill policy or change save/RNG");
        int ap=legacy.actionPoints[legacy.player];check(legacy.recruit(home.id,actor.id).ok&&legacy.actionPoints[legacy.player]==ap-10,"old normal recruit debits exactly10AP");
        check(legacy.extensions.get(BasicCityPolicy.NAMESPACE)==null&&!legacy.officerAbilities.enabled(),"old successful command does not upgrade policy/base/XP");
        actor=legacy.idle(home).get(0);int gain=Math.min(100-home.order,5+(Math.min(100,actor.politics)+Math.min(100,actor.charm))/20);
        preview=legacy.strategy.previewCityAction(CityActionPlan.Operation.PATROL,home.id,actor.id,new int[0]);
        check(preview.actionPointsCost==10&&preview.effects.orderAfter==home.order+gain,"old patrol preview follows saved legacy model");
        int order=home.order;ap=legacy.actionPoints[legacy.player];check(legacy.patrol(home.id,actor.id).ok&&home.order==order+gain&&legacy.actionPoints[legacy.player]==ap-10,"old patrol effect and debit preserve genuine source policy");
        byte[] committed=SaveCodec.encode(legacy);check(Arrays.equals(committed,SaveCodec.encode(SaveCodec.decode(committed))),"old policy survives full save/load without marker insertion");
        World authored=new World(10,10);authored.cities.add(new World.City(1,"甲",new Hex(2,2),0));authored.cities.add(new World.City(2,"乙",new Hex(7,7),1));authored.officers.add(new World.Officer(1,"甲将",0,1,50,50,50,50,50));authored.officers.add(new World.Officer(2,"乙将",1,2,50,50,50,50,50));authored.strategy.initializeOffices();
        byte[] fresh=SaveCodec.encode(authored);World copy=SaveCodec.decode(fresh);check(BasicCityPolicy.nativeRules(copy)&&Arrays.equals(fresh,SaveCodec.encode(copy)),"fresh unmanaged authoring stores native policy across transaction copies");
        for(String id:List.of("coalition-190","heroes-250","central-mobile-sandbox")){
            World current=ScenarioCatalog.load(id,0,23);before=SaveCodec.encode(current);check(ByteBuffer.wrap(before).getInt(4)==37&&current.extensions.get(BasicCityPolicy.NAMESPACE)==null&&BasicCityPolicy.nativeRules(current),"saved managed37 policy unchanged/no additional marker");
            check(Arrays.equals(before,SaveCodec.encode(SaveCodec.decode(before))),"managed37 roundtrip retains saved policy");
        }
        System.out.println("PASS BasicCityPolicyTest "+checks+" historical33/native37/full-save policy checks");
    }
}
