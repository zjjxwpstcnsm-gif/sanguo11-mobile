package game.sanguo.mobile;
import android.content.Context;
import android.content.pm.PackageInfo;
import game.sanguo.core.World;
/** Reads the installed target package, not an inlined BuildConfig from the test APK. */
final class Map61InstalledIdentity {
    static void verify(MapTap57Harness probe,World world)throws Exception {
        Context context=probe.test.getTargetContext();
        PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
        long code=info.getLongVersionCode();String name=info.versionName;
        probe.require("game.sanguo.mobile.dev".equals(info.packageName),"actual target package identity");
        probe.require(world.mapRevision==61,"unchanged formal map revision61");
        probe.require((code==61&&"0.61.0".equals(name))||(code==62&&"0.62.0-road-candidate".equals(name)),"explicit installed app identity: "+name+"/"+code);
    }
}
