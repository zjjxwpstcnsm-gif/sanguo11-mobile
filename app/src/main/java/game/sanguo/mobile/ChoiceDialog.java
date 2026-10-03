package game.sanguo.mobile;

import android.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Searchable object picker used by command flows, with the chosen object resolved after filtering. */
final class ChoiceDialog {
    private ChoiceDialog(){}
    static <T> void enabledChoices(Activity a,World w,String title,List<T> options,Function<T,String> label,Predicate<T> enabled,Consumer<T> next){
        if(options.isEmpty()){new AlertDialog.Builder(a).setTitle(title).setMessage("当前没有可用战法").setPositiveButton("返回",null).show();return;}
        final AlertDialog[] holder={null};
        LinearLayout choices=new LinearLayout(a);choices.setOrientation(LinearLayout.VERTICAL);
        for(T item:options){
            String caption=label.apply(item);boolean available=enabled.test(item);Button button=CompactButtons.create(a);
            button.setText(caption);button.setTextSize(14);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);button.setAllCaps(false);button.setMaxLines(Integer.MAX_VALUE);
            button.setPadding(UiTheme.dp(a,14),UiTheme.dp(a,10),UiTheme.dp(a,14),UiTheme.dp(a,10));button.setMinHeight(UiTheme.dp(a,56));button.setEnabled(available);
            button.setTextColor(available?UiTheme.TEXT:UiTheme.MUTED);button.setTag("choice."+item.toString());button.setContentDescription(caption.replace('\n',' ')+(available?"，点选":"，不可选"));
            choices.addView(button,new LinearLayout.LayoutParams(-1,-2));button.setOnClickListener(v->{
                if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;
                holder[0].dismiss();next.accept(item);
            });
        }
        ScrollView scroll=new ScrollView(a);scroll.addView(choices);FrameLayout body=new FrameLayout(a);body.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        ViewTreeObserver.OnGlobalLayoutListener fit=()->{android.graphics.Rect frame=new android.graphics.Rect();body.getWindowVisibleDisplayFrame(frame);choices.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,body.getWidth()),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int height=Math.min(choices.getMeasuredHeight(),Math.max(UiTheme.dp(a,64),frame.height()-UiTheme.dp(a,180)));if(body.getLayoutParams().height!=height){body.getLayoutParams().height=height;body.requestLayout();}};
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(body).setNegativeButton("取消",null).create();
        body.getViewTreeObserver().addOnGlobalLayoutListener(fit);dialog.setOnDismissListener(d->body.getViewTreeObserver().removeOnGlobalLayoutListener(fit));
        holder[0]=dialog;dialog.show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog);
    }
    static <T> void show(Activity a,World w,String title,List<T> options,Function<T,String> label,Consumer<T> next){show(a,w,title,options,label,next,null);}
    @SuppressWarnings("unchecked")
    static <T> void show(Activity a,World w,String title,List<T> options,Function<T,String> label,Consumer<T> next,Runnable back){
        if(!options.isEmpty()&&options.get(0) instanceof World.Officer){DataTable.choose(a,w,title,(List<World.Officer>)(List<?>)options,o->label.apply((T)o),o->next.accept((T)o),back);return;}
        if(!options.isEmpty()&&options.size()<=12&&(options.get(0) instanceof Enum<?>||options.get(0) instanceof Number)){
            final AlertDialog[] holder={null};
            InlineChoices<T> grid=new InlineChoices<>(a,w,options,Math.min(4,options.size()),label,item->true,null,item->{
                if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;
                holder[0].dismiss();next.accept(item);
            });
            AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(title).setView(grid).setNegativeButton("取消",null);if(back!=null)builder.setNeutralButton("上一步",(d,n)->back.run());AlertDialog dialog=builder.create();if(back!=null)dialog.setOnCancelListener(d->back.run());holder[0]=dialog;
            dialog.show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog);return;
        }
        LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);host.setBackgroundColor(UiTheme.INK);int pad=UiTheme.dp(a,12);host.setPadding(pad,0,pad,UiTheme.dp(a,8));
        LinearLayout search=new LinearLayout(a);EditText query=new EditText(a);query.setHint("搜索名称 / 所在地");query.setContentDescription("选项搜索");query.setSingleLine();UiTheme.search(query);search.addView(query,new LinearLayout.LayoutParams(0,UiTheme.dp(a,48),1));
        Button clear=CompactButtons.create(a);clear.setText("清空");clear.setContentDescription("清空选项搜索");clear.setOnClickListener(v->query.setText(""));search.addView(clear,new LinearLayout.LayoutParams(UiTheme.dp(a,48),UiTheme.dp(a,48)));host.addView(search);
        List<T> rows=new ArrayList<>(options);List<String> labels=new ArrayList<>();for(T item:options)labels.add(label.apply(item).toLowerCase(Locale.ROOT));
        FrameLayout body=new FrameLayout(a);ListView list=new ListView(a);list.setTag("choices.list");list.setFastScrollEnabled(true);body.addView(list,new FrameLayout.LayoutParams(-1,-1));
        TextView empty=new TextView(a);UiTheme.text(empty);empty.setTextColor(UiTheme.MUTED);empty.setTextSize(14);empty.setGravity(Gravity.CENTER);empty.setText(options.isEmpty()?"当前没有可选对象":"没有匹配的选项\n清空搜索后可查看全部选项");body.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);host.addView(body,new LinearLayout.LayoutParams(-1,UiTheme.dp(a,240)));
        ArrayAdapter<T> adapter=GameIcon.adapter(a,w,rows,label);list.setAdapter(adapter);
        AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(title).setView(host).setNegativeButton("取消",null);if(back!=null)builder.setNeutralButton("上一步",(d,n)->back.run());AlertDialog dialog=builder.create();if(back!=null)dialog.setOnCancelListener(d->back.run());
        list.setOnItemClickListener((p,v,i,id)->{if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;T chosen=rows.get(i);dialog.dismiss();next.accept(chosen);});
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int n){rows.clear();String term=s.toString().trim().toLowerCase(Locale.ROOT);for(int i=0;i<options.size();i++)if(labels.get(i).contains(term))rows.add(options.get(i));adapter.notifyDataSetChanged();}});
        ViewTreeObserver.OnGlobalLayoutListener fit=()->{android.graphics.Rect frame=new android.graphics.Rect();host.getWindowVisibleDisplayFrame(frame);int height=Math.max(UiTheme.dp(a,64),Math.min(UiTheme.dp(a,340),frame.height()-UiTheme.dp(a,200)));if(body.getLayoutParams().height!=height){body.getLayoutParams().height=height;body.requestLayout();}};
        host.getViewTreeObserver().addOnGlobalLayoutListener(fit);dialog.setOnDismissListener(d->host.getViewTreeObserver().removeOnGlobalLayoutListener(fit));
        dialog.show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
