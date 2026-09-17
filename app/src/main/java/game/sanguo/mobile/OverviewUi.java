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
        List<T> rows,lastRows; TextView emptyView; int page; Runnable updatePager=()->{};ListView nativeList;
        final String pageKey=state.page;
        String signature(){switch(pageKey){
            case "cities":return state.cityQuery+"|"+state.cityOwner+"|"+state.cityFilter+"|"+state.citySort+"|"+state.cityDistrict;
            case "officers":return state.query+"|"+state.owner+"|"+state.city+"|"+state.officerSort;
            case "tasks":return state.taskType+"|"+state.taskQuery;
            default:return state.factionQuery;
        }}
        int size(){return state.listPageSize==50?50:state.listPageSize==100?100:20;}
        int pages(){return Math.max(1,(rows.size()+size()-1)/size());}
        void changePage(int value){page=Math.max(0,Math.min(pages()-1,value));state.listPages.putInt(pageKey,page);state.cityPosition=0;state.cityTop=0;super.notifyDataSetChanged();if(nativeList!=null)nativeList.setSelection(0);updatePager.run();}
        @Override public void notifyDataSetChanged(){if(lastRows!=rows){page=0;lastRows=rows;state.cityPosition=0;state.cityTop=0;if(nativeList!=null)nativeList.setSelection(0);}page=Math.max(0,Math.min(page,pages()-1));state.listPages.putInt(pageKey,page);state.listPages.putString(pageKey+"Filter",signature());super.notifyDataSetChanged();updatePager.run();}
         final ToLongFunction<T> key; final Function<T,String> title,detail;
        Rows(List<T> rows,ToLongFunction<T> key,Function<T,String> title,Function<T,String> detail) {this.rows=rows;this.lastRows=rows;this.key=key;this.title=title;this.detail=detail;
            String signature=signature();
            page=signature.equals(state.listPages.getString(pageKey+"Filter"))?Math.min(state.listPages.getInt(pageKey,0),pages()-1):0;
            state.listPages.putString(pageKey+"Filter",signature);state.listPages.putInt(pageKey,page);}
        public int getCount(){return Math.max(0,Math.min(size(),rows.size()-page*size()));} public T getItem(int p){return rows.get(page*size()+p);} public long getItemId(int p){return key.applyAsLong(getItem(p));} public boolean hasStableIds(){return true;}
        public View getView(int p,View reuse,ViewGroup parent) {
            LinearLayout row;
            if(reuse instanceof LinearLayout)row=(LinearLayout)reuse;
            else {row=column();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(a.dp(82));row.setPadding(a.dp(10),a.dp(8),a.dp(8),a.dp(8));
                android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(0xff203142);background.setCornerRadius(a.dp(12));
                row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x446ddcc5),background,null));
                ImageView icon=new ImageView(a);row.addView(icon,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
                LinearLayout copy=column();copy.addView(text("",16));copy.addView(text("",13));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));}
            Object item=getItem(p);ImageView icon=(ImageView)row.getChildAt(0);boolean visible=GameIcon.supports(item);icon.setVisibility(visible?View.VISIBLE:View.GONE);
            icon.setImageDrawable(visible?GameIcon.drawable(a,w,item):null);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout copy=(LinearLayout)row.getChildAt(1);((TextView)copy.getChildAt(0)).setText(title.apply(getItem(p)));((TextView)copy.getChildAt(0)).setTextColor(a.gold);
            ((TextView)copy.getChildAt(1)).setText(detail.apply(getItem(p)));return row;
        }
    }
    private <T> Rows<T> list(LinearLayout host,List<T> rows,ToLongFunction<T> key,Function<T,String> title,Function<T,String> detail,Consumer<T> select,String empty) {
        FrameLayout content=new FrameLayout(a);host.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        ListView list=new ListView(a);list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));list.setDividerHeight(a.dp(6));list.setCacheColorHint(Color.TRANSPARENT);list.setContentDescription("概览列表");
        TextView blank=text(empty,16);blank.setGravity(Gravity.CENTER);content.addView(blank,new FrameLayout.LayoutParams(-1,-1));content.addView(list,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(blank);
        boolean cityPage=state.page.equals("cities");int position=state.cityPosition,top=state.cityTop;
        boolean[] restoring={cityPage};
        Rows<T> adapter=new Rows<>(rows,key,title,detail);adapter.emptyView=blank;adapter.nativeList=list;list.setAdapter(adapter);
        LinearLayout pager=new LinearLayout(a);pager.setGravity(Gravity.CENTER_VERTICAL);host.addView(pager,new LinearLayout.LayoutParams(-1,a.dp(52)));
        Button previous=a.button("‹",v->adapter.changePage(adapter.page-1)),next=a.button("›",v->adapter.changePage(adapter.page+1));previous.setContentDescription("上一页");next.setContentDescription("下一页");
        Button number=a.button("",v->{EditText input=new EditText(a);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setHint("1–"+adapter.pages());input.setContentDescription("跳转页码");new AlertDialog.Builder(a).setTitle("跳转到页").setView(input).setPositiveButton("跳转",(d,n)->{try{adapter.changePage(Integer.parseInt(input.getText().toString())-1);}catch(NumberFormatException ignored){input.setError("请输入页码");}}).setNegativeButton("取消",null).show();});
        Button size=a.button(adapter.size()+"条",v->new AlertDialog.Builder(a).setTitle("每页条数").setItems(new String[]{"20条","50条","100条"},(d,n)->{state.listPageSize=new int[]{20,50,100}[n];adapter.changePage(0);}).setNegativeButton("取消",null).show());
        pager.addView(previous,new LinearLayout.LayoutParams(a.dp(40),-1));pager.addView(number,new LinearLayout.LayoutParams(0,-1,1));pager.addView(next,new LinearLayout.LayoutParams(a.dp(40),-1));pager.addView(size,new LinearLayout.LayoutParams(a.dp(64),-1));
        adapter.updatePager=()->{previous.setEnabled(adapter.page>0);next.setEnabled(adapter.page+1<adapter.pages());number.setText((adapter.page+1)+"/"+adapter.pages()+" · "+adapter.rows.size()+"项");number.setContentDescription("第"+(adapter.page+1)+"页，共"+adapter.pages()+"页，"+adapter.rows.size()+"项，点击跳转");size.setText(adapter.size()+"条");};adapter.updatePager.run();
        if(cityPage){
            list.setSelectionFromTop(position,top);
            list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView view,int scrollState){}public void onScroll(AbsListView view,int first,int visible,int total){
                if(!restoring[0]&&state.page.equals("cities")&&view.isAttachedToWindow()&&view.getChildCount()>0){state.cityPosition=first;state.cityTop=view.getChildAt(0).getTop();}
            }});
            list.post(()->{list.setSelectionFromTop(position,top);restoring[0]=false;});
        }
        list.setOnItemClickListener((p,v,index,id)->select.accept(adapter.getItem(index)));
        if(state.page.equals("cities"))list.setOnItemLongClickListener((p,v,index,id)->{Object row=adapter.getItem(index);if(row instanceof World.City){World.City city=(World.City)row;new AlertDialog.Builder(a).setTitle(city.name+" · 物流预测").setMessage(new DistrictManagement(w).forecast(city)).setPositiveButton("查看相关任务",(d,n)->{state.taskQuery=city.name;state.taskType=0;state.page="tasks";a.refresh();}).setNegativeButton("返回",null).show();return true;}return false;});return adapter;
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
        LinearLayout host=column();CityOverview overview=new CityOverview(w);
        final Rows<World.City>[] holder=new Rows[1];
        LinearLayout searchBar=new LinearLayout(a);host.addView(searchBar);
        EditText query=search(searchBar,"搜索城市或势力",state.cityQuery,q->{state.cityQuery=q;holder[0].rows=overview.cities(state.cityFilter,state.cityDistrict,state.citySort,q,state.cityOwner);holder[0].notifyDataSetChanged();});
        query.setHint("搜索城市 / 势力");query.setLayoutParams(new LinearLayout.LayoutParams(0,a.dp(48),1));
        String[] sorts=CityOverview.SORTS;
        searchBar.addView(a.button("排序 · "+sorts[state.citySort],v->new AlertDialog.Builder(a).setTitle("城市排序").setItems(sorts,(d,i)->{state.citySort=i;state.cityPosition=0;state.cityTop=0;a.refresh();}).setNegativeButton("取消",null).show()),new LinearLayout.LayoutParams(0,a.dp(48),1));
        String[] owners=new String[w.factions.length+2];owners[0]="全部城池";owners[1]="中立城池";System.arraycopy(w.factions,0,owners,2,w.factions.length);
        LinearLayout controls=new LinearLayout(a);host.addView(controls);
        controls.addView(a.button(state.cityOwner==-1?"全部城池":state.cityOwner==-2?"中立城池":w.faction(state.cityOwner),v->new AlertDialog.Builder(a).setTitle("所属势力").setItems(owners,(dialog,i)->{state.cityOwner=i==0?-1:i==1?-2:i-2;state.cityPosition=0;state.cityTop=0;a.refresh();}).setNegativeButton("取消",null).show()),new LinearLayout.LayoutParams(0,a.dp(48),1));
        controls.addView(a.button("筛选 · "+CityOverview.FILTERS[state.cityFilter],v->new AlertDialog.Builder(a).setTitle("全国管理筛选").setItems(new String[]{"城池状态","所属军团","批量划入军团","军团经营总览"},(dialog,n)->{
            if(n==0)new AlertDialog.Builder(a).setTitle("城池状态").setItems(CityOverview.FILTERS,(d,i)->{state.cityFilter=i;state.cityPosition=0;state.cityTop=0;a.refresh();}).setNegativeButton("取消",null).show();
            else if(n==1){List<Districts.District> groups=w.districts.all();String[] labels=new String[groups.size()+2];labels[0]="全部军团";labels[1]="第一军团（直属）";for(int i=0;i<groups.size();i++)labels[i+2]=groups.get(i).name();new AlertDialog.Builder(a).setTitle("所属军团").setItems(labels,(d,i)->{state.cityDistrict=i==0?-1:i==1?0:groups.get(i-2).id;state.cityPosition=0;state.cityTop=0;a.refresh();}).setNegativeButton("取消",null).show();}
            else if(n==2)new WorldUi(a,w,a::applyResult).batch(holder[0].rows);
            else new WorldUi(a,w,a::applyResult).districts();
        }).setNegativeButton("返回",null).show()),new LinearLayout.LayoutParams(0,a.dp(48),1));
        holder[0]=list(host,overview.cities(state.cityFilter,state.cityDistrict,state.citySort,state.cityQuery,state.cityOwner),c->c.id,c->c.name+" · "+w.faction(c.owner),
            overview::detail,c->a.selectAndFocus(c.hex),"没有符合条件的城池 · 清空检索或选择全部城池");return host;
    }
    View officers() {
        LinearLayout host=column();
        LinearLayout searchBar=new LinearLayout(a);host.addView(searchBar);
        EditText search=new EditText(a);search.setSingleLine();search.setTextColor(a.paper);search.setHintTextColor(a.muted);search.setHint("搜索武将姓名");search.setContentDescription("搜索武将姓名");search.setText(state.query);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setOnEditorActionListener((v,action,event)->{if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)return false;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();return true;});searchBar.addView(search,new LinearLayout.LayoutParams(0,a.dp(48),1));
        LinearLayout filters=new LinearLayout(a);host.addView(filters);
        Button faction=a.button(state.owner==-2?"在野武将":state.owner<0?"全部势力":w.faction(state.owner),v->{String[] labels=new String[w.factions.length+2];labels[0]="全部势力";System.arraycopy(w.factions,0,labels,1,w.factions.length);labels[labels.length-1]="在野武将";new AlertDialog.Builder(a).setTitle("按势力筛选").setItems(labels,(d,i)->{state.owner=i==w.factions.length+1?-2:i-1;a.refresh();}).show();});
        Button city=a.button(state.city<0?"全部城市":w.city(state.city).name,v->{String[] labels=new String[w.cities.size()+1];labels[0]="全部城市（含在途）";for(int i=0;i<w.cities.size();i++)labels[i+1]=w.cities.get(i).name;new AlertDialog.Builder(a).setTitle("按城市筛选").setItems(labels,(d,i)->{state.city=i==0?-1:w.cities.get(i-1).id;a.refresh();}).show();});
        filters.addView(faction,new LinearLayout.LayoutParams(0,a.dp(48),1));filters.addView(city,new LinearLayout.LayoutParams(0,a.dp(48),1));
        String[] sorts={"姓名","统率","武力","智力","政治","魅力"};searchBar.addView(a.button("排序 · "+sorts[state.officerSort],v->new AlertDialog.Builder(a).setTitle("武将排序").setItems(sorts,(d,i)->{state.officerSort=i;a.refresh();}).setNegativeButton("取消",null).show()),new LinearLayout.LayoutParams(a.dp(132),a.dp(48)));
        Rows<World.Officer> adapter=list(host,UiModels.officers(w,state.query,state.owner,state.city,state.officerSort),o->o.id,o->o.name+" · "+UiModels.faction(w,o),
            o->"统 "+o.leadership+"  武 "+o.war+"  智 "+o.intelligence+"  政 "+o.politics+"  魅 "+o.charm+"\n"+o.role.label+" · 忠诚 "+o.loyalty+"\n"+UiModels.location(w,o)+" · "+UiModels.status(w,o),a::officerDetail,"没有符合筛选条件的武将\n试试清空姓名或选择全部城市");
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){state.query=s.toString();adapter.rows=UiModels.officers(w,state.query,state.owner,state.city,state.officerSort);adapter.notifyDataSetChanged();}public void afterTextChanged(Editable e){}});return host;
    }
    private String taskEmpty(){return !state.taskQuery.isEmpty()?"没有符合检索条件的任务":state.taskType==1?"当前没有建设中的设施":"当前没有在途任务\n可从城池下达命令";}
    View tasks() {
        LinearLayout host=column();heading(host,"任务 / 在途");
        final Rows<UiModels.Task>[] holder=new Rows[1];
        search(host,"搜索任务、武将或城市",state.taskQuery,q->{state.taskQuery=q;holder[0].rows=UiModels.tasks(w,state.taskType,q);holder[0].emptyView.setText(taskEmpty());holder[0].notifyDataSetChanged();});String[] types={"全部任务","建设","调动","运输","研究 / 培养","军备制造","行军","援军"};
        filter(host,"筛选 · "+types[state.taskType],types,i->{state.taskType=i;a.refresh();});
        holder[0]=list(host,UiModels.tasks(w,state.taskType,state.taskQuery),t->t.id,t->t.title,t->t.detail,t->{
            AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(t.title).setMessage(t.detail).setNegativeButton("返回",null);
            if(t.aid!=null){dialog.setPositiveButton("定位援军",(d,n)->a.selectAndFocus(t.location));dialog.setNeutralButton("援军详情",(d,n)->new DiplomacyUi(a,w,a::applyResult).missions(t.aid.ally));}
            else if(t.marching!=null){dialog.setPositiveButton("定位部队",(d,n)->a.selectAndFocus(t.location));dialog.setNeutralButton("停止行军",(d,n)->a.applyResult(w.marches.stop(t.marching.id)));}
            else if(t.facility!=null) {dialog.setPositiveButton("定位城池",(d,n)->a.selectAndFocus(w.city(t.facility.cityId).hex));dialog.setNeutralButton("管理设施",(d,n)->a.domesticUi().facility(t.facility));}
            else if(t.production!=null){dialog.setPositiveButton("制造详情",(d,n)->new ArmyUi(a,w,a::applyResult,a::selectAndFocus).production(t.production));}
            else if(t.project!=null){dialog.setPositiveButton("定位研究城市",(d,n)->a.selectAndFocus(w.city(t.project.cityId).hex));}
            else if(t.abilityResearch!=null||t.abilityTraining!=null){dialog.setPositiveButton("定位城市",(d,n)->a.selectAndFocus(t.location));dialog.setNeutralButton("PK进度",(d,n)->new AbilityUi(a,w,a::applyResult).progress(w.city(t.abilityResearch!=null?t.abilityResearch.cityId:t.abilityTraining.cityId)));}
            else {dialog.setPositiveButton("定位目的地",(d,n)->a.selectAndFocus(w.city(t.mission.targetCity).hex));dialog.setNeutralButton("起点 / 改道",(d,n)->new AlertDialog.Builder(a).setTitle("任务操作").setItems(new String[]{"定位起点","定位当前位置","改道 / 返回"},(x,i)->{if(i==0)a.selectAndFocus(w.city(t.mission.sourceCity).hex);else if(i==1)a.selectAndFocus(t.location);else a.domesticUi().mission(t.mission);}).show());}
            dialog.show();
        },taskEmpty());return host;
    }
}
