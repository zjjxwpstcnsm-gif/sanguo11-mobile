package game.sanguo.mobile;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.*;

/** Stable-ID recycled table with responsive column groups and a cached search index. */
final class DataTable<T> extends LinearLayout {
    static final class Column<T> {
        final String name;final int width;final Function<T,String> text;final Comparator<T> order;final boolean numeric;
        Column(String name,int width,Function<T,String> text,Comparator<T> order,boolean numeric){this.name=name;this.width=width;this.text=text;this.order=order;this.numeric=numeric;}
    }
    final EditText search;final LinearLayout searchBar;final ListView list;
    int sort=-1;boolean descending;
    BiConsumer<Integer,Boolean> onSort=(i,d)->{};Consumer<String> onQuery=q->{};
    private final Activity a;private final List<T> source=new ArrayList<>(),shown=new ArrayList<>();
    private final List<Column<T>> columns;private int[] visible,widths;private final Function<T,String> summary;private final ToLongFunction<T> key;
    private final Map<Long,String> searchIndex=new HashMap<>();
    private final List<TextView> headers=new ArrayList<>();private final TextView count;private final BaseAdapter adapter;
    private final HorizontalScrollView horizontal;private final LinearLayout grid,header,groups;
    private int[][] groupColumns;private String[] groupLabels;private int group;private int contentWidth;
    private boolean compact;private final Button groupPicker;
    private Predicate<T> selected=t->false;
    private final Runnable filterTask=this::refresh;
    private int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    DataTable(Activity a,List<T> options,List<Column<T>> columns,int[] visible,Function<T,String> summary,ToLongFunction<T> key,Consumer<T> select,Consumer<T> detail){
        super(a);this.a=a;compact=a.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE||a.getResources().getConfiguration().screenHeightDp<450;this.columns=columns;this.visible=visible.clone();this.summary=summary;this.key=key;source.addAll(options);setOrientation(VERTICAL);indexSearch();
        searchBar=new LinearLayout(a);searchBar.setGravity(Gravity.CENTER_VERTICAL);searchBar.setPadding(dp(2),dp(compact?0:4),dp(2),dp(compact?0:4));
        search=new EditText(a);search.setSingleLine();search.setHint("搜索姓名、势力、所在地、特技");search.setContentDescription("表格搜索");UiTheme.search(search);
        searchBar.addView(search,new LayoutParams(0,dp(44),1));
        Button clear=CompactButtons.create(a);clear.setText("×");clear.setContentDescription("清空搜索");clear.setOnClickListener(v->search.setText(""));searchBar.addView(clear,new LayoutParams(dp(40),dp(compact?44:48)));
        groupPicker=CompactButtons.create(a);groupPicker.setVisibility(GONE);groupPicker.setContentDescription("切换表格指标");
        groupPicker.setOnClickListener(v->{PopupMenu menu=new PopupMenu(a,groupPicker);for(int i=0;i<groupLabels.length;i++)menu.getMenu().add(0,i,i,groupLabels[i]);menu.setOnMenuItemClickListener(item->{selectGroup(item.getItemId());return true;});menu.show();});
        searchBar.addView(groupPicker,new LayoutParams(dp(100),dp(44)));addView(searchBar);
        groups=new LinearLayout(a);groups.setVisibility(GONE);addView(groups,new LayoutParams(-1,dp(44)));
        count=new TextView(a);UiTheme.text(count);count.setTextColor(UiTheme.MUTED);count.setTextSize(11);count.setPadding(dp(6),dp(4),dp(6),dp(4));count.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);addView(count);if(compact)count.setVisibility(GONE);
        horizontal=new HorizontalScrollView(a);horizontal.setFillViewport(true);horizontal.setOverScrollMode(OVER_SCROLL_NEVER);horizontal.setHorizontalScrollBarEnabled(false);addView(horizontal,new LayoutParams(-1,0,1));
        grid=new LinearLayout(a){
            @Override protected void onMeasure(int width,int height){
                // HorizontalScrollView supplies UNSPECIFIED width; enforce our computed grid width.
                int exact=getLayoutParams()==null?0:getLayoutParams().width;
                super.onMeasure(exact>0?MeasureSpec.makeMeasureSpec(exact,MeasureSpec.EXACTLY):width,height);
            }
        };grid.setOrientation(VERTICAL);horizontal.addView(grid,new HorizontalScrollView.LayoutParams(-1,-1));
        header=new LinearLayout(a);header.setBackgroundColor(0xff263b44);grid.addView(header,new LayoutParams(-1,dp(compact?36:44)));
        list=new ListView(a);list.setPadding(0,0,0,0);list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);list.setDivider(new ColorDrawable(0xff2a3b43));list.setDividerHeight(dp(1));list.setFastScrollEnabled(true);list.setContentDescription("概览列表");list.setCacheColorHint(0);list.setScrollingCacheEnabled(false);grid.addView(list,new LayoutParams(-1,0,1));
        adapter=new BaseAdapter(){
            public int getCount(){return shown.size();}public T getItem(int i){return shown.get(i);}public long getItemId(int i){return key.applyAsLong(getItem(i));}public boolean hasStableIds(){return true;}
            public View getView(int i,View reuse,ViewGroup parent){
                LinearLayout row=reuse instanceof LinearLayout?(LinearLayout)reuse:new LinearLayout(a);
                if(row.getChildCount()!=DataTable.this.visible.length){row.removeAllViews();for(int j=0;j<DataTable.this.visible.length;j++)row.addView(cell(j));}
                T item=getItem(i);row.setMinimumHeight(dp(compact?44:46));row.setBackgroundColor(selected.test(item)?0xff2b514a:i%2==0?0xff152630:0xff1b2e38);
                for(int j=0;j<DataTable.this.visible.length;j++){
                    int index=DataTable.this.visible[j];TextView text=(TextView)row.getChildAt(j);Column<T> col=columns.get(index);
                    text.setLayoutParams(new LayoutParams(widths[j],-1));text.setGravity(col.numeric?Gravity.CENTER:Gravity.CENTER_VERTICAL);
                    text.setText(col.text.apply(item));text.setTextColor(index==sort?UiTheme.JADE:UiTheme.TEXT);
                }
                row.setContentDescription(summary.apply(item));return row;
            }
        };
        list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->{if(i<shown.size())select.accept(shown.get(i));});
        list.setOnItemLongClickListener((p,v,i,id)->{if(i<shown.size()){v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);detail.accept(shown.get(i));}return true;});
        horizontal.addOnLayoutChangeListener((v,l,t,right,b,ol,ot,or,ob)->{
            int viewport=right-l-horizontal.getPaddingLeft()-horizontal.getPaddingRight();
            if(viewport>0&&contentWidth!=viewport){contentWidth=viewport;rebuildColumns(viewport);}
        });
        rebuildColumns(0);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int n){onQuery.accept(s.toString());removeCallbacks(filterTask);postDelayed(filterTask,100);}});
        search.setOnEditorActionListener((v,action,event)->{refresh();return false;});refresh();
    }
    private void indexSearch(){searchIndex.clear();for(T row:source)searchIndex.put(key.applyAsLong(row),summary.apply(row).toLowerCase(Locale.ROOT));}
    void columnGroups(String[] labels,int[][] choices){
        if(labels.length!=choices.length||labels.length==0)throw new IllegalArgumentException("Column groups must match");
        groupLabels=labels;groupColumns=choices;group=0;visible=choices[0].clone();groups.setVisibility(VISIBLE);groups.removeAllViews();
        for(int i=0;i<labels.length;i++){final int index=i;Button b=CompactButtons.create(a);b.setText(labels[i]);b.setSelected(i==group);b.setContentDescription("表格列组 · "+labels[i]);b.setOnClickListener(v->selectGroup(index));groups.addView(b,new LayoutParams(0,-1,1));}
        if(compact){groups.setVisibility(GONE);groupPicker.setVisibility(VISIBLE);groupPicker.setText(labels[0]+" ▾");}
        rebuildColumns(contentWidth);refresh();
    }
    private void selectGroup(int index){
        group=index;visible=groupColumns[index].clone();for(int n=0;n<groups.getChildCount();n++)groups.getChildAt(n).setSelected(n==group);
        if(compact)groupPicker.setText(groupLabels[index]+" ▾");rebuildColumns(contentWidth);refresh();
    }
    @Override protected void onConfigurationChanged(android.content.res.Configuration config){
        super.onConfigurationChanged(config);compact=config.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE||config.screenHeightDp<450;
        searchBar.setPadding(dp(2),dp(compact?0:4),dp(2),dp(compact?0:4));
        searchBar.getChildAt(1).getLayoutParams().height=dp(compact?44:48);
        count.setVisibility(compact?GONE:VISIBLE);header.getLayoutParams().height=dp(compact?36:44);
        if(groupLabels!=null){groups.setVisibility(compact?GONE:VISIBLE);groupPicker.setVisibility(compact?VISIBLE:GONE);groupPicker.setText(groupLabels[group]+" ▾");}
        adapter.notifyDataSetChanged();requestLayout();
    }
    @Override protected void onDetachedFromWindow(){removeCallbacks(filterTask);super.onDetachedFromWindow();}
    private void rebuildColumns(int available){
        int natural=0;for(int index:visible)natural+=dp(columns.get(index).width);
        float ratio=available>0?available/(float)Math.max(1,natural):1f;
        widths=new int[visible.length];int total=0;
        for(int j=0;j<visible.length;j++){Column<T> c=columns.get(visible[j]);widths[j]=Math.max(dp(c.numeric?30:52),Math.round(dp(c.width)*ratio));total+=widths[j];}
        if(available>0&&total>available&&total-available<=visible.length){widths[0]-=total-available;total=available;}
        if(available>total){widths[widths.length-1]+=available-total;total=available;}
        grid.setLayoutParams(new HorizontalScrollView.LayoutParams(total,-1));header.removeAllViews();headers.clear();
        for(int j=0;j<visible.length;j++){final int index=visible[j];TextView text=cell(j);text.setTypeface(null,android.graphics.Typeface.BOLD);text.setTextColor(UiTheme.JADE);
            text.setBackground(new RippleDrawable(ColorStateList.valueOf(0x407ee2c3),null,null));text.setOnClickListener(v->sortBy(index));text.setContentDescription("按"+columns.get(index).name+"排序");header.addView(text);headers.add(text);}
        updateHeaders();if(adapter!=null)adapter.notifyDataSetChanged();horizontal.scrollTo(0,0);
    }
    private TextView cell(int position){Column<T> c=columns.get(visible[position]);TextView v=new TextView(a);UiTheme.text(v);v.setTextColor(UiTheme.TEXT);v.setTextSize(c.numeric?12:13);v.setGravity(c.numeric?Gravity.CENTER:Gravity.CENTER_VERTICAL);v.setSingleLine();v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(dp(c.numeric?2:6),0,dp(c.numeric?2:6),0);v.setLayoutParams(new LayoutParams(widths[position],-1));return v;}
    private void sortBy(int i){descending=sort==i?!descending:columns.get(i).numeric;sort=i;onSort.accept(sort,descending);refresh();list.setSelection(0);}
    void rows(List<T> items){source.clear();source.addAll(items);indexSearch();refresh();}
    void selection(Predicate<T> predicate){selected=predicate;adapter.notifyDataSetChanged();}
    void order(int index,boolean reverse){sort=index;descending=reverse;refresh();}
    private void updateHeaders(){for(int j=0;j<visible.length;j++){int i=visible[j];headers.get(j).setText(columns.get(i).name+(sort==i?(descending?"↓":"↑"):""));}}
    void refresh(){removeCallbacks(filterTask);shown.clear();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        for(T t:source)if(searchIndex.getOrDefault(key.applyAsLong(t),"").contains(q))shown.add(t);
        if(sort>=0&&sort<columns.size()){Comparator<T> c=columns.get(sort).order;shown.sort(descending?c.reversed():c);}
        list.setContentDescription("概览列表 · 共 "+shown.size()+" 项");updateHeaders();count.setText(shown.isEmpty()?"没有符合条件的对象 · 可清空搜索重试":"共 "+shown.size()+" 项 · 点击列名排序 · 长按详情");adapter.notifyDataSetChanged();
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
        DataTable<World.Officer> table=new DataTable<>(a,officers,officerColumns(w),new int[]{0,1,2,3,4,5,6},o->o.name+" "+Skill.label(o.skillId)+" "+UiModels.faction(w,o)+" "+UiModels.location(w,o)+" "+UiModels.status(w,o)+" "+extra.apply(o),o->o.id,select,o->{if(a instanceof MainActivity)((MainActivity)a).officerDetail(o);});
        table.columnGroups(new String[]{"能力","归属 / 特技","任务状态"},new int[][]{{0,1,2,3,4,5,6},{0,7,8,9},{0,10,6}});return table;
    }
    static void choose(Activity a,World w,String title,List<World.Officer> options,Function<World.Officer,String> extra,Consumer<World.Officer> next,Runnable back){
        final AlertDialog[] dialog={null};DataTable<World.Officer> table=officers(a,w,options,extra,o->{if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;dialog[0].dismiss();next.accept(o);});
        LinearLayout host=new LinearLayout(a);host.setOrientation(VERTICAL);host.addView(table,new LayoutParams(-1,Math.round(Math.max(160,Math.min(390,a.getResources().getConfiguration().screenHeightDp-145))*a.getResources().getDisplayMetrics().density)));
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle(title).setView(host).setNegativeButton("取消",null);if(back!=null)b.setNeutralButton("返回",(d,i)->back.run());dialog[0]=b.create();dialog[0].show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog[0]);
        dialog[0].getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
