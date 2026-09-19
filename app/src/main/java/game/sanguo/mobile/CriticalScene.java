package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import game.sanguo.core.*;

/** Shared portrait cut-in. Reuses the shipped art atlas; no synthetic result or gameplay mutation. */
final class CriticalScene {
    static final float DURATION=680f;
    private final Context context;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path slash=new Path();
    private CriticalHit hit;
    private Drawable portrait;
    CriticalScene(Context context){this.context=context;}
    void set(World w,CriticalHit value){if(hit==value)return;hit=value;portrait=value==null?null:new OfficerPortrait(context,w,value.officerCopy());}
    void draw(Canvas canvas,int width,int height,float phase){
        if(hit==null||width<=0||height<=0)return;
        float d=context.getResources().getDisplayMetrics().density;
        float enter=Math.min(1,Math.max(0,phase)*7),exit=Math.min(1,Math.max(0,1-phase)*7);
        int alpha=Math.round(255*Math.min(enter,exit));
        canvas.saveLayerAlpha(0,0,width,height,alpha);
        paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xe508151d,0xe61a2833,0xee07151c},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,width,height,paint);paint.setShader(null);
        float band=Math.min(height*.8f,300*d),top=(height-band)*.46f;
        paint.setColor(0xffa98244);canvas.drawRect(0,top,width,top+2*d,paint);canvas.drawRect(0,top+band,width,top+band+2*d,paint);
        for(int i=0;i<7;i++){
            float x=width*.05f+i*width*.18f;
            slash.rewind();slash.moveTo(x,top);slash.lineTo(x+38*d,top);slash.lineTo(x-95*d,top+band);slash.lineTo(x-125*d,top+band);slash.close();
            paint.setColor(0x157dd5c0);canvas.drawPath(slash,paint);
        }
        int portraitSize=(int)Math.min(band*.9f,width*.49f);
        int px=(int)(width*.02f-(1-enter)*40*d),py=(int)(top+(band-portraitSize)/2);
        portrait.setBounds(px,py,px+portraitSize,py+portraitSize);portrait.draw(canvas);
        float tx=width*.53f,available=width-tx-8*d;
        paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.create("serif",Typeface.BOLD));
        paint.setTextSize(Math.min(30*d,available/4));paint.setColor(0xffefd7a0);
        canvas.drawText("战 法 暴 击",tx,top+band*.30f,paint);
        paint.setTypeface(Typeface.create("serif",Typeface.BOLD));paint.setTextSize(Math.min(35*d,available/Math.max(2,hit.name.length())));paint.setColor(0xfffcf7e9);
        canvas.drawText(hit.name,tx,top+band*.54f,paint);
        paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(Math.min(17*d,available/Math.max(4,hit.tactic.length())));paint.setColor(0xff96dbc5);
        canvas.drawText(hit.tactic,tx,top+band*.70f,paint);
        paint.setColor(0xffd7bd83);canvas.drawRect(tx,top+band*.78f,width-22*d,top+band*.78f+d,paint);
        paint.setTextSize(11*d);paint.setColor(0xffabc1c2);paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("高光瞬间 · 点击可跳过",width/2f,Math.min(height-12*d,top+band+30*d),paint);
        canvas.restore();
    }
}
