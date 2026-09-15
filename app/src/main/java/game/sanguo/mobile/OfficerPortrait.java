package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import game.sanguo.core.*;
import java.io.*;

/** One bounded atlas allocation, shared by recycled list rows and detail cards. No network or per-face bitmaps. */
final class OfficerPortrait extends Drawable {
    private static Bitmap atlas;private static boolean loaded;
    private final World.Officer officer;private final int age,index,variant;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path path=new Path();private final Rect source=new Rect();private final RectF area=new RectF();
    OfficerPortrait(Context context,World w,World.Officer officer){
        this.officer=officer;age=w.life.age(officer.id);index=PortraitCatalog.index(officer.name);variant=PortraitCatalog.variant(officer.id,officer.name);
        if(!loaded){loaded=true;try(InputStream input=context.getAssets().open("portraits/officers.png")){
            BitmapFactory.Options options=new BitmapFactory.Options();atlas=BitmapFactory.decodeStream(input,null,options);
        }catch(IOException ignored){/* Custom and unknown officers still have deterministic vector portraits. */}}
    }
    private void fill(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);}
    private void oval(Canvas c,float l,float t,float r,float b,int color){fill(color);c.drawOval(l,t,r,b,paint);}
    private void poly(Canvas c,int color,float... xy){path.reset();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();fill(color);c.drawPath(path,paint);}
    @Override public void draw(Canvas c){
        area.set(getBounds());c.save();path.reset();path.addRoundRect(area,area.width()*.1f,area.width()*.1f,Path.Direction.CW);c.clipPath(path);
        if(index>=0&&atlas!=null){int col=index%4,row=index/4;source.set(col*atlas.getWidth()/4,row*atlas.getHeight()/4,(col+1)*atlas.getWidth()/4,(row+1)*atlas.getHeight()/4);fill(Color.WHITE);c.drawBitmap(atlas,source,area,paint);}
        else {c.translate(area.left,area.top);c.scale(area.width()/100,area.height()/100);fallback(c);}
        c.restore();fill(0xffc9ae73);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,area.width()/70));c.drawRoundRect(area,area.width()*.1f,area.width()*.1f,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void fallback(Canvas c){
        int[] robes={0xff435e59,0xff6c4645,0xff665c4c,0xff4a536c,0xff666b51,0xff795d41};
        int robe=robes[variant%robes.length],skin=new int[]{0xffcba783,0xffd9b995,0xffb28c6f,0xffe1c39e}[(variant/7)%4];
        int hair=age>=55?0xffa6a69e:0xff242d2c;boolean female=officer.sex==World.Sex.FEMALE,scholar=officer.intelligence+officer.politics>officer.leadership+officer.war;
        fill(0xff213c40);c.drawRect(0,0,100,100,paint);oval(c,6,1,98,99,0xff2e4b4a);
        poly(c,robe,2,100,9,81,35,68,65,68,92,81,100,100);poly(c,0xffcfbf98,30,73,47,95,51,83,64,72);poly(c,robe,56,75,42,100,92,100);
        oval(c,26,17,74,82,hair);oval(c,28,43,35,58,skin);oval(c,66,43,73,58,skin);
        oval(c,31,23,70,76,skin);poly(c,0x30452214,65,31,69,48,63,67,52,72,62,58);
        fill(hair);paint.setStrokeWidth(2.4f);c.drawLine(36,43,45,42-(variant%3),paint);c.drawLine(55,42,64,44,paint);
        oval(c,37,47,44,50,0xff292d29);oval(c,56,47,63,50,0xff292d29);
        poly(c,0xffac805f,51,49,48,58,54,58);fill(0xff8c5c4c);paint.setStrokeWidth(1.6f);c.drawLine(44,64,57,64,paint);
        if(female){oval(c,34,8,65,30,hair);poly(c,hair,28,22,50,18,71,23,70,38,55,28,32,38);fill(0xffc9ae73);c.drawCircle(67,28,4,paint);c.drawCircle(68,61,2,paint);}
        else if(scholar){poly(c,0xff202c31,27,35,30,17,68,17,73,36);poly(c,0xff26343a,37,19,35,2,63,2,65,20);fill(0xffab976a);c.drawRect(30,30,70,34,paint);}
        else {poly(c,0xff76837f,26,41,29,22,48,10,69,23,76,43,65,32,50,27,34,34);poly(c,0xffb6bdad,34,28,49,11,53,27);poly(c,robe,49,12,45,0,60,1,57,17);}
        if(!female&&(age>=40||variant%3==0)){poly(c,hair,40,61,48,60,50,63,54,60,62,62,56,67,46,67);poly(c,hair,40,69,48,73,60,69,57,82,50,88,43,81);}
        if(!scholar&&!female){for(int x=15;x<86;x+=9){fill(0xffb7baa3);c.drawRect(x,88,x+6,91,paint);c.drawRect(x+2,94,x+8,97,paint);}}
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
