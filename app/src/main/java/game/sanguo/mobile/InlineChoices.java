package game.sanguo.mobile;

import android.app.Activity;
import android.view.Gravity;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Small, finite choices are visible controls, not a searchable navigation destination. */
final class InlineChoices<T> extends LinearLayout {
    private final Map<T,Button> buttons=new LinkedHashMap<>();
    private final Function<T,String> label;
    InlineChoices(Activity a,World w,List<T> options,int columns,Function<T,String> label,
                  Predicate<T> enabled,T selected,Consumer<T> chosen){
        super(a);this.label=label;setOrientation(VERTICAL);
        columns=Math.max(1,Math.min(columns,options.size()));LinearLayout row=null;
        for(int i=0;i<options.size();i++){
            if(i%columns==0){row=new LinearLayout(a);addView(row,new LayoutParams(-1,-2));}
            T item=options.get(i);Button button=CompactButtons.create(a);button.setText(label.apply(item));
            button.setGravity(Gravity.CENTER);button.setMaxLines(2);button.setTextSize(11);
            button.setPadding(UiTheme.dp(a,2),UiTheme.dp(a,7),UiTheme.dp(a,2),UiTheme.dp(a,7));button.setEnabled(enabled.test(item));
            if(GameIcon.supports(item)){
                android.graphics.drawable.Drawable icon=GameIcon.drawable(a,w,item);int size=UiTheme.dp(a,28);
                icon.setBounds(0,0,size,size);button.setCompoundDrawables(null,icon,null,null);button.setCompoundDrawablePadding(UiTheme.dp(a,2));
            }
            button.setTag("choice."+item.toString());
            row.addView(button,new LayoutParams(0,UiTheme.dp(a,GameIcon.supports(item)?80:56),1));buttons.put(item,button);
            button.setOnClickListener(v->{select(item);chosen.accept(item);});
        }
        if(row!=null&&options.size()%columns!=0)for(int i=options.size()%columns;i<columns;i++)row.addView(new android.view.View(a),new LayoutParams(0,1,1));
        select(selected);
    }
    void select(T selected){for(Map.Entry<T,Button> e:buttons.entrySet()){
        boolean active=Objects.equals(selected,e.getKey());Button b=e.getValue();b.setSelected(active);
        b.setContentDescription(label.apply(e.getKey()).replace('\n',' ')+(active?"，已选择":b.isEnabled()?"，点选":"，库存不足"));
    }}
}
