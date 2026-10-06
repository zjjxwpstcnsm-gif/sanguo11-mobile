package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

/** Standalone test-package diagnostic. Never included in the production manifest. */
public final class SessionAAudioCaptureActivity extends Activity {
    private String run;
    private int seconds;private int sampleRate;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text=new TextView(this);text.setText("Android game mix measurement");setContentView(text);
        run=getIntent().getStringExtra("run");seconds=getIntent().getIntExtra("seconds",130);sampleRate=getIntent().getIntExtra("sampleRate",44100);
        if(Build.VERSION.SDK_INT<29||run==null||!run.matches("[A-Za-z0-9_-]{1,64}")||seconds<1||seconds>180||(sampleRate!=44100&&sampleRate!=48000))
            throw new IllegalArgumentException("Bounded Android29 game capture required");
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            throw new IllegalStateException("Test-only recording permission must be explicitly granted and restored by the transaction");
        MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(),91);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=91)return;
        if(result!=RESULT_OK||data==null){finish();return;}
        Intent service=new Intent(this,SessionAAudioCaptureService.class)
            .putExtra("run",run).putExtra("seconds",seconds).putExtra("sampleRate",sampleRate).putExtra("projectionResult",result)
            .putExtra("projectionData",data);
        startForegroundService(service);finish();
    }
}
