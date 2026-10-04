package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.runtime.GameSession;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual installed all-source pixels and age lookup, not a claim that all normal UI caller forms are integrated. */
public final class PortraitPixelsInstrumentation extends Instrumentation {
    private int checks;
    private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static String hex(byte[] bytes){StringBuilder text=new StringBuilder();for(byte b:bytes)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return text.toString();}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();AtomicReference<GameSession> game=new AtomicReference<>();byte[][] before={null};JSONObject evidence=new JSONObject();
        try{
            runOnMainSync(()->{try{game.set(new GameSession(ScenarioCatalog.load("coalition-190",0,20260923L)));before[0]=game.get().captureSave();}catch(Exception error){throw new IllegalStateException(error);}});
            PcPortraitCatalog catalog=new PcPortraitCatalog(getTargetContext());JSONObject manifest;
            try(var input=getTargetContext().getAssets().open("portraits/pc/media-manifest.json")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=input.read(b))!=-1;)bytes.write(b,0,n);manifest=new JSONObject(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));}
            check(catalog.imageCount()==2892&&catalog.identityCount()==10656,"complete corrected source images and approved joins");
            JSONArray images=manifest.getJSONArray("images");long totalBytes=0;int[] rgba=new int[240*240];byte[] pixels=new byte[240*240*4];
            for(int index=0;index<images.length();index++){
                JSONObject image=images.getJSONObject(index);byte[] png;try(var input=getTargetContext().getAssets().open(image.getString("asset"))){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=input.read(buffer))!=-1;)bytes.write(buffer,0,n);png=bytes.toByteArray();}
                check(hex(MessageDigest.getInstance("SHA-256").digest(png)).equals(image.getString("pngSha256")),"installed original PNG "+index);
                BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap bitmap=BitmapFactory.decodeByteArray(png,0,png.length,options);
                check(bitmap!=null&&bitmap.getWidth()==image.getInt("width")&&bitmap.getHeight()==image.getInt("height"),"original dimensions "+index);
                int count=bitmap.getWidth()*bitmap.getHeight();bitmap.getPixels(rgba,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
                for(int i=0;i<count;i++){int value=rgba[i];pixels[4*i]=(byte)(value>>>16);pixels[4*i+1]=(byte)(value>>>8);pixels[4*i+2]=(byte)value;pixels[4*i+3]=(byte)(value>>>24);}
                MessageDigest hash=MessageDigest.getInstance("SHA-256");hash.update(pixels,0,count*4);check(hex(hash.digest()).equals(image.getString("rgbaSha256")),"actual Android original RGBA pixels "+index);totalBytes+=bitmap.getAllocationByteCount();bitmap.recycle();
            }
            JSONArray identities=manifest.getJSONArray("identities");int ageChecks=0;
            for(int index=0;index<identities.length();index++){
                JSONObject row=identities.getJSONObject(index);var identity=new PortraitMediaIdentity(row.getInt("officerId"),row.getInt("nativeId"),row.getString("sourceVariant"),row.getString("sourcePath"),row.getString("sourceSha256"),row.getString("recordSha256"));
                int birth=row.getInt("birth");JSONArray boundaries=row.getJSONArray("ageBoundaries");
                for(int boundary=0;boundary<boundaries.length();boundary++){
                    JSONObject original=boundaries.getJSONObject(boundary);int age=original.getInt("age"),selected=original.getInt("resolvedNormalFaceId");
                    check(catalog.normalFace(identity,birth+age-1)==selected,"source age/normal flag lookup "+index+"/"+age);ageChecks++;
                }
                var wrong=new PortraitMediaIdentity(identity.officerId,identity.nativeId,identity.sourceVariant,identity.sourcePath,identity.sourceSha,"0000000000000000000000000000000000000000000000000000000000000000");check(catalog.normalFace(wrong,birth)==-1,"record guard rejects mismatched provenance "+index);
            }
            runOnMainSync(()->{try{check(Arrays.equals(before[0],game.get().captureSave()),"complete pixel/lookup work preserves Save/RNG");}catch(Exception error){throw new IllegalStateException(error);}});
            evidence.put("images",images.length()).put("identityJoins",identities.length()).put("ageChecks",ageChecks).put("totalSequentialBitmapBytes",totalBytes).put("maxPixelWorkBufferBytes",pixels.length+rgba.length*4);
            result.putString("portraitPixels","PORTRAIT_PIXELS PASS checks="+checks+"; all original source pixel/age guards; not normal caller/runtime coverage acceptance");
        }catch(Throwable error){result.putString("portraitPixels","FAIL "+android.util.Log.getStackTraceString(error));}
        finally{try{evidence.put("result",result.getString("portraitPixels"));Files.write(getTargetContext().getFilesDir().toPath().resolve("portrait-pixels.json"),evidence.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception error){result.putString("portraitPixels","FAIL evidence "+error);}runOnMainSync(()->{if(game.get()!=null)game.get().close();});}
        finish(result.getString("portraitPixels").startsWith("PORTRAIT_PIXELS PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
}
