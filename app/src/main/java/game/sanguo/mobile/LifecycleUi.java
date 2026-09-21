package game.sanguo.mobile;

import android.app.*;
import android.view.View;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Biographical setup and a persisted, mandatory ruler succession choice. */
final class LifecycleUi {
    private final Activity a;private final World w;private final Consumer<World.Result> apply;
    LifecycleUi(Activity a,World w,Consumer<World.Result> apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String text){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("返回",null).show();}
    private void confirm(String title,String text,Runnable action){new AlertDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("执行",(d,n)->action.run()).setNegativeButton("取消",null).show();}
    private ScrollView scroll(LinearLayout panel){ScrollView s=new ScrollView(a);s.addView(panel);return s;}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(a);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(18,12,18,20);return p;}
    private void text(LinearLayout p,String value){TextView t=new TextView(a);t.setText(value);t.setTextSize(16);t.setTextColor(0xffebe3cd);t.setPadding(0,8,0,8);p.addView(t);}
    private void button(LinearLayout p,String value,Runnable action){Button b=CompactButtons.create(a);b.setText(value);b.setAllCaps(false);b.setMinHeight((int)(48*a.getResources().getDisplayMetrics().density));b.setOnClickListener(v->action.run());p.addView(b);}
    View succession(){LinearLayout p=panel();int departed=w.life.departedRuler();
        text(p,"君主继承 · "+w.officer(departed).name+(w.life.state(departed)==Lifecycle.State.DEAD?"已故":"已离任"));text(p,"请选择本势力的继承人。城池、资源、外交与军团归属保留；完成前暂停其他命令。可随时保存，读档后继续选择。");
        for(World.Officer o:w.life.successors())button(p,"继承 · "+o.name,()->confirm("立"+o.name+"为君主","统率"+o.leadership+" · 政治"+o.politics+" · 魅力"+o.charm+"\n原任官职解除，忠诚为100；其他武将按与新君主的相性差损失5至20忠诚，配偶与义兄弟免降。",()->apply.accept(w.life.inherit(departed,o.id))));
        return scroll(p);
    }
    void menu(){new AlertDialog.Builder(a).setTitle("生卒与继承").setItems(new String[]{"自然死亡 · "+(w.life.enabled()?"开启":"关闭"),"武将生卒资料","事件履历"},(d,n)->{
        if(n==0)confirm("自然死亡设置",w.life.enabled()?"关闭新的寿终判定，已故人物不复活。":"仅对已配置预计没年的武将执行月初寿终判定；未知年份不会猜测。",()->apply.accept(w.life.toggle()));
        else if(n==1)people();else{LinearLayout p=panel();if(w.life.history().isEmpty())text(p,"尚无登场、死亡或继承事件");for(String s:w.life.history())text(p,s);new AlertDialog.Builder(a).setTitle("事件履历").setView(scroll(p)).setPositiveButton("返回",null).show();}
    }).setNegativeButton("返回",null).show();}
    private void people(){List<World.Officer> list=new ArrayList<>(w.officers);list.sort(Comparator.comparing(o->o.name));
        EditText query=new EditText(a);query.setSingleLine(true);query.setHint("武将姓名");query.setContentDescription("生卒武将姓名");
        new AlertDialog.Builder(a).setTitle("查找武将生卒").setView(query).setPositiveButton("查找",(d,n)->{
            list.removeIf(o->!o.name.contains(query.getText().toString().trim()));
            if(list.isEmpty()){info("武将生卒","没有符合条件的武将");return;}
            new AlertDialog.Builder(a).setTitle("武将生卒资料").setItems(list.stream().map(o->o.name+" · "+w.life.state(o.id).label).toArray(String[]::new),(dialog,index)->detail(list.get(index))).setNegativeButton("返回",null).show();
        }).setNegativeButton("返回",null).show();
    }
    private void detail(World.Officer o){AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(o.name+" · 生卒").setMessage(w.life.describe(o.id)).setNegativeButton("返回",null);
        if(w.life.state(o.id)!=Lifecycle.State.DEAD&&!w.commandsBlocked())dialog.setPositiveButton("编辑生卒",(d,n)->edit(o));dialog.show();}
    private EditText year(LinearLayout p,String title,int value){TextView t=new TextView(a);t.setText(title+"（0为未知）");p.addView(t);EditText e=new EditText(a);e.setSingleLine(true);e.setInputType(2);e.setContentDescription(title);e.setText(Integer.toString(value));p.addView(e);return e;}
    void edit(World.Officer o){Lifecycle.Life old=w.life.life(o.id);LinearLayout p=panel();
        EditText birth=year(p,"出生年",old==null?0:old.birth),appearance=year(p,"登场年",old==null?0:old.appearance),death=year(p,"预计没年",old==null?0:old.expectedDeath);
        int home=old!=null?old.home:o.cityId>=0?o.cityId:w.home().id;
        TextView hint=new TextView(a);hint.setText("修改生卒属于局面编辑，将标记此局为已编辑。登场据点："+w.city(home).name+"。未登场人物必须由剧本配置。");p.addView(hint);
        new AlertDialog.Builder(a).setTitle("编辑生卒 · "+o.name).setView(scroll(p)).setPositiveButton("预览",(d,n)->{
            try{Editor.Draft draft=w.editor.lifetime(o.id,Integer.parseInt(birth.getText().toString()),Integer.parseInt(appearance.getText().toString()),Integer.parseInt(death.getText().toString()),home,w.life.state(o.id));
                if(!draft.valid()){info("无法编辑",draft.error);return;}confirm("生卒编辑预览",draft.summary,()->apply.accept(w.editor.apply(draft)));
            }catch(NumberFormatException ex){info("无法编辑","年份须为0—9999整数");}
        }).setNegativeButton("取消",null).show();
    }
}
