package game.sanguo.mobile;

import android.content.Context;
import android.widget.*;
import java.util.function.IntConsumer;

/** App-owned media settings only. Existing effect gain and master mute retain their behavior. */
final class MediaVolumeControls {
    private MediaVolumeControls(){}
    static void add(Context context,LinearLayout panel,SoundEffects sounds){
        slider(context,panel,"音乐音量","media.music-volume",sounds.musicVolume(),sounds::musicVolume);
        slider(context,panel,"语音音量","media.voice-volume",sounds.voiceVolume(),sounds::voiceVolume);
        Button resume=new Button(context);resume.setText("继续播放音乐");resume.setContentDescription("恢复当前音乐播放");resume.setOnClickListener(v->sounds.resumeEstablishedMusic());panel.addView(resume);
    }
    private static void slider(Context context,LinearLayout panel,String title,String tag,int current,IntConsumer change){
        TextView label=new TextView(context);label.setTextColor(UiTheme.TEXT);label.setTextSize(15);label.setText(title+" "+current+"%");UiTheme.text(label);panel.addView(label);
        SeekBar slider=new SeekBar(context);slider.setTag(tag);slider.setContentDescription(title);slider.setMax(100);slider.setProgress(current);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar view,int value,boolean user){if(user){change.accept(value);label.setText(title+" "+value+"%");}}
            public void onStartTrackingTouch(SeekBar view){}
            public void onStopTrackingTouch(SeekBar view){}
        });panel.addView(slider);
    }
}
