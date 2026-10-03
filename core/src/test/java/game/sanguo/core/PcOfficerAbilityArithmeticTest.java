package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Expected outputs are exported directly from the pinned original executable. */
public final class PcOfficerAbilityArithmeticTest {
    public static void main(String[] args)throws Exception {
        int rows=0,curves=0,abilities=0;
        InputStream resource=PcOfficerAbilityArithmeticTest.class.getResourceAsStream("/pc-officer-ability-native.tsv");
        if(resource==null)throw new AssertionError("Missing original ability oracle");
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(resource,StandardCharsets.UTF_8))) {
            String header=reader.readLine();
            if(header==null||!header.startsWith("kind\tv0\t"))throw new AssertionError("Bad ability oracle header");
            for(String line;(line=reader.readLine())!=null;) {
                String[] cells=line.split("\t",-1);
                if(cells.length!=15)throw new AssertionError("Bad oracle row width");
                int[] v=new int[14];for(int i=0;i<v.length;i++)v[i]=Integer.parseInt(cells[i+1]);
                int actual;
                if(cells[0].equals("curve")) {
                    actual=PcOfficerAbilityRules.growthPercent(v[0],v[1]);curves++;
                } else if(cells[0].equals("ability")) {
                    boolean spouseBonus=v[10]!=0&&(v[9]==99||v[11]==99);
                    actual=PcOfficerAbilityRules.current(v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],
                            spouseBonus,v[0]>=700&&v[0]<=799,v[12]!=0);abilities++;
                } else throw new AssertionError("Unknown oracle kind "+cells[0]);
                if(actual!=v[13])throw new AssertionError("Native ability mismatch row="+(rows+1)+" expected="+v[13]+" actual="+actual+" source="+line);
                rows++;
            }
        }
        if(curves!=1143||abilities!=3670)throw new AssertionError("Oracle scope changed "+curves+"/"+abilities);
        int dates=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(PcOfficerAbilityArithmeticTest.class.getResourceAsStream("/pc-officer-date-native.tsv"),StandardCharsets.UTF_8))) {
            if(!reader.readLine().equals("start_year\tstart_month\tstart_day\telapsed_turns\tbirth\tfixed_age_flag\trequested_growth_disabled\teffective_growth_disabled\tnative_age\tnative_politics"))throw new AssertionError("Bad original date header");
            for(String line;(line=reader.readLine())!=null;) {
                String[] cells=line.split("\t",-1);if(cells.length!=10)throw new AssertionError("Bad original date row");
                int[] v=new int[10];for(int i=0;i<v.length;i++)v[i]=Integer.parseInt(cells[i]);
                int age=PcOfficerAbilityRules.age(v[0],v[1],v[2],v[3],v[4],v[5]!=0);
                boolean disabled=PcOfficerAbilityRules.growthDisabled(v[6]!=0,v[5]!=0);
                int politics=PcOfficerAbilityRules.current(80,8,age,105,3,1,3,5,true,false,disabled);
                if(age!=v[8]||disabled!=(v[7]!=0)||politics!=v[9])throw new AssertionError("Native date/composition mismatch: "+line+" actual="+age+"/"+disabled+"/"+politics);
                dates++;
            }
        }
        if(dates!=1040)throw new AssertionError("Date oracle scope changed "+dates);
        System.out.println("PASS PcOfficerAbilityArithmeticTest rows="+rows+" date_rows="+dates+" date_checks="+(dates*3)+" (pure arithmetic suite; gameplay separately verified by PcOfficerStateTest)");
    }
}
