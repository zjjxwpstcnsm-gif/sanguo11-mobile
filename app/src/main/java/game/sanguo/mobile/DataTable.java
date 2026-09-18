package game.sanguo.mobile;

import android.app.*;
import android.graphics.drawable.ColorDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Recycled, sortable columns. Selection always resolves the displayed row's stable ID. */
final class DataTable<T> extends LinearLayout {
    static final class Column<T> {
        final String name;final int width;final Function<T,String> text;final Comparator<T> order;final boolean numeric;
        Column(String name,int width,Function<T,String> text,Comparator<T> order,boolean numeric){this.name=name;this.width=width;this.text=text;this.order=order;this.numeric=numeric;}
    }
    final EditText search;final LinearLayout searchBar;final ListView list;
    int sort=-1;boolean descending;
    BiConsumer<Integer,Boolean> onSort=(i,d)->{};Consumer<String> onQuery=q->{};
    private final Activity a;private final List<T> source=new ArrayList<>(),shown=new ArrayList<>();
    private final List<Column<T>> columns;private final int[] visible;private final Function<T,String> summary;
    private final List<TextView> headers=new ArrayList<>();private final TextView count;private final BaseAdapter adapter;
    private Predicate<T> selected=t->false;
    private int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    DataTable(Activity a,List<T> options,List<Column<T>> columns,int[] visible,Function<T,String> summary,ToLongFunction<T> key,Consumer<T> select,Consumer<T> detail){
        super(a);this.a=a;this.columns=columns;this.visible=visible;this.summary=summary;source.addAll(options);setOrientation(VERTICAL);
        searchBar=new LinearLayout(a);search=new EditText(a);search.setSingleLine();search.setTextSize(14);search.setHint("搜索姓名、势力、所在地、特技");search.setContentDescription("表格搜索");search.setTextColor(0xffe6eef4);search.setHintTextColor(0xff9bb0c3);
        searchBar.addView(search,new LayoutParams(0,dp(42),1));addView(searchBar);
        count=new TextView(a);count.setTextColor(0xff9bb0c3);count.setTextSize(12);count.setPadding(dp(6),dp(3),dp(6),dp(3));addView(count);
        HorizontalScrollView horizontal=new HorizontalScrollView(a);horizontal.setFillViewport(true);addView(horizontal,new LayoutParams(-1,0,1));
        LinearLayout grid=new LinearLayout(a);grid.setOrientation(VERTICAL);int width=0;for(int i:visible)width+=columns.get(i).width;
        horizontal.addView(grid,new HorizontalScrollView.LayoutParams(dp(width),-1));
        LinearLayout header=new LinearLayout(a);header.setBackgroundColor(0xff294352);grid.addView(header,new LayoutParams(-1,dp(36)));
        for(int index:visible){TextView cell=cell(columns.get(index));cell.setTypeface(null,android.graphics.Typeface.BOLD);cell.setTextColor(0xff6ddcc5);cell.setOnClickListener(v->sortBy(index));cell.setContentDescription("按"+columns.get(index).name+"排序");header.addView(cell);headers.add(cell);}
        list=new ListView(a);list.setDivider(new ColorDrawable(0xff304353));list.setDividerHeight(dp(1));list.setFastScrollEnabled(true);list.setContentDescription("概览列表");grid.addView(list,new LayoutParams(-1,0,1));
        adapter=new BaseAdapter(){
            public int getCount(){return shown.size();}public T getItem(int i){return shown.get(i);}public long getItemId(int i){return key.applyAsLong(getItem(i));}public boolean hasStableIds(){return true;}
            public View getView(int i,View reuse,ViewGroup parent){
                LinearLayout row=reuse instanceof LinearLayout?(LinearLayout)reuse:new LinearLayout(a);
                if(row.getChildCount()==0)for(int index:visible)row.addView(cell(columns.get(index)));
                T item=getItem(i);row.setMinimumHeight(dp(40));row.setBackgroundColor(selected.test(item)?0xff345b55:i%2==0?0xff182a38:0xff203142);
                for(int j=0;j<visible.length;j++)((TextView)row.getChildAt(j)).setText(columns.get(visible[j]).text.apply(item));
                row.setContentDescription(summary.apply(item));return row;
            }
        };list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->select.accept(shown.get(i)));
        list.setOnItemLongClickListener((p,v,i,id)->{detail.accept(shown.get(i));return true;});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int n){onQuery.accept(s.toString());refresh();}});
        refresh();
    }
    private TextView cell(Column<T> c){TextView v=new TextView(a);v.setTextColor(0xffebf1f7);v.setTextSize(13);v.setGravity(c.numeric?Gravity.CENTER:Gravity.CENTER_VERTICAL);v.setSingleLine();v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(dp(6),0,dp(6),0);v.setLayoutParams(new LayoutParams(dp(c.width),-1));return v;}
    private void sortBy(int i){descending=sort==i?!descending:columns.get(i).numeric;sort=i;onSort.accept(sort,descending);refresh();}
    void rows(List<T> items){source.clear();source.addAll(items);refresh();}
    void selection(Predicate<T> predicate){selected=predicate;adapter.notifyDataSetChanged();}
    void order(int index,boolean reverse){sort=index;descending=reverse;refresh();}
    void refresh(){shown.clear();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(T t:source)if(summary.apply(t).toLowerCase(Locale.ROOT).contains(q))shown.add(t);
        if(sort>=0&&sort<columns.size()){Comparator<T> c=columns.get(sort).order;shown.sort(descending?c.reversed():c);}
        for(int j=0;j<visible.length;j++){int i=visible[j];headers.get(j).setText(columns.get(i).name+(sort==i?(descending?" ↓":" ↑"):""));}
        count.setText(shown.isEmpty()?"没有符合条件的对象":"共 "+shown.size()+" 项 · 点击列名排序 · 长按详情 · 左右滑动查看各列");adapter.notifyDataSetChanged();
    }
    static List<Column<World.Officer>> officerColumns(World w){
        List<Column<World.Officer>> out=new ArrayList<>();out.add(new Column<>("姓名",78,o->o.name,Comparator.comparing(o->o.name),false));
        String[] names={"统","武","智","政","魅"};for(int i=1;i<=5;i++){final int n=i;out.add(new Column<>(names[i-1],44,o->""+UiModels.ability(o,n),Comparator.comparingInt(o->UiModels.ability(o,n)),true));}
        out.add(new Column<>("忠诚",50,o->""+o.loyalty,Comparator.comparingInt(o->o.loyalty),true));
        out.add(new Column<>("特技",64,o->Skill.label(o.skillId),Comparator.comparing(o->Skill.label(o.skillId)),false));
        out.add(new Column<>("势力",86,o->UiModels.faction(w,o),Comparator.comparing(o->UiModels.faction(w,o)),false));
        out.add(new Column<>("所在地",120,o->UiModels.location(w,o),Comparator.comparing(o->UiModels.location(w,o)),false));
        out.add(new Column<>("状态",150,o->UiModels.status(w,o),Comparator.comparing(o->UiModels.status(w,o)),false));return out;
    }
    static DataTable<World.Officer> officers(Activity a,World w,List<World.Officer> officers,Function<World.Officer,String> extra,Consumer<World.Officer> select){
        return new DataTable<>(a,officers,officerColumns(w),new int[]{0,1,2,3,4,5,6,7,8,9,10},o->o.name+" "+Skill.label(o.skillId)+" "+UiModels.faction(w,o)+" "+UiModels.location(w,o)+" "+UiModels.status(w,o)+" "+extra.apply(o),o->o.id,select,o->{if(a instanceof MainActivity)((MainActivity)a).officerDetail(o);});
    }
    static void choose(Activity a,World w,String title,List<World.Officer> options,Function<World.Officer,String> extra,Consumer<World.Officer> next,Runnable back){
        final AlertDialog[] dialog={null};DataTable<World.Officer> table=officers(a,w,options,extra,o->{if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;dialog[0].dismiss();next.accept(o);});
        LinearLayout host=new LinearLayout(a);host.setOrientation(VERTICAL);host.addView(table,new LayoutParams(-1,Math.round(Math.max(160,Math.min(390,a.getResources().getConfiguration().screenHeightDp-145))*a.getResources().getDisplayMetrics().density)));
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle(title).setView(host).setNegativeButton("取消",null);if(back!=null)b.setNeutralButton("返回",(d,i)->back.run());dialog[0]=b.create();dialog[0].show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog[0]);
        dialog[0].getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
