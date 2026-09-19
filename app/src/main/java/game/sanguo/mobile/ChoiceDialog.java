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
    @SuppressWarnings("unchecked")
    static <T> void show(Activity a,World w,String title,List<T> options,Function<T,String> label,Consumer<T> next){
        if(!options.isEmpty()&&options.get(0) instanceof World.Officer){DataTable.choose(a,w,title,(List<World.Officer>)(List<?>)options,o->label.apply((T)o),o->next.accept((T)o),null);return;}
        if(!options.isEmpty()&&options.size()<=12&&(options.get(0) instanceof Enum<?>||options.get(0) instanceof Number)){
            final AlertDialog[] holder={null};
            InlineChoices<T> grid=new InlineChoices<>(a,w,options,Math.min(4,options.size()),label,item->true,null,item->{
                if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;
                holder[0].dismiss();next.accept(item);
            });
            AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(grid).setNegativeButton("取消",null).create();holder[0]=dialog;
            dialog.show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog);return;
        }
        float density=a.getResources().getDisplayMetrics().density;LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);EditText query=new EditText(a);query.setHint("搜索名称 / 所在地");query.setSingleLine();host.addView(query);
        List<T> rows=new ArrayList<>(options);ListView list=new ListView(a);list.setFastScrollEnabled(true);host.addView(list,new LinearLayout.LayoutParams(-1,Math.round(Math.max(150,Math.min(340,a.getResources().getConfiguration().screenHeightDp-150))*density)));
        ArrayAdapter<T> adapter=GameIcon.adapter(a,w,rows,label);list.setAdapter(adapter);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(host).setNegativeButton("取消",null).create();list.setOnItemClickListener((p,v,i,id)->{if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;T chosen=rows.get(i);dialog.dismiss();next.accept(chosen);});
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int n){rows.clear();for(T item:options)if(label.apply(item).contains(s.toString().trim()))rows.add(item);adapter.notifyDataSetChanged();}});
        dialog.show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}
