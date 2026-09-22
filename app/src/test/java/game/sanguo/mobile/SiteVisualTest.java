package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.nio.file.*;
public final class SiteVisualTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        Set<String> models=new HashSet<>();List<String> mapping=new ArrayList<>();
        for(World.City c:w.cities){SiteVisual v=new SiteVisual(w,c,g.grid);models.add(v.model);
            for(int lod=0;lod<3;lod++)check(Files.isRegularFile(Path.of("app/src/main/assets/3d/sites/"+v.model+"-lod"+lod+".glb")),"runtime asset "+c.name);
            check(v.cells.equals(SiteFootprint.cells(c)),"exact footprint");for(Hex h:v.cells)check(w.cityAt(h)==c,"seven-cell site identity");
            if(c.kind==World.SiteKind.PORT){boolean has=false,aligned=false;for(Hex h:c.hex.neighbors())if(w.army.water(h)){has=true;float a=(float)Math.atan2(g.grid.x(h)-g.grid.x(c.hex),g.grid.z(h)-g.grid.z(c.hex));if(Math.abs(a-v.yaw)<.001)aligned=true;}check(!has||aligned,"actual navigable water orientation");}
            mapping.add(c.id+"\t"+c.name+"\t"+v.model+"\t"+v.yaw+"\t"+v.scale);
        }
        check(models.size()==5,"all three city variants plus port gate");check(Arrays.equals(before,SaveCodec.encode(w)),"presentation never mutates save");
        for(World.SiteKind kind:World.SiteKind.values()){World.City custom=new World.City(99999,"自定义",w.cities.get(0).hex,-1);custom.kind=kind;SiteVisual v=new SiteVisual(w,custom,g.grid);check(models.contains(v.model),"custom type defaults");}
        World.City city=w.cities.get(0);city.defense=0;SiteVisual damaged=new SiteVisual(w,city,g.grid);check(damaged.damage==2,"damage updates");city.owner=-1;MapSceneSnapshot s=new MapSceneSnapshot(g,w,city.hex,-1);check(s.items.get(0).color==FactionColors.color(w,-1),"ownership updates");String key=s.items.get(0).key;w.cities.remove(city);s=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);check(s.items.stream().noneMatch(i->i.key.equals(key)),"deleted site removed");
        int lod=0;for(int i=0;i<100;i++)check(SiteVisual.lod(17,lod)==0,"LOD hysteresis near");check(SiteVisual.lod(50,1)==2&&SiteVisual.lod(45,2)==2&&SiteVisual.lod(39,2)==1,"LOD hysteresis far");
        SceneCamera c=new SceneCamera();c.width=1080;c.height=1920;c.span=12;
        for(int facing:new int[]{-1,1}){c.facing=facing;for(int r=0;r<g.height;r+=11)for(int q=0;q<g.width;q+=9){Hex h=new Hex(q,r);if(!g.valid(h))continue;float x=g.grid.x(h),z=g.grid.z(h);c.x=x+.7f;c.z=z-.5f;check(h.equals(g.surface.pick(c,c.screenX(x),c.screenY(z,g.surface.at(h)))),"both camera directions height pick");}}
        Files.write(Path.of("docs/3d/site-mapping.tsv"),mapping);
        System.out.println("PASS S03: "+checks+" checks; "+mapping.size()+" national sites mapped; 5 model families; custom fallback, ownership, damage, deletion, reverse-camera picks");
    }
}
