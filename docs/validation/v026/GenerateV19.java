package game.sanguo.core;

import java.nio.file.*;
import java.util.Base64;

/** Run with the unchanged f194ee2 / v0.25 classes, before changing SaveCodec. */
public final class GenerateV19 {
    public static void main(String[] args)throws Exception {
        World w=StrategicManagementTest.fixture();
        if(!w.districts.configure(-1,"旧档军团",new int[]{11,12},Districts.Policy.CITY_ATTACK,20,12,true,true).ok)throw new AssertionError();
        if(!w.districts.settings(1,14000,6000,45000,true,true).ok)throw new AssertionError();
        // Actual v0.25 encoder and command, including its historical 100-gold fee.
        if(!w.domestic.transport(10,12,1,500,5000,1000,new int[4]).ok)throw new AssertionError();
        World.Unit u=StrategicManagementTest.unit(w,3,World.Weapon.SPEAR,new Hex(10,8));
        w.districts.units.put(u.id,1);AiOrders.Order o=w.aiOrders.get(u);o.home=11;o.target=20;o.staging=true;
        w.districts.cleanup();byte[] bytes=SaveCodec.encode(w);if(bytes[7]!=19)throw new AssertionError("Must use real v19 encoder");
        Files.writeString(Path.of(args[0]),Base64.getEncoder().encodeToString(bytes)+"\n");
    }
}
