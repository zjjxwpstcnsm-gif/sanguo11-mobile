package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Normal command/save checks against the pinned native equations and credit boundary. */
public final class PcMerchantMarketTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static int version(byte[] bytes){return (bytes[4]&255)<<24|(bytes[5]&255)<<16|(bytes[6]&255)<<8|(bytes[7]&255);}
    private static World fixture(int base,int xp,int rate)throws Exception{
        World w=PcOfficerRankTest.world();w.officerAbilities.initializeOpening(null,false,false);
        OfficerAbilities.setBase(w.officer(2),3,base);w.officerAbilities.gainExperience(2,3,xp);w.merchantMarket.initializeOpening();
        w.city(10).gold=50000;w.city(10).food=900000;w.city(10).merchantRate=rate;w.governance.reconcile(false);w.reports.rebase();return w;
    }
    public static void main(String[] args)throws Exception{
        orderedCommands();monthly();legacy();openings();
        System.out.println("PASS PcMerchantMarketTest checks="+checks+" normal native quotes/quantities/XP/credit cap, v35 and explicit v34 schema/price-mode continuation, monthly saved RNG (full PC global-stream/weather parity pending)");
    }
    private static void orderedCommands()throws Exception{
        for(int base:new int[]{1,50,80,99})for(int xp:new int[]{0,94,95,99,100,2995,3000})for(int rate:new int[]{30,50,70})for(boolean buy:new boolean[]{true,false}){
            int beforePolitics=Math.min(100,base+xp/100),afterPolitics=Math.min(100,base+Math.min(3000,xp+5)/100);
            int maximum=(int)(buy?Math.min(50000L*rate*400/((450-beforePolitics)*10L),100000):Math.min(50000L*(450-beforePolitics)*rate/3200,899999));
            for(int food:new int[]{1,999,1000,20001,50000,maximum,maximum+1}){
                World w=fixture(base,xp,rate);byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();
                TradePlan p=w.campaign.previewTrade(10,2,buy?TradePlan.Operation.BUY:TradePlan.Operation.SELL,food);
                long raw=buy?food*(450-afterPolitics)*10L/(rate*400L):food*3200L/((450-afterPolitics)*rate);
                long settled=buy?raw:Math.min(50000,raw);
                check(p.nativePricing&&p.minimum==1&&p.step==1&&p.marketRate==rate,"source quantity/rate metadata");
                check(p.availableMaximum==maximum&&p.pricingPoliticsBefore==beforePolitics&&p.pricingPoliticsAfter==afterPolitics,"native maximum precedes XP refresh");
                check(p.rawGoldQuote==raw&&p.quotedGold==settled,"post-XP integer quote and actual capped settlement");
                for(int n=0;n<3;n++)w.campaign.previewTrade(10,2,buy?TradePlan.Operation.BUY:TradePlan.Operation.SELL,food);
                check(Arrays.equals(before,SaveCodec.encode(w))&&rng==w.strategy.getRandomState(),"all previews preserve full save/RNG");
                if(food>maximum){check(!p.allowed()&&!w.campaign.trade(10,2,buy,food).ok&&Arrays.equals(before,SaveCodec.encode(w)),"outside normal quantity UI bound rejects atomically");continue;}
                check(p.allowed(),p.failure==null?"allowed":p.failure.detail);
                World replay=SaveCodec.decode(before);check(w.campaign.trade(10,2,buy,food).ok&&replay.campaign.trade(10,2,buy,food).ok,"real normal command executes");
                check(w.city(10).gold==50000+(buy?-settled:settled)&&w.city(10).food==900000+(buy?food:-food),"native exact actual stocks");
                check(w.officerAbilities.experience(2,3)==Math.min(3000,xp+5)&&w.officer(2).politics==afterPolitics,"XP first, base preserved");
                check(w.government.merit(2)==50&&w.actionPoints[0]==40&&w.officer(2).acted&&w.campaign.traded(10)==food,"one native merit/AP/action/use effect");
                byte[] after=SaveCodec.encode(w);check(version(after)==35&&Arrays.equals(after,SaveCodec.encode(replay))&&Arrays.equals(after,SaveCodec.encode(SaveCodec.decode(after))),"v35 command/restore full byte parity");
                check(w.strategy.getRandomState()==rng,"ordinary trade never consumes rule RNG");
                check(!w.campaign.trade(10,2,!buy,1).ok&&Arrays.equals(after,SaveCodec.encode(w)),"same-city opposite-direction duplicate rejected");
            }
        }
        for(int invalid:new int[]{Integer.MIN_VALUE,-1,0,1000001,Integer.MAX_VALUE}){
            World w=fixture(80,95,50);byte[] before=SaveCodec.encode(w);
            check(!w.campaign.trade(10,2,true,invalid).ok&&Arrays.equals(before,SaveCodec.encode(w)),"invalid quantity does not award XP or change any state");
        }
        World w=fixture(80,0,50);w.city(10).gold=500000;
        check(w.campaign.previewTrade(10,2,TradePlan.Operation.BUY,1).allowed()&&w.campaign.trade(10,2,true,1).ok&&w.city(10).gold==500000,"author surplus gold not silently clamped on debit");
        w=fixture(80,0,50);w.city(10).gold=500000;byte[] before=SaveCodec.encode(w);
        check(!w.campaign.trade(10,2,false,1).ok&&Arrays.equals(before,SaveCodec.encode(w)),"no new sale credits above native cap");
        w=fixture(80,0,0);before=SaveCodec.encode(w);
        check(!w.campaign.trade(10,2,true,1).ok&&Arrays.equals(before,SaveCodec.encode(w)),"invalid stored rate is rejected, not silently initialized");
    }
    private static void monthly()throws Exception{
        World w=fixture(80,95,50);check(w.campaign.trade(10,2,true,999).ok,"non-thousand order persists before monthly clock");
        World replay=SaveCodec.decode(SaveCodec.encode(w));
        for(int n=0;n<15;n++){
            int rate=w.city(10).merchantRate;check(w.nextTurn().ok&&replay.nextTurn().ok,"actual complete turn advances");
            if(w.turn%3!=0)check(w.city(10).merchantRate==rate,"mid-month turns do not randomize price");
            check(w.city(10).merchantRate>=30&&w.city(10).merchantRate<=70,"native monthly price clamp");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"full normal turns/save/RNG are reproducible");
            byte[] before=SaveCodec.encode(w);w.merchantMarket.tick();check(Arrays.equals(before,SaveCodec.encode(w)),"monthly processing idempotent after normal turn");
        }
        for(int flags:new int[]{0,1,2,4,5,6}){
            w=fixture(80,0,50);w.turn=3;
            if((flags&3)!=0)w.events.hazards.put(10,new WorldEvents.Hazard(10,(flags&1)!=0?WorldEvents.Disaster.PLAGUE:WorldEvents.Disaster.LOCUST,9));
            w.city(10).merchantHarvest=(flags&4)!=0;long before=w.strategy.getRandomState();
            w.merchantMarket.tick();int rate=w.city(10).merchantRate;
            check((flags&3)!=0?rate==30||rate==40:(flags&4)!=0?rate==60||rate==70:rate>=30&&rate<=70,"native disaster precedence over harvest");
            check(w.strategy.getRandomState()!=before,"monthly rule consumes saved RNG, never preview RNG");
            w.startMonth=7;w.city(10).merchantHarvest=true;w.merchantMarket.beforeWeather();check(!w.city(10).merchantHarvest,"August clears harvest before repricing");
        }
    }
    private static void legacy()throws Exception{
        String path=System.getProperty("java.vm.name","").toLowerCase(Locale.ROOT).contains("dalvik")?"pre-merchant-r25-v34-art.sg11":"pre-merchant-r25-v34.sg11";
        try(InputStream in=PcMerchantMarketTest.class.getResourceAsStream("/"+path)){
            check(in!=null,"frozen R25 v34 fixture exists");ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int count;
            while((count=in.read(chunk))!=-1)buffer.write(chunk,0,count);
            byte[] raw=buffer.toByteArray();World w=SaveCodec.decode(raw);
            check(version(raw)==34&&w.officerAbilities.enabled()&&!w.merchantMarket.enabled(),"legacy v34 retains managed ability and unguessed price mode");
            check(Arrays.equals(raw,SaveCodec.encode(w)),"frozen v34 bytes exact, no format or price migration");
            World replay=SaveCodec.decode(raw);for(int n=0;n<3;n++){check(w.nextTurn().ok&&replay.nextTurn().ok,"v34 ordinary saved turn");check(version(SaveCodec.encode(w))==34&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"v34 semantics and RNG remain stable");}
        }
    }
    private static void openings()throws Exception{
        for(ScenarioCatalog.Summary summary:ScenarioCatalog.summaries()){
            World w=ScenarioCatalog.load(summary.id,0,23);byte[] before=SaveCodec.encode(w);
            check(version(before)==37&&w.officerAbilities.enabled()&&w.merchantMarket.enabled()&&w.pcProduction.enabled()&&w.pcTechniquePoints.enabled(),"new v37 normal opening includes ability, market, production and technique policy");
            check(SaveCodec.decode(before).pcProduction.enabled(),"source production mode survives complete new opening");
            // Keep the former v35 expectation against actual frozen-engine saves.
            String vm=System.getProperty("java.vm.name","").toLowerCase(Locale.ROOT).contains("dalvik")?"art":"host";
            String resource="/legacy-market-v35/"+vm+"/"+summary.id+".sg11";
            byte[] old;
            try(InputStream in=PcMerchantMarketTest.class.getResourceAsStream(resource)){
                check(in!=null,"genuine frozen v35 opening exists");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1)bytes.write(chunk,0,n);old=bytes.toByteArray();
            }
            String expected=null;
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(PcMerchantMarketTest.class.getResourceAsStream("/legacy-market-v35/manifest.tsv"),java.nio.charset.StandardCharsets.UTF_8))){
                for(String row;(row=reader.readLine())!=null;){String[] c=row.split("\t");if(c.length==3&&c[0].equals(vm)&&c[1].equals(summary.id))expected=c[2];}
            }
            StringBuilder digest=new StringBuilder();for(byte value:java.security.MessageDigest.getInstance("SHA-256").digest(old))digest.append(String.format(Locale.ROOT,"%02x",value&255));check(digest.toString().equals(expected),"genuine v35 source SHA exact");
            World legacy=SaveCodec.decode(old);
            check(version(old)==35&&legacy.officerAbilities.enabled()&&legacy.merchantMarket.enabled(),"new normal opening includes all ability and market state");
            check(!legacy.pcProduction.enabled()&&Arrays.equals(old,SaveCodec.encode(legacy)),"v35 mode and complete values preserved without guessed production migration");
            for(World.City c:w.cities)if(c.kind==World.SiteKind.CITY)check(c.merchantRate>=30&&c.merchantRate<=70,"real initialized city price");
            check(Arrays.equals(before,SaveCodec.encode(SaveCodec.decode(before))),"new opening price/ability state exact round trip");
        }
    }
}
