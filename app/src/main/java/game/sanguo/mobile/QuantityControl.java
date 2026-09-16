package game.sanguo.mobile;

import android.text.*;
import android.view.Gravity;
import android.widget.*;

/** One quantity with synchronized touch, exact entry and stock presets. */
final class QuantityControl extends LinearLayout {
    private final SeekBar slider;
    private final EditText input;
    private final TextView caption;
    private final String label;
    private int min,max,value;
    private boolean syncing;
    private Runnable changed=()->{};
    QuantityControl(MainActivity a,String label,int min,int max,int initial){
        super(a);this.label=label;setOrientation(VERTICAL);
        caption=a.text("",13,a.paper);addView(caption);
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        input=new EditText(a);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);
        input.setTextColor(a.paper);input.setContentDescription(label+"数量");input.setSelectAllOnFocus(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        row.addView(input,new LayoutParams(0,a.dp(48),1));
        row.addView(a.button("一半",v->set(Math.max(this.min,this.max/2))),new LayoutParams(a.dp(56),a.dp(48)));
        row.addView(a.button("最大",v->set(this.max)),new LayoutParams(a.dp(56),a.dp(48)));addView(row);
        slider=new SeekBar(a);slider.setContentDescription(label+"滑块");addView(slider,new LayoutParams(-1,a.dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){if(user)set(QuantityControl.this.min+progress);}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){}
            public void afterTextChanged(Editable s){
                if(syncing)return;
                int entered;try{entered=Integer.parseInt(s.toString());}catch(NumberFormatException e){entered=-1;}
                boolean valid=entered>=QuantityControl.this.min&&entered<=QuantityControl.this.max;
                input.setError(valid?null:"范围 "+QuantityControl.this.min+"–"+QuantityControl.this.max);
                value=entered;syncing=true;slider.setProgress(Math.max(0,Math.min(QuantityControl.this.max,entered)-QuantityControl.this.min));syncing=false;changed.run();
            }
        });
        bounds(min,max);set(initial);
    }
    int value(){return value;}
    boolean valid(){return value>=min&&value<=max;}
    void onChange(Runnable action){changed=action;}
    void bounds(int low,int high){min=low;max=Math.max(low,high);slider.setMax(max-min);caption.setText(label+" · 可选 "+min+"–"+max);set(Math.max(min,Math.min(max,value)));}
    void set(int n){
        value=Math.max(min,Math.min(max,n));syncing=true;input.setText(Integer.toString(value));input.setError(null);slider.setProgress(value-min);syncing=false;changed.run();
    }
}
