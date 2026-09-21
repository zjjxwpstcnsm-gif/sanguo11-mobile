package game.sanguo.mobile;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.function.*;

/** Reusable, tabbed NEW-GAME officer content editor; never holds the active campaign. */
public final class CustomOfficerActivity extends Activity {
    private static final int IMAGE=711,IMPORT=712,EXPORT=713;
    private CustomOfficerLibrary library;
    private LinearLayout root,content,rows;
    private JSONObject editing,raw=new JSONObject();
    private final Map<String,EditText> inputs=new LinkedHashMap<>();
    private int page,sort,sourceFilter;private String query="";
    private byte[] exportBytes;private boolean binding;
    @Override public void onCreate(Bundle state){super.onCreate(state);try{library=new CustomOfficerLibrary(this);if(state!=null&&state.getString("editing")!=null){editing=new JSONObject(state.getString("editing"));raw=new JSONObject(state.getString("raw","{}"));page=state.getInt("page");renderEditor();}else showLibrary();}catch(Exception e){error(e);}}
    private int dp(int n){return UiTheme.dp(this,n);}
    private void error(Exception e){if(isFinishing())return;new AlertDialog.Builder(this).setTitle("自定义武将").setMessage(e.getMessage()==null?e.toString():e.getMessage()).setPositiveButton("返回",null).show();}
    private LinearLayout column(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(8),dp(12),dp(8));return box;}
    private TextView label(String value){TextView t=new TextView(this);t.setText(value);t.setTextColor(UiTheme.TEXT);t.setTextSize(14);t.setPadding(0,dp(6),0,dp(6));return t;}
    private Button button(String text,Runnable action){Button b=CompactButtons.create(this);b.setText(text);b.setAllCaps(false);b.setMinHeight(dp(46));b.setOnClickListener(v->{try{action.run();}catch(Exception e){error(e);}});return b;}
    private void screen(String title){root=column();root.setBackgroundColor(UiTheme.INK);TextView heading=label(title);heading.setTextSize(22);root.addView(heading);setContentView(root);root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(12)+insets.getSystemWindowInsetLeft(),dp(8)+insets.getSystemWindowInsetTop(),dp(12)+insets.getSystemWindowInsetRight(),dp(8)+insets.getSystemWindowInsetBottom());return insets;});root.requestApplyInsets();}
    private void body(){ScrollView scroll=new ScrollView(this);content=column();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
    private void row(LinearLayout box,Button... buttons){LinearLayout r=new LinearLayout(this);for(Button b:buttons)r.addView(b,new LinearLayout.LayoutParams(0,-2,1));box.addView(r);}
    private void reload()throws IOException{library=new CustomOfficerLibrary(this);}
    private void showLibrary(){try{
        reload();editing=null;raw=new JSONObject();inputs.clear();screen("武将自定义 · 模板库");root.addView(label("只影响明确启用的新战局 · 不热修改当前存档"));
        row(root,button("新建武将",()->{try{edit(CustomOfficerLibrary.definition(new Editor.Template("新武将",new int[]{70,70,70,70,70},new int[]{1,1,1,1,1,1},World.Sex.MALE,"none",Debate.Temper.CALM,0)));}catch(Exception e){error(e);}}),button("历史复制 / 覆盖",this::historical));
        String draft=getSharedPreferences("customOfficers-draft",MODE_PRIVATE).getString("draft","");if(!draft.isEmpty())root.addView(button("恢复未完成草稿",()->{try{JSONObject saved=new JSONObject(draft);editing=saved.has("definition")?saved.getJSONObject("definition"):saved;raw=saved.optJSONObject("raw")==null?new JSONObject():saved.getJSONObject("raw");page=saved.optInt("page",0);renderEditor();}catch(Exception e){error(e);}}));
        EditText search=new EditText(this);search.setHint("搜索姓名 / 特技 / 稳定ID");search.setTextColor(UiTheme.TEXT);search.setHintTextColor(UiTheme.MUTED);search.setSingleLine(true);search.setText(query);root.addView(search);search.addTextChangedListener(watch(()->{query=search.getText().toString();refreshRows();}));
        LinearLayout filters=new LinearLayout(this);Spinner order=new Spinner(this),source=new Spinner(this);order.setAdapter(adapter(new String[]{"按姓名","统率↓","武力↓","智力↓","政治↓","魅力↓"}));order.setSelection(sort);source.setAdapter(adapter(new String[]{"全部来源","自定义人物","历史覆盖"}));source.setSelection(sourceFilter);filters.addView(order,new LinearLayout.LayoutParams(0,dp(44),1));filters.addView(source,new LinearLayout.LayoutParams(0,dp(44),1));root.addView(filters);
        body();rows=content;order.setOnItemSelectedListener(selected(n->{sort=n;refreshRows();}));source.setOnItemSelectedListener(selected(n->{sourceFilter=n;refreshRows();}));
        row(root,button("导入数据包",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT)),button("导出 / 分享",this::exportPack),button("剧本投放",this::chooseScenario));root.addView(button("返回游戏",this::finish));refreshRows();
    }catch(Exception e){error(e);}}
    private void refreshRows(){if(rows==null||editing!=null)return;rows.removeAllViews();try{
        List<JSONObject> entries=library.entries();entries.sort((a,b)->sort==0?a.optString("name").compareTo(b.optString("name")):Integer.compare(b.optJSONArray("stats").optInt(sort-1),a.optJSONArray("stats").optInt(sort-1)));
        for(JSONObject o:entries){if(sourceFilter==1&&o.optInt("targetId",-1)>=0||sourceFilter==2&&o.optInt("targetId",-1)<0)continue;String id=o.getString("id"),text=o.getString("name")+" · "+Skill.label(o.getString("skill"))+" · "+id;if(!text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);ImageView image=new ImageView(this);image.setImageDrawable(preview(this,o));card.addView(image,new LinearLayout.LayoutParams(dp(48),dp(54)));Button b=button(o.getString("name")+" · "+(o.optInt("targetId",-1)<0?"自定义":"历史覆盖")+" · r"+o.getInt("revision")+"\n"+o.getJSONArray("stats")+" · "+id.substring(0,8),()->actions(o));card.addView(b,new LinearLayout.LayoutParams(0,-2,1));rows.addView(card);}
        if(rows.getChildCount()==0)rows.addView(label("暂无匹配武将。模板与投放方案均保存在应用私有目录。"));
    }catch(Exception e){error(e);}}
    private static android.graphics.drawable.Drawable preview(Context c,JSONObject o){try{Editor.Template t=CustomOfficerLibrary.template(o);World.Officer officer=new World.Officer(o.optInt("runtimeId",100000),t.name,-1,-1,t.stat(0),t.stat(1),t.stat(2),t.stat(3),t.stat(4));officer.sex=t.sex;return CustomOfficerImages.preview(c,o.optString("portrait",""),officer);}catch(Exception e){return null;}}
    private void historical(){try{search(this,"历史武将 · 复制或独立覆盖",ContentCatalog.get().officers(),o->o.name+" · #"+o.id+" · "+ContentCatalogName.skill(o.skillId),o->new AlertDialog.Builder(this).setTitle(o.name).setMessage("复制创建新ID，不继承事件身份、职务、忠诚变化或关系。历史覆盖保留稳定ID，只影响启用的新局；默认保留原关系，新增关系若冲突会阻止开局。").setPositiveButton("复制为新人物",(d,n)->fromHistory(o,false)).setNeutralButton("创建历史覆盖",(d,n)->fromHistory(o,true)).setNegativeButton("取消",null).show());}catch(Exception e){error(e);}}
    private static final class ContentCatalogName{static String skill(String id){try{return ContentCatalog.get().skillName(id);}catch(IOException e){return id;}}}
    private void fromHistory(ContentCatalog.Officer officer,boolean override){try{JSONObject o=CustomOfficerSetup.json(CustomOfficers.historical(officer,override));o.remove("id");o.remove("runtimeId");o.remove("revision");int portrait=PortraitCatalog.index(officer.name);if(portrait>=0)o.put("portrait","builtin:"+portrait);edit(o);}catch(Exception e){error(e);}}
    private void actions(JSONObject o){new AlertDialog.Builder(this).setTitle(o.optString("name")).setItems(new String[]{"编辑模板","复制（不继承人物关系）","删除模板 / 处理引用"},(d,n)->{try{if(n==0)edit(o);if(n==1){JSONObject copy=CustomOfficerLibrary.copy(o);for(String key:new String[]{"id","runtimeId","revision","origin"})copy.remove(key);copy.put("targetId",-1).put("baseName","").put("replaceRelations",false).put("relationships",new JSONArray());edit(copy);}if(n==2)delete(o);}catch(Exception e){error(e);}}).setNegativeButton("取消",null).show();}
    private void delete(JSONObject entry)throws JSONException{
        String id=entry.getString("id");JSONObject next=library.snapshot();JSONArray entries=next.getJSONArray("entries"),plans=next.getJSONArray("plans");List<String> refs=new ArrayList<>();
        for(int i=0;i<entries.length();i++){JSONObject o=entries.getJSONObject(i);JSONArray links=o.getJSONArray("relationships");for(int j=0;j<links.length();j++)if(id.equals(links.getJSONObject(j).optString("target")))refs.add(o.getString("name")+" → "+Relations.Kind.valueOf(links.getJSONObject(j).getString("kind")).label);}
        for(int i=0;i<plans.length();i++){JSONArray ps=plans.getJSONObject(i).getJSONArray("placements");for(int j=0;j<ps.length();j++)if(id.equals(ps.getJSONObject(j).getString("definition")))refs.add("剧本 "+plans.getJSONObject(i).getString("scenario"));}
        new AlertDialog.Builder(this).setTitle(refs.isEmpty()?"删除模板？":"删除并清理以下引用？").setMessage((refs.isEmpty()?"没有依赖。":String.join("\n",refs))+"\n已开战局不受影响。共享头像保留，不进行自动垃圾回收。").setPositiveButton(refs.isEmpty()?"删除":"明确清理引用并删除",(d,n)->{try{
            JSONArray keep=new JSONArray();for(int i=0;i<entries.length();i++){JSONObject o=entries.getJSONObject(i);if(id.equals(o.getString("id")))continue;JSONArray links=o.getJSONArray("relationships");for(int j=links.length()-1;j>=0;j--)if(id.equals(links.getJSONObject(j).getString("target"))){links.remove(j);o.put("revision",o.getInt("revision")+1);}keep.put(o);}next.put("entries",keep);
            for(int i=0;i<plans.length();i++){JSONArray ps=plans.getJSONObject(i).getJSONArray("placements");for(int j=ps.length()-1;j>=0;j--)if(id.equals(ps.getJSONObject(j).getString("definition")))ps.remove(j);}library.replace(next);showLibrary();
        }catch(Exception e){error(e);}}).setNegativeButton("取消",null).show();
    }
    private void edit(JSONObject source)throws JSONException{editing=CustomOfficerLibrary.copy(source);raw=new JSONObject();page=0;renderEditor();}
    private void renderEditor(){binding=true;inputs.clear();try{
        screen("编辑 · "+editing.optString("name"));HorizontalScrollView tabs=new HorizontalScrollView(this);LinearLayout buttons=new LinearLayout(this);String[] titles={"基本资料","能力适性","特技性格","人物关系","剧本投放"};for(int i=0;i<titles.length;i++){final int index=i;Button b=button(titles[i],()->{try{capture();page=index;persistDraft();renderEditor();}catch(Exception e){error(e);}});b.setSelected(page==i);buttons.addView(b);}tabs.addView(buttons);root.addView(tabs);body();
        switch(page){case 0:basic();break;case 1:abilities();break;case 2:skills();break;case 3:relationships();break;case 4:content.addView(label("模板不保存所属势力或驻地。保存后到剧本投放配置。\n加入已有势力 / 在野 / 未登场；历史覆盖保留原始身份。\n未启用的人物不会自动进入游戏，当前战局不会热修改。"));content.addView(button("保存并配置剧本投放",()->save(true)));break;default:page=0;basic();}
        row(root,button("保存模板",()->save(false)),button("草稿返回",()->{persistDraft();showLibrary();}));
    }catch(Exception e){error(e);}finally{binding=false;}}
    private void basic()throws JSONException{
        content.addView(label(editing.has("id")?"稳定ID "+editing.getString("id")+" · 修订 "+editing.optInt("revision"):"新人物：首次保存分配稳定ID；重命名不会改变ID。"));
        field("name","姓名（1—24字）",editing.getString("name"),false);Spinner sex=spinner("性别",Arrays.stream(World.Sex.values()).map(s->s==World.Sex.MALE?"男":s==World.Sex.FEMALE?"女":"未知").toArray(String[]::new),World.Sex.valueOf(editing.getString("sex")).ordinal(),n->put("sex",World.Sex.values()[n].name()));
        ImageView image=new ImageView(this);image.setImageDrawable(preview(this,editing));content.addView(image,new LinearLayout.LayoutParams(dp(100),dp(100)));
        row(content,button("内置头像",()->{List<Integer> values=new ArrayList<>();for(int i=0;i<PortraitCatalog.NAMES.length;i++)values.add(i);search(this,"选择内置头像",values,n->PortraitCatalog.NAMES[n]+" · #"+n,n->{put("portrait","builtin:"+n);renderEditor();});}),button("手机图片",()->{try{capture();persistDraft();startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),IMAGE);}catch(Exception e){error(e);}}),button("默认头像",()->{put("portrait","");renderEditor();}));
        if(editing.optString("portrait").endsWith(".png"))try{CustomOfficerImages.read(this,editing.optString("portrait"));}catch(IOException e){content.addView(label("头像缺失/损坏：正在显示默认头像，请重新选择；不会导致闪退。"));}
        field("birth","出生年（0=未知）",Integer.toString(editing.optInt("birth",0)),true);field("appearance","登场年（0=未知）",Integer.toString(editing.optInt("appearance",0)),true);field("death","预计没年（0=未知）",Integer.toString(editing.optInt("death",0)),true);
        content.addView(label("未登场人物按现有年初结算登场。预计没年参与已开启的自然死亡规则；超过剧本年代须显式处理。"));
    }
    private void abilities()throws JSONException{
        content.addView(label("模板只保存基础能力。成长/培养、宝物、伤势与关系编队加成由战局系统计算，不写回模板。"));String[] names={"统率","武力","智力","政治","魅力"};for(int i=0;i<5;i++){final String key="stat"+i;EditText number=field(key,names[i]+"（0—100）",Integer.toString(editing.getJSONArray("stats").getInt(i)),true);row(content,button("−1",()->increment(number,-1)),button("+1",()->increment(number,1)),button("+10",()->increment(number,10)));}
        String[] names2={"枪兵","戟兵","弩兵","骑兵","器械","水军"};for(int i=0;i<6;i++){final int at=i;spinner(names2[i]+"适性",new String[]{"C","B","A","S"},editing.getJSONArray("aptitudes").getInt(i),n->{try{editing.getJSONArray("aptitudes").put(at,n);persistDraft();}catch(JSONException e){error(e);}});}
        content.addView(label("正式部队攻防预览在投放页：按实际地形、兵种和现有计算入口生成，不另存手填战斗力。"));
    }
    private void skills()throws JSONException{
        String skill=editing.getString("skill");content.addView(label("当前特技："+Skill.label(skill)+"\n"+Skill.description(skill)+"\n"+SkillSupport.status(skill)));content.addView(button("搜索选择特技（单特技）",()->{List<String> ids=new ArrayList<>();ids.add("none");for(Skill s:Skill.values())ids.add(s.id);search(this,"特技 · 当前执行状态与条件",ids,id->Skill.label(id)+" · "+id+"\n"+Skill.description(id)+"\n"+SkillSupport.status(id),id->{if(!SkillSupport.enabled(id)){error(new IllegalArgumentException("此条目未确认实现，不能作为可用特技"));return;}put("skill",id);renderEditor();});}));
        spinner("性格（影响当前舌战模型）",Arrays.stream(Debate.Temper.values()).map(t->t.label).toArray(String[]::new),Debate.Temper.valueOf(editing.getString("temper")).ordinal(),n->put("temper",Debate.Temper.values()[n].name()));
        for(int i=0;i<5;i++){final int bit=1<<i;CheckBox check=new CheckBox(this);check.setTextColor(UiTheme.TEXT);check.setText("话术 · "+Debate.Talk.values()[i].label);check.setChecked((editing.optInt("talkMask")&bit)!=0);check.setOnCheckedChangeListener((b,on)->put("talkMask",on?editing.optInt("talkMask")|bit:editing.optInt("talkMask")&~bit));content.addView(check);}
        field("affinity","相性（0—149；用于现有登用/忠诚判断）",Integer.toString(editing.optInt("affinity",75)),true);field("honor","义理（1—5；用于现有忠诚模型）",Integer.toString(editing.optInt("honor",3)),true);
    }
    private void relationships()throws JSONException{
        content.addView(label("父母使用子→父母ID；配偶/结义由引擎维护对称与结义组闭包。亲爱/厌恶为单向。历史覆盖默认保留原关系；勾选显式覆盖后可删除/替换。已有关系冲突会明确阻止开局。"));
        if(editing.optInt("targetId",-1)>=0){CheckBox replace=new CheckBox(this);replace.setTextColor(UiTheme.TEXT);replace.setText("显式覆盖该历史人物原有直接关系");replace.setChecked(editing.optBoolean("replaceRelations",false));replace.setOnCheckedChangeListener((v,on)->{if(on)new AlertDialog.Builder(this).setTitle("载入历史关系以编辑？").setMessage("此操作会用编辑后的关系列表替换启用新局中的原关系，并通过原引擎更新对称关系。不改动历史资料或旧战局。缺失剧本人物将阻止投放，须明确处理。").setPositiveButton("载入并覆盖",(dlg,n)->{try{JSONArray baseline=new JSONArray();for(ContentCatalog.Relation r:ContentCatalog.get().relations())if(r.officer==editing.getInt("targetId"))baseline.put(new JSONObject().put("kind",r.kind.name()).put("target","h:"+r.target));editing.put("relationships",baseline);put("replaceRelations",true);renderEditor();}catch(Exception e){error(e);}}).setNegativeButton("取消",(dlg,n)->renderEditor()).show();else{put("replaceRelations",false);renderEditor();}});content.addView(replace);}
        JSONArray links=editing.getJSONArray("relationships");for(int i=0;i<links.length();i++){final int at=i;JSONObject l=links.getJSONObject(i);String name=relationName(l.getString("target"));content.addView(button(Relations.Kind.valueOf(l.getString("kind")).label+" → "+name+" · 点击移除",()->{editing.optJSONArray("relationships").remove(at);persistDraft();renderEditor();}));}
        Spinner kind=new Spinner(this);kind.setAdapter(adapter(Arrays.stream(Relations.Kind.values()).map(k->k.label).toArray(String[]::new)));content.addView(kind);
        content.addView(button("搜索添加关系对象",()->{try{
            capture();List<RelationChoice> targets=new ArrayList<>();for(ContentCatalog.Officer h:ContentCatalog.get().officers())if(h.id!=editing.optInt("targetId",-1))targets.add(new RelationChoice("h:"+h.id,h.name+" · 历史 #"+h.id,h));for(JSONObject o:library.entries())if(!o.getString("id").equals(editing.optString("id")))targets.add(new RelationChoice(o.getString("id"),o.getString("name")+" · 自定义 "+o.getString("id").substring(0,8),o));
            search(this,"关系对象 · 稳定ID",targets,t->t.name,t->{try{links.put(new JSONObject().put("kind",Relations.Kind.values()[kind.getSelectedItemPosition()].name()).put("target",t.id));persistDraft();renderEditor();}catch(Exception e){error(e);}});
        }catch(Exception e){error(e);}}));
    }
    private static final class RelationChoice{final String id,name;final Object portrait;RelationChoice(String id,String name,Object portrait){this.id=id;this.name=name;this.portrait=portrait;}}
    private String relationName(String id)throws IOException,JSONException{if(id.startsWith("h:")){ContentCatalog.Officer h=ContentCatalog.get().officer(Integer.parseInt(id.substring(2)));return (h==null?"缺失历史人物":h.name)+" #"+id.substring(2);}JSONObject o=library.find(id);return (o==null?"缺失人物":o.getString("name"))+" · "+id.substring(0,8);}
    private EditText field(String key,String title,String value,boolean number){content.addView(label(title));EditText e=new EditText(this);e.setTextColor(UiTheme.TEXT);e.setSingleLine(true);e.setContentDescription(title);e.setTag("officer."+key);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setText(raw.optString(key,value));inputs.put(key,e);content.addView(e);e.addTextChangedListener(watch(()->{try{raw.put(key,e.getText().toString());persistDraft();}catch(JSONException x){error(x);}}));return e;}
    private void increment(EditText e,int delta){try{e.setText(Integer.toString(Math.max(0,Math.min(100,Integer.parseInt(e.getText().toString())+delta))));}catch(NumberFormatException ex){error(new IllegalArgumentException("请先输入有效能力值"));}}
    private Spinner spinner(String title,String[] values,int index,IntConsumer change){content.addView(label(title));Spinner s=new Spinner(this);s.setContentDescription(title);s.setAdapter(adapter(values));s.setSelection(index);content.addView(s);s.setOnItemSelectedListener(selected(n->{if(!binding)change.accept(n);}));return s;}
    private ArrayAdapter<String> adapter(String[] labels){return new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);}
    private static AdapterView.OnItemSelectedListener selected(IntConsumer action){return new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int n,long id){action.accept(n);}public void onNothingSelected(AdapterView<?> p){}};}
    private static TextWatcher watch(Runnable action){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){action.run();}public void afterTextChanged(Editable e){}};}
    private void put(String key,Object value){try{editing.put(key,value);persistDraft();}catch(JSONException e){error(e);}}
    private void capture()throws JSONException{
        if(editing==null)return;for(Map.Entry<String,EditText> e:inputs.entrySet())raw.put(e.getKey(),e.getValue().getText().toString());
        Iterator<String> keys=raw.keys();while(keys.hasNext()){String key=keys.next(),value=raw.getString(key);if(key.equals("name"))editing.put(key,value.trim());else{int n;try{n=Integer.parseInt(value);}catch(NumberFormatException e){throw new IllegalArgumentException("请输入有效整数："+key);}if(key.startsWith("stat"))editing.getJSONArray("stats").put(Integer.parseInt(key.substring(4)),n);else editing.put(key,n);}}
        CustomOfficerLibrary.template(editing);
    }
    private void persistDraft(){if(binding||editing==null)return;try{JSONObject draft=new JSONObject().put("definition",editing).put("raw",raw).put("page",page);getSharedPreferences("customOfficers-draft",MODE_PRIVATE).edit().putString("draft",draft.toString()).commit();}catch(JSONException e){error(e);}}
    private void save(boolean placement){try{capture();if(!SkillSupport.enabled(editing.getString("skill")))throw new IllegalArgumentException("请选择已接入效果的特技");JSONObject saved=library.save(editing);editing=null;raw=new JSONObject();getSharedPreferences("customOfficers-draft",MODE_PRIVATE).edit().remove("draft").commit();Toast.makeText(this,"已保存 "+saved.getString("name")+" · r"+saved.getInt("revision"),Toast.LENGTH_LONG).show();showLibrary();if(placement)chooseScenario();}catch(Exception e){error(e);}}
    private void chooseScenario(){try{search(this,"配置剧本投放",ScenarioCatalog.summaries(),s->s.name+" · "+s.id,s->{new Thread(()->{try{World w=ScenarioCatalog.load(s.id,0);runOnUiThread(()->{if(!isFinishing())new CustomOfficerPlacementUi(this,w).show();});}catch(Exception e){runOnUiThread(()->error(e));}},"officer-scenario").start();});}catch(Exception e){error(e);}}
    private void exportPack(){new Thread(()->{try{byte[] bytes=CustomOfficerPack.exportPack(this,new CustomOfficerLibrary(this).snapshot());runOnUiThread(()->{exportBytes=bytes;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip").putExtra(Intent.EXTRA_TITLE,"sanguo11-officers.zip"),EXPORT);});}catch(Exception e){runOnUiThread(()->error(e));}},"officer-pack-export").start();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(request==IMAGE){if(editing==null){error(new IOException("图片返回时草稿不存在，请恢复草稿后重新导入"));return;}CustomOfficerImages.importCrop(this,uri,name->{put("portrait",name);renderEditor();},this::error);}
        if(request==IMPORT)new Thread(()->{try{reload();CustomOfficerPack.Preview preview;try(InputStream in=getContentResolver().openInputStream(uri)){preview=CustomOfficerPack.preview(this,in,library);}runOnUiThread(()->new AlertDialog.Builder(this).setTitle("导入预览").setMessage(preview.summary).setPositiveButton("跳过同ID",(d,n)->importPack(preview,CustomOfficerPack.Conflict.SKIP)).setNeutralButton("更新同源",(d,n)->confirmImport(preview,CustomOfficerPack.Conflict.UPDATE_SAME_SOURCE)).setNegativeButton("更多 / 取消",(d,n)->new AlertDialog.Builder(this).setMessage("作为新人物导入会重映射包内关系与投放引用；同一包重复执行不重复创建。历史覆盖将变成独立新人物。").setPositiveButton("作为新人物",(x,y)->importPack(preview,CustomOfficerPack.Conflict.COPY)).setNegativeButton("取消",null).show()).show());}catch(Exception e){runOnUiThread(()->error(e));}},"officer-pack-preview").start();
        if(request==EXPORT){final byte[] bytes=exportBytes;new Thread(()->{try{byte[] actual=bytes==null?CustomOfficerPack.exportPack(this,new CustomOfficerLibrary(this).snapshot()):bytes;try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("无法写入目标文件");out.write(actual);}runOnUiThread(()->new AlertDialog.Builder(this).setTitle("数据包已保存").setMessage("人物、关系、投放方案和实际头像已写入所选文件。").setPositiveButton("分享",(d,n)->startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setClipData(ClipData.newRawUri("武将包",uri)),"分享武将数据包"))).setNegativeButton("返回",null).show());}catch(Exception e){runOnUiThread(()->error(e));}},"officer-pack-save").start();}
    }
    private void confirmImport(CustomOfficerPack.Preview pack,CustomOfficerPack.Conflict policy){new AlertDialog.Builder(this).setTitle("确认更新同源模板？").setMessage("同一人物ID保持不变并产生新修订；异源同ID冲突将阻止导入。既有战局不变。相同库的投放方案会被导入版替换并关闭。").setPositiveButton("确认更新",(d,n)->importPack(pack,policy)).setNegativeButton("取消",null).show();}
    private void importPack(CustomOfficerPack.Preview pack,CustomOfficerPack.Conflict policy){new Thread(()->{try{CustomOfficerLibrary latest=new CustomOfficerLibrary(this);int changed=CustomOfficerPack.apply(this,latest,pack,policy);runOnUiThread(()->{Toast.makeText(this,"已导入/更新 "+changed+"人；投放方案请重新预览启用",Toast.LENGTH_LONG).show();showLibrary();});}catch(Exception e){runOnUiThread(()->error(e));}},"officer-pack-import").start();}
    @Override protected void onSaveInstanceState(Bundle out){if(editing!=null){out.putString("editing",editing.toString());out.putString("raw",raw.toString());out.putInt("page",page);}super.onSaveInstanceState(out);}
    @Override protected void onPause(){persistDraft();super.onPause();}
    @Override public void onBackPressed(){if(editing!=null){persistDraft();showLibrary();}else super.onBackPressed();}
    private static android.graphics.drawable.Drawable choicePortrait(Activity a,Object value){
        if(value instanceof RelationChoice)return choicePortrait(a,((RelationChoice)value).portrait);
        if(value instanceof JSONObject)return preview(a,(JSONObject)value);
        World.Officer officer;String ref="";
        if(value instanceof ContentCatalog.Officer){ContentCatalog.Officer h=(ContentCatalog.Officer)value;officer=new World.Officer(h.id,h.name,-1,-1,h.stat(0),h.stat(1),h.stat(2),h.stat(3),h.stat(4));officer.sex="男".equals(h.gender)?World.Sex.MALE:World.Sex.FEMALE;}
        else if(value instanceof Integer){int index=(Integer)value;if(index<0||index>=PortraitCatalog.NAMES.length)return null;officer=new World.Officer(index,PortraitCatalog.NAMES[index],-1,-1,70,70,70,70,70);ref="builtin:"+index;}
        else return null;
        return CustomOfficerImages.preview(a,ref,officer);
    }
    static <T> void search(Activity a,String title,List<T> values,Function<T,String> label,Consumer<T> chosen){
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);EditText query=new EditText(a);query.setSingleLine(true);query.setHint("输入姓名、ID、特技或关键词");query.setContentDescription("搜索列表");box.addView(query);ListView list=new ListView(a);box.addView(list,new LinearLayout.LayoutParams(-1,Math.max(220,a.getResources().getDisplayMetrics().heightPixels/2)));List<T> filtered=new ArrayList<>(values);List<String> labels=new ArrayList<>();for(T value:values)labels.add(label.apply(value));
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,labels){
            @Override public View getView(int position,View old,ViewGroup parent){
                android.graphics.drawable.Drawable portrait=choicePortrait(a,filtered.get(position));
                if(portrait==null)return super.getView(position,old instanceof TextView?old:null,parent);
                LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(12,8,12,8);ImageView image=new ImageView(a);image.setImageDrawable(portrait);row.addView(image,new LinearLayout.LayoutParams(UiTheme.dp(a,44),UiTheme.dp(a,48)));TextView text=new TextView(a);text.setText(getItem(position));text.setTextSize(15);text.setPadding(12,0,4,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));return row;
            }
        };list.setAdapter(adapter);AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(box).setNegativeButton("取消",null).create();query.addTextChangedListener(watch(()->{String text=query.getText().toString().toLowerCase(Locale.ROOT);filtered.clear();adapter.clear();for(T v:values){String name=label.apply(v);if(name.toLowerCase(Locale.ROOT).contains(text)){filtered.add(v);adapter.add(name);}}adapter.notifyDataSetChanged();}));list.setOnItemClickListener((p,v,n,id)->{T selected=filtered.get(n);dialog.dismiss();chosen.accept(selected);});dialog.show();
    }
}
