package game.sanguo.mobile;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Recycled native lists inside the map side panel, with stable engine IDs. */
final class OverviewUi {
    private final MainActivity a; private final World w; private final ClientState state;
    OverviewUi(MainActivity a, World w, ClientState state) {this.a=a;this.w=w;this.state=state;}
    private LinearLayout column() {LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(a.dp(10),0,a.dp(10),0);return v;}
    private TextView text(String s,int size) {TextView v=a.text(s,size,a.paper);v.setPadding(a.dp(6),a.dp(6),a.dp(6),a.dp(6));return v;}
    private void heading(LinearLayout host,String title) {host.addView(text(title,21));}
    private void filter(LinearLayout host,String label,String[] labels,IntConsumer choose) {
        host.addView(a.button(label,v->new AlertDialog.Builder(a).setTitle(label).setItems(labels,(d,i)->choose.accept(i)).setNegativeButton("取消",null).show()),new LinearLayout.LayoutParams(-1,a.dp(48)));
    }
    private final class Rows<T> extends BaseAdapter {
        List<T> rows; TextView emptyView; final ToLongFunction<T> key; final Function<T,String> title,detail;
        Rows(List<T> rows,ToLongFunction<T> key,Function<T,String> title,Function<T,String> detail) {this.rows=rows;this.key=key;this.title=title;this.detail=detail;}
        public int getCount(){return rows.size();} public T getItem(int p){return rows.get(p);} public long getItemId(int p){return key.applyAsLong(rows.get(p));} public boolean hasStableIds(){return true;}
        public View getView(int p,View reuse,ViewGroup parent) {
            LinearLayout row;
            if(reuse instanceof LinearLayout)row=(LinearLayout)reuse;
            else {row=column();row.setMinimumHeight(a.dp(80));row.addView(text("",16));row.addView(text("",13));}
            ((TextView)row.getChildAt(0)).setText(title.apply(rows.get(p)));((TextView)row.getChildAt(0)).setTextColor(a.gold);
            ((TextView)row.getChildAt(1)).setText(detail.apply(rows.get(p)));return row;
        }
    }
    private <T> Rows<T> list(LinearLayout host,List<T> rows,ToLongFunction<T> key,Function<T,String> title,Function<T,String> detail,Consumer<T> select,String empty) {
        FrameLayout content=new FrameLayout(a);host.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        ListView list=new ListView(a);list.setDividerHeight(a.dp(1));list.setCacheColorHint(Color.TRANSPARENT);list.setContentDescription("概览列表");
        TextView blank=text(empty,16);blank.setGravity(Gravity.CENTER);content.addView(blank,new FrameLayout.LayoutParams(-1,-1));content.addView(list,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(blank);
        Rows<T> adapter=new Rows<>(rows,key,title,detail);adapter.emptyView=blank;list.setAdapter(adapter);list.setOnItemClickListener((p,v,index,id)->select.accept(adapter.getItem(index)));return adapter;
    }
    private EditText search(LinearLayout host,String hint,String value,Consumer<String> change){
        EditText field=new EditText(a);field.setSingleLine();field.setTextColor(a.paper);field.setHintTextColor(a.muted);field.setHint(hint);field.setContentDescription(hint);field.setText(value);host.addView(field,new LinearLayout.LayoutParams(-1,a.dp(48)));
        field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){change.accept(s.toString());}});return field;
    }
    View factions(){
        LinearLayout host=column();heading(host,"势力一览");
        final Rows<Integer>[] holder=new Rows[1];
        search(host,"搜索势力",state.factionQuery,q->{state.factionQuery=q;holder[0].rows=UiModels.factions(w,q);holder[0].notifyDataSetChanged();});
        holder[0]=list(host,UiModels.factions(w,state.factionQuery),i->i,i->w.faction(i),i->{int cities=0,officers=0,troops=0;for(World.City c:w.cities)if(c.owner==i){cities++;troops+=c.troops;}for(World.Officer o:w.officers)if(o.owner==i)officers++;return cities+"城 · "+officers+"将 · 驻军"+troops+(w.alive(i)?"":" · 已灭亡");},i->{state.cityOwner=i;state.cityQuery="";state.page="cities";a.refresh();},"没有符合条件的势力");return host;
    }
    View cities() {
        LinearLayout host=column();heading(host,"城池一览");
        final Rows<World.City>[] holder=new Rows[1];
        search(host,"搜索城市或势力",state.cityQuery,q->{state.cityQuery=q;holder[0].rows=UiModels.cities(w,state.citySort,q,state.cityOwner);holder[0].notifyDataSetChanged();});
        String[] owners=new String[w.factions.length+2];owners[0]="全部城池";owners[1]="中立城池";System.arraycopy(w.factions,0,owners,2,w.factions.length);
        filter(host,state.cityOwner==-1?"全部城池":state.cityOwner==-2?"中立城池":w.faction(state.cityOwner),owners,i->{state.cityOwner=i==0?-1:i==1?-2:i-2;a.refresh();});
        String[] sorts={"己方优先","金最多","粮最多","兵最多","驻将最多"};
        filter(host,"排序 · "+sorts[state.citySort],sorts,i->{state.citySort=i;a.refresh();});
        holder[0]=list(host,UiModels.cities(w,state.citySort,state.cityQuery,state.cityOwner),c->c.id,c->c.name+" · "+w.faction(c.owner),
            c->"金 "+c.gold+" · 粮 "+c.food+"\n兵 "+c.troops+" · 驻将 "+UiModels.officerCount(w,c.id)+"  › 定位",c->a.selectAndFocus(c.hex),"没有符合条件的城池 · 清空检索或选择全部城池");return host;
    }
    View officers() {
        LinearLayout host=column();heading(host,"武将一览");
        EditText search=new EditText(a);search.setSingleLine();search.setTextColor(a.paper);search.setHintTextColor(a.muted);search.setHint("搜索武将姓名");search.setContentDescription("搜索武将姓名");search.setText(state.query);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setOnEditorActionListener((v,action,event)->{if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)return false;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();return true;});host.addView(search,new LinearLayout.LayoutParams(-1,a.dp(48)));
        LinearLayout filters=new LinearLayout(a);host.addView(filters);
        Button faction=a.button(state.owner==-2?"在野武将":state.owner<0?"全部势力":w.faction(state.owner),v->{String[] labels=new String[w.factions.length+2];labels[0]="全部势力";System.arraycopy(w.factions,0,labels,1,w.factions.length);labels[labels.length-1]="在野武将";new AlertDialog.Builder(a).setTitle("按势力筛选").setItems(labels,(d,i)->{state.owner=i==w.factions.length+1?-2:i-1;a.refresh();}).show();});
        Button city=a.button(state.city<0?"全部城市":w.city(state.city).name,v->{String[] labels=new String[w.cities.size()+1];labels[0]="全部城市（含在途）";for(int i=0;i<w.cities.size();i++)labels[i+1]=w.cities.get(i).name;new AlertDialog.Builder(a).setTitle("按城市筛选").setItems(labels,(d,i)->{state.city=i==0?-1:w.cities.get(i-1).id;a.refresh();}).show();});
        filters.addView(faction,new LinearLayout.LayoutParams(0,a.dp(48),1));filters.addView(city,new LinearLayout.LayoutParams(0,a.dp(48),1));
        String[] sorts={"姓名","统率","武力","智力","政治","魅力"};filter(host,"排序 · "+sorts[state.officerSort],sorts,i->{state.officerSort=i;a.refresh();});
        Rows<World.Officer> adapter=list(host,UiModels.officers(w,state.query,state.owner,state.city,state.officerSort),o->o.id,o->o.name+" · "+UiModels.faction(w,o),
            o->"统 "+o.leadership+"  武 "+o.war+"  智 "+o.intelligence+"  政 "+o.politics+"  魅 "+o.charm+"\n"+o.role.label+" · 忠诚 "+o.loyalty+"\n"+UiModels.location(w,o)+" · "+UiModels.status(w,o),a::officerDetail,"没有符合筛选条件的武将\n试试清空姓名或选择全部城市");
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){state.query=s.toString();adapter.rows=UiModels.officers(w,state.query,state.owner,state.city,state.officerSort);adapter.notifyDataSetChanged();}public void afterTextChanged(Editable e){}});return host;
    }
    private String taskEmpty(){return !state.taskQuery.isEmpty()?"没有符合检索条件的任务":state.taskType==1?"当前没有建设中的设施":"当前没有在途任务\n可从城池下达命令";}
    View tasks() {
        LinearLayout host=column();heading(host,"任务 / 在途");
        final Rows<UiModels.Task>[] holder=new Rows[1];
        search(host,"搜索任务、武将或城市",state.taskQuery,q->{state.taskQuery=q;holder[0].rows=UiModels.tasks(w,state.taskType,q);holder[0].emptyView.setText(taskEmpty());holder[0].notifyDataSetChanged();});String[] types={"全部任务","建设","调动","运输","研究 / 培养","军备制造","行军"};
        filter(host,"筛选 · "+types[state.taskType],types,i->{state.taskType=i;a.refresh();});
        holder[0]=list(host,UiModels.tasks(w,state.taskType,state.taskQuery),t->t.id,t->t.title,t->t.detail,t->{
            AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(t.title).setMessage(t.detail).setNegativeButton("返回",null);
            if(t.marching!=null){dialog.setPositiveButton("定位部队",(d,n)->a.selectAndFocus(t.location));dialog.setNeutralButton("停止行军",(d,n)->a.applyResult(w.marches.stop(t.marching.id)));}
            else if(t.facility!=null) {dialog.setPositiveButton("定位城池",(d,n)->a.selectAndFocus(w.city(t.facility.cityId).hex));dialog.setNeutralButton("管理设施",(d,n)->a.domesticUi().facility(t.facility));}
            else if(t.production!=null){dialog.setPositiveButton("制造详情",(d,n)->new ArmyUi(a,w,a::applyResult,a::selectAndFocus).production(t.production));}
            else if(t.project!=null){dialog.setPositiveButton("定位研究城市",(d,n)->a.selectAndFocus(w.city(t.project.cityId).hex));}
            else if(t.abilityResearch!=null||t.abilityTraining!=null){dialog.setPositiveButton("定位城市",(d,n)->a.selectAndFocus(t.location));dialog.setNeutralButton("PK进度",(d,n)->new AbilityUi(a,w,a::applyResult).progress(w.city(t.abilityResearch!=null?t.abilityResearch.cityId:t.abilityTraining.cityId)));}
            else {dialog.setPositiveButton("定位目的地",(d,n)->a.selectAndFocus(w.city(t.mission.targetCity).hex));dialog.setNeutralButton("起点 / 改道",(d,n)->new AlertDialog.Builder(a).setTitle("任务操作").setItems(new String[]{"定位起点","定位当前位置","改道 / 返回"},(x,i)->{if(i==0)a.selectAndFocus(w.city(t.mission.sourceCity).hex);else if(i==1)a.selectAndFocus(t.location);else a.domesticUi().mission(t.mission);}).show());}
            dialog.show();
        },taskEmpty());return host;
    }
}
