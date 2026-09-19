package game.sanguo.mobile;

import android.animation.*;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.*;
import android.view.*;
import android.widget.*;

/** Ink, jade and restrained brass. Shared surfaces, typography and native feedback. */
final class UiTheme {
    static final int INK=0xff0c141c, SURFACE=0xff17252f, EDGE=0xff30444d;
    static final int TEXT=0xffedf3f3, MUTED=0xffa4b6bf, JADE=0xff81e2c4, BRASS=0xffd7bc87;
    private UiTheme() {}
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable surface(Context c,int top,int bottom,float radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
        d.setCornerRadius(dp(c,radius));d.setStroke(dp(c,1),EDGE);return d;
    }
    static void panel(View v){
        v.setBackground(surface(v.getContext(),0xff1a2b35,0xff101d27,18));
        v.setElevation(dp(v.getContext(),12));v.setClipToOutline(true);
    }
    static void text(TextView v){
        v.setIncludeFontPadding(false);v.setFontFeatureSettings("tnum");
        v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        v.setLineSpacing(dp(v.getContext(),2),1f);
    }
    static void title(TextView v){
        CharSequence value=v.getText();int line=value.toString().indexOf('\n');
        if(line<0)return;
        SpannableString s=new SpannableString(value);
        s.setSpan(new ForegroundColorSpan(TEXT),0,line,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new StyleSpan(Typeface.BOLD),0,line,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(MUTED),line,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        v.setText(s);
    }
    static void press(Button b){
        StateListAnimator states=new StateListAnimator();
        AnimatorSet down=new AnimatorSet();down.playTogether(ObjectAnimator.ofFloat(b,"scaleX",.965f),ObjectAnimator.ofFloat(b,"scaleY",.965f));down.setDuration(70);
        AnimatorSet up=new AnimatorSet();up.playTogether(ObjectAnimator.ofFloat(b,"scaleX",1f),ObjectAnimator.ofFloat(b,"scaleY",1f));up.setDuration(130);up.setInterpolator(UiMotion.EASE);
        states.addState(new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled},down);
        states.addState(new int[]{},up);b.setStateListAnimator(states);
    }
    static void icon(Button b,String kind){
        Glyph d=new Glyph(kind,b.getTextColors());int size=dp(b.getContext(),15);d.setBounds(0,0,size,size);
        b.setCompoundDrawablePadding(dp(b.getContext(),4));b.setCompoundDrawables(d,null,null,null);
    }
    static void dialog(AlertDialog d){
        if(d.getWindow()==null)return;
        d.getWindow().setBackgroundDrawable(surface(d.getContext(),0xff1c2d37,0xff111e28,20));
        d.getWindow().setDimAmount(.42f);
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){
            Button b=d.getButton(which);if(b!=null){b.setTextColor(which==AlertDialog.BUTTON_POSITIVE?JADE:MUTED);b.setAllCaps(false);}
        }
    }
    static void search(EditText edit){
        text(edit);edit.setTextSize(14);edit.setPadding(dp(edit.getContext(),12),0,dp(edit.getContext(),8),0);
        edit.setBackground(surface(edit.getContext(),0xff101e28,0xff101e28,10));
        edit.setTextColor(TEXT);edit.setHintTextColor(MUTED);
        edit.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
    }
    /** Tiny vector icons, no bitmap allocation or asset decoding in a draw pass. */
    private static final class Glyph extends Drawable {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final String kind;final ColorStateList colors;
        Glyph(String kind,ColorStateList colors){this.kind=kind;this.colors=colors;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
        @Override public void draw(Canvas c){
            c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
            p.setColor(colors.getColorForState(getState(),TEXT));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(kind.equals("menu")){for(int y=5;y<=17;y+=6){c.drawLine(4,y,7,y,p);c.drawLine(11,y,20,y,p);}}
            else if(kind.equals("map")){c.drawRect(4,4,20,20,p);c.drawLine(9,4,9,20,p);c.drawLine(15,4,15,20,p);}
            else if(kind.equals("layers")){c.drawLine(4,7,20,7,p);c.drawLine(4,12,20,12,p);c.drawLine(4,17,20,17,p);c.drawCircle(9,7,2,p);c.drawCircle(16,12,2,p);c.drawCircle(11,17,2,p);}
            else {c.drawLine(4,12,19,12,p);c.drawLine(13,6,19,12,p);c.drawLine(19,12,13,18,p);}
            c.restore();
        }
        @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
