import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** Authors a named battle setup on the actual source map; executes no UI order. */
public final class NativeCityDisplacementFixture {
 public static void main(String[] args)throws Exception {
  World w=ScenarioCatalog.load("heroes-250",0);w.units.clear();for(World.Officer o:w.officers)o.unitId=-1;
  World.Officer actor=w.officers.stream().filter(o->o.owner==0&&o.role!=Strategy.Role.RULER&&!Arrays.asList("刘备","关羽","张飞","赵云","诸葛亮","曹操").contains(o.name)).findFirst().orElseThrow();
  World.Officer defender=w.officers.stream().filter(o->o.owner==1&&o.role!=Strategy.Role.RULER).findFirst().orElseThrow();
  World.City city=null;Hex rim=null,origin=null,target=null,stop=null;
  for(World.City c:w.cities)if(c.owner==1&&c.kind==World.SiteKind.CITY){for(Hex edge:SiteFootprint.cells(c)){
   if(edge.equals(c.hex))continue;int dq=edge.q-c.hex.q,dr=edge.r-c.hex.r;
   Hex a=new Hex(edge.q+3*dq,edge.r+3*dr),b=new Hex(edge.q+2*dq,edge.r+2*dr),s=new Hex(edge.q+dq,edge.r+dr);
   boolean legal=true;for(Hex h:new Hex[]{a,b,s})if(!w.sourceInside(h)||w.terrain[h.q][h.r]!=World.Terrain.PLAIN||w.cityAt(h)!=null||w.domestic.at(h)!=null)legal=false;
   if(legal){city=c;rim=edge;origin=a;target=b;stop=s;break;}
  }if(city!=null)break;}
  if(city==null)throw new AssertionError("No source-map plain approach to enemy city");
  actor.cityId=-1;actor.unitId=1;Arrays.fill(actor.aptitude,3);defender.cityId=-1;defender.unitId=2;defender.skillId="none";
  World.Unit a=new World.Unit(1,0,actor.id,World.Weapon.CAVALRY,origin,8000,30000),b=new World.Unit(2,1,defender.id,World.Weapon.SPEAR,target,8000,30000);a.energy=100;b.energy=100;b.status=War.Status.CONFUSED;b.statusTurns=1;w.units.add(a);w.units.add(b);w.nextUnitId=3;w.strategy.setSeed(29016);w.reports.rebase();
  byte[] bytes=SaveCodec.encode(w);w=SaveCodec.decode(bytes);Displacement.Preview p=w.war.tacticPreview(1,2,War.Tactic.ADVANCE);
  if(!p.valid()||!rim.equals(p.blocked)||p.targetPath.size()!=2)throw new AssertionError("City rim preview "+p.text+" "+p.targetPath);
  World reference=SaveCodec.decode(bytes);if(!reference.war.tactic(1,2,War.Tactic.ADVANCE).ok||!reference.unit(2).hex.equals(stop)||reference.cityAt(stop)!=null)throw new AssertionError("Real command does not stop outside actual city");
  Path output=Paths.get(args[0]);Files.createDirectories(output.getParent());if(Files.exists(output))throw new AssertionError("Preserve fixture provenance");Files.write(output,bytes);
  System.out.println("PASS source-map fixture city="+city.name+" id="+city.id+" footprint="+SiteFootprint.cells(city)+" actor="+origin+" target="+target+" stop="+stop+" blocked="+rim+" saveVersion="+java.nio.ByteBuffer.wrap(bytes,4,4).getInt()+" bytes="+bytes.length);
 }
}
