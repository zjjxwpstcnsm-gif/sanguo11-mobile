package game.sanguo.mobile;

import android.app.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import org.json.*;
import java.util.*;

/** Receives effective sites through World; never knows map patch classes, coordinates or city counts. */
final class CustomOfficerPlacementUi {
    private final Activity a;private final World resolved;private CustomOfficerLibrary library;
    private JSONObject root,plan;private AlertDialog dialog;private LinearLayout rows;private TextView status;
    CustomOfficerPlacementUi(Activity a,World resolved){this.a=a;this.resolved=resolved;}
    private void fail(Exception e){new AlertDialog.Builder(a).setTitle("投放未通过").setMessage(e.getMessage()).setPositiveButton("返回",null).show();}
    private Button button(String label,Runnable run){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->{try{run.run();}catch(Exception e){fail(e);}});return b;}
    void show(){try{
        library=new CustomOfficerLibrary(a);root=library.snapshot();plan=CustomOfficerLibrary.copy(CustomOfficerSetup.plan(root,resolved.scenarioId));
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,8,16,8);CheckBox enable=new CheckBox(a);enable.setText("本剧本启用自定义武将（默认关闭）");enable.setChecked(plan.optBoolean("enabled",false));enable.setOnCheckedChangeListener((b,on)->{try{plan.put("enabled",on);}catch(JSONException e){fail(e);}});box.addView(enable);
        status=new TextView(a);status.setText(resolved.scenarioName+" · "+resolved.date()+"\n据点来自当前实际地图；启用前必须通过投放预览。");box.addView(status);
        box.addView(button("添加投放人物 / 历史覆盖",this::add));box.addView(button("模板库（先保存本页配置）",()->{a.startActivity(new android.content.Intent(a,CustomOfficerActivity.class));dialog.dismiss();}));
        rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(a);scroll.addView(rows);box.addView(scroll,new LinearLayout.LayoutParams(-1,Math.max(180,a.getResources().getDisplayMetrics().heightPixels/3)));
        box.addView(button("校验并预览实际战局",this::preview));
        dialog=new AlertDialog.Builder(a).setTitle("自定义武将 · 剧本投放").setView(box).setPositiveButton("保存配置",null).setNegativeButton("取消",null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{try{CustomOfficerSetup.putPlan(root,plan);if(plan.optBoolean("enabled",false))CustomOfficerSetup.apply(a,resolved,root);library.replace(root);dialog.dismiss();Toast.makeText(a,"投放配置已保存，下一次新开该剧本生效",Toast.LENGTH_LONG).show();}catch(Exception e){fail(e);}}));refresh();dialog.show();
    }catch(Exception e){fail(e);}}
    private void refresh()throws JSONException{
        rows.removeAllViews();JSONArray placements=plan.getJSONArray("placements");for(int i=0;i<placements.length();i++){final int at=i;JSONObject p=placements.getJSONObject(i),d=library.find(p.getString("definition"));World.City c=resolved.city(p.getInt("city"));
            rows.addView(button((d==null?"缺失人物":d.getString("name"))+" · "+CustomOfficers.Mode.valueOf(p.getString("mode")).label+"\n"+(c==null?"缺失据点 #"+p.getInt("city"):c.name)+" · "+(p.optBoolean("ignoreDates")?"显式忽略年代":"按年代校验"),()->{new AlertDialog.Builder(a).setItems(new String[]{"修改投放","移除（不删除模板）"},(dlg,n)->{try{if(n==0)edit(d,p,at);else{plan.getJSONArray("placements").remove(at);refresh();}}catch(Exception e){fail(e);}}).show();}));}
    }
    private void add(){try{List<JSONObject> choices=new ArrayList<>();Set<String> selected=new HashSet<>();JSONArray placements=plan.getJSONArray("placements");for(int i=0;i<placements.length();i++)selected.add(placements.getJSONObject(i).getString("definition"));for(JSONObject d:library.entries())if(!selected.contains(d.getString("id")))choices.add(d);
        CustomOfficerActivity.search(a,"选择模板",choices,o->o.optString("name")+" · "+(o.optInt("targetId",-1)<0?"新武将":"历史覆盖 #"+o.optInt("targetId"))+" · "+o.optString("id").substring(0,8),d->{try{edit(d,null,-1);}catch(Exception e){fail(e);}});
    }catch(Exception e){fail(e);}}
    private void edit(JSONObject d,JSONObject previous,int index)throws JSONException{
        if(d==null)throw new IllegalArgumentException("模板不存在");boolean override=d.optInt("targetId",-1)>=0;
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);TextView note=new TextView(a);note.setText(d.getString("name")+" · 生 "+d.optInt("birth")+" / 登场 "+d.optInt("appearance")+" / 没 "+d.optInt("death")+"\n历史覆盖保留君主、军师、太守与原归属，不重新任命。");box.addView(note);
        List<CustomOfficers.Mode> modes=override?Collections.singletonList(CustomOfficers.Mode.KEEP):Arrays.asList(CustomOfficers.Mode.FACTION,CustomOfficers.Mode.WILD,CustomOfficers.Mode.UNAPPEARED);
        Spinner mode=new Spinner(a);mode.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,modes.stream().map(m->m.label).toArray(String[]::new)));if(previous!=null)mode.setSelection(Math.max(0,modes.indexOf(CustomOfficers.Mode.valueOf(previous.getString("mode")))));box.addView(mode);
        List<Integer> owners=new ArrayList<>();for(int i=0;i<resolved.factions.length;i++)if(resolved.alive(i))owners.add(i);Spinner owner=new Spinner(a);owner.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,owners.stream().map(resolved::faction).toArray(String[]::new)));if(previous!=null)owner.setSelection(Math.max(0,owners.indexOf(previous.optInt("owner",-1))));box.addView(owner);
        final int[] site={previous==null?-1:previous.optInt("city",-1)};final Button[] siteButton={null};Button chooseSite=button("选择有效驻地（必选）",()->{
            CustomOfficers.Mode m=modes.get(mode.getSelectedItemPosition());int faction=owners.isEmpty()?-1:owners.get(owner.getSelectedItemPosition());List<World.City> cities=new ArrayList<>();for(World.City c:resolved.cities)if(m!=CustomOfficers.Mode.FACTION||c.owner==faction)cities.add(c);
            CustomOfficerActivity.search(a,"实际地图驻地",cities,c->c.name+" · "+resolved.faction(c.owner)+" · #"+c.id,c->{site[0]=c.id;siteButton[0].setText("驻地："+c.name+" · #"+c.id+"（可更改）");});
        });siteButton[0]=chooseSite;box.addView(chooseSite);if(previous!=null)chooseSite.setText("驻地 #"+site[0]+" · 点此更改");
        if(override){World.Officer historic=resolved.officer(d.optInt("targetId"));site[0]=historic==null?-1:historic.cityId>=0?historic.cityId:resolved.life.life(historic.id)==null?-1:resolved.life.life(historic.id).home;chooseSite.setEnabled(false);chooseSite.setText("沿用历史驻地 #"+site[0]);owner.setEnabled(false);}
        TextView loyaltyLabel=new TextView(a);loyaltyLabel.setText("初始忠诚（0—100；在野/未登场强制0）");box.addView(loyaltyLabel);EditText loyalty=new EditText(a);loyalty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);loyalty.setText(Integer.toString(previous==null?85:previous.optInt("loyalty",85)));box.addView(loyalty);
        CheckBox ignore=new CheckBox(a);ignore.setText("明确忽略出生/登场/没年限制（会写入本局快照）");ignore.setChecked(previous!=null&&previous.optBoolean("ignoreDates",false));box.addView(ignore);
        AlertDialog form=new AlertDialog.Builder(a).setTitle("投放配置").setView(box).setPositiveButton("确定",null).setNegativeButton("取消",null).create();form.setOnShowListener(v->form.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{try{
            CustomOfficers.Mode m=modes.get(mode.getSelectedItemPosition());if(site[0]<0)throw new IllegalArgumentException("尚未选择驻地，或历史目标未在本剧本出现");int side=m==CustomOfficers.Mode.FACTION?owners.get(owner.getSelectedItemPosition()):-1;int loyal=Integer.parseInt(loyalty.getText().toString());if(loyal<0||loyal>100)throw new IllegalArgumentException("忠诚范围0—100");if(side<0)loyal=0;
            JSONObject p=new JSONObject().put("id",previous==null?UUID.randomUUID().toString():previous.getString("id")).put("definition",d.getString("id")).put("mode",m.name()).put("owner",side).put("city",site[0]).put("loyalty",loyal).put("ignoreDates",ignore.isChecked());JSONArray placements=plan.getJSONArray("placements");if(index<0)placements.put(p);else placements.put(index,p);refresh();form.dismiss();
        }catch(Exception e){fail(e);}}));form.show();
    }
    private void preview(){try{
        JSONObject candidate=CustomOfficerLibrary.copy(root),p=CustomOfficerLibrary.copy(plan);p.put("enabled",true);CustomOfficerSetup.putPlan(candidate,p);World next=CustomOfficerSetup.apply(a,resolved,candidate);StringBuilder summary=new StringBuilder("校验通过；预览使用副本，当前战局/基础剧本未改变。\n");JSONArray placements=p.getJSONArray("placements");for(int i=0;i<placements.length();i++){JSONObject d=library.find(placements.getJSONObject(i).getString("definition"));CustomOfficers.Definition def=CustomOfficerSetup.definition(d);World.Officer o=next.officer(def.effectiveId());summary.append(o.name).append(" #").append(o.id).append(" · ").append(next.life.state(o.id).label).append(" · ").append(next.faction(o.owner)).append(" · ").append(o.cityId<0?"未登场":next.city(o.cityId).name).append('\n');World.Unit unit=new World.Unit(999999,o.owner<0?next.player:o.owner,o.id,World.Weapon.SPEAR,next.city(placements.getJSONObject(i).getInt("city")).hex,5000,10000);summary.append("5000枪兵正式计算预览：攻击 ").append(next.combat.attackRating(unit)).append(" / 防御 ").append(next.combat.defenseRating(unit)).append('\n');}
        status.setText("已完成投放校验 · "+placements.length()+"人");new AlertDialog.Builder(a).setTitle("正式初始化预览").setMessage(summary.toString()).setPositiveButton("返回",null).show();
    }catch(Exception e){fail(e);}}
}
