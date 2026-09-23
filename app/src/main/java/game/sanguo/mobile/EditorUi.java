package game.sanguo.mobile;

import android.app.*;
import android.text.InputType;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

/** Bounded native editor and reusable custom-officer library. No changes before confirmation. */
final class EditorUi {
    private final MainActivity a;private final World w;private final LegacyCommandSink apply;
    EditorUi(MainActivity a,World w,LegacyCommandSink apply){this.a=a;this.w=w;this.apply=apply;}
    private void info(String title,String message){new AlertDialog.Builder(a).setTitle(title).setMessage(message).setPositiveButton("返回",null).show();}
    private <T> void choose(String title,List<T> values,Function<T,String> label,Consumer<T> next){
        if(values.isEmpty()){info(title,"没有可选对象");return;}String[] names=new String[values.size()];for(int i=0;i<names.length;i++)names[i]=label.apply(values.get(i));
        new AlertDialog.Builder(a).setTitle(title).setItems(names,(d,n)->next.accept(values.get(n))).setNegativeButton("返回",null).show();
    }
    void menu(){
        if(w.contests.busy()){info("PK编辑","请先完成当前对局");return;}
        new AlertDialog.Builder(a).setTitle("PK编辑 / 新武将 / 地图").setItems(new String[]{"编辑武将","编辑据点","编辑势力","编辑部队","编辑人物关系","配置宝物","制作新武将模板","已保存新武将","导入新武将模板","导出新武将模板","地图编辑器（独立草稿）"},(d,n)->{
            switch(n){
                case 0:choose("选择编辑武将",w.officers,o->o.name+" · "+w.faction(o.owner),this::officer);break;
                case 1:choose("选择编辑据点",w.cities,c->c.name,this::city);break;
                case 2:faction();break;
                case 3:choose("选择编辑部队",w.units,u->w.officer(u.officerId).name,this::unit);break;
                case 4:relation();break;case 5:treasure();break;
                case 6:templateForm();break;case 7:templates(false);break;
                case 8:a.importOfficerTemplate();break;case 9:templates(true);break;
                case 10:new MapEditorUi(a).show();break;
            }
        }).setNegativeButton("返回",null).show();
    }
    private void preview(Editor.Draft draft){
        if(!draft.valid()){info("编辑未通过",draft.error);return;}
        new AlertDialog.Builder(a).setTitle("确认PK编辑").setMessage(draft.summary+"\n\n应用到当前局面，不消耗行动力；存档将标记已编辑。")
            .setPositiveButton("应用修改",(d,n)->apply.execute(w,()->w.editor.apply(draft))).setNegativeButton("取消",null).show();
    }
    private final class Form {
        final LinearLayout box=new LinearLayout(a);
        Form(){box.setOrientation(LinearLayout.VERTICAL);int pad=a.dp(16);box.setPadding(pad,pad,pad,pad);}
        EditText field(String label,String value,boolean number){TextView t=new TextView(a);t.setText(label);box.addView(t);EditText e=new EditText(a);e.setSingleLine(true);e.setText(value);e.setContentDescription(label);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);box.addView(e);return e;}
        EditText number(String label,int value){return field(label,Integer.toString(value),true);}
        <T> Spinner pick(String label,List<T> options,Function<T,String> name,T selected){TextView t=new TextView(a);t.setText(label);box.addView(t);String[] labels=new String[options.size()];for(int i=0;i<labels.length;i++)labels[i]=name.apply(options.get(i));Spinner s=new Spinner(a);s.setContentDescription(label);s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels));s.setSelection(Math.max(0,options.indexOf(selected)));box.addView(s);return s;}
        void show(String title,String button,Runnable done){ScrollView scroll=new ScrollView(a);scroll.addView(box);AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(scroll).setPositiveButton(button,null).setNegativeButton("取消",null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{try{done.run();dialog.dismiss();}catch(IllegalArgumentException e){info("输入无效",e.getMessage());}}));dialog.show();}
    }
    private int value(EditText e){try{return Integer.parseInt(e.getText().toString());}catch(NumberFormatException ex){throw new IllegalArgumentException("请输入范围内的整数");}}
    private List<String> skills(){List<String> out=new ArrayList<>();out.add("none");for(Skill s:Skill.values())out.add(s.id);return out;}
    private final class PersonForm {
        final Form f=new Form();final EditText name;final EditText[] stats=new EditText[5],apt=new EditText[6];
        final Spinner sex,skill,temper;final CheckBox[] talks=new CheckBox[5];final List<String> skillIds=skills();
        PersonForm(Editor.Template t,boolean rename){
            name=rename?f.field("姓名",t.name,false):null;String[] names={"统率","武力","智力","政治","魅力"};
            for(int i=0;i<5;i++)stats[i]=f.number(names[i]+"（0—100）",t.stat(i));
            for(int i=0;i<6;i++)apt[i]=f.number(new String[]{"枪","戟","弩","骑","器械","水军"}[i]+"适性（C=0/B=1/A=2/S=3）",t.aptitude(i));
            sex=f.pick("性别",Arrays.asList(World.Sex.values()),s->s==World.Sex.MALE?"男":s==World.Sex.FEMALE?"女":"未知",t.sex);
            skill=f.pick("特技（部分效果仍待实现）",skillIds,Skill::label,t.skill);
            temper=f.pick("性格",Arrays.asList(Debate.Temper.values()),s->s.label,t.temper);
            for(int i=0;i<5;i++){talks[i]=new CheckBox(a);talks[i].setText(Debate.Talk.values()[i].label);talks[i].setChecked((t.talkMask&(1<<i))!=0);f.box.addView(talks[i]);}
        }
        Editor.Template read(String oldName){int[] s=new int[5],ap=new int[6];for(int i=0;i<5;i++)s[i]=value(stats[i]);for(int i=0;i<6;i++)ap[i]=value(apt[i]);int mask=0;for(int i=0;i<5;i++)if(talks[i].isChecked())mask|=1<<i;
            Editor.Template t=new Editor.Template(name==null?oldName:name.getText().toString().trim(),s,ap,World.Sex.values()[sex.getSelectedItemPosition()],skillIds.get(skill.getSelectedItemPosition()),Debate.Temper.values()[temper.getSelectedItemPosition()],mask);t.validate();return t;}
    }
    private void officer(World.Officer o){
        PersonForm p=new PersonForm(w.editor.template(o.id),false);EditText loyalty=p.f.number("忠诚（0—100）",o.loyalty),merit=p.f.number("功绩（0—1000000）",w.government.merit(o.id));
        p.f.show("编辑武将 · "+o.name,"预览修改",()->{Editor.Template t=p.read(o.name);int[] s=new int[5],ap=new int[6];for(int i=0;i<5;i++)s[i]=t.stat(i);for(int i=0;i<6;i++)ap[i]=t.aptitude(i);preview(w.editor.officer(o.id,s,ap,t.sex,t.skill,value(loyalty),value(merit),t.temper,t.talkMask));});
    }
    private void city(World.City c){
        Form f=new Form();EditText gold=f.number("金",c.gold),food=f.number("粮",c.food),troops=f.number("兵力",c.troops),order=f.number("治安",c.order),morale=f.number("气力",c.morale),defense=f.number("城防",c.defense),reserve=f.number("兵源",c.recruitReserve);
        EditText[] gear=new EditText[9],ships=new EditText[2];for(int i=0;i<9;i++)gear[i]=f.number(World.Weapon.values()[i].label+"库存",c.equipment[i]);for(int i=0;i<2;i++)ships[i]=f.number(Army.Ship.values()[i+1].label+"库存",c.ships[i]);
        f.show("编辑据点 · "+c.name,"预览修改",()->{int[] g=new int[9],s=new int[2];for(int i=0;i<9;i++)g[i]=value(gear[i]);for(int i=0;i<2;i++)s[i]=value(ships[i]);preview(w.editor.city(c.id,value(gold),value(food),value(troops),value(order),value(morale),value(defense),value(reserve),g,s));});
    }
    private void faction(){List<Integer> sides=new ArrayList<>();for(int i=0;i<w.factions.length;i++)sides.add(i);choose("选择编辑势力",sides,w::faction,side->new AlertDialog.Builder(a).setTitle(w.faction(side)).setItems(new String[]{"行动力与技巧点","解锁技巧（含前置）"},(d,n)->{
        if(n==0){Form f=new Form();EditText ap=f.number("行动力（0—60）",w.actionPoints[side]),tp=f.number("技巧点（0—100000）",w.campaign.points(side));f.show("编辑势力资源","预览修改",()->preview(w.editor.faction(side,value(ap),value(tp))));}
        else {List<Campaign.Tech> techs=new ArrayList<>();for(Campaign.Tech t:Campaign.Tech.values())if(t.level>0)techs.add(t);choose("解锁技巧",techs,t->t.label,t->preview(w.editor.learnTechnology(side,t)));}
    }).setNegativeButton("返回",null).show());}
    private void unit(World.Unit u){Form f=new Form();EditText troops=f.number("兵力（1—18000）",u.troops),food=f.number("携粮",u.food),gold=f.number("携金（0—10000）",u.gold),energy=f.number("气力",u.energy),turns=f.number("异常状态剩余旬数",u.statusTurns);Spinner status=f.pick("部队状态",Arrays.asList(War.Status.values()),s->s.label,u.status);
        f.show("编辑部队 · "+w.officer(u.officerId).name,"预览修改",()->preview(w.editor.unit(u.id,value(troops),value(food),value(gold),value(energy),War.Status.values()[status.getSelectedItemPosition()],value(turns))));}
    private void relation(){choose("人物关系 · 第一位",w.officers,o->o.name,first->choose("人物关系 · 类型",Arrays.asList(Relations.Kind.values()),k->k.label,k->{
        List<World.Officer> others=new ArrayList<>();for(World.Officer o:w.officers)if(o.id!=first.id)others.add(o);
        choose("人物关系 · 第二位",others,o->o.name,second->preview(w.editor.relation(first.id,second.id,k,w.relations.links(first.id,k).contains(second.id))));
    }));}
    private void treasure(){try{choose("配置宝物",Treasures.catalog(),d->d.name+" · "+d.kind.label+" · "+d.id,d->choose("宝物放置方式",Arrays.asList(Treasures.Place.values()),p->p==Treasures.Place.OFFICER?"武将持有":p==Treasures.Place.TREASURY?"势力府库":"城内未发现",p->{
        if(p==Treasures.Place.OFFICER)choose("宝物持有人",w.officers,o->o.name,o->preview(w.editor.treasure(d.id,p,o.id)));
        else if(p==Treasures.Place.HIDDEN)choose("宝物隐藏据点",w.cities,c->c.name,c->preview(w.editor.treasure(d.id,p,c.id)));
        else {List<Integer> sides=new ArrayList<>();for(int i=0;i<w.factions.length;i++)sides.add(i);choose("宝物所属势力",sides,w::faction,s->preview(w.editor.treasure(d.id,p,s)));}
    }));}catch(IOException e){info("宝物读取失败",e.getMessage());}}
    private void templateForm(){PersonForm p=new PersonForm(new Editor.Template("新武将",new int[]{70,70,70,70,70},new int[]{1,1,1,1,1,1},World.Sex.MALE,"none",Debate.Temper.CALM,0),true);
        p.f.show("制作新武将模板","保存模板",()->{Editor.Template t=p.read("");try{store(a,t);info("模板已保存",t.name+"已加入新武将列表；可用于当前或之后的新游戏。");}catch(IOException e){info("保存失败",e.getMessage());}});}
    static void store(Activity a,Editor.Template t)throws IOException{
        try{CustomOfficerLibrary library=new CustomOfficerLibrary(a);byte[] bytes=OfficerTemplateCodec.encode(t);
            for(org.json.JSONObject o:library.entries())if(Arrays.equals(bytes,OfficerTemplateCodec.encode(CustomOfficerLibrary.template(o))))return;
            library.save(CustomOfficerLibrary.definition(t));
        }catch(org.json.JSONException|IllegalArgumentException e){throw new IOException("模板保存失败："+e.getMessage(),e);}
    }
    private void templates(boolean export){List<Editor.Template> list=new ArrayList<>();
        try{for(org.json.JSONObject o:new CustomOfficerLibrary(a).entries())list.add(CustomOfficerLibrary.template(o));}
        catch(IOException|org.json.JSONException e){info("模板读取失败",e.getMessage());return;}
        choose(export?"导出新武将模板":"选择已保存新武将",list,t->t.name,t->{if(export)a.exportOfficerTemplate(t);else choose("新武将登场据点",w.cities,c->c.name,c->{
            List<Integer> owners=new ArrayList<>();if(c.owner>=0)owners.add(c.owner);owners.add(-1);
            choose("新武将身份",owners,o->o<0?"在野武将":w.faction(o),o->preview(w.editor.createOfficer(t,c.id,o)));
        });});
    }
}
