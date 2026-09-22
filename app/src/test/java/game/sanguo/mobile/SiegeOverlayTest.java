package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class SiegeOverlayTest {
    public static void main(String[] args)throws Exception{
        World w=new World(30,30);World.City c=new World.City(1,"测试",new Hex(10,10),0);w.cities.add(c);
        World.Unit enemy=new World.Unit(1,1,1,World.Weapon.SPEAR,new Hex(13,10),6000,12000);w.units.add(enemy);
        int checks=0;
        for(World.SiteKind kind:World.SiteKind.values()){
            c.kind=kind;w.invalidateSiteIndex();enemy.hex=new Hex(kind==World.SiteKind.CITY?13:12,10);
            for(Hex selected:SiteFootprint.cells(c)){
                SiegeOverlay overlay=SiegeOverlay.selected(w,selected);
                if(!overlay.cells.equals(SiegeRules.cells(w,c))||!overlay.enemies.contains(enemy.hex))throw new AssertionError("selection/core parity");checks++;
            }
            if(!SiegeOverlay.selected(w,new Hex(20,20)).cells.isEmpty())throw new AssertionError("clear previous range");checks++;
        }
        enemy.owner=0;
        if(!SiegeOverlay.selected(w,c.hex).enemies.isEmpty())throw new AssertionError("ally no red enemy marker");checks++;
        if(!SiegeOverlay.selected(w,null).cells.isEmpty())throw new AssertionError("null selection");checks++;
        System.out.println("PASS: "+checks+" siege overlay contracts");
    }
}
