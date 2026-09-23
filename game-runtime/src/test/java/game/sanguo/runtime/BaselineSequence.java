package game.sanguo.runtime;

import game.sanguo.core.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Intentionally imports only pre-refactor core. This SAME source runs in an isolated old checkout. */
public final class BaselineSequence {
    static World prepared()throws Exception{
        World w=ScenarioCatalog.load("coalition-190",0,20260923L);
        World.City city=w.home();int builder=w.idle(city).get(0).id;
        World.Result built=w.domestic.build(city.id,builder,Domestic.Kind.BARRACKS,w.domestic.buildSites(city.id).get(0));
        if(!built.ok)throw new AssertionError(built.message);
        for(int n=0;n<3;n++)if(!w.nextTurn().ok)throw new AssertionError("baseline turn");
        if(w.domestic.capacity(city.id,Domestic.Kind.BARRACKS)<1)throw new AssertionError("real barracks not ready");
        w.reports.prepare();return w;
    }
    static int[][] operations(World w){
        List<World.Officer> officers=w.idle(w.home());int city=w.home().id;
        return new int[][]{{0,city,-1},{1,city,officers.get(0).id},{0,city,officers.get(1).id},
            {1,city,officers.get(2).id},{0,city,officers.get(1).id},{1,-1,officers.get(2).id}};
    }
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static World.Result direct(World world,int[] op,boolean nativePath){
        return nativePath?(op[0]==1?world.strategy.recruitSoldiers(op[1],op[2]):world.strategy.patrol(op[1],op[2])):
            (op[0]==1?world.recruit(op[1],op[2]):world.patrol(op[1],op[2]));
    }
    public static void main(String[] args)throws Exception{
        World nativeWorld=prepared(),bridgeWorld=SaveCodec.decode(SaveCodec.encode(nativeWorld));
        List<String> lines=new ArrayList<>();lines.add("initial "+hash(SaveCodec.encode(nativeWorld)));
        int index=0;
        for(int[] op:operations(nativeWorld)){
            World.Result nativeResult=direct(nativeWorld,op,true),bridgeResult=direct(bridgeWorld,op,false);
            if(nativeResult.ok!=bridgeResult.ok||!Arrays.equals(SaveCodec.encode(nativeWorld),SaveCodec.encode(bridgeWorld)))
                throw new AssertionError("old native/bridge divergence step "+index);
            lines.add((index++)+" "+nativeResult.ok+" "+hash(SaveCodec.encode(nativeWorld)));
        }
        Files.write(Path.of(args[0]),lines);
        System.out.println("Original native and bridge full-save baseline: "+lines.size()+" states (includes serialized RNG).");
    }
}
