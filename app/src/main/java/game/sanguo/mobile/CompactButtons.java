package game.sanguo.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;

/** Shared compact visual surface; standard controls keep a 48dp touch target. */
final class CompactButtons {
    private CompactButtons() {}
    static Button create(Context context) {
        float density=context.getResources().getDisplayMetrics().density;
        Button b=new Button(context);
        b.setAllCaps(false);b.setTextSize(12);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setIncludeFontPadding(false);b.setGravity(Gravity.CENTER);b.setMaxLines(2);b.setEllipsize(TextUtils.TruncateAt.END);
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(Math.round(48*density));b.setMinimumHeight(Math.round(48*density));
        b.setPadding(Math.round(6*density),0,Math.round(6*density),0);
        b.setStateListAnimator(null);b.setElevation(0);b.setBackgroundTintList(null);
        int[][] states={{-android.R.attr.state_enabled},{android.R.attr.state_selected},{android.R.attr.state_focused},{}};
        b.setTextColor(new ColorStateList(states,new int[]{0xff81909f,0xffd7fff3,0xffeef6ff,0xffe5edf5}));
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(new ColorStateList(states,new int[]{0xff182632,0xff244d50,0xff2b4356,0xff213443}));
        shape.setCornerRadius(8*density);
        shape.setStroke(Math.max(1,Math.round(density)),new ColorStateList(states,new int[]{0xff243541,0xff6ddcc5,0xff89b9dd,0xff385263}));
        InsetDrawable surface=new InsetDrawable(shape,Math.round(2*density),Math.round(6*density),Math.round(2*density),Math.round(6*density));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x466ddcc5),surface,null));
        return b;
    }
}
