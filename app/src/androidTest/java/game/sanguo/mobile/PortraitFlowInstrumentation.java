package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.api.OfficerSnapshot;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import org.json.*;

/** Normal source-choice/list/row/detail/save/read/exit. No test binding or alternate portrait source. */
public final class PortraitFlowInstrumentation extends Instrumentation {
    private MainActivity activity;private int checks;private boolean resume;private String run;
    private File output;private JSONObject evidence=new JSONObject();private final JSONArray rows=new JSONArray();
    private PcPortraitCatalog catalog;private PcPortraitLoader loader;
    @Override public void onCreate(Bundle args){super.onCreate(args);resume=args!=null&&"1".equals(args.getString("resume"));run=args==null?"":args.getString("run","");if(run.isEmpty())run="run-"+System.nanoTime();if(!run.matches("[A-Za-z0-9_-]+"))throw new IllegalArgumentException("Invalid run ID");start();}
    private static String hex(byte[] bytes){StringBuilder text=new StringBuilder();for(byte value:bytes)text.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return text.toString();}
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private void ui(Runnable work){Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable failure){error[0]=failure;}});if(error[0]!=null)throw new AssertionError("Normal portrait main-thread check",error[0]);}
    private void settle(){ui(()->{});SystemClock.sleep(70);}
    private View find(View view,Predicate<View> match){if(!view.isShown())return null;if(match.test(view))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=find(((ViewGroup)view).getChildAt(i),match);if(found!=null)return found;}return null;}
    private View await(Predicate<View> match){long end=SystemClock.uptimeMillis()+30000;while(SystemClock.uptimeMillis()<end){View[] value={null};ui(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--)if(roots.get(i).hasWindowFocus()&&(value[0]=find(roots.get(i),match))!=null)break;});if(value[0]!=null)return value[0];settle();}throw new AssertionError("Normal portrait control timeout");}
    private void tap(View view){Rect bounds=new Rect();ui(()->{int[] offset=new int[2];check(view.getGlobalVisibleRect(bounds),"normal clickable control visible");view.getRootView().getLocationOnScreen(offset);bounds.offset(offset[0],offset[1]);});long down=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,bounds.centerX(),bounds.centerY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"normal pointer accepted");event.recycle();}settle();}
    private void description(String name){tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(name)));}
    private void text(String name){tap(await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(name)&&(v.isClickable()||v.getParent() instanceof AdapterView)));}
    private void nav(String name){description("打开功能导航");description("导航 · "+name);}
    private byte[] capture(){byte[][] value={null};ui(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException error){throw new IllegalStateException(error);}});return value[0];}
    private static Object field(Object object,String name)throws Exception{Field value=object.getClass().getDeclaredField(name);value.setAccessible(true);return value.get(object);}
    private void shot(String name)throws IOException{Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"actual screen captured");try(FileOutputStream file=new FileOutputStream(new File(output,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,file);}finally{image.recycle();}}
    private OfficerPortrait portrait(View view){if(view instanceof ImageView&&((ImageView)view).getDrawable() instanceof OfficerPortrait)return (OfficerPortrait)((ImageView)view).getDrawable();if(view instanceof TextView)for(Drawable image:((TextView)view).getCompoundDrawables())if(image instanceof OfficerPortrait)return (OfficerPortrait)image;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){OfficerPortrait found=portrait(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
    private OfficerPortrait detailPortrait(){View view=await(v->v instanceof ImageView&&((ImageView)v).getDrawable() instanceof OfficerPortrait);return (OfficerPortrait)((ImageView)view).getDrawable();}
    private void verify(OfficerPortrait drawable,OfficerSnapshot.Officer row,int year,String phase)throws Exception{
        check(drawable!=null,phase+" real drawable "+row.id);
        PortraitMediaIdentity actual=(PortraitMediaIdentity)field(drawable,"sourceIdentity");
        if(row.source==null){check(actual==null,"unknown source stays unknown "+row.id);return;}
        check(actual!=null&&actual.officerId==row.id&&actual.nativeId==row.source.nativeId&&actual.sourceVariant.equals(row.source.sourceVariant)&&actual.sourceSha.equals(row.source.sourceSha)&&actual.recordSha.equals(row.source.recordSha),phase+" readonly DTO provenance "+row.id);
        PcPortraitCatalog.Image source=catalog.resolve(actual,year,0);check(source!=null,"source lookup has original image "+row.id);
        Bitmap[] pixel={null};long started=SystemClock.uptimeMillis(),end=started+30000;PcPortraitLoader.Target target=()->{};
        while(SystemClock.uptimeMillis()<end){ui(()->pixel[0]=loader.get(actual,year,0,target));if(pixel[0]!=null)break;SystemClock.sleep(10);}
        check(pixel[0]!=null&&loader.error().isEmpty(),phase+" background bitmap ready "+row.id);
        Bitmap expected;try(InputStream input=getTargetContext().getAssets().open(source.asset)){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;expected=BitmapFactory.decodeStream(input,null,options);}
        check(expected!=null&&expected.sameAs(pixel[0]),phase+" loaded exact original pixels "+row.id);
        Bitmap rendered=Bitmap.createBitmap(240,240,Bitmap.Config.ARGB_8888);Rect saved=new Rect(drawable.getBounds());ui(()->{drawable.setBounds(0,0,240,240);drawable.draw(new Canvas(rendered));drawable.setBounds(saved);});
        for(int y=32;y<208;y+=8)for(int x=32;x<208;x+=8)check(rendered.getPixel(x,y)==expected.getPixel(x,y),phase+" drawable interior original pixel "+row.id);
        rendered.recycle();expected.recycle();check(loader.bytes()<=16*1024*1024,"production LRU within16MiB");
        if("list".equals(phase))rows.put(new JSONObject().put("officerId",row.id).put("nativeId",actual.nativeId).put("sourceVariant",actual.sourceVariant).put("asset",source.asset).put("face",source.face).put("loadWaitMs",SystemClock.uptimeMillis()-started));
    }
    private void roster(boolean allDetails)throws Exception{
        byte[] before=capture();World control=SaveCodec.decode(before);nav("武将");DataTable<?> table=(DataTable<?>)await(v->v instanceof DataTable);
        OfficerSnapshot[] value={null};ui(()->value[0]=activity.officerSnapshot());OfficerSnapshot snapshot=value[0];int[] size={0};ui(()->size[0]=table.list.getAdapter().getCount());check(size[0]==snapshot.officers.size(),"normal directory contains entire saved roster");
        int mapped=0,unknown=0,details=0;Set<Integer> seen=new HashSet<>();
        for(int position=0;position<size[0];position++){
            final int index=position;View[] visible={null};long end=SystemClock.uptimeMillis()+10000;int[] id={-1};
            ui(()->{id[0]=(int)table.list.getAdapter().getItemId(index);table.list.setSelectionFromTop(index,0);});
            while(visible[0]==null&&SystemClock.uptimeMillis()<end){settle();ui(()->visible[0]=table.list.getChildAt(index-table.list.getFirstVisiblePosition()));}
            OfficerSnapshot.Officer row=snapshot.officer(id[0]);check(row!=null&&seen.add(row.id),"actual visible stable row ID "+id[0]);check(visible[0]!=null,"actual recycled row shown "+id[0]);
            OfficerPortrait[] image={null};ui(()->{TextView name=(TextView)((ViewGroup)visible[0]).getChildAt(0);check(name.getText().toString().equals(row.name),"name text retains DTO");check(name.getLayout()!=null&&name.getLayout().getEllipsisCount(0)==0,"entire officer name visible beside image "+row.id);image[0]=portrait(visible[0]);});verify(image[0],row,control.life.year(),"list");if(row.source==null)unknown++;else mapped++;
            if(allDetails||details<3&&row.source!=null){tap(visible[0]);verify(detailPortrait(),row,control.life.year(),"detail");if(details<3)shot("detail-"+row.id);tap(await(v->v.getId()==android.R.id.button2));details++;}
            if(position%100==0){shot("list-"+position);Files.write(new File(output,"progress.json").toPath(),new JSONObject().put("position",position).put("checks",checks).put("mapped",mapped).put("unknown",unknown).toString().getBytes("UTF-8"));}
        }
        check(Arrays.equals(before,capture()),"entire normal roster/details preserve all Save/RNG bytes");check(mapped>500,"normal source opening connects broad approved roster");
        evidence.put("roster",size[0]).put("mapped",mapped).put("sourceUnknown",unknown).put("details",details).put("decoded",loader.decoded()).put("bitmapCacheBytes",loader.bytes()).put("rows",rows);
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("portrait-normal"),run);if(!output.mkdirs())throw new IOException("Fresh portrait evidence directory required");catalog=new PcPortraitCatalog(getTargetContext());loader=PcPortraitLoader.shared(getTargetContext());
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        byte[] initial=capture();World legacy=SaveCodec.decode(initial);
        if(!resume){check(PcOfficerInfo.saved(legacy).isEmpty(),"legacy user source remains unknown before explicit choice");nav("菜单");text("新游戏 / 选择势力");description("选择剧本 190");description("选择本局人物文字资料来源");text("黃巾之亂 · 184");check(Arrays.equals(initial,capture()),"new-game source selection itself preserves old Save/RNG");description("确认开局势力");text("开始新局");long end=SystemClock.uptimeMillis()+60000;while(Arrays.equals(initial,capture())&&SystemClock.uptimeMillis()<end)settle();check(!Arrays.equals(initial,capture()),"normal new game commits explicit approved source");}
        else{check(!PcOfficerInfo.saved(legacy).isEmpty(),"cold process restores saved source identity");check(Arrays.equals(initial,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"cold startup all saved source/RNG bytes match disk");}
        roster(!resume);byte[] before=capture();nav("菜单");text("保存局面");text("槽位 3");text("覆盖存档");settle();check(Arrays.equals(before,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"normal save persists exact source and RNG");
        game.sanguo.api.StateToken[] prior={null};ui(()->prior[0]=activity.deploymentState());text("读取存档");text("槽位 3");text("读取存档");long loadEnd=SystemClock.uptimeMillis()+60000;boolean[] loaded={false};while(!loaded[0]&&SystemClock.uptimeMillis()<loadEnd){settle();ui(()->loaded[0]=!prior[0].equals(activity.deploymentState()));}check(loaded[0],"normal read replaces actual session token");check(Arrays.equals(before,capture()),"normal read retains source and every Save/RNG byte");Files.write(new File(output,"source-campaign.sg11").toPath(),before);
        nav("地图");sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);SystemClock.sleep(500);getTargetContext().startActivity(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));settle();check(Arrays.equals(before,capture()),"background/resume preserves authority");
        ui(()->activity.finish());settle();activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(before,capture()),"exit/reopen preserves authority");
        nav("武将");DataTable<?> table=(DataTable<?>)await(v->v instanceof DataTable);View[] first={null};ui(()->first[0]=table.list.getChildAt(0));int[] id={0};ui(()->id[0]=(int)table.list.getAdapter().getItemId(0));OfficerSnapshot[] snapshot={null};ui(()->snapshot[0]=activity.officerSnapshot());OfficerPortrait[] image={null};ui(()->image[0]=portrait(first[0]));verify(image[0],snapshot[0].officer(id[0]),SaveCodec.decode(before).life.year(),"reopen");shot("reopen");
        evidence.put("saveSha256",hex(java.security.MessageDigest.getInstance("SHA-256").digest(before))).put("saveBytes",before.length).put("normalAutomaticSourceBinding",true).put("allCallerFormsProven",false).put("armEvidence",false);result.putString("portraitFlow","PORTRAIT_FLOW PASS checks="+checks);
    }catch(Throwable error){result.putString("portraitFlow","FAIL "+android.util.Log.getStackTraceString(error));try{shot("FAIL");}catch(Throwable ignored){}}
    finally{try{evidence.put("result",result.getString("portraitFlow"));Files.write(new File(output,"result.json").toPath(),evidence.toString(2).getBytes("UTF-8"));}catch(Exception error){result.putString("portraitFlow","FAIL evidence "+error);}}
    finish(result.getString("portraitFlow").startsWith("PORTRAIT_FLOW PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
