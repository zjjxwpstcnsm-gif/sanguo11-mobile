package game.sanguo.mobile;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import game.sanguo.core.World;

/** Aligned numerical hierarchy without decorative animation or per-frame work. */
final class CommandStats extends LinearLayout {
    private CommandStats(Context context,String[] labels,int[] values){
        super(context);setPadding(UiTheme.dp(context,4),UiTheme.dp(context,8),UiTheme.dp(context,4),UiTheme.dp(context,8));
        setBackground(UiTheme.surface(context,0xff22333a,0xff17262f,12));
        for(int i=0;i<labels.length;i++){
            LinearLayout cell=new LinearLayout(context);cell.setOrientation(VERTICAL);cell.setGravity(Gravity.CENTER);
            TextView number=new TextView(context);UiTheme.text(number);number.setGravity(Gravity.CENTER);number.setTypeface(null,Typeface.BOLD);number.setTextColor(i==0?UiTheme.BRASS:UiTheme.TEXT);
            number.setSingleLine();number.setAutoSizeTextTypeUniformWithConfiguration(11,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            number.setText(String.format(java.util.Locale.ROOT,"%,d",values[i]));
            cell.addView(number,new LayoutParams(-1,UiTheme.dp(context,24)));
            TextView label=new TextView(context);UiTheme.text(label);label.setText(labels[i]);label.setTextColor(UiTheme.MUTED);label.setTextSize(10);label.setGravity(Gravity.CENTER);
            cell.addView(label,new LayoutParams(-1,UiTheme.dp(context,18)));cell.setContentDescription(labels[i]+" "+values[i]);
            addView(cell,new LayoutParams(0,-2,1));
        }
    }
    static CommandStats city(Context c,World.City city){return new CommandStats(c,new String[]{"金","粮","兵力","城防"},new int[]{city.gold,city.food,city.troops,city.defense});}
    static CommandStats unit(Context c,World.Unit unit){return new CommandStats(c,new String[]{"兵力","气力","携粮"},new int[]{unit.troops,unit.energy,unit.food});}
}
