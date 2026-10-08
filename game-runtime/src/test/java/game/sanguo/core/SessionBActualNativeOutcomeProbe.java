package game.sanguo.core;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Read exact actual Android battle/terminal/final campaign; no rules executed. */
public final class SessionBActualNativeOutcomeProbe {
    static String sha(byte[] b)throws Exception{return PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(b));}
    public static void main(String[]args)throws Exception{
        Path initialPath=Path.of(args[0]),terminalPath=Path.of(args[1]),finalPath=Path.of(args[2]);
        byte[] initialRaw=Files.readAllBytes(initialPath),terminalRaw=Files.readAllBytes(terminalPath),finalRaw=Files.readAllBytes(finalPath);
        World initial=SaveCodec.decode(initialRaw),terminal=SaveCodec.decode(terminalRaw),finished=SaveCodec.decode(finalRaw);
        var s=terminal.contests.current();if(s==null||s.nativeDuel==null)throw new AssertionError("Actual native terminal missing");
        var facts=s.nativeDuel.facts();if(!facts.terminal||finished.contests.busy()||finished.turn!=initial.turn+3)throw new AssertionError("Terminal/final three turns boundary");
        byte[] before=SaveCodec.encode(finished);
        System.out.println("ACTUAL initialSha="+sha(initialRaw)+" terminalSha="+sha(terminalRaw)+" finalSha="+sha(finalRaw));
        System.out.println("TERMINAL contest="+s.id+" winnerSide="+facts.winner+" round="+facts.round+" frames="+facts.frames+" inputs="+facts.inputs+" initialTurn="+initial.turn+" finalTurn="+finished.turn);
        for(var team:facts.teams)for(var fighter:team.fighters){
            var a=initial.officer(fighter.officerId);var b=finished.officer(fighter.officerId);var prisoner=finished.government.prisoner(b.id);
            System.out.println("FIGHTER side="+team.side+" stable="+b.id+" native="+fighter.nativeId+" name="+b.name+" terminalHP="+fighter.health+" outcome="+fighter.terminalOutcome+" life="+finished.life.state(b.id)+" injury="+finished.contests.injury(b.id)+" merit="+initial.government.merit(a.id)+"->"+finished.government.merit(b.id)+" warXP="+initial.officerAbilities.experience(a.id,1)+"->"+finished.officerAbilities.experience(b.id,1)+" baseWar="+initial.officerAbilities.base(a.id,1)+"->"+finished.officerAbilities.base(b.id,1)+" location="+b.cityId+" unit="+b.unitId+" prisoner="+(prisoner==null?"none":prisoner.captor+"/city"+prisoner.cityId+"/unit"+prisoner.unitId));
        }
        for(int id:new int[]{16,25}){var u=finished.unit(id);System.out.println("FINAL_UNIT id="+id+" value="+(u==null?"removed":u.hex+" troops="+u.troops+" food="+u.food+" gold="+u.gold+" acted="+u.acted));}
        if(!Arrays.equals(before,SaveCodec.encode(finished)))throw new AssertionError("Outcome read mutates entire World/allRNG");
        if(!Arrays.equals(initialRaw,Files.readAllBytes(initialPath))||!Arrays.equals(terminalRaw,Files.readAllBytes(terminalPath))||!Arrays.equals(finalRaw,Files.readAllBytes(finalPath)))throw new AssertionError("Original actual evidence bytes changed");
        System.out.println("PASS actual terminal/three whole turns/captured outcome read only; fullWorld/bothRNG and original files unchanged; no outcome forced");
    }
}
