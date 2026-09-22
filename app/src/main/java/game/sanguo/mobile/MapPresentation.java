package game.sanguo.mobile;
import android.os.Bundle;
import game.sanguo.core.*;

/** Shared world/selection/camera contract; commands remain owned by MainActivity. */
interface MapPresentation {
    void setWorld(World world,Hex selected,int moving);
    void fit();
    void focus(Hex h);
    void center(Hex h);
    void saveCamera(Bundle b);
    void restoreCamera(Bundle b);
}
