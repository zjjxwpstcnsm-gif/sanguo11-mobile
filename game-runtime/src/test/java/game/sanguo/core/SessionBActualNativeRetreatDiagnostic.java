package game.sanguo.core;

import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;

/** Declared host clone of actual saved ordinary battle; diagnoses legal retreat only. */
public final class SessionBActualNativeRetreatDiagnostic {
    public static void main(String[]args)throws Exception{
        Path input=Path.of(args[0]);byte[] raw=Files.readAllBytes(input);
        if(!PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(raw)).equals("668b77e99cbed02f6093530f17f0b38bdc2b0e4745a6b2f69ba8a58c0455a2b2"))throw new AssertionError("Exact actual battle required");
        World w=SaveCodec.decode(raw);boolean attempted=false,requested=false;int minimumRound=args.length>1?Integer.parseInt(args[1]):0;
        if(minimumRound<0||minimumRound>50)throw new IllegalArgumentException("Examined battle round domain");
        for(int i=0;i<300;i++){
            var s=w.contests.current();if(s==null||s.nativeDuel==null)throw new AssertionError("Battle unexpectedly absent");var f=s.nativeDuel.facts();
            if(f.terminal){System.out.println("TERMINAL winner="+f.winner+" round="+f.round+" frames="+f.frames+" attemptedRetreat="+attempted);break;}
            byte[] before=SaveCodec.encode(w);var choices=f.choices;var retreat=choices.stream().filter(c->c.special==3&&c.error.isEmpty()).findFirst();
            if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Admission query changes full world/allRNG");
            PcDuelCampaign.InputFacts choice;
            if(!attempted&&f.round>=minimumRound&&retreat.isPresent()){
                choice=retreat.get();requested=true;if(f.phase==7)attempted=true;
                System.out.println("ACTUAL_CLONE_LEGAL_RETREAT input="+s.revision+" phase="+f.phase+" round="+f.round+" spirit="+f.teams.get(0).fighters.get(0).spirit+" requested="+requested+" selected="+attempted);
            }
            else{var ordinary=choices.stream().filter(c->c.special==-1&&c.replacement==-1&&c.error.isEmpty()).toList();choice=ordinary.stream().filter(c->c.stance==2).findFirst().orElseGet(()->ordinary.stream().filter(c->c.stance==-1).findFirst().orElseThrow());}
            var result=w.contests.nativeDuelInput(s.id,s.revision,choice.stance,choice.special,choice.replacement);
            if(!result.ok){System.out.println("REJECTED special="+choice.special+" detail="+result.message);if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Rejected input changes full World/RNG");break;}
            if(choice.special==3){var next=w.contests.current().nativeDuel;System.out.println("RETREAT_POST rawResult584="+next.state.model.get(0x584)+" phase="+next.facts().phase+" terminal="+next.terminal()+" winner="+next.facts().winner);}
            byte[] accepted=SaveCodec.encode(w);w=SaveCodec.decode(accepted);if(!Arrays.equals(accepted,SaveCodec.encode(w)))throw new AssertionError("Accepted input full save continuation differs");
        }
        if(!Arrays.equals(raw,Files.readAllBytes(input)))throw new AssertionError("Original actual evidence changed");
        System.out.println("PASS admission/input/cold clone diagnosis only; no Android retreat/campaign acceptance or outcome forced");
    }
}
