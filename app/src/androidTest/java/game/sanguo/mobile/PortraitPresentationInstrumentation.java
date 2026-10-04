package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import game.sanguo.core.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import org.json.*;

/** Installed production Drawable constructors on actual saved-source presentation copies; no test source binding. */
public final class PortraitPresentationInstrumentation extends Instrumentation {
    private int checks;private MainActivity activity;private PcPortraitCatalog catalog;private JSONObject evidence=new JSONObject();
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private void ui(Runnable work){Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable failure){error[0]=failure;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private byte[] capture(){byte[][] value={null};ui(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception error){throw new IllegalStateException(error);}});return value[0];}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void awaitSource(OfficerPortrait image,World copy,int id)throws Exception{
        long end=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<end){boolean[] done={false};ui(()->done[0]=!PortraitMediaSources.pending(copy,image));if(done[0])break;SystemClock.sleep(10);}
        check(PortraitMediaSources.error(copy).isEmpty(),"saved source worker has no error");check(PortraitMediaSources.source(copy,id)!=null,"actual constructor binds immutable saved source copy");
    }
    private void pixels(OfficerPortrait image,World copy,int id,int year)throws Exception{
        awaitSource(image,copy,id);PortraitMediaIdentity identity=PortraitMediaSources.source(copy,id);var source=catalog.resolve(identity,year,0);check(source!=null,"original source face available");
        Bitmap target=Bitmap.createBitmap(240,240,Bitmap.Config.ARGB_8888);long end=SystemClock.uptimeMillis()+30000;Bitmap[] actual={null};PcPortraitLoader loader=PcPortraitLoader.shared(activity);
        while(SystemClock.uptimeMillis()<end){ui(()->{image.setBounds(0,0,240,240);image.draw(new Canvas(target));actual[0]=loader.get(identity,year,0,image);});if(actual[0]!=null)break;SystemClock.sleep(10);}
        check(actual[0]!=null,"original bitmap is ready on background worker");Bitmap expected;try(InputStream in=activity.getAssets().open(source.asset)){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;expected=BitmapFactory.decodeStream(in,null,options);}
        ui(()->image.draw(new Canvas(target)));check(expected.sameAs(actual[0]),"loaded bitmap equals packaged original pixels");for(int y=32;y<208;y+=8)for(int x=32;x<208;x+=8)check(expected.getPixel(x,y)==target.getPixel(x,y),"presentation copy draws source interior");
        check(((Integer)field(image,"sourceYear"))==year,"immutable requested event year retained");check(loader.bytes()<=16*1024*1024,"bounded production bitmap cache");expected.recycle();target.recycle();
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));byte[] before=capture();World saved=SaveCodec.decode(before);Map<Integer,PcOfficerInfo.Person> originals=PcOfficerInfo.saved(saved);check(originals.size()>500,"actual pinned normal source campaign restored");catalog=new PcPortraitCatalog(activity);
        int id=originals.keySet().iterator().next();World visual=SaveCodec.decode(before);OfficerPortrait[] image={null};ui(()->{check(!PortraitMediaSources.bound(visual),"new detached presentation copy is initially unbound");image[0]=new OfficerPortrait(activity,visual,visual.officer(id));});pixels(image[0],visual,id,visual.life.year());check(Arrays.equals(before,SaveCodec.encode(visual)),"presentation constructor/worker preserve every detached save/RNG byte");
        // Same stable officer and saved source, while current presentation calendar differs from applied event year.
        OfficerPortrait[] applied={null};JSONObject manifest;
        try(InputStream input=getTargetContext().getAssets().open("portraits/pc/media-manifest.json");ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];for(int n;(n=input.read(buffer))!=-1;)bytes.write(buffer,0,n);manifest=new JSONObject(bytes.toString("UTF-8"));}
        JSONObject ageRow=null;JSONArray identities=manifest.getJSONArray("identities");var provenance=originals.get(id);
        for(int index=0;index<identities.length();index++){JSONObject row=identities.getJSONObject(index);if(row.getInt("officerId")==id&&row.getString("sourceVariant").equals(provenance.sourceVariant)){ageRow=row;break;}}
        check(ageRow!=null,"age boundary comes from exact approved source row");int boundaryYear=ageRow.getInt("birth")+ageRow.getInt("ageThreshold")-1;PortraitMediaIdentity sourceIdentity=PortraitMediaSources.source(visual,id);int young=catalog.normalFace(sourceIdentity,boundaryYear-1),old=catalog.normalFace(sourceIdentity,boundaryYear);check(young!=old,"source young and old face IDs actually differ");
        int earlierYear=boundaryYear-1;ui(()->applied[0]=new OfficerPortrait(activity,visual,visual.officer(id),earlierYear));pixels(applied[0],visual,id,earlierYear);
        OfficerPortrait[] aged={null};ui(()->aged[0]=new OfficerPortrait(activity,visual,visual.officer(id),boundaryYear));pixels(aged[0],visual,id,boundaryYear);evidence.put("youngFace",young).put("oldFace",old).put("boundaryYear",boundaryYear);
        // Invalid identity follows completed OfficerQuery behavior; never borrows the previous copy's approved image.
        World changed=SaveCodec.decode(before);World.Officer previous=changed.officer(id);changed.officers.set(changed.officers.indexOf(previous),new World.Officer(id,"unapproved changed identity",previous.owner,previous.cityId,previous.leadership,previous.war,previous.intelligence,previous.politics,previous.charm));OfficerPortrait[] rejected={null};ui(()->rejected[0]=new OfficerPortrait(activity,changed,changed.officer(id)));long deadline=SystemClock.uptimeMillis()+30000;while(SystemClock.uptimeMillis()<deadline){boolean[] done={false};ui(()->done[0]=!PortraitMediaSources.pending(changed,rejected[0]));if(done[0])break;SystemClock.sleep(10);}check(PortraitMediaSources.source(changed,id)==null,"changed identity stays unbound rather than reusing previous image");
        World legacy=ScenarioCatalog.load("coalition-190",0,20260923L);OfficerPortrait[] unknown={null};ui(()->unknown[0]=new OfficerPortrait(activity,legacy,legacy.officers.get(0)));check(PortraitMediaSources.source(legacy,legacy.officers.get(0).id)==null,"unrelated old source-less scene does not borrow source");
        ui(()->{PortraitMediaSources.discard(visual);PortraitMediaSources.discard(changed);PortraitMediaSources.discard(legacy);});check(!PortraitMediaSources.bound(visual),"explicit retired-view discard removes source mapping");Bitmap retired=Bitmap.createBitmap(240,240,Bitmap.Config.ARGB_8888);ui(()->image[0].draw(new Canvas(retired)));check(field(image[0],"sourceIdentity")==null&&retired.getPixel(120,120)==0xff213c40,"retired original drawable cannot keep drawing stale approved identity");retired.recycle();World holder=SaveCodec.decode(before),replacement=SaveCodec.decode(before);OfficerPortrait[] retained={null};ui(()->{OfficerPortrait.bindView(activity,holder);retained[0]=new OfficerPortrait(activity,holder,holder.officer(id));});awaitSource(retained[0],holder,id);ui(()->OfficerPortrait.bindView(activity,replacement));check(!PortraitMediaSources.bound(holder)&&!PortraitMediaSources.error(holder).isEmpty(),"production bindView replacement automatically retires prior source");
        check(Arrays.equals(before,capture()),"all media copies/year/rejection/retirement preserve actual complete authority and RNG");
        evidence.put("officerId",id).put("sources",originals.size()).put("actualConstructorNoTestBinding",true).put("coreApiRuntimeChanged",false).put("allBattleCallerFormsProven",false);result.putString("portraitPresentation","PORTRAIT_PRESENTATION PASS checks="+checks);
    }catch(Throwable error){result.putString("portraitPresentation","FAIL "+android.util.Log.getStackTraceString(error));}
    finally{try{evidence.put("result",result.getString("portraitPresentation"));Files.write(getTargetContext().getFilesDir().toPath().resolve("portrait-presentation.json"),evidence.toString(2).getBytes("UTF-8"));}catch(Exception error){result.putString("portraitPresentation","FAIL evidence "+error);}}
    finish(result.getString("portraitPresentation").startsWith("PORTRAIT_PRESENTATION PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
