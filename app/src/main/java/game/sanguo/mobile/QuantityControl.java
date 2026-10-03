package game.sanguo.mobile;

import android.text.*;
import android.view.Gravity;
import android.widget.*;

/** One quantity with synchronized touch, exact entry and stock presets. */
final class QuantityControl extends LinearLayout {
    private final SeekBar slider;
    private final EditText input;
    private Button half,maximum;
    private final TextView caption,error;
    private final String label;
    private int min,max,value;
    private boolean syncing;
    private Runnable changed=()->{};
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener focusLayout=this::revealInput;
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnGlobalLayoutListener(focusLayout);}
    @Override protected void onDetachedFromWindow(){if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnGlobalLayoutListener(focusLayout);super.onDetachedFromWindow();}
    private void revealInput(){
        if(input==null||!input.hasFocus()||!input.isShown())return;
        android.view.View row=(android.view.View)input.getParent();
        int top=row.getTop(),bottom=error.getVisibility()==VISIBLE?error.getBottom():row.getBottom();
        // Include the quantity name whenever it fits; exact entry must keep its context.
        for(android.view.ViewParent parent=getParent();parent!=null;parent=parent.getParent())if(parent instanceof ScrollView){
            ScrollView viewport=(ScrollView)parent;
            if(bottom-caption.getTop()<=viewport.getHeight()-viewport.getPaddingTop()-viewport.getPaddingBottom())top=caption.getTop();
            break;
        }
        requestRectangleOnScreen(new android.graphics.Rect(0,top,getWidth(),bottom),true);
    }
    QuantityControl(MainActivity a,String label,int min,int max,int initial){
        super(a);this.label=label;setOrientation(VERTICAL);
        caption=a.text("",13,a.paper);addView(caption);
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        input=new EditText(a);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);
        input.setTextColor(a.paper);input.setContentDescription(label+"数量");input.setSelectAllOnFocus(true);input.setOnFocusChangeListener((v,focused)->{if(focused)post(this::revealInput);});
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setOnEditorActionListener((v,action,event)->{if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_DONE)return false;((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);input.clearFocus();return true;});
        row.addView(input,new LayoutParams(0,a.dp(48),1));
        row.addView(half=a.button("一半",v->set(Math.max(this.min,this.max/2))),new LayoutParams(a.dp(56),a.dp(48)));
        row.addView(maximum=a.button("最大",v->set(this.max)),new LayoutParams(a.dp(56),a.dp(48)));addView(row);
        error=a.text("",12,0xfff2aa9e);error.setContentDescription(label+"输入错误");error.setVisibility(GONE);
        error.setPadding(a.dp(4),a.dp(4),a.dp(4),a.dp(4));error.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);addView(error);
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
                showError(valid);
                value=entered;syncing=true;slider.setProgress(Math.max(0,Math.min(QuantityControl.this.max,entered)-QuantityControl.this.min));syncing=false;changed.run();post(QuantityControl.this::revealInput);
            }
        });
        bounds(min,max);set(initial);
    }
    String draftValue(){return input.getText().toString();}
    void restoreValue(String raw){input.setText(raw);}
    void inputDescription(String name){input.setContentDescription(name);}
    void rangeDescription(String description){caption.setText(description);}
    int value(){return value;}
    boolean valid(){return value>=min&&value<=max;}
    void onChange(Runnable action){changed=action;}
    private void showError(boolean valid){error.setText(valid?"":max<min?"当前没有可用的"+label+"额度":"请输入 "+min+"–"+max+" 之间的"+label+"数量");error.setVisibility(valid?GONE:VISIBLE);}
    void bounds(int low,int high){
        if(min==low&&max==high&&caption.length()>0)return;
        min=low;max=high;slider.setMax(Math.max(0,max-min));slider.setEnabled(max>=min);half.setEnabled(max>=min);maximum.setEnabled(max>=min);
        caption.setText(label+(max<min?" · 暂无可用额度":" · 可选 "+min+"–"+max));
        syncing=true;slider.setProgress(Math.max(0,Math.min(max,value)-min));syncing=false;
        if(input.length()>0)showError(valid());
    }
    void set(int n){
        if(max<min)return;
        value=Math.max(min,Math.min(max,n));syncing=true;input.setText(Integer.toString(value));error.setText("");error.setVisibility(GONE);slider.setProgress(value-min);syncing=false;changed.run();
    }
}
