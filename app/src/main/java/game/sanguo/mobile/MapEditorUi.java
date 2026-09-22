package game.sanguo.mobile;

import android.content.Intent;

/** PK entry launches an isolated production editor, never mutates MainActivity.world. */
final class MapEditorUi {
    private final MainActivity activity;
    MapEditorUi(MainActivity activity){this.activity=activity;}
    void show(){activity.startActivity(new Intent(activity,MapEditorActivity.class));}
}
