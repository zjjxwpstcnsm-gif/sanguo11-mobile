package game.sanguo.core;

import java.nio.file.*;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;

/** First-launch source selection without constructing a campaign or token;
 * verifies the same facts and pure existing/busy/historical save boundaries. */
public final class PcNewGameOptionsCatalogTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void equal(PcNewGameOptionsSnapshot catalog,PcOpeningOptionsSnapshot current){
        check(current.previewSourceId.equals(catalog.scenarioId)&&current.sourceFlag18==catalog.sourceFlag18,"existing session and first-launch bind same exact source");
        check(current.executableSha.equals(catalog.executableSha)&&current.receiptSha.equals(catalog.receiptSha),"same original menu provenance");
        check(current.groups.size()==catalog.groups.size(),"same original group count");
        for(int k=0;k<current.groups.size();k++){
            var a=catalog.groups.get(k);var b=current.groups.get(k);
            check(a.id.equals(b.id)&&a.title.equals(b.title)&&Objects.equals(a.overrideLabel,b.overrideLabel)&&Objects.equals(a.fixedMenuValue,b.fixedMenuValue),"same source constraint "+k);
            check(a.savedValue==null&&b.savedValue==null&&a.choices.size()==b.choices.size(),"new draft never derives current saved values "+k);
            for(int j=0;j<a.choices.size();j++){var p=a.choices.get(j);var q=b.choices.get(j);check(p.value==q.value&&p.controlId==q.controlId&&p.label.equals(q.label)&&p.rawHex.equals(q.rawHex),"same original raw/control/value "+k+"/"+j);}
        }
    }
    public static void main(String[]args)throws Exception{
        // This block runs before any World/GameSession/StateToken is created.
        var sources=PcScenarioCatalog.all();List<PcNewGameOptionsSnapshot> drafts=new ArrayList<>();
        for(int k=0;k<sources.size();k++){
            var source=sources.get(k).identity;var f=GameSession.previewNewSourceOptions(source.scenarioId);drafts.add(f);
            check(f.scenarioId.equals(source.scenarioId)&&f.sourcePath.equals(source.path)&&f.sourceSha.equals(source.sha)&&f.sharedSha.equals(source.sharedSha)&&f.sourceVariant.equals(source.sourceVariant)&&f.sourceUnknown.equals(source.unknown),"exact original source id/path/SHA/unknown "+k);
            check(!f.defaultsKnown&&f.automaticOverridesKnown&&f.groups.size()==3,"new draft has no guessed default "+k);
            check(f.sourceFlag18==((k==7||k==13||k==14)?1:0),"independently pinned16 header flags "+k);
            for(int field=0;field<3;field++){
                var group=f.groups.get(field);check(group.savedValue==null&&group.choices.size()==3,"all fields start without saved value "+k+"/"+field);
                check(Objects.equals(group.fixedMenuValue,field==2&&f.sourceFlag18!=0?Integer.valueOf(2):null),"verified life-only menu constraint "+k+"/"+field);
                check(group.choices.stream().map(c->c.value).toList().equals(List.of(0,1,2)),"no fourth menu item or ordinal conversion "+k+"/"+field);
            }
            boolean immutable=false;try{f.groups.clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"source options immutable");
            immutable=false;try{f.sourceUnknown.clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"source unknown provenance immutable");
        }
        check(drafts.size()==16,"all16 original catalog sources, no merged or invented source");
        for(String invalid:new String[]{null,"", "pc-scen000-invalid"}){boolean rejected=false;try{GameSession.previewNewSourceOptions(invalid);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid draft source rejected without campaign");}
        boolean rejected=false;try{PcOpeningOptionsSnapshot.unsupported(null);}catch(NullPointerException e){rejected=true;}check(rejected,"existing same-state DTO retains non-null StateToken contract");
        Path checkpoint=Path.of("out/session-b/duel-ruler-ai-seed1-nonterminal-v1.sg11");byte[] busy=Files.readAllBytes(checkpoint);
        try(GameSession game=new GameSession(SaveCodec.decode(busy))){
            var state=game.state();check(game.contest().nativeDuel!=null,"genuine prior format5 pending battle");
            for(var draft:drafts){equal(draft,game.pcOpeningOptions(draft.scenarioId));GameSession.previewNewSourceOptions(draft.scenarioId);check(state.equals(game.state())&&Arrays.equals(busy,game.captureSave()),"switch/dismiss first-launch draft leaves busy World/all RNG/token/policies exact");}
        }
        Path old=Path.of("docs/handoff/20261004/session1/batch19-actual-art-mid.sg11");byte[] raw=Files.readAllBytes(old);World historical=SaveCodec.decode(raw);byte[] canonical=SaveCodec.encode(historical);
        try(GameSession game=new GameSession(historical)){
            var state=game.state();for(var draft:drafts)GameSession.previewNewSourceOptions(draft.scenarioId);
            check(state.equals(game.state())&&Arrays.equals(canonical,game.captureSave())&&Arrays.equals(raw,Files.readAllBytes(old)),"genuine39 all fields/RNG/identity/file preserved; existing gzip OS/CRC difference unchanged");
            check(!game.pcOpeningOptions().hasSavedValues(),"genuine39 does not adopt new menu or native policy");
        }
        System.out.println("PASS first-launch source catalog/no dummy World or StateToken/all16/busy/genuine39 purity "+checks+" checks; A actual picker/new APK still pending");
    }
}
