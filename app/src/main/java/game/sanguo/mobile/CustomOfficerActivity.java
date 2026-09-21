package game.sanguo.mobile;

import android.app.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import org.json.*;
import java.util.*;

/** New-game content editor. It never receives or mutates the running campaign. */
public final class CustomOfficerActivity extends Activity {
    private CustomOfficerLibrary library;
    private LinearLayout root,rows;
    private JSONObject editing;
    private final Map<String,EditText> inputs=new LinkedHashMap<>();
    private final Map<String,Spinner> picks=new LinkedHashMap<>();
    private String query="";private int sort=0;
    @Override public void onCreate(Bundle state){super.onCreate(state);try{library=new CustomOfficerLibrary(this);showLibrary();}catch(Exception e){error(e);}}
    private void error(Exception e){new AlertDialog.Builder(this).setTitle("自定义武将").setMessage(e.getMessage()==null?e.toString():e.getMessage()).setPositiveButton("返回",null).show();}
    private LinearLayout column(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,12,16,12);return box;}
    private void screen(String title){root=column();root.setBackgroundColor(UiTheme.INK);TextView heading=label(title);heading.setTextSize(22);root.addView(heading);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);}
    private TextView label(String value){TextView text=new TextView(this);text.setText(value);text.setTextColor(UiTheme.TEXT);text.setPadding(4,10,4,10);return text;}
    private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->{try{action.run();}catch(Exception e){error(e);}});return b;}
    private void showLibrary(){
        editing=null;inputs.clear();picks.clear();screen("武将自定义 · 模板库");
        root.addView(label("只影响明确启用的新战局；不热修改当前存档。重命名保留人物ID。"));
        root.addView(button("新建武将",()->{try{edit(CustomOfficerLibrary.definition(new Editor.Template("新武将",new int[]{70,70,70,70,70},new int[]{1,1,1,1,1,1},World.Sex.MALE,"none",Debate.Temper.CALM,0)));}catch(Exception e){error(e);}}));
        String draft=getSharedPreferences("customOfficers-draft",MODE_PRIVATE).getString("draft","");
        if(!draft.isEmpty())root.addView(button("恢复未完成草稿",()->{try{edit(new JSONObject(draft));}catch(Exception e){error(e);}}));
        EditText search=new EditText(this);search.setHint("搜索姓名 / 特技 / ID");search.setTextColor(UiTheme.TEXT);search.setText(query);root.addView(search);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();refreshRows();}public void afterTextChanged(Editable e){}});
        Spinner order=new Spinner(this);order.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"姓名","统率高到低","武力高到低","智力高到低","政治高到低","魅力高到低"}));order.setSelection(sort);root.addView(order);
        order.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){sort=n;refreshRows();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        rows=column();root.addView(rows);root.addView(button("返回游戏",this::finish));refreshRows();
    }
    private void refreshRows(){if(rows==null)return;rows.removeAllViews();try{
        List<JSONObject> values=library.entries();values.sort((a,b)->sort==0?a.optString("name").compareTo(b.optString("name")):Integer.compare(b.optJSONArray("stats").optInt(sort-1),a.optJSONArray("stats").optInt(sort-1)));
        for(JSONObject entry:values){String name=entry.getString("name"),id=entry.getString("id");if(!(name+" "+Skill.label(entry.getString("skill"))+" "+id).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            rows.addView(button(name+" · "+(entry.optInt("targetId",-1)<0?"自定义":"历史覆盖")+" · r"+entry.getInt("revision")+"\n"+entry.getJSONArray("stats")+" · "+id.substring(0,8),()->actions(entry)));
        }
        if(rows.getChildCount()==0)rows.addView(label("暂无匹配武将"));
    }catch(Exception e){error(e);}}
    private void actions(JSONObject entry){new AlertDialog.Builder(this).setTitle(entry.optString("name")).setItems(new String[]{"编辑模板","复制为新人物（不复制人物关系）","删除模板"},(d,n)->{try{
        if(n==0)edit(entry);
        if(n==1){JSONObject copy=CustomOfficerLibrary.copy(entry);copy.remove("id");copy.remove("runtimeId");copy.remove("revision");copy.put("targetId",-1).put("relationships",new JSONArray());edit(copy);}
        if(n==2)delete(entry);
    }catch(Exception e){error(e);}}).setNegativeButton("取消",null).show();}
    private void delete(JSONObject entry)throws JSONException{
        String id=entry.getString("id");JSONObject next=library.snapshot();JSONArray entries=next.getJSONArray("entries"),plans=next.getJSONArray("plans");List<String> refs=new ArrayList<>();
        for(int i=0;i<entries.length();i++){JSONObject o=entries.getJSONObject(i);JSONArray links=o.optJSONArray("relationships");if(links!=null)for(int j=0;j<links.length();j++)if(id.equals(links.getJSONObject(j).optString("target")))refs.add(o.optString("name")+"的人物关系");}
        for(int i=0;i<plans.length();i++)if(plans.getJSONObject(i).toString().contains(id))refs.add("剧本投放方案");
        if(!refs.isEmpty()){new AlertDialog.Builder(this).setTitle("不能删除：存在引用").setMessage(String.join("\n",refs)+"\n请先修改关系或投放方案。").setPositiveButton("返回",null).show();return;}
        new AlertDialog.Builder(this).setTitle("删除模板？").setMessage("已开战局不受影响，头像资产不会被删除。").setPositiveButton("删除",(d,n)->{try{JSONArray keep=new JSONArray();for(int i=0;i<entries.length();i++)if(!id.equals(entries.getJSONObject(i).getString("id")))keep.put(entries.getJSONObject(i));next.put("entries",keep);library.replace(next);showLibrary();}catch(Exception e){error(e);}}).setNegativeButton("取消",null).show();
    }
    private void edit(JSONObject source)throws JSONException{
        editing=CustomOfficerLibrary.copy(source);inputs.clear();picks.clear();screen("编辑 · "+editing.optString("name"));root.addView(label("基础能力与成长、宝物、编队加成分离。五维0—100，适性C/B/A/S。"));
        field("name","姓名（1—24字）",editing.getString("name"),false);
        String[] labels={"统率","武力","智力","政治","魅力"};for(int i=0;i<5;i++)field("stat"+i,labels[i],editing.getJSONArray("stats").get(i).toString(),true);
        String[] arms={"枪兵","戟兵","弩兵","骑兵","器械","水军"};for(int i=0;i<6;i++)pick("apt"+i,arms[i]+"适性",new String[]{"C","B","A","S"},editing.getJSONArray("aptitudes").getInt(i));
        pick("sex","性别",new String[]{"男","女","未知"},World.Sex.valueOf(editing.getString("sex")).ordinal());
        root.addView(label("特技："+Skill.label(editing.getString("skill"))));
        root.addView(button("保存模板",()->{try{capture();JSONObject saved=library.save(editing);getSharedPreferences("customOfficers-draft",MODE_PRIVATE).edit().remove("draft").commit();Toast.makeText(this,"已保存 "+saved.getString("name")+" · r"+saved.getInt("revision"),Toast.LENGTH_LONG).show();showLibrary();}catch(Exception e){error(e);}}));
        root.addView(button("保存草稿并返回",()->{persistDraft();showLibrary();}));
    }
    private void field(String key,String title,String value,boolean numeric){root.addView(label(title));EditText e=new EditText(this);e.setTextColor(UiTheme.TEXT);e.setSingleLine(true);if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setText(value);inputs.put(key,e);root.addView(e);e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){persistDraft();}public void afterTextChanged(Editable e){}});}
    private void pick(String key,String title,String[] values,int at){root.addView(label(title));Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));s.setSelection(at);picks.put(key,s);root.addView(s);}
    private void capture()throws JSONException{
        editing.put("name",inputs.get("name").getText().toString().trim());JSONArray stats=new JSONArray(),apt=new JSONArray();for(int i=0;i<5;i++)stats.put(Integer.parseInt(inputs.get("stat"+i).getText().toString()));for(int i=0;i<6;i++)apt.put(picks.get("apt"+i).getSelectedItemPosition());editing.put("stats",stats).put("aptitudes",apt).put("sex",World.Sex.values()[picks.get("sex").getSelectedItemPosition()].name());CustomOfficerLibrary.template(editing);
    }
    private void persistDraft(){if(editing==null||inputs.size()<6||picks.size()<7)return;try{capture();getSharedPreferences("customOfficers-draft",MODE_PRIVATE).edit().putString("draft",editing.toString()).commit();}catch(Exception ignored){/* Invalid partial numeric input remains on screen; the last valid draft is retained. */}}
    @Override protected void onPause(){persistDraft();super.onPause();}
    @Override public void onBackPressed(){if(editing!=null){persistDraft();showLibrary();}else super.onBackPressed();}
}
