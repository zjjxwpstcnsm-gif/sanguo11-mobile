package game.sanguo.core;
import java.util.*;
import static game.sanguo.core.Native56Checks.*;
public final class CityArtCatalogTest {
 public static void main(String[] args)throws Exception {
  Set<CityArtCatalog.Variant> styles=new HashSet<>();for(int id=20000;id<=20041;id++){styles.add(CityArtCatalog.variant(id));check(!CityArtCatalog.key(id).isEmpty(),"all 42 new art bindings");}
  check(styles.size()==6,"six visual families");check(!CityArtCatalog.key(20015).equals(CityArtCatalog.key(20017)),"independent capitals");pass("CityArtCatalogTest");
 }
}
