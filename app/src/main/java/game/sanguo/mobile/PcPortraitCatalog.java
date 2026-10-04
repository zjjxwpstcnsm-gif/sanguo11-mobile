package game.sanguo.mobile;

import android.content.Context;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Original face flag/age lookup and exact immutable source join. Load/parse only on the media worker. */
final class PcPortraitCatalog {
    static final class Image {
        final int face,form,width,height;
        final String asset,pngSha,rgbaSha;
        Image(JSONObject row)throws Exception{
            face=row.getInt("faceId");form=row.getInt("imageGroup");width=row.getInt("width");height=row.getInt("height");asset=row.getString("asset");pngSha=row.getString("pngSha256");rgbaSha=row.getString("rgbaSha256");
            if(face<0||face>2399||form<0||form>2||width!=(form==0?240:64)||height!=(form==0?240:80)||!asset.equals(String.format(java.util.Locale.ROOT,"portraits/pc/group-%d/face-%04d.png",form,face))||!pngSha.matches("[0-9a-f]{64}")||!rgbaSha.matches("[0-9a-f]{64}"))throw new IOException("Invalid original pixel identity");
        }
    }
    private static final class Person {
        final PortraitMediaIdentity identity;
        final int face,birth,threshold,sex;
        Person(JSONObject row)throws Exception{identity=new PortraitMediaIdentity(row.getInt("officerId"),row.getInt("nativeId"),row.getString("sourceVariant"),row.getString("sourcePath"),row.getString("sourceSha256"),row.getString("recordSha256"));face=row.getInt("faceId");birth=row.getInt("birth");threshold=row.getInt("ageThreshold");sex=row.getInt("sexRaw");if(threshold<0||threshold>255)throw new IOException("Original age threshold");}
    }
    private final Map<String,Person> people=new HashMap<>();
    private final Map<Integer,Image> images=new HashMap<>();
    private final int flagStart;
    private final int[] flags;
    PcPortraitCatalog(Context context)throws IOException{
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Portrait manifest parsed on UI thread");
        try(var input=new GZIPInputStream(context.getAssets().open("portraits/pc/media-manifest.json.gz"))){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=input.read(buffer))!=-1;){if(bytes.size()+n>16*1024*1024)throw new IOException("Media manifest too large");bytes.write(buffer,0,n);}
            JSONObject root=new JSONObject(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
            if(root.getInt("schema")!=1||!root.getString("sourceFaceSha256").equals("5e6a69ac3555910196465a40a7d02092cc7b8e7dc7de8e106eac1173b6e29519"))throw new IOException("Unexamined FCE source");
            flagStart=root.getInt("flagStartFace");JSONArray f=root.getJSONArray("faceFlags");flags=new int[f.length()];for(int i=0;i<flags.length;i++)flags[i]=(int)f.getLong(i);
            JSONArray rows=root.getJSONArray("images");for(int i=0;i<rows.length();i++){Image image=new Image(rows.getJSONObject(i));if(images.put(image.face*3+image.form,image)!=null)throw new IOException("Duplicate original image");}
            rows=root.getJSONArray("identities");for(int i=0;i<rows.length();i++){Person person=new Person(rows.getJSONObject(i));if(people.put(person.identity.key(),person)!=null)throw new IOException("Duplicate original join");}
        }catch(IOException error){throw error;}catch(Exception error){throw new IOException("Original portrait catalog",error);}
    }
    int normalFace(PortraitMediaIdentity identity,int currentYear){
        if(identity==null)return -1;Person person=people.get(identity.key());
        if(person==null||!person.identity.sourcePath.equals(identity.sourcePath)||!person.identity.sourceSha.equals(identity.sourceSha)||!person.identity.recordSha.equals(identity.recordSha))return -1;
        int face=person.face;if(face>=0&&face<1000&&(long)currentYear-person.birth+1>=person.threshold)face+=1000;
        if(face>=0&&face<=2399){int index=face-flagStart;int flag=index>=0&&index<flags.length?flags[index]:0;if(((flag>>>24)&2)==0)face=person.sex==1?2100:2000;}
        return face;
    }
    Image resolve(PortraitMediaIdentity identity,int currentYear,int form){if(form<0||form>2)return null;int face=normalFace(identity,currentYear);return face<0?null:images.get(face*3+form);}
    int imageCount(){return images.size();}
    int identityCount(){return people.size();}
}
