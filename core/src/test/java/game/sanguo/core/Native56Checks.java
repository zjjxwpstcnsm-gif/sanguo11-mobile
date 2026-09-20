package game.sanguo.core;
import java.util.*;
final class Native56Checks {
    static int checks;
    static World cached;
    static World world()throws Exception {if(cached==null)cached=ScenarioCatalog.load("heroes-250",0,56L);return cached;}
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static void pass(String name){System.out.println(name+" PASS cumulative="+checks);}
}
