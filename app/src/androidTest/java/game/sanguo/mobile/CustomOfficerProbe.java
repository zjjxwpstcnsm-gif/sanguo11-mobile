package game.sanguo.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

/** Installed UI, document-handler, storage, real command and subsequent process-restart checks. */
final class CustomOfficerProbe {
    private final Instrumentation test;private int checks;private final StringBuilder report=new StringBuilder();
    CustomOfficerProbe(Instrumentation test){this.test=test;}
    private android.content.Context context(){return test.getTargetContext();}
    void run(String mode)throws Exception{
        try{runChecks(mode);}catch(Exception|AssertionError e){
            try{shot("failure-"+mode);try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(directory(),"failure.txt")),"UTF-8")){out.write(report.toString()+"\n"+e);}}catch(Exception ignored){}
            throw e;
        }
    }
    private void runChecks(String mode)throws Exception{
        if(mode.equals("restart")){restart();write("restart");return;}
        MainActivity main=(MainActivity)test.startActivitySync(new Intent(context(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();click("武将自定义 · 模板与投放",true);
        CustomOfficerActivity editor=(CustomOfficerActivity)current();click("新建武将",true);editText(editor,"name","手机自定义甲");shot("01-create-name");
        // Real provider-backed URI reaches the production image result handler, then the actual crop controls.
        ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"officer-fixture.png");values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");Uri uri=context().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);check(uri!=null,"provider image URI created");
        Bitmap photo=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(photo);canvas.drawColor(Color.LTGRAY);Paint ink=new Paint();ink.setColor(Color.DKGRAY);canvas.drawCircle(200,230,110,ink);try(OutputStream out=context().getContentResolver().openOutputStream(uri)){photo.compress(Bitmap.CompressFormat.PNG,100,out);}photo.recycle();
        ui(()->call(editor,"onActivityResult",new Class<?>[]{int.class,int.class,Intent.class},711,Activity.RESULT_OK,new Intent().setData(uri)));waitText("预览并裁剪头像");shot("02-crop");click("使用头像",true);settle();
        click("能力适性",true);editText(editor,"stat0","95");editText(editor,"stat3","97");ui(()->{Spinner s=(Spinner)findDescription(editor.getWindow().getDecorView(),"枪兵适性");check(s!=null,"aptitude is a real selector");s.setSelection(3);});
        click("特技性格",true);click("搜索选择特技（单特技）",true);searchText("百出");click("百出 · san11.baichu",false);shot("03-skill-status");click("保存模板",true);
        CustomOfficerLibrary library=new CustomOfficerLibrary(context());JSONObject first=library.entries().get(0);check(first.getString("name").equals("手机自定义甲")&&first.getJSONArray("stats").getInt(0)==95&&first.getString("skill").equals(Skill.BAICHU.id),"UI saved name/stats and selected implemented skill");check(first.getString("portrait").endsWith(".png")&&CustomOfficerImages.read(context(),first.getString("portrait")).length>0,"processed portrait is persistent PNG, not temporary URI");
        click("新建武将",true);editText(editor,"name","手机自定义乙");ui(()->((Spinner)findDescription(editor.getWindow().getDecorView(),"性别")).setSelection(World.Sex.FEMALE.ordinal()));click("保存模板",true);JSONObject second=new CustomOfficerLibrary(context()).entries().get(1);
        click("手机自定义甲 · 自定义",false);click("编辑模板",true);click("人物关系",true);
        ui(()->{LinearLayout body=(LinearLayout)field(editor,"content");for(int i=0;i<body.getChildCount();i++)if(body.getChildAt(i) instanceof Spinner)((Spinner)body.getChildAt(i)).setSelection(Relations.Kind.SPOUSE.ordinal());});click("搜索添加关系对象",true);searchText("手机自定义乙");click("手机自定义乙 · 自定义",false);shot("04-relationship-id-selector");click("保存模板",true);
        first=new CustomOfficerLibrary(context()).find(first.getString("id"));check(first.getInt("revision")==2&&first.getJSONArray("relationships").getJSONObject(0).getString("target").equals(second.getString("id")),"relationship uses stable UUID and edit increments revision");
        World base=ScenarioCatalog.load("guandu-200",0,42L);World.City home=base.cities.stream().filter(c->c.owner==0).findFirst().orElseThrow();
        ui(()->new CustomOfficerPlacementUi(editor,base).show());settle();toggle("本剧本启用自定义武将（默认关闭）");addPlacement(home,"手机自定义甲");addPlacement(home,"手机自定义乙");click("校验并预览实际战局",true);waitText("正式初始化预览");shot("05-placement-preview");click("返回",true);click("保存配置",true);
        ui(()->editor.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();shot("06-library-landscape");CustomOfficerActivity rotated=(CustomOfficerActivity)current();ui(rotated::finish);settle();main=(MainActivity)current();final MainActivity game=main;
        ui(()->call(game,"startScenario",new Class<?>[]{String.class,int.class},"guandu-200",0));waitWorld(game,first.getInt("runtimeId"));World actual=(World)field(game,"world");int firstId=first.getInt("runtimeId"),secondId=second.getInt("runtimeId");
        check(actual.officer(firstId)!=null&&actual.relations.spouse(firstId)==secondId&&actual.officer(firstId).aptitude[0]==3,"actual MainActivity new-game hook composes persons and relations");ui(()->game.openRealmPage("officers",-1));settle();shot("07-in-game-roster");
        // Exercise actual installed commands in a compact test world after verifying the production scenario startup above.
        World fixture=Ux64Fixture.create();fixture.scenarioId="guandu-200";fixture.startYear=200;JSONObject testRoot=new CustomOfficerLibrary(context()).snapshot();JSONObject plan=CustomOfficerSetup.plan(testRoot,"guandu-200");for(int i=0;i<plan.getJSONArray("placements").length();i++)plan.getJSONArray("placements").getJSONObject(i).put("city",10);CustomOfficerSetup.putPlan(testRoot,plan);
        World commands=CustomOfficerSetup.apply(context(),fixture,testRoot);commands.city(10).order=50;ui(()->{call(game,"activateWorld",new Class<?>[]{World.class},commands);World.Result patrol=commands.patrol(10,firstId);check(patrol.ok,"installed custom officer completes domestic patrol");game.applyResult(patrol);});
        World.Result turn=commands.nextTurn();check(turn.ok,"real turn ends to restore action allowance");World.Result deploy=commands.army.deploy(10,firstId,new int[]{secondId},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000);check(deploy.ok,"installed real sortie with custom commander and deputy: "+deploy.message);World.Unit unit=commands.units.stream().filter(u->u.officerId==firstId).findFirst().orElseThrow();check(commands.war.plotCost(unit.id,War.Plot.CONFUSE)==1,"installed custom skill affects official cost");
        // Legal battlefield fixture; attack remains the normal public command, not a test formula.
        World.Officer enemyOfficer=new World.Officer(990001,"测试守军",1,-1,70,70,70,70,70);commands.officers.add(enemyOfficer);World.Unit enemy=new World.Unit(commands.nextUnitId++,1,enemyOfficer.id,World.Weapon.SPEAR,new Hex(13,12),5000,10000);enemyOfficer.cityId=-1;enemyOfficer.unitId=enemy.id;commands.units.add(enemy);unit.hex=new Hex(12,12);unit.acted=false;int enemyTroops=enemy.troops;World.Result hit=commands.attack(unit.id,enemy.id);check(hit.ok&&enemy.troops<enemyTroops,"installed real attack inflicts damage");ui(()->{game.selectUnitAndFocus(unit.id);game.applyResult(hit);});settle();shot("08-custom-crew-battle");
        byte[] save=SaveCodec.encode(commands);try(OutputStream out=context().openFileOutput("auto.sg11",0)){out.write(save);}check(CustomOfficers.portrait(commands,firstId).png.length>0,"campaign embeds portrait bytes");
        packs(first,second,save);draft();
        JSONObject expected=new JSONObject().put("first",firstId).put("name","手机自定义甲");try(Writer out=new OutputStreamWriter(context().openFileOutput("officer-restart-expect.json",0),"UTF-8")){out.write(expected.toString());}
        write("create");
    }
    private void addPlacement(World.City home,String person)throws Exception{click("添加投放人物 / 历史覆盖",true);searchText(person);click(person+" · 新武将",false);click("选择有效驻地（必选）",true);searchText("#"+home.id);click(home.name+" · ",false);click("确定",true);}
    private void packs(JSONObject first,JSONObject second,byte[] saved)throws Exception{
        CustomOfficerLibrary library=new CustomOfficerLibrary(context());byte[] bytes=CustomOfficerPack.exportPack(context(),library.snapshot());CustomOfficerPack.Preview p=CustomOfficerPack.preview(context(),new ByteArrayInputStream(bytes),library);check(p.images.size()==1&&p.root.getJSONArray("entries").length()==2,"portable ZIP has actual portrait and people");
        String unchanged=library.snapshot().toString();
        try{CustomOfficerPack.preview(context(),new ByteArrayInputStream(Arrays.copyOf(bytes,bytes.length-22)),library);throw new AssertionError("truncated central directory accepted");}catch(IOException expected){check(unchanged.equals(library.snapshot().toString()),"truncated ZIP rejected before any library change");}
        JSONObject wrongVersion=new JSONObject(p.root.toString());wrongVersion.put("packageVersion",1.5);
        try{CustomOfficerPack.preview(context(),new ByteArrayInputStream(wrongVersion.toString().getBytes("UTF-8")),library);throw new AssertionError("fractional package version accepted");}catch(IOException expected){check(unchanged.equals(library.snapshot().toString()),"fractional package version rejected atomically");}
        for(String field:new String[]{"honor","talkMask"}){
            JSONObject fractional=library.snapshot();fractional.getJSONArray("entries").getJSONObject(0).put(field,1.5);
            try{library.replace(fractional);throw new AssertionError("fractional "+field+" accepted");}catch(IllegalArgumentException expected){check(unchanged.equals(library.snapshot().toString()),"fractional "+field+" rejected atomically");}
        }
        // Deletion of the complete library and underlying avatar file must not affect the already saved campaign.
        JSONObject empty=library.snapshot();empty.put("entries",new JSONArray()).put("plans",new JSONArray());library.replace(empty);check(CustomOfficerImages.file(context(),first.getString("portrait")).delete(),"test removes original library portrait file");World restored=SaveCodec.decode(saved);check(restored.officer(first.getInt("runtimeId")).name.equals(first.getString("name"))&&CustomOfficers.portrait(restored,first.getInt("runtimeId")).png.length>0,"save is independent of deleted templates and original PNG file");
        check(CustomOfficerPack.apply(context(),library,p,CustomOfficerPack.Conflict.SKIP)==2,"portable reimport restores both definitions");check(CustomOfficerImages.read(context(),first.getString("portrait")).length>0,"portrait bytes restored from package");check(CustomOfficerPack.apply(context(),library,p,CustomOfficerPack.Conflict.SKIP)==0&&library.entries().size()==2,"repeated import is idempotent");
        check(CustomOfficerPack.apply(context(),library,p,CustomOfficerPack.Conflict.COPY)==2,"as-new imports allocate stable new identities");List<JSONObject> copies=library.entries().subList(2,4);check(copies.get(0).getJSONArray("relationships").getJSONObject(0).getString("target").equals(copies.get(1).getString("id")),"as-new package remaps relationship IDs atomically");check(CustomOfficerPack.apply(context(),library,p,CustomOfficerPack.Conflict.COPY)==0&&library.entries().size()==4,"repeated as-new import does not create duplicates");
        String before=library.snapshot().toString();ByteArrayOutputStream malicious=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(malicious)){zip.putNextEntry(new ZipEntry("../escape.json"));zip.write(1);zip.closeEntry();}try{CustomOfficerPack.preview(context(),new ByteArrayInputStream(malicious.toByteArray()),library);throw new AssertionError("path traversal accepted");}catch(IOException expected){check(before.equals(library.snapshot().toString()),"invalid ZIP cannot partially change library");}
        JSONObject invalid=library.snapshot();invalid.getJSONArray("entries").getJSONObject(0).getJSONArray("stats").put(0,101);try{library.replace(invalid);throw new AssertionError("invalid stat accepted");}catch(IllegalArgumentException expected){check(before.equals(library.snapshot().toString()),"invalid numeric input preserves transaction");}
        CustomOfficerLibrary stale=new CustomOfficerLibrary(context());JSONObject latest=library.entries().get(0);library.save(latest);try{stale.save(stale.entries().get(0));throw new AssertionError("stale writer accepted");}catch(IOException expected){check(true,"stale library revision cannot overwrite a newer editor");}
        try(OutputStream out=new FileOutputStream(new File(directory(),"roundtrip-officers.zip"))){out.write(bytes);}try(OutputStream out=new FileOutputStream(new File(directory(),"self-contained.sg11"))){out.write(saved);}
    }
    private void draft()throws Exception{
        CustomOfficerActivity activity=(CustomOfficerActivity)test.startActivitySync(new Intent(context(),CustomOfficerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();click("新建武将",true);editText(activity,"name","未完成草稿");editText(activity,"birth","");click("草稿返回",true);check(context().getSharedPreferences("customOfficers-draft",0).getString("draft","").contains("未完成草稿"),"invalid partial numeric draft persists instead of being dropped");shot("09-draft-recovery-entry");
    }
    private void restart()throws Exception{
        JSONObject expected;try(InputStream in=context().openFileInput("officer-restart-expect.json")){expected=new JSONObject(new String(CustomOfficerLibrary.readBounded(in,4096),"UTF-8"));}
        MainActivity main=(MainActivity)test.startActivitySync(new Intent(context(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();World world=(World)field(main,"world");check(world!=null&&world.officer(expected.getInt("first")).name.equals(expected.getString("name")),"fresh process normal startup restores custom campaign");check(CustomOfficers.portrait(world,expected.getInt("first")).png.length>0,"fresh process uses portrait embedded in campaign");
        CustomOfficerLibrary library=new CustomOfficerLibrary(context());check(library.entries().size()==4,"fresh process keeps imported library and stable identities");CustomOfficerActivity editor=(CustomOfficerActivity)test.startActivitySync(new Intent(context(),CustomOfficerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();click("恢复未完成草稿",true);check(((EditText)editor.getWindow().getDecorView().findViewWithTag("officer.name")).getText().toString().equals("未完成草稿"),"fresh process restores named draft");check(((EditText)editor.getWindow().getDecorView().findViewWithTag("officer.birth")).getText().length()==0,"unfinished invalid numeric input is retained across process death");shot("10-process-restart-draft");
    }
    private void editText(Activity a,String key,String text){ui(()->{EditText input=a.getWindow().getDecorView().findViewWithTag("officer."+key);check(input!=null,"real form field "+key);input.setText(text);});settle();}
    private void searchText(String value)throws Exception{AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();AccessibilityNodeInfo node=find(root,"搜索列表",true);check(node!=null,"native searchable selector");Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);check(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b),"search query entered");settle();}
    private void toggle(String text)throws Exception{AccessibilityNodeInfo n=find(test.getUiAutomation().getRootInActiveWindow(),text,false);check(n!=null&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"actual switch toggled");settle();}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo root,String text,boolean description){if(root==null)return null;CharSequence value=description?root.getContentDescription():root.getText();if(value!=null&&value.toString().equals(text))return root;for(int i=0;i<root.getChildCount();i++){AccessibilityNodeInfo n=find(root.getChild(i),text,description);if(n!=null)return n;}return null;}
    private View findDescription(View root,String description){if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=findDescription(((ViewGroup)root).getChildAt(i),description);if(found!=null)return found;}return null;}
    private void waitText(String text)throws Exception{for(int i=0;i<100;i++){AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();if(root!=null&&!root.findAccessibilityNodeInfosByText(text).isEmpty())return;SystemClock.sleep(150);}throw new AssertionError("Missing text: "+text);}
    private void waitWorld(MainActivity a,int id)throws Exception{for(int i=0;i<300;i++){World w=(World)field(a,"world");if(w!=null&&w.officer(id)!=null){settle();return;}SystemClock.sleep(150);}throw new AssertionError("Custom scenario did not activate");}
    private Activity current()throws Exception{return (Activity)field(test,"current");}
    private void click(String text,boolean exact)throws Exception{call(test,"click",new Class<?>[]{String.class,boolean.class},text,exact);settle();}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(250);test.waitForIdleSync();}
    private File directory(){File d=new File(context().getExternalFilesDir(null),"custom-officers");d.mkdirs();return d;}
    private void shot(String name)throws Exception{Bitmap bitmap=test.getUiAutomation().takeScreenshot();check(bitmap!=null,"real screenshot "+name);try(OutputStream out=new FileOutputStream(new File(directory(),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
    private void write(String mode)throws Exception{try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(directory(),mode+".txt")),"UTF-8")){out.write("CUSTOM OFFICERS ANDROID PASS "+checks+" checks\n"+report);}}
    private void check(boolean yes,String message){checks++;report.append(yes?"PASS ":"FAIL ").append(message).append('\n');if(!yes)throw new AssertionError(message);}
    interface Action{void run()throws Exception;}
    private void ui(Action action){Throwable[] error={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable t){error[0]=t;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}
}
