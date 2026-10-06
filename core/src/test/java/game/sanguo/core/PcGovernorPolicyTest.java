package game.sanguo.core;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Original primitive/real current capability joins. Production lifecycle integration is separate. */
public final class PcGovernorPolicyTest {
    static int checks;
    static void check(boolean ok,String detail){checks++;if(!ok)throw new AssertionError(detail);}
    static World.Officer person(World w,int nativeId)throws Exception {for(var p:PcScenarioPeople.saved(w))if(p.nativeId==nativeId)return w.officer(p.officerId);throw new AssertionError("Missing joined person");}
    static void normalAndLegacy()throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,42);
        byte[] legacy=java.nio.file.Files.readAllBytes(java.nio.file.Path.of("out/session-b/governor-legacy-installed-source20.sg11"));check(PcGovernorPolicy.recognized(w),"only explicitly created source new game initializes governor strategy");
        World.Officer leader=person(w,440),he=person(w,58);World.City city=w.city(leader.cityId);
        check(leader.role==Strategy.Role.DISTRICT&&city.governorId==leader.id,"native440 district identity preserved as governor");
        byte[] before=SaveCodec.encode(w);
        var preview=w.strategy.previewCityAction(CityActionPlan.Operation.APPOINT_GOVERNOR,city.id,he.id,new int[]{he.id});
        check(!preview.allowed(),"unverified manual appointment refused by same preview gate");
        check(!w.strategy.appointGovernor(city.id,he.id,he.id).ok&&Arrays.equals(before,SaveCodec.encode(w)),"manual confirmation refuses without spending or state changes");
        var deploy=w.army.deploy(city.id,leader.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,1000,2000,0);check(deploy.ok,deploy.message);
        check(leader.role==Strategy.Role.DISTRICT&&city.governorId==person(w,509).id,"real deployment removes district leader and elects original ordinary replacement");
        byte[] deployed=SaveCodec.encode(w);w=SaveCodec.decode(deployed);check(Arrays.equals(deployed,SaveCodec.encode(w)),"deployed completeWorld/bothRNG exact");
        check(w.nextTurn().ok,"normal whole-turn settlement");leader=person(w,440);city=w.city(city.id);
        var entered=w.marches.execute(w.marches.previewCity(leader.unitId,city.id));check(entered.ok,entered.message);for(int n=0;n<4&&leader.unitId>=0;n++)check(w.nextTurn().ok,"normal garrison route settlement");check(city.governorId==leader.id&&leader.role==Strategy.Role.DISTRICT,"normal garrison restores district leader priority without consuming identity");
        byte[] returned=SaveCodec.encode(w);check(Arrays.equals(returned,SaveCodec.encode(SaveCodec.decode(returned))),"returned completeWorld/bothRNG exact");
        World transfer=SaveCodec.decode(before);World.Officer moving=person(transfer,440);int origin=moving.cityId;
        var src=PcGovernorPolicy.source(transfer);int destination=-1;for(var site:src.sites.values())if(site.nativeId==3)destination=site.id;
        check(destination>=0&&transfer.city(destination).owner==moving.owner,"strict original destination join");
        var dispatch=transfer.domestic.transfer(origin,destination,moving.id);check(dispatch.ok,dispatch.message);
        check(transfer.city(origin).governorId==person(transfer,509).id&&moving.role==Strategy.Role.DISTRICT,"normal civil departure replaces governor and retains district role");
        int turns=0;while(transfer.domestic.busy(moving.id)&&turns++<30){check(transfer.nextTurn().ok,"normal civil transfer whole turn");byte[] b=SaveCodec.encode(transfer);transfer=SaveCodec.decode(b);moving=person(transfer,440);check(Arrays.equals(b,SaveCodec.encode(transfer)),"civil transfer completeWorld/bothRNG each turn");}
        check(!transfer.domestic.busy(moving.id)&&moving.cityId==destination&&transfer.city(destination).governorId==moving.id&&moving.role==Strategy.Role.GOVERNOR,"full original arrival changes station/army and demotes old district leader to elected governor");
        check(PcGovernorPolicy.data(transfer).armyLeaders.get(6)==436&&person(transfer,436).role==Strategy.Role.DISTRICT,"full original arrival replaces old army6 commander with native436");
        var assignment=PcGovernorPolicy.data(transfer).assignments.get(moving.id);check(assignment.home==3&&assignment.army==5,"native administrative home and army updated independently of lifecycle home");
        // Controlled explicit assignment mirrors original concrete-city47cbd0 + person4a32f0.
        // This verifies live saved selection, not a complete capture-controller command.
        World armyChange=SaveCodec.decode(before);var state=PcGovernorPolicy.data(armyChange);int joinedSite=person(armyChange,440).cityId;
        state.siteArmies.put(joinedSite,5);PcGovernorPolicy.write(armyChange,state);armyChange.governance.reconcile(false);
        check(armyChange.city(joinedSite).governorId==-1,"original changed site army excludes old army residents");
        state.assignments.get(person(armyChange,440).id).army=5;PcGovernorPolicy.write(armyChange,state);armyChange.governance.reconcile(false);
        check(armyChange.city(joinedSite).governorId==person(armyChange,440).id,"original changed resident army restores district leader selection");
        byte[] changedArmy=SaveCodec.encode(armyChange);World reloadedArmy=SaveCodec.decode(changedArmy);check(Arrays.equals(changedArmy,SaveCodec.encode(reloadedArmy))&&PcGovernorPolicy.data(reloadedArmy).siteArmies.get(joinedSite)==5,"live site army is saved independently of original source roster");
        World conquest=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,29,42);
        var ruler=person(conquest,91);var attackCity=conquest.city(20057);var invasion=conquest.army.deploy(ruler.cityId,ruler.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,5000,20000,0);check(invasion.ok,invasion.message);int unitId=ruler.unitId;
        var route=conquest.marches.execute(conquest.marches.previewCity(unitId,attackCity.id));check(route.ok,route.message);
        for(int i=0;i<5&&ruler.unitId>=0;i++){check(conquest.nextTurn().ok,"normal real capture march turn");byte[] b=SaveCodec.encode(conquest);conquest=SaveCodec.decode(b);ruler=person(conquest,91);attackCity=conquest.city(attackCity.id);check(Arrays.equals(b,SaveCodec.encode(conquest)),"capture route fullWorld/bothRNG exact");}
        check(attackCity.owner==29&&ruler.cityId==attackCity.id&&PcGovernorPolicy.data(conquest).siteArmies.get(attackCity.id)==7,"real ordinary source neutral-port capture stores original army7 and garrisons");
        check(!PcGovernorPolicy.data(conquest).unitArmies.containsKey(unitId)&&attackCity.governorId==ruler.id,"entered unit army record cleared and actual ruler governs");
        byte[] viewBefore=SaveCodec.encode(conquest);var view=PcGovernorPolicy.view(conquest);check(view.enabled&&view.armies.size()==47&&view.siteArmies.get(attackCity.id)==7,"detached governance facts");check(Arrays.equals(viewBefore,SaveCodec.encode(conquest)),"read-only governance facts preserve fullWorld/bothRNG");
        World recruitment=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,42);
        var recruiter=person(recruitment,116);var recruit=person(recruitment,222);recruitment.loyalty.join(recruiter,recruit,recruiter.cityId);recruitment.governance.reconcile(false);
        var recruitData=PcGovernorPolicy.data(recruitment).assignments.get(recruit.id);
        check(recruit.owner==2&&recruit.role==Strategy.Role.OFFICER&&recruitData.army==0&&recruitData.home==8,"original successful recruitment joins current native army0/home8 and ordinary identity");
        byte[] recruited=SaveCodec.encode(recruitment);check(Arrays.equals(recruited,SaveCodec.encode(SaveCodec.decode(recruited))),"successful recruitment fullWorld/bothRNG exact");
        World death=SaveCodec.decode(before);var dying=person(death,58);var deadUnit=death.army.deploy(dying.cityId,dying.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,1000,2000,0);check(deadUnit.ok,deadUnit.message);int removedId=dying.unitId;death.life.die(dying.id,"metadata regression");check(death.unit(removedId)==null&&!PcGovernorPolicy.view(death).unitArmies.containsKey(removedId),"commander death removes saved unit army record");byte[] afterDeath=SaveCodec.encode(death);check(Arrays.equals(afterDeath,SaveCodec.encode(SaveCodec.decode(afterDeath))),"commander death completeWorld/bothRNG remains valid");
        World collapse=SaveCodec.decode(before);var falling=person(collapse,58);var fallenUnit=collapse.army.deploy(falling.cityId,falling.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,1000,2000,0);check(fallenUnit.ok,fallenUnit.message);int fallenId=falling.unitId;FactionCollapse.resolve(collapse,falling.owner);check(collapse.unit(fallenId)==null&&!PcGovernorPolicy.view(collapse).unitArmies.containsKey(fallenId),"faction collapse removes saved unit army record");byte[] afterCollapse=SaveCodec.encode(collapse);check(Arrays.equals(afterCollapse,SaveCodec.encode(SaveCodec.decode(afterCollapse))),"faction collapse completeWorld/bothRNG remains valid");
        World old=SaveCodec.decode(legacy);check(old.extensions.get(PcGovernorPolicy.NAMESPACE)==null,"prior installed source file has no governor policy");byte[] canonicalLegacy=SaveCodec.encode(old);check(Arrays.equals(canonicalLegacy,SaveCodec.encode(SaveCodec.decode(canonicalLegacy))),"prior source file canonical completeWorld/bothRNG unchanged");
        byte[] opaque={1,2,3,4,5};old.extensions.put(PcGovernorPolicy.NAMESPACE,opaque);byte[] encoded=SaveCodec.encode(old);check(Arrays.equals(encoded,SaveCodec.encode(SaveCodec.decode(encoded))),"unknown namespace opaque and inactive");
        World broken=SaveCodec.decode(before);byte[] policy=broken.extensions.get(PcGovernorPolicy.NAMESPACE);policy[7]=2;broken.extensions.put(PcGovernorPolicy.NAMESPACE,policy);boolean rejected=false;try{SaveCodec.validate(broken);}catch(IOException expected){rejected=true;}check(rejected,"recognized unsupported version rejected");
        for(String file:List.of("core/src/test/resources/save-v32-central-native.sg11","core/src/test/resources/pre-base-construction-v33.sg11","core/src/test/resources/pre-merchant-r25-v34.sg11","core/src/test/resources/legacy-market-v35/host/coalition-190.sg11","core/src/test/resources/legacy-production-v36/coalition-190-host.sg11","docs/handoff/20261004/session2/six-turn-authority-29/authority-before.sg11","docs/handoff/20261004/session1/batch19-actual-art-mid.sg11")){
            World historical=SaveCodec.decode(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(file)));byte[] b=SaveCodec.encode(historical);check(!PcGovernorPolicy.recognized(historical)&&Arrays.equals(b,SaveCodec.encode(SaveCodec.decode(b))),"historical no-upgrade wholeWorld/RNG "+file);
        }
    }
    public static void main(String[] args)throws Exception{
        byte[] raw;
        try(InputStream in=new GZIPInputStream(PcGovernorPolicyTest.class.getResourceAsStream("/pc-governor-rosters/rosters.bin.gz"))){raw=in.readAllBytes();}
        check(PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(raw)).equals("909254e0e7e8801e8e505e4c0be3c633f808101fcdc461c06759e4f82cfd8ef6"),"pinned original inputs");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));check(in.readInt()==0x50474f31,"input format");
        check(PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA),"original executable");
        for(int i=0;i<3;i++)PcOfficerInfo.text(in,64);check(in.readInt()==16,"source domain");
        int sites=0;
        for(int source=0;source<16;source++){
            String id=PcOfficerInfo.text(in,80),path=PcOfficerInfo.text(in,1024),variant=PcOfficerInfo.text(in,300),sha=PcOfficerInfo.text(in,64),shared=PcOfficerInfo.text(in,64);
            World world=PcScenarioCatalog.preview(id);var identity=PcScenarioIdentity.saved(world);
            check(identity.path.equals(path)&&identity.sourceVariant.equals(variant)&&identity.sha.equals(sha)&&identity.sharedSha.equals(shared),"source tuple");
            byte[] before=SaveCodec.encode(world);
            check(in.readInt()==87,"site domain");List<int[]> expected=new ArrayList<>();
            for(int j=0;j<87;j++){int[] row=new int[5];for(int k=0;k<5;k++)row[k]=in.readInt();check(row[0]==j&&world.city(row[1])!=null,"site native/runtime join");expected.add(row);}
            Map<Integer,PcScenarioPeople.Person> mapped=new HashMap<>();for(var person:PcScenarioPeople.saved(world))mapped.put(person.nativeId,person);
            check(in.readInt()==850,"source record domain");List<PcGovernorPolicy.Candidate> candidates=new ArrayList<>();
            for(int j=0;j<850;j++){
                int nativeId=in.readInt(),runtime=in.readInt();String record=PcOfficerInfo.text(in,64);int[] values=new int[12];for(int k=0;k<12;k++)values[k]=in.readInt();
                var person=mapped.get(nativeId);check(nativeId==j&&person!=null&&person.officerId==runtime&&person.recordSha.equals(record),"source/person/runtime record join");
                World.Officer o=runtime<0?null:world.officer(runtime);
                int capacity=o==null?values[5]:world.government.commandLimit(o.id),lead=o==null?values[6]:o.leadership,war=o==null?values[7]:o.war,merit=o==null?values[8]:world.government.merit(o.id);
                candidates.add(new PcGovernorPolicy.Candidate(nativeId,runtime,values[0],values[2],values[3],values[1],values[4],capacity,lead,war,merit,values[9]!=0,values[11]!=0,values[10]!=0));
            }
            check(in.readInt()==47,"original army domain");for(int j=0;j<47;j++){check(in.readInt()==j,"army native order");for(int k=0;k<9;k++)in.readInt();}
            for(int[] site:expected){var selected=PcGovernorPolicy.elect(site[0],site[3],site[2],candidates);int result=selected==null?-1:selected.nativeId;check(result==site[4],path+" native site"+site[0]+" governor "+result+" != "+site[4]);sites++;}
            check(Arrays.equals(before,SaveCodec.encode(world)),"primitive reads preserve fullWorld/bothRNG");
            long strategicRng=world.strategy.randomState,lifeRng=world.life.randomState;
            check(PcGovernorPolicy.recognized(world),"source preview/new game has explicit saved election strategy");
            check(strategicRng==world.strategy.randomState&&lifeRng==world.life.randomState,"new policy initialization preserves both authority RNG streams");
            for(int[] site:expected){var selected=world.officer(world.city(site[1]).governorId);int nativeId=-1;if(selected!=null)for(var person:mapped.values())if(person.officerId==selected.id)nativeId=person.nativeId;check(nativeId==site[4],"live original election "+path+" site"+site[0]+": "+nativeId+" != "+site[4]);}
            byte[] live=SaveCodec.encode(world);World restored=SaveCodec.decode(live);check(Arrays.equals(live,SaveCodec.encode(restored)),"live fullWorld/bothRNG persisted");
            world.governance.reconcile(false);check(Arrays.equals(live,SaveCodec.encode(world)),"same-state reconciliation idempotent");
        }
        check(in.available()==0&&sites==1392,"complete proof input coverage");
        normalAndLegacy();
        System.out.println("PASS original governor primitive "+checks+" checks/1392 elections using actual production capabilities and strict source/person joins; live initial election/departure/fullsave/legacy guards pass; production activation/capture/menu/APK pending");
    }
}
