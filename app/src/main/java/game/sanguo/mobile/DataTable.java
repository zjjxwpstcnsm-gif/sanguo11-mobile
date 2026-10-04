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
    Runnable onClear;
    private void clearQuery(){if(onClear!=null)onClear.run();else search.setText("");}
    private final Runnable filterTask=this::refresh;
    private int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    DataTable(Activity a,List<T> options,List<Column<T>> columns,int[] visible,Function<T,String> summary,ToLongFunction<T> key,Consumer<T> select,Consumer<T> detail){
        super(a);this.a=a;compact=a.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE||a.getResources().getConfiguration().screenHeightDp<450;this.columns=columns;this.visible=visible.clone();this.summary=summary;this.key=key;source.addAll(options);setOrientation(VERTICAL);indexSearch();
        searchBar=new LinearLayout(a);searchBar.setGravity(Gravity.CENTER_VERTICAL);searchBar.setPadding(dp(2),dp(compact?0:4),dp(2),dp(compact?0:4));
        search=new EditText(a);search.setSingleLine();search.setHint("搜索姓名、势力、所在地、特技");search.setContentDescription("表格搜索");UiTheme.search(search);
        searchBar.addView(search,new LayoutParams(0,dp(48),1));
        Button clear=CompactButtons.create(a);clear.setText("×");clear.setContentDescription("清空搜索");clear.setOnClickListener(v->clearQuery());searchBar.addView(clear,new LayoutParams(dp(48),dp(48)));
        groupPicker=CompactButtons.create(a);groupPicker.setVisibility(GONE);groupPicker.setContentDescription("切换表格指标");
        groupPicker.setOnClickListener(v->{PopupMenu menu=new PopupMenu(a,groupPicker);for(int i=0;i<groupLabels.length;i++)menu.getMenu().add(0,i,i,groupLabels[i]);menu.setOnMenuItemClickListener(item->{selectGroup(item.getItemId());return true;});menu.show();});
        searchBar.addView(groupPicker,new LayoutParams(dp(100),dp(48)));addView(searchBar);
        groups=new LinearLayout(a);groups.setVisibility(GONE);addView(groups,new LayoutParams(-1,dp(48)));
        count=new TextView(a);UiTheme.text(count);count.setTextColor(UiTheme.MUTED);count.setTextSize(11);count.setPadding(dp(6),dp(4),dp(6),dp(4));count.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);addView(count);if(compact)count.setVisibility(GONE);
        horizontal=new HorizontalScrollView(a);horizontal.setFillViewport(true);horizontal.setOverScrollMode(OVER_SCROLL_NEVER);horizontal.setHorizontalScrollBarEnabled(true);
        FrameLayout content=new FrameLayout(a);addView(content,new LayoutParams(-1,0,1));content.addView(horizontal,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout empty=new LinearLayout(a);empty.setOrientation(VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(12),dp(16),dp(12));empty.setBackgroundColor(UiTheme.INK);
        TextView emptyText=new TextView(a);UiTheme.text(emptyText);emptyText.setText("没有符合条件的对象\n请清空关键词，或调整上方筛选条件");emptyText.setTextSize(14);emptyText.setTextColor(UiTheme.MUTED);emptyText.setGravity(Gravity.CENTER);empty.addView(emptyText);
        Button reset=CompactButtons.create(a);reset.setText("清空搜索");reset.setOnClickListener(v->clearQuery());empty.addView(reset,new LayoutParams(-1,dp(48)));content.addView(empty,new FrameLayout.LayoutParams(-1,-1));
        content.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            boolean shortSpace=b-t<dp(112);reset.setVisibility(shortSpace?GONE:VISIBLE);
            empty.setPadding(dp(16),dp(shortSpace?8:12),dp(16),dp(shortSpace?8:12));
            emptyText.setTextSize(shortSpace?12:14);
            emptyText.setText(shortSpace?"没有符合条件的对象\n请清空关键词或调整筛选":"没有符合条件的对象\n请清空关键词，或调整上方筛选条件");
        });
        grid=new LinearLayout(a){
            @Override protected void onMeasure(int width,int height){
                // HorizontalScrollView supplies UNSPECIFIED width; enforce our computed grid width.
                int exact=getLayoutParams()==null?0:getLayoutParams().width;
                super.onMeasure(exact>0?MeasureSpec.makeMeasureSpec(exact,MeasureSpec.EXACTLY):width,height);
            }
        };grid.setOrientation(VERTICAL);horizontal.addView(grid,new HorizontalScrollView.LayoutParams(-1,-1));
        header=new LinearLayout(a);header.setBackgroundColor(0xff263b44);grid.addView(header,new LayoutParams(-1,dp(48)));
        list=new ListView(a);list.setPadding(0,0,0,0);list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);list.setDivider(new ColorDrawable(0xff2a3b43));list.setDividerHeight(dp(1));list.setFastScrollEnabled(true);list.setContentDescription("概览列表");list.setCacheColorHint(0);list.setScrollingCacheEnabled(false);grid.addView(list,new LayoutParams(-1,0,1));
        adapter=new BaseAdapter(){
            public int getCount(){return shown.size();}public T getItem(int i){return shown.get(i);}public long getItemId(int i){return key.applyAsLong(getItem(i));}public boolean hasStableIds(){return true;}
            public View getView(int i,View reuse,ViewGroup parent){
                LinearLayout row=reuse instanceof LinearLayout?(LinearLayout)reuse:new LinearLayout(a);
                if(row.getChildCount()!=DataTable.this.visible.length){row.removeAllViews();for(int j=0;j<DataTable.this.visible.length;j++)row.addView(cell(j));}
                T item=getItem(i);row.setMinimumHeight(dp(52));row.setActivated(selected.test(item));row.setBackgroundColor(row.isActivated()?0xff2b514a:i%2==0?0xff152630:0xff1b2e38);
                for(int j=0;j<DataTable.this.visible.length;j++){
                    int index=DataTable.this.visible[j];TextView text=(TextView)row.getChildAt(j);Column<T> col=columns.get(index);
                    text.setLayoutParams(new LayoutParams(widths[j],-1));text.setGravity(col.numeric?Gravity.CENTER:Gravity.CENTER_VERTICAL);
                    text.setText(col.text.apply(item));text.setTextColor(index==sort?UiTheme.JADE:UiTheme.TEXT);
                }
                row.setContentDescription((row.isActivated()?"已选 · ":"")+summary.apply(item));return row;
            }
        };
        list.setEmptyView(empty);list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->{if(i<shown.size())select.accept(shown.get(i));});
        list.setOnItemLongClickListener((p,v,i,id)->{if(i<shown.size()){v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);detail.accept(shown.get(i));}return true;});
        horizontal.addOnLayoutChangeListener((v,l,t,right,b,ol,ot,or,ob)->{
            int viewport=right-l-horizontal.getPaddingLeft()-horizontal.getPaddingRight();
            if(viewport>0&&contentWidth!=viewport){contentWidth=viewport;rebuildColumns(viewport);}
        });
        rebuildColumns(0);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int n){onQuery.accept(s.toString());removeCallbacks(filterTask);postDelayed(filterTask,100);}});
        search.setOnEditorActionListener((v,action,event)->{
            refresh();android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();return true;
        });refresh();
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
    @Override protected void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);adaptControls(getHeight());}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);adaptControls(h);}
    private void adaptControls(int height){
        if(searchBar==null||header==null||adapter==null)return;
        android.content.res.Configuration config=getResources().getConfiguration();
        boolean tight=config.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE||config.screenHeightDp<450||(height>0&&height<dp(260));
        if(compact!=tight){compact=tight;searchBar.setPadding(dp(2),dp(compact?0:4),dp(2),dp(compact?0:4));count.setVisibility(compact?GONE:VISIBLE);adapter.notifyDataSetChanged();}
        if(groupLabels!=null){
            boolean dropdown=compact&&searchBar.getVisibility()==VISIBLE;
            groups.setVisibility(dropdown?GONE:VISIBLE);groupPicker.setVisibility(dropdown?VISIBLE:GONE);groupPicker.setText(groupLabels[group]+" ▾");
            if(groupPicker.getLayoutParams().width!=dp(compact?84:100))groupPicker.setLayoutParams(new LayoutParams(dp(compact?84:100),dp(48)));
        }
    }
    @Override protected void onDetachedFromWindow(){removeCallbacks(filterTask);super.onDetachedFromWindow();}
    private void rebuildColumns(int available){
        int natural=0;for(int index:visible)natural+=dp(columns.get(index).width);
        float ratio=available>0?available/(float)Math.max(1,natural):1f;
        widths=new int[visible.length];int total=0;
        for(int j=0;j<visible.length;j++){Column<T> c=columns.get(visible[j]);widths[j]=Math.max(dp(c.numeric?(c.width>=75?70:48):(c.width>=100?72:56)),Math.round(dp(c.width)*ratio));total+=widths[j];}
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
        list.setContentDescription("概览列表 · 共 "+shown.size()+" 项");updateHeaders();count.setText(shown.isEmpty()?"没有符合条件的对象 · 可清空搜索重试":"共 "+shown.size()+" 项 · 列名排序 · 左右滑动查看更多 · 长按详情");adapter.notifyDataSetChanged();
    }
    static List<Column<World.Officer>> officerColumns(World w){
        List<Column<World.Officer>> out=new ArrayList<>();out.add(new Column<>("姓名",78,o->o.name,Comparator.comparing(o->o.name),false));
        String[] names={"统","武","智","政","魅"};for(int i=1;i<=5;i++){final int n=i;out.add(new Column<>(names[i-1],44,o->""+UiModels.ability(o,n),Comparator.comparingInt(o->UiModels.ability(o,n)),true));}
        out.add(new Column<>("忠诚",50,o->""+o.loyalty,Comparator.comparingInt(o->o.loyalty),true));
        out.add(new Column<>("特技",64,o->Skill.label(o.skillId),Comparator.comparing(o->Skill.label(o.skillId)),false));
        out.add(new Column<>("势力",86,o->UiModels.faction(w,o),Comparator.comparing(o->UiModels.faction(w,o)),false));
        out.add(new Column<>("所在地",120,o->UiModels.location(w,o),Comparator.comparing(o->UiModels.location(w,o)),false));
        out.add(new Column<>("状态",150,o->UiModels.status(w,o),Comparator.comparing(o->UiModels.status(w,o)),false));
        out.add(new Column<>("功绩",82,o->""+w.government.merit(o.id),Comparator.comparingInt(o->w.government.merit(o.id)),true));
        out.add(new Column<>("指挥兵数",86,o->""+w.government.commandLimit(o.id),Comparator.comparingInt(o->w.government.commandLimit(o.id)),true));
        out.add(new Column<>("官职",104,o->w.governance.office(o),Comparator.comparing(o->w.governance.office(o)),false));
        out.add(new Column<>("身份",66,o->o.role.label,Comparator.comparing(o->o.role.label),false));
        out.add(new Column<>("宝物数",66,o->""+w.treasures.held(o.id).size(),Comparator.comparingInt(o->w.treasures.held(o.id).size()),true));return out;
    }
    static DataTable<World.Officer> officers(Activity a,World w,List<World.Officer> officers,Function<World.Officer,String> extra,Consumer<World.Officer> select){
        DataTable<World.Officer> table=new DataTable<>(a,officers,officerColumns(w),new int[]{0,14,13,11,12,15,6},o->o.name+" "+o.role.label+" "+w.governance.office(o)+" 功绩"+w.government.merit(o.id)+" 指挥"+w.government.commandLimit(o.id)+" 宝物"+w.treasures.held(o.id).size()+" "+Skill.label(o.skillId)+" "+UiModels.faction(w,o)+" "+UiModels.location(w,o)+" "+UiModels.status(w,o)+" "+extra.apply(o),o->o.id,select,o->{if(a instanceof MainActivity)((MainActivity)a).officerDetail(o);});
        table.columnGroups(new String[]{"履历 / 指挥","能力","归属 / 特技","任务状态"},new int[][]{{0,14,13,11,12,15,6},{0,1,2,3,4,5,6},{0,7,8,9},{0,10,6}});return table;
    }
    /** Text, search and sorting use the same session DTO as officer details.
     * World.Officer remains only the existing media/selection identity adapter. */
    static DataTable<World.Officer> officers(Activity a,List<World.Officer> rows,game.sanguo.api.OfficerSnapshot snapshot,Consumer<World.Officer> select){
        Function<World.Officer,game.sanguo.api.OfficerSnapshot.Officer> fact=o->Objects.requireNonNull(snapshot.officer(o.id));
        List<Column<World.Officer>> columns=new ArrayList<>();
        columns.add(new Column<>("姓名",78,o->fact.apply(o).name,Comparator.comparing(o->fact.apply(o).name),false));
        String[] names={"统","武","智","政","魅"};for(int i=0;i<5;i++){final int stat=i;columns.add(new Column<>(names[i],44,o->""+fact.apply(o).current.get(stat),Comparator.comparingInt(o->fact.apply(o).current.get(stat)),true));}
        columns.add(new Column<>("忠诚",50,o->""+fact.apply(o).loyalty,Comparator.comparingInt(o->fact.apply(o).loyalty),true));
        columns.add(new Column<>("特技",64,o->fact.apply(o).skillName,Comparator.comparing(o->fact.apply(o).skillName),false));
        columns.add(new Column<>("势力",86,o->fact.apply(o).faction,Comparator.comparing(o->fact.apply(o).faction),false));
        columns.add(new Column<>("所在地",120,o->fact.apply(o).location,Comparator.comparing(o->fact.apply(o).location),false));
        columns.add(new Column<>("状态",150,o->fact.apply(o).status,Comparator.comparing(o->fact.apply(o).status),false));
        columns.add(new Column<>("功绩",82,o->""+fact.apply(o).merit,Comparator.comparingInt(o->fact.apply(o).merit),true));
        columns.add(new Column<>("指挥兵数",86,o->""+fact.apply(o).commandLimit,Comparator.comparingInt(o->fact.apply(o).commandLimit),true));
        columns.add(new Column<>("官职",104,o->fact.apply(o).office,Comparator.comparing(o->fact.apply(o).office),false));
        columns.add(new Column<>("身份",66,o->fact.apply(o).role,Comparator.comparing(o->fact.apply(o).role),false));
        columns.add(new Column<>("宝物数",66,o->""+fact.apply(o).treasureCount,Comparator.comparingInt(o->fact.apply(o).treasureCount),true));
        DataTable<World.Officer> table=new DataTable<>(a,rows,columns,new int[]{0,14,13,11,12,15,6},o->fact.apply(o).searchText(),o->o.id,select,select);
        table.columnGroups(new String[]{"履历 / 指挥","能力","归属 / 特技","任务状态"},new int[][]{{0,14,13,11,12,15,6},{0,1,2,3,4,5,6},{0,7,8,9},{0,10,6}});return table;
    }
    static void choose(Activity a,World w,String title,List<World.Officer> options,Function<World.Officer,String> extra,Consumer<World.Officer> next,Runnable back){
        choose(a,w,title,options,extra,next,back,null);
    }
    static void choose(Activity a,World w,String title,List<World.Officer> options,Function<World.Officer,String> extra,Consumer<World.Officer> next,Runnable back,Runnable closed){
        chooseDialog(a,w,title,options,extra,next,back,closed,false);
    }
    /** Keep the exact query, sort, scroll and selected row underneath a review dialog. */
    static AlertDialog chooseRetained(Activity a,World w,String title,List<World.Officer> options,Consumer<World.Officer> next){
        return chooseRetained(a,w,title,options,next,null);
    }
    static AlertDialog chooseRetained(Activity a,World w,String title,List<World.Officer> options,Consumer<World.Officer> next,Runnable closed){
        return chooseDialog(a,w,title,options,o->"",next,null,closed,true);
    }
    private static AlertDialog chooseDialog(Activity a,World w,String title,List<World.Officer> options,Function<World.Officer,String> extra,Consumer<World.Officer> next,Runnable back,Runnable closed,boolean retained){
        final AlertDialog[] dialog={null};final int[] selected={-1};final Runnable[] highlight={()->{}};DataTable<World.Officer> table=officers(a,w,options,extra,o->{if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;selected[0]=o.id;highlight[0].run();
            if(retained){((android.view.inputmethod.InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(dialog[0].getWindow().getDecorView().getWindowToken(),0);}
            else dialog[0].dismiss();next.accept(o);});
        highlight[0]=()->table.selection(o->o.id==selected[0]);
        // Command selection begins with abilities so the acting officer is easy to compare.
        table.selectGroup(1);
        LinearLayout host=new LinearLayout(a);host.setOrientation(VERTICAL);host.addView(table,new LayoutParams(-1,Math.round(Math.max(160,Math.min(390,a.getResources().getConfiguration().screenHeightDp-145))*a.getResources().getDisplayMetrics().density)));
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle(title).setView(host).setNegativeButton("取消",null);if(back!=null)b.setNeutralButton("上一步",(d,i)->back.run());dialog[0]=b.create();if(back!=null)dialog[0].setOnCancelListener(d->back.run());
        ViewTreeObserver.OnGlobalLayoutListener fit=()->{android.graphics.Rect frame=new android.graphics.Rect();host.getWindowVisibleDisplayFrame(frame);int height=Math.max(UiTheme.dp(a,160),Math.min(UiTheme.dp(a,390),frame.height()-UiTheme.dp(a,145)));if(table.getLayoutParams().height!=height){table.getLayoutParams().height=height;table.requestLayout();}};host.getViewTreeObserver().addOnGlobalLayoutListener(fit);dialog[0].setOnDismissListener(d->{host.getViewTreeObserver().removeOnGlobalLayoutListener(fit);if(closed!=null)closed.run();});dialog[0].show();if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog[0]);
        dialog[0].getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog[0];
    }
}
