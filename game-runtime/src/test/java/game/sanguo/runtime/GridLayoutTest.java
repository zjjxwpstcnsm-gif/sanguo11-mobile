package game.sanguo.runtime;
import game.sanguo.api.GridLayout;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class GridLayoutTest {
 public static void main(String[] args)throws Exception{
  int checks=0;
  try(BufferedReader lines=new BufferedReader(new InputStreamReader(GridLayoutTest.class.getResourceAsStream("/architecture/grid-layout.csv"),StandardCharsets.UTF_8))){
   for(String line;(line=lines.readLine())!=null;){if(line.startsWith("#")||line.isBlank())continue;
    String[] n=line.split(",");GridLayout grid=new GridLayout(Integer.parseInt(n[0])!=0,Double.parseDouble(n[1]),Integer.parseInt(n[2]),Integer.parseInt(n[3]));
    int q=Integer.parseInt(n[4]),r=Integer.parseInt(n[5]);double x=Double.parseDouble(n[6]),z=Double.parseDouble(n[7]);
    if(grid.x(q,r)!=x||grid.z(q,r)!=z||grid.q(x,z)!=q||grid.r(x,z)!=r)throw new AssertionError(line);checks+=4;
   }
  }
  System.out.println("GridLayoutTest PASS: "+checks+" shared Java/C# coordinate assertions");
 }
}
