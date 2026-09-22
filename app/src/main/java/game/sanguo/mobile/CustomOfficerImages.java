package game.sanguo.mobile;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;

/** Content-addressed processed PNGs; no temporary provider URI is retained. */
final class CustomOfficerImages {
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(4*1024*1024){@Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}};
    static File file(Context c,String name)throws IOException{
        if(!name.matches("[a-f0-9]{64}\\.png"))throw new IOException("头像文件名无效");File dir=new File(c.getFilesDir(),"customOfficers/portraits");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建头像目录");return new File(dir,name);
    }
    static String digest(byte[] bytes)throws IOException{try{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    static byte[] read(Context c,String name)throws IOException{try(InputStream in=new FileInputStream(file(c,name))){byte[] bytes=CustomOfficerLibrary.readBounded(in,CustomOfficers.MAX_PORTRAIT);if(!name.equals(digest(bytes)+".png"))throw new IOException("头像校验失败，请重新选择头像");validate(bytes);return bytes;}}
    static void validate(byte[] bytes)throws IOException{
        if(bytes.length<24||bytes.length>CustomOfficers.MAX_PORTRAIT||bytes[0]!=(byte)137||bytes[1]!=80||bytes[2]!=78||bytes[3]!=71)throw new IOException("头像须为处理后的PNG");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth!=256||bounds.outHeight!=256)throw new IOException("头像尺寸须为256×256");
        Bitmap decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(decoded==null)throw new IOException("头像损坏");decoded.recycle();
    }
    static String store(Context c,byte[] bytes)throws IOException{validate(bytes);String name=digest(bytes)+".png";AtomicFile target=new AtomicFile(file(c,name));FileOutputStream out=null;try{out=target.startWrite();out.write(bytes);target.finishWrite(out);return name;}catch(IOException e){if(out!=null)target.failWrite(out);throw e;}}
    static Bitmap bitmap(String name,byte[] png){synchronized(CACHE){Bitmap cached=CACHE.get(name);if(cached!=null)return cached;try{validate(png);Bitmap b=BitmapFactory.decodeByteArray(png,0,png.length);if(b!=null)CACHE.put(name,b);return b;}catch(IOException|RuntimeException e){return null;}}}
    static Drawable preview(Context c,String ref,World.Officer officer){
        World w=new World(3,3,new String[]{"甲","乙"});return new OfficerPortrait(c,w,officer,ref,localPng(c,ref));
    }
    private static byte[] localPng(Context c,String ref){try{return ref.endsWith(".png")?read(c,ref):new byte[0];}catch(IOException e){return new byte[0];}}
    static void importCrop(Activity a,Uri uri,Consumer<String> done,Consumer<Exception> failed){
        new Thread(()->{try{
            byte[] bytes;try(InputStream in=a.getContentResolver().openInputStream(uri)){bytes=CustomOfficerLibrary.readBounded(in,8*1024*1024);}
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<1||options.outHeight<1||(long)options.outWidth*options.outHeight>100_000_000L)throw new IOException("图片损坏或分辨率超过安全范围");
            options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(image==null)throw new IOException("无法解码图片");
            a.runOnUiThread(()->{if(a.isFinishing()){image.recycle();return;}crop(a,image,done,failed);});
        }catch(Exception e){a.runOnUiThread(()->failed.accept(e));}},"officer-image-import").start();
    }
    private static void crop(Activity a,Bitmap source,Consumer<String> done,Consumer<Exception> failed){
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);ImageView preview=new ImageView(a);box.addView(preview,new LinearLayout.LayoutParams(-1,256));
        Bitmap output=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);SeekBar zoom=new SeekBar(a),x=new SeekBar(a),y=new SeekBar(a);zoom.setMax(200);x.setMax(100);y.setMax(100);x.setProgress(50);y.setProgress(50);
        TextView note=new TextView(a);note.setText("方形适配：缩放、水平与垂直裁剪。可旋转纠正照片方向。");box.addView(note);box.addView(zoom);box.addView(x);box.addView(y);int[] rotation={0};
        Runnable render=()->{Canvas canvas=new Canvas(output);canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);float base=Math.max(256f/source.getWidth(),256f/source.getHeight()),scale=base*(1+zoom.getProgress()/100f);canvas.save();canvas.rotate(rotation[0],128,128);canvas.translate(-(source.getWidth()*scale-256)*x.getProgress()/100f,-(source.getHeight()*scale-256)*y.getProgress()/100f);canvas.scale(scale,scale);canvas.drawBitmap(source,0,0,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));canvas.restore();preview.setImageBitmap(output);preview.invalidate();};
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int n,boolean user){render.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}};zoom.setOnSeekBarChangeListener(listener);x.setOnSeekBarChangeListener(listener);y.setOnSeekBarChangeListener(listener);Button rotate=new Button(a);rotate.setText("旋转90°");rotate.setOnClickListener(v->{rotation[0]=(rotation[0]+90)%360;render.run();});box.addView(rotate);render.run();
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("预览并裁剪头像").setView(box).setPositiveButton("使用头像",(d,n)->{try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();output.compress(Bitmap.CompressFormat.PNG,100,bytes);done.accept(store(a,bytes.toByteArray()));}catch(Exception e){failed.accept(e);}}).setNegativeButton("取消",null).create();dialog.setOnDismissListener(d->{preview.setImageDrawable(null);output.recycle();source.recycle();});dialog.show();
    }
}
