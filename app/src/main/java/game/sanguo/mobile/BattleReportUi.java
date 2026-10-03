package game.sanguo.mobile;

import android.app.AlertDialog;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;

/** Virtualized history browser. Opens from the persistent map button, not the transient HUD. */
final class BattleReportUi {
    private final MainActivity a;private final World w;
    private List<BattleReports.Entry> rows=Collections.emptyList();
    private int visible=80;
    private boolean tightLayout;
    private final List<Integer> turns=new ArrayList<>();
    private Spinner time,scope,kind;private EditText search;private TextView count,empty;private Button more;
    private ReportAdapter adapter;private AlertDialog dialog;
    BattleReportUi(MainActivity a,World w){this.a=a;this.w=w;}
    void show(){show(w.turn);}
    void show(int preferredTurn){
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(12),a.dp(6),a.dp(12),a.dp(8));root.setBackgroundColor(a.ink);
        TextView subtitle=a.text("近三个月 · 全势力实际结果 · 随存档保存",12,a.muted);root.addView(subtitle);
        List<String> dates=new ArrayList<>();turns.add(w.turn);dates.add("本旬 · "+w.reports.date(w.turn));turns.add(-1);dates.add("近三个月 · 全部9旬");
        for(int t=w.turn-1;t>=Math.max(0,w.turn-8);t--){turns.add(t);dates.add(w.reports.date(t)+(t==w.turn-1?" · 上一旬":""));}
        LinearLayout filters=new LinearLayout(a);time=spinner(dates);scope=spinner(Arrays.asList("我方相关","全部势力","我方主动","我方受影响"));
        time.setTag("reports.turn");scope.setTag("reports.scope");filters.addView(time,new LinearLayout.LayoutParams(0,a.dp(48),1.25f));filters.addView(scope,new LinearLayout.LayoutParams(0,a.dp(48),1));root.addView(filters);
        List<String> kinds=new ArrayList<>();kinds.add("全部类型");for(BattleReports.Kind k:BattleReports.Kind.values())kinds.add(k.label);
        LinearLayout second=new LinearLayout(a);kind=spinner(kinds);kind.setTag("reports.kind");second.addView(kind,new LinearLayout.LayoutParams(a.dp(120),a.dp(48)));
        search=new EditText(a);search.setSingleLine(true);search.setTextColor(a.paper);search.setHintTextColor(a.muted);search.setTextSize(13);search.setHint("搜索武将 / 据点 / 结果");search.setTag("reports.search");search.setContentDescription("战报搜索");search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);second.addView(search,new LinearLayout.LayoutParams(0,a.dp(48),1));
        Button clear=a.button("清空",v->search.setText(""));clear.setContentDescription("清空战报搜索");second.addView(clear,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));root.addView(second);
        Button compactFilter=a.button("筛选",view->{
            String[] labels={"时间 · "+time.getSelectedItem(),"范围 · "+scope.getSelectedItem(),"类型 · "+kind.getSelectedItem()};
            new AlertDialog.Builder(a).setTitle("战报筛选").setItems(labels,(d,which)->{
                List<String> values=which==0?dates:which==1?Arrays.asList("我方相关","全部势力","我方主动","我方受影响"):kinds;
                Spinner target=which==0?time:which==1?scope:kind;
                AlertDialog choose=new AlertDialog.Builder(a).setTitle(labels[which]).setSingleChoiceItems(values.toArray(new String[0]),target.getSelectedItemPosition(),(picker,index)->{target.setSelection(index);picker.dismiss();}).setNegativeButton("返回",null).create();choose.show();a.trackDialog(choose);
            }).setNegativeButton("返回",null).show();
        });compactFilter.setContentDescription("战报时间、范围与类型筛选");second.addView(compactFilter,0,new LinearLayout.LayoutParams(a.dp(56),a.dp(48)));compactFilter.setVisibility(View.GONE);
        count=a.text("",12,a.gold);count.setPadding(0,a.dp(5),0,a.dp(5));count.setTag("reports.count");count.setEllipsize(TextUtils.TruncateAt.END);root.addView(count);
        FrameLayout body=new FrameLayout(a);ListView list=new ListView(a);list.setTag("reports.list");list.setDividerHeight(a.dp(1));list.setCacheColorHint(android.graphics.Color.TRANSPARENT);body.addView(list,new FrameLayout.LayoutParams(-1,-1));
        empty=a.text("",14,a.muted);empty.setGravity(Gravity.CENTER);empty.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(8));body.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
        more=a.button("再显示80条",v->{visible+=80;adapter.notifyDataSetChanged();updateCount();});adapter=new ReportAdapter();list.setAdapter(adapter);
        list.setOnItemClickListener((parent,view,position,id)->{if(position<Math.min(visible,rows.size()))detail(rows.get(position));});root.addView(body,new LinearLayout.LayoutParams(-1,0,1));root.addView(more,new LinearLayout.LayoutParams(-1,a.dp(48)));
        dialog=new AlertDialog.Builder(a).setTitle("战报中心").setView(root).setPositiveButton("返回地图",null).setNeutralButton("刷新",null).create();
        AdapterView.OnItemSelectedListener listener=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){refresh();}};
        time.setOnItemSelectedListener(listener);scope.setOnItemSelectedListener(listener);kind.setOnItemSelectedListener(listener);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){refresh();}public void afterTextChanged(Editable e){}});
        int selected=turns.indexOf(preferredTurn);time.setSelection(selected<0?0:selected);
        // Use the actual visible window: rotation and the keyboard change the space for results.
        ViewTreeObserver.OnGlobalLayoutListener fit=()->{
            android.graphics.Rect frame=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(frame);
            boolean ime=search.hasFocus()&&frame.height()<a.getResources().getDisplayMetrics().heightPixels*.75f;
            boolean narrow=frame.width()<a.dp(340);
            if(narrow&&kind.getParent()==second){second.removeView(kind);filters.addView(kind,new LinearLayout.LayoutParams(0,a.dp(48),1));}
            else if(!narrow&&kind.getParent()==filters){filters.removeView(kind);second.addView(kind,0,new LinearLayout.LayoutParams(a.dp(120),a.dp(48)));}
            boolean compact=frame.height()<a.dp(480);tightLayout=frame.height()<a.dp(300)||(ime&&frame.height()<a.dp(360));
            subtitle.setVisibility(compact?View.GONE:View.VISIBLE);compactFilter.setVisibility(tightLayout?View.VISIBLE:View.GONE);
            filters.setVisibility(tightLayout?View.GONE:View.VISIBLE);kind.setVisibility(tightLayout?View.GONE:View.VISIBLE);count.setVisibility(tightLayout?View.GONE:View.VISIBLE);
            int lines=compact?1:2;if(count.getMaxLines()!=lines)count.setMaxLines(lines);
            String emptyText=tightLayout?(w.reports.size()==0?"还没有历史战报":"没有符合筛选条件的战报"):(w.reports.size()==0?"还没有历史战报\n新行动会自动记录；旧版存档无法补回未曾保存的历史。":"没有符合筛选条件的战报\n可切换『近三个月』或『全部势力』。");if(!TextUtils.equals(empty.getText(),emptyText))empty.setText(emptyText);
            empty.setTextSize(tightLayout?13:14);empty.setPadding(a.dp(8),a.dp(tightLayout?2:8),a.dp(8),a.dp(tightLayout?2:8));
            boolean minimal=tightLayout&&frame.height()<a.dp(300);
            for(String panel:new String[]{"topPanel","buttonPanel"}){int id=a.getResources().getIdentifier(panel,"id","android");View chrome=dialog.findViewById(id);if(chrome!=null)chrome.setVisibility(minimal?View.GONE:View.VISIBLE);}
            int reserve=a.dp(minimal?32:112),height=Math.max(a.dp(64),Math.min(a.dp(720),frame.height()-reserve));
            ViewGroup.LayoutParams size=root.getLayoutParams();if(size!=null&&size.height!=height){size.height=height;root.setLayoutParams(size);}

        };
        root.getViewTreeObserver().addOnGlobalLayoutListener(fit);
        dialog.setOnDismissListener(v->root.getViewTreeObserver().removeOnGlobalLayoutListener(fit));
        dialog.setOnShowListener(v->{dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view->refresh());refresh();fit.onGlobalLayout();});
        dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private Spinner spinner(List<String> values){Spinner result=new Spinner(a);ArrayAdapter<String> data=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,values){
        @Override public View getView(int p,View convert,ViewGroup parent){TextView text=a.text(getItem(p),12,a.paper);text.setGravity(Gravity.CENTER_VERTICAL);text.setPadding(a.dp(6),0,a.dp(3),0);text.setMaxLines(2);return text;}
        @Override public View getDropDownView(int p,View convert,ViewGroup parent){TextView text=a.text(getItem(p),14,a.paper);text.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));text.setBackgroundColor(a.ink);return text;}
    };result.setAdapter(data);return result;}
    private void refresh(){
        if(adapter==null||search==null)return;BattleReports.Scope selected=new BattleReports.Scope[]{BattleReports.Scope.RELATED,BattleReports.Scope.ALL,BattleReports.Scope.INITIATED,BattleReports.Scope.RECEIVED}[Math.max(0,scope.getSelectedItemPosition())];int type=kind.getSelectedItemPosition();
        rows=w.reports.query(turns.get(Math.max(0,time.getSelectedItemPosition())),w.player,selected,type<=0?null:BattleReports.Kind.values()[type-1],search.getText().toString());visible=80;
        String emptyText=tightLayout?(w.reports.size()==0?"还没有历史战报":"没有符合筛选条件的战报"):(w.reports.size()==0?"还没有历史战报\n新行动会自动记录；旧版存档无法补回未曾保存的历史。":"没有符合筛选条件的战报\n可切换『近三个月』或『全部势力』。");if(!TextUtils.equals(empty.getText(),emptyText))empty.setText(emptyText);adapter.notifyDataSetChanged();updateCount();
    }
    private void updateCount(){count.setText("找到 "+rows.size()+" 条 · 已展示 "+Math.min(visible,rows.size())+" 条 · 我方相关包含敌方对我方的影响");more.setVisibility(rows.size()>visible?View.VISIBLE:View.GONE);}
    private final class ReportAdapter extends BaseAdapter {
        public int getCount(){return Math.min(visible,rows.size());}public BattleReports.Entry getItem(int p){return rows.get(p);}public long getItemId(int p){return rows.get(p).id;}public boolean hasStableIds(){return true;}
        public View getView(int position,View convert,ViewGroup parent){
            LinearLayout row;TextView meta,title,detail;
            if(convert instanceof LinearLayout){row=(LinearLayout)convert;meta=(TextView)row.getChildAt(0);title=(TextView)row.getChildAt(1);detail=(TextView)row.getChildAt(2);}
            else{row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(a.dp(9),a.dp(9),a.dp(9),a.dp(9));meta=a.text("",11,a.gold);title=a.text("",14,a.paper);detail=a.text("",12,a.muted);title.setMaxLines(2);detail.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);detail.setEllipsize(TextUtils.TruncateAt.END);row.addView(meta);row.addView(title);row.addView(detail);}
            BattleReports.Entry entry=getItem(position);meta.setText(w.reports.date(entry.turn)+" · "+entry.kind.label+" · "+(entry.actor<0?"结算结果":w.faction(entry.actor)));title.setText(entry.title);
            String description=entry.detail;int newline=description.indexOf('\n');detail.setText(newline>=0?description.substring(newline+1):"点击查看完整结果"+(entry.location==null?"":" / 定位地图"));row.setContentDescription(meta.getText()+"，"+entry.title);return row;
        }
    }
    private void detail(BattleReports.Entry entry){
        TextView body=a.text(entry.detail,14,a.paper);body.setTextIsSelectable(true);body.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));ScrollView scroll=new ScrollView(a);scroll.setBackgroundColor(a.ink);scroll.addView(body);
        AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(w.reports.date(entry.turn)+" · "+entry.kind.label).setView(scroll).setPositiveButton("返回战报",null);
        if(entry.location!=null&&w.inside(entry.location))builder.setNeutralButton("定位地图",(d,i)->{if(a.currentWorld(w)){dialog.dismiss();a.selectAndFocus(entry.location);}});AlertDialog detail=builder.create();detail.setOnDismissListener(v->{if(dialog.isShowing())a.trackDialog(dialog);});detail.show();a.trackDialog(detail);
    }
}
