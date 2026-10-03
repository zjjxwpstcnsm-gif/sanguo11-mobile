package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/** Cancellable read/prepare work. Only the current, still visible request may publish UI. */
final class UiReadTask implements AutoCloseable {
    private final Activity activity;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"ui-read"));
    private AlertDialog pending;
    private Future<?> job;
    private long generation;
    private boolean closed;

    UiReadTask(Activity activity){this.activity=activity;}
    <T> void run(String label,Callable<T> read,Consumer<T> ready,Consumer<Exception> failed){
        if(closed||activity.isFinishing()||activity.isDestroyed()||pending!=null)return;
        long request=++generation;
        LinearLayout content=new LinearLayout(activity);content.setGravity(Gravity.CENTER_VERTICAL);
        int pad=UiTheme.dp(activity,20);content.setPadding(pad,pad,pad,pad);
        content.addView(new ProgressBar(activity),new LinearLayout.LayoutParams(UiTheme.dp(activity,32),UiTheme.dp(activity,32)));
        TextView text=new TextView(activity);UiTheme.text(text);text.setText(label);text.setTextColor(UiTheme.TEXT);text.setTextSize(16);text.setPadding(pad,0,0,0);
        content.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        AlertDialog dialog=new AlertDialog.Builder(activity).setView(content).setNegativeButton("取消",(d,n)->cancel(request)).create();
        dialog.setOnCancelListener(d->cancel(request));pending=dialog;dialog.show();UiTheme.dialog(dialog);
        job=worker.submit(()->{
            try{T value=read.call();activity.runOnUiThread(()->{if(finish(request))ready.accept(value);});}
            catch(Exception error){activity.runOnUiThread(()->{if(finish(request))failed.accept(error);});}
        });
    }
    private void cancel(long request){if(request==generation){generation++;pending=null;if(job!=null){job.cancel(false);job=null;}}}
    private boolean finish(long request){
        if(closed||request!=generation||activity.isFinishing()||activity.isDestroyed())return false;
        AlertDialog dialog=pending;pending=null;job=null;if(dialog!=null)dialog.dismiss();return true;
    }
    @Override public void close(){closed=true;generation++;if(pending!=null){pending.dismiss();pending=null;}worker.shutdownNow();}
}
