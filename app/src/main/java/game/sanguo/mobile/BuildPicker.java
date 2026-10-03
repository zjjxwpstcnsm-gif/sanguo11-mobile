package game.sanguo.mobile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.*;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.util.*;

/** A parcel-first construction form. A draft is never an issued command. */
final class BuildPicker {
    private final MainActivity a;private final World w;private final Bundle draft;
    BuildPicker(MainActivity a,World w,Bundle draft){this.a=a;this.w=w;this.draft=new Bundle(draft);}
    static void open(MainActivity a,World w,World.City c,Hex h){
        Bundle d=new Bundle();d.putString("kind","build");d.putInt("city",c.id);d.putInt("q",h.q);d.putInt("r",h.r);d.putBoolean("open",true);new BuildPicker(a,w,d).show();
    }
    private void remember(){draft.putBoolean("open",true);a.rememberForm(draft);}
    void show(){
        World.City c=w.city(draft.getInt("city",-1));Hex h=new Hex(draft.getInt("q"),draft.getInt("r"));
        if(c==null||c.owner!=w.player||!w.domestic.buildSites(c.id).contains(h)){a.closeForm();new AlertDialog.Builder(a).setTitle("开发地不可用").setMessage("地块已被占用或城池归属已改变，请重新选择。").setPositiveButton("返回",null).show();return;}
        remember();boolean officers=draft.containsKey("facility");
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(a.dp(12),0,a.dp(12),0);
        TextView description=a.text(c.name+" · 开发地 "+h.q+","+h.r+"\n已用 "+w.domestic.count(c.id)+" / "+w.development.capacity(c.id)+" · 可用金 "+c.gold+"\n选择执行人后核对费用和工期 · 闲置武将 "+w.idle(c).size()+"人",14,a.paper);form.addView(description);
        EditText query=new EditText(a);query.setSingleLine();query.setHint(officers?"搜索执行武将":"搜索设施");query.setContentDescription("建设搜索");query.setText(draft.getString(officers?"officerQuery":"facilityQuery",""));UiTheme.search(query);
        LinearLayout search=new LinearLayout(a);search.addView(query,new LinearLayout.LayoutParams(0,a.dp(48),1));
        Button clear=a.button("×",v->query.setText(""));clear.setContentDescription("清空建设搜索");search.addView(clear,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));form.addView(search);
        query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);query.clearFocus();return true;}return false;});
        TextView count=a.text("",13,a.gold);count.setTag("build.count");form.addView(count);
        ListView list=new ListView(a);list.setContentDescription("建设选项列表");form.addView(list,new LinearLayout.LayoutParams(-1,a.dp(240)));
        LinearLayout pager=new LinearLayout(a);Button previous=a.button("上一页",v->{}),next=a.button("下一页",v->{});TextView page=a.text("",13,a.paper);page.setGravity(android.view.Gravity.CENTER);
        pager.addView(previous,new LinearLayout.LayoutParams(0,a.dp(48),1));pager.addView(page,new LinearLayout.LayoutParams(0,a.dp(48),1));pager.addView(next,new LinearLayout.LayoutParams(0,a.dp(48),1));form.addView(pager);
        List<Object> filtered=new ArrayList<>(),visible=new ArrayList<>();int[] current={draft.getInt("page",0)};
        Runnable refresh=()->{
            filtered.clear();String q=query.getText().toString().trim();
            if(officers){for(World.Officer o:w.idle(c))if(o.name.contains(q))filtered.add(o);filtered.sort(Comparator.comparingInt(o->-((World.Officer)o).politics));}
            else for(Domestic.Kind kind:Domestic.Kind.values())if(kind.label.contains(q)&&(kind!=Domestic.Kind.SHIPYARD||h.neighbors().stream().anyMatch(w.army::water)))filtered.add(kind);
            int pages=Math.max(1,(filtered.size()+7)/8);current[0]=Math.max(0,Math.min(current[0],pages-1));visible.clear();int from=current[0]*8;visible.addAll(filtered.subList(Math.min(from,filtered.size()),Math.min(from+8,filtered.size())));
            list.setAdapter(GameIcon.adapter(a,w,visible,o->o instanceof World.Officer?((World.Officer)o).name+" · 政治"+((World.Officer)o).politics:((Domestic.Kind)o).label+" · Lv"+Domestic.buildLevel((Domestic.Kind)o)+" · 金"+((Domestic.Kind)o).cost+"\n"+Domestic.buildEffect((Domestic.Kind)o)));
            count.setText(filtered.isEmpty()?(officers?"暂无匹配的闲置武将，可清空搜索或下旬再来":"没有匹配设施，请修改搜索"):"共 "+filtered.size()+" 项"+(officers?" · 按政治排序":" · 选择后查看建设详情"));pager.setVisibility(pages>1?android.view.View.VISIBLE:android.view.View.GONE);page.setText((current[0]+1)+" / "+pages);previous.setEnabled(current[0]>0);next.setEnabled(current[0]+1<pages);draft.putInt("page",current[0]);remember();
        };
        ScrollView formScroll=new ScrollView(a);formScroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(officers?"建设 · 选择执行人":"开发地 · 选择设施").setView(formScroll).setNegativeButton("取消",(d,n)->a.closeForm()).setNeutralButton(officers?"上一步":"返回地图",(d,n)->{if(officers){draft.remove("facility");draft.putInt("page",0);show();}else a.closeForm();}).create();
        dialog.setOnCancelListener(d->a.closeForm());
        list.setOnItemClickListener((parent,v,i,id)->{
            Object selected=visible.get(i);
            if(!officers){dialog.dismiss();draft.putInt("facility",((Domestic.Kind)selected).ordinal());draft.putInt("page",0);show();return;}
            World.Officer o=(World.Officer)selected;Domestic.Kind kind=Domestic.Kind.values()[draft.getInt("facility")];
            ConstructionCommand command=new ConstructionCommand(a.deploymentState(),c.id,o.id,kind.name(),h.q,h.r);
            ConstructionPreview preview=a.previewConstruction(command);
            StringBuilder detail=new StringBuilder(c.name+" · "+o.name+" · 地块"+h);
            if(preview.resources!=null){ConstructionPreview.Resources r=preview.resources;detail.append("\n金 ").append(r.goldCost).append(" / 行动力 ").append(r.actionPointsCost).append("\n当前金 ").append(r.goldAvailable).append(" / 行动力 ").append(r.actionPointsAvailable);}
            if(preview.completion!=null){ConstructionPreview.Completion completion=preview.completion;detail.append("\n预计 ").append(completion.turns).append("旬 · Lv").append(completion.level).append("\n耐久 ").append(completion.initialDurability).append(" / ").append(completion.maximumDurability).append("\n").append(completion.effect).append("\n建设期间占用武将；中止、易主或毁坏会影响完成。");}
            if(!preview.allowed())detail.append("\n\n不能开工：").append(MainActivity.commandError(preview.error,preview.detail));
            AlertDialog review=a.commandDialog("建设"+kind.label,detail.toString(),"开工","返回选择",w,()->{a.closeForm();dialog.dismiss();a.executeConstruction(command);});
            review.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(preview.allowed());
            review.setOnDismissListener(d->{if(dialog.isShowing())a.trackDialog(dialog);});
        });
        previous.setOnClickListener(v->{current[0]--;refresh.run();});next.setOnClickListener(v->{current[0]++;refresh.run();});
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable s){draft.putString(officers?"officerQuery":"facilityQuery",s.toString());current[0]=0;refresh.run();}});
        CharSequence fullDescription=description.getText();
        android.view.ViewTreeObserver.OnGlobalLayoutListener fit=()->{
            android.graphics.Rect frame=new android.graphics.Rect();form.getWindowVisibleDisplayFrame(frame);
            boolean compact=frame.height()<a.dp(480);CharSequence summary=compact?c.name+" · 地块 "+h.q+","+h.r+" · 金"+c.gold:fullDescription;
            if(!description.getText().toString().contentEquals(summary))description.setText(summary);
            int available=frame.height()-a.dp(150)-description.getHeight()-search.getHeight()-count.getHeight()-(pager.getVisibility()==android.view.View.VISIBLE?a.dp(48):0);
            int desired=filtered.isEmpty()?a.dp(8):Math.max(a.dp(64),Math.min(a.dp(240),available));
            if(list.getLayoutParams().height!=desired){list.getLayoutParams().height=desired;list.requestLayout();}
        };
        form.getViewTreeObserver().addOnGlobalLayoutListener(fit);
        dialog.setOnDismissListener(d->form.getViewTreeObserver().removeOnGlobalLayoutListener(fit));
        refresh.run();dialog.show();a.trackDialog(dialog);dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
