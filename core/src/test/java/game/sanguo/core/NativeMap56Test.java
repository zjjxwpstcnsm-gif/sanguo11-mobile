package game.sanguo.core;
public final class NativeMap56Test {
 public static void main(String[] args)throws Exception {
  LegacyMapCoordinateCheckpointTest.main(args);MapCoordinateTest.main(args);MapBoundsTest.main(args);SiteFootprintTest.main(args);SiteOverlapTest.main(args);NationalSiteCountTest.main(args);ScenarioNationalMapTest.main(args);SaveMapRevisionTest.main(args);CityArtCatalogTest.main(args);NoLegacyCityFallbackTest.main(args);
  System.out.println("NATIVE56 PASS total="+Native56Checks.checks);
 }
}
