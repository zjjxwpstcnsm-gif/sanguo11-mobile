package game.sanguo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Searchable civil/military appointment sheet. Locked offices explain, but cannot execute. */
final class OfficePicker {
    private OfficePicker() {}
    static void show(Activity a,World w,World.City city,World.Officer actor,World.Officer target,Consumer<Government.Rank> select){
        float density=a.getResources().getDisplayMetrics().density;
        LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);int pad=Math.round(12*density);host.setPadding(pad,pad,pad,pad);
        TextView summary=new TextView(a);summary.setText(w.governance.titleProgress(city.owner)+"\n"+target.name+" · 功绩"+w.government.merit(target.id)+"\n"+w.government.commandDescription(target.id));summary.setTextSize(13);host.addView(summary);
        RadioGroup filters=new RadioGroup(a);filters.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"可任命","武官","文官","全部"};
        int[] filterIds=new int[names.length];
        for(int i=0;i<names.length;i++){RadioButton radio=new RadioButton(a);filterIds[i]=View.generateViewId();radio.setId(filterIds[i]);radio.setText(names[i]);radio.setTextSize(12);filters.addView(radio,new RadioGroup.LayoutParams(0,-2,1));}
        filters.check(filterIds[0]);host.addView(filters);
        EditText query=new EditText(a);query.setSingleLine();query.setHint("搜索官职 / 所需爵位 / 功绩");host.addView(query);
        List<Government.Rank> rows=new ArrayList<>();
        ArrayAdapter<Government.Rank> adapter=new ArrayAdapter<Government.Rank>(a,android.R.layout.simple_list_item_1,rows){
            @Override public boolean areAllItemsEnabled(){return false;}
            @Override public boolean isEnabled(int position){return w.government.appointmentError(city.id,actor.id,target.id,rows.get(position).id)==null;}
            @Override public View getView(int position,View convert,ViewGroup parent){
                TextView text=(TextView)super.getView(position,convert,parent);Government.Rank r=rows.get(position);
                String error=w.government.appointmentError(city.id,actor.id,target.id,r.id);
                text.setText(r.id+" · "+(r.civilian?"文官":"武官")+"\n需"+r.requiredTitle.label+" / 功绩"+r.merit+" · 基础统兵"+r.troops+" · 月俸"+r.salary+"\n"+(error==null?"可任命":error));
                text.setTextSize(13);text.setSingleLine(false);text.setAlpha(error==null?1f:.55f);return text;
            }
        };
        ListView list=new ListView(a);list.setAdapter(adapter);list.setFastScrollEnabled(true);
        host.addView(list,new LinearLayout.LayoutParams(-1,Math.round(Math.max(140,Math.min(320,a.getResources().getConfiguration().screenHeightDp-260))*density)));
        TextView empty=new TextView(a);empty.setText("没有符合条件的官职；切换“全部”查看爵位、功绩或占用原因。");empty.setTextSize(13);host.addView(empty);list.setEmptyView(empty);
        Runnable refresh=()->{
            rows.clear();int filter=filters.getCheckedRadioButtonId();String needle=query.getText().toString().trim();
            for(Government.Rank r:Government.ranks()){
                String error=w.government.appointmentError(city.id,actor.id,target.id,r.id);
                if(filter==filterIds[0]&&error!=null||filter==filterIds[1]&&r.civilian||filter==filterIds[2]&&!r.civilian)continue;
                if(!(r.id+" "+r.requiredTitle.label+" "+r.merit+" "+(error==null?"可任命":error)).contains(needle))continue;
                rows.add(r);
            }
            rows.sort(Comparator.comparingInt((Government.Rank r)->-r.requiredTitle.grade()).thenComparing(r->r.civilian).thenComparing(r->r.id));adapter.notifyDataSetChanged();
        };
        filters.setOnCheckedChangeListener((group,id)->refresh.run());
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){refresh.run();}public void afterTextChanged(Editable s){}});
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("授予官职 · "+target.name).setView(host).setNeutralButton("爵位对照",(d,n)->showTitleTable(a)).setNegativeButton("返回",null).create();
        list.setOnItemClickListener((parent,view,position,id)->{
            if(a instanceof MainActivity&&!((MainActivity)a).currentWorld(w))return;
            Government.Rank rank=rows.get(position);String error=w.government.appointmentError(city.id,actor.id,target.id,rank.id);
            if(error!=null){refresh.run();return;}dialog.dismiss();select.accept(rank);
        });
        refresh.run();dialog.show();UiTheme.dialog(dialog);if(a instanceof MainActivity)((MainActivity)a).trackDialog(dialog);
    }
    static void showTitleTable(Activity a){
        StringBuilder text=new StringBuilder("仅统计城池，不计港口、关卡或七格占地。高爵位累积解锁低阶官职；每个官职同势力限一人，仍须满足武将功绩。\n无官武将5000；军制改革在主将基础上另加3000，副将不叠加统兵上限。\n");
        for(RulerTitles.Title title:RulerTitles.all()){
            text.append("\n").append(title.label).append(" · ").append(title.cities).append("城 · 君主").append(title.troops).append("兵\n");
            for(boolean civil:new boolean[]{false,true}){
                StringJoiner offices=new StringJoiner(" / ");Government.Rank sample=null;
                for(Government.Rank rank:Government.ranks())if(rank.requiredTitle==title&&rank.civilian==civil){offices.add(rank.id);sample=rank;}
                if(sample!=null)text.append(civil?"文：":"武：").append(offices).append("\n统兵").append(sample.troops).append(" · 功绩").append(sample.merit).append(" · 月俸").append(sample.salary).append("\n");
            }
        }
        text.append("\n当前手游保留按城数取得爵位的结算方式；原版月初授爵、汉帝禅让及黄巾特殊身份事件尚未复刻。官职能力加成不在本轮统兵表校正范围。已获爵位失城不降；旧存档已有官职与部队保留。");
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("君主爵位与官职").setMessage(text.toString()).setPositiveButton("返回",null).show();UiTheme.dialog(dialog);
    }
}
