package game.sanguo.core;

import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;

/** Captured actual Android before/after. Host replay is diagnosis, never UI acceptance. */
public final class SessionBActualMarchBoundaryProbe {
    static String sha(byte[] b)throws Exception{return PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(b));}
    public static void main(String[]args)throws Exception{
        Path beforeFile=Path.of(args[0]),afterFile=Path.of(args[1]);
        byte[] beforeRaw=Files.readAllBytes(beforeFile),afterRaw=Files.readAllBytes(afterFile);
        World before=SaveCodec.decode(beforeRaw),actual=SaveCodec.decode(afterRaw);
        int id=16;Hex target=new Hex(115,118);World.Unit u=before.unit(id),v=actual.unit(id);
        if(u==null||v==null||before.turn!=18||actual.turn!=18)throw new AssertionError("Exact actual turn18 unit16 required");
        byte[] untouched=SaveCodec.encode(before);var p=before.marches.previewMove(id,target);
        System.out.println("INPUT before="+sha(beforeRaw)+" after="+sha(afterRaw));
        System.out.println("PLAN source="+before.scenarioId+" origin="+u.hex+" target="+target+" cost="+p.cost+" stepsNow="+p.stepsNow+" turns="+p.estimatedTurns+" error="+p.error+" remaining="+before.orders.remaining(u)+" path="+p.path);
        for(int i=1;i<p.path.size();i++)System.out.println("STEP "+i+" cell="+p.path.get(i)+" cost="+before.army.moveCost(u,p.path.get(i-1),p.path.get(i))+" interception="+before.advancedBattle.zone(u,p.path.get(i)));
        if(!Arrays.equals(untouched,SaveCodec.encode(before)))throw new AssertionError("Preview mutates actual before world");
        System.out.println("ACTUAL position="+v.hex+" remaining="+actual.orders.remaining(v)+" spent="+v.movementSpent+" budget="+v.movementBudget+" task="+actual.marches.describe(v)+" log="+actual.log);
        // A separate decoded clone follows the same persistent move command.
        World replay=SaveCodec.decode(beforeRaw);var plan=replay.marches.previewMove(id,target);var result=replay.marches.execute(plan);
        var r=replay.unit(id);boolean same=Arrays.equals(SaveCodec.encode(actual),SaveCodec.encode(replay));
        System.out.println("DECLARED_HOST_REPLAY ok="+result.ok+" result="+result.message+" position="+(r==null?null:r.hex)+" fullCanonicalWorldAllRngSame="+same);
        if(!result.ok||!same||!v.hex.equals(p.path.get(p.stepsNow)))throw new AssertionError("Actual formal endpoint differs from preview/clone semantics");
        if(!Arrays.equals(beforeRaw,Files.readAllBytes(beforeFile))||!Arrays.equals(afterRaw,Files.readAllBytes(afterFile)))throw new AssertionError("Original actual Android files changed");
        System.out.println("PASS actual preview immediate endpoint/formal wholeWorld allRNG exact; persistent goal remains, original files untouched; diagnostic only, Native terminal not proved");
    }
}
