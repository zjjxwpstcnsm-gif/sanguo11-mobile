package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Expected results are raw original-x86 outputs, not a second Java formula. */
public final class PcMerchantArithmeticTest {
    public static void main(String[] args)throws Exception{
        int rows=0,checks=0;
        try(var stream=PcMerchantArithmeticTest.class.getResourceAsStream("/pc-merchant-native.tsv")){
            if(stream==null)throw new AssertionError("Native oracle missing");
            var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));reader.readLine();String line;
            while((line=reader.readLine())!=null){
                String[] s=line.split("\t");int[] v=new int[8];for(int n=0;n<v.length;n++)v[n]=(int)Long.parseLong(s[n+1]);
                int actual,state=v[4],draws=0;
                switch(s[0]){
                    case "quote": actual=PcMerchantRules.quote(v[0],v[1],v[2]);break;
                    case "buymax": case "sellmax": actual=PcMerchantRules.maximum(s[0].equals("buymax"),v[0],v[1],v[2],v[3]);break;
                    case "initial": case "monthly": {
                        var p=s[0].equals("initial")?PcMerchantRules.initial(v[0],v[1],v[4]):PcMerchantRules.monthly(v[1],v[2],v[4]);
                        actual=p.rate;state=p.state;draws=p.draws;break;
                    }
                    case "uniform": case "percent": {
                        var random=new PcMerchantRules.Random(v[4]);actual=s[0].equals("uniform")?random.uniform(v[0]):random.percent(v[0])?1:0;
                        state=random.state;draws=random.draws;break;
                    }
                    default: throw new AssertionError("Unknown oracle row");
                }
                if(actual!=v[5]||state!=v[6]||draws!=v[7])throw new AssertionError(line+" got="+actual+","+Integer.toUnsignedString(state)+","+draws);
                rows++;checks+=3;
            }
        }
        if(rows!=3865)throw new AssertionError("Oracle coverage changed: "+rows);
        System.out.println("PASS PcMerchantArithmeticTest rows="+rows+" checks="+checks+" (arithmetic only; gameplay price/quantity integration pending)");
    }
}
