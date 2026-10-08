#!/usr/bin/env python3
"""Stage actual attached portrait drawing verification; leave live test unchanged."""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/portrait-draw-check289'
NAME='app/src/androidTest/java/game/sanguo/mobile/SessionAMapRepairInstrumentation.java'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 assert not OUT.exists();source=ROOT/'out/session-a/map-fire-upload-build269/source'/NAME;raw=source.read_bytes();before=raw.decode()
 marker='expected.recycle();check(loader.bytes()<=16*1024*1024,"actual portrait cache stays within16MiB "+caller);'
 assert before.count(marker)==1
 replacement='''
        int[] drawnBounds={0,0};
        ui(()->{
            Rect bounds=new Rect(image.getBounds());int width=bounds.width(),height=bounds.height();
            check(width>0&&height>0&&width<=1024&&height<=1024,"actual attached portrait has bounded drawable dimensions "+caller);
            drawnBounds[0]=width;drawnBounds[1]=height;
            Bitmap actual=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888),reference=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            try{
                android.graphics.Canvas canvas=new android.graphics.Canvas(actual);canvas.translate(-bounds.left,-bounds.top);image.draw(canvas);
                android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG|android.graphics.Paint.FILTER_BITMAP_FLAG);
                paint.setColor(android.graphics.Color.WHITE);
                new android.graphics.Canvas(reference).drawBitmap(expected,null,new Rect(0,0,width,height),paint);
                check(actual.sameAs(reference),"actual attached original Drawable draws full rectangular source pixels without clip/frame "+caller);
                check(image.getBounds().equals(bounds),"portrait drawing leaves actual caller bounds unchanged "+caller);
            }finally{actual.recycle();reference.recycle();}
        });
        expected.recycle();check(loader.bytes()<=16*1024*1024,"actual portrait cache stays within16MiB "+caller);'''
 after=before.replace(marker,replacement)
 marker2='.put("fullBitmapSameAs",true)';assert after.count(marker2)==1
 after=after.replace(marker2,marker2+'.put("attachedDrawableFullRectangleSameAs",true).put("drawnWidth",drawnBounds[0]).put("drawnHeight",drawnBounds[1]).put("drawCheckScope","Actual attached Drawable Canvas at current bounds; not Windows framebuffer/small-family/Screen PixelCopy")')
 target=OUT/NAME;target.parent.mkdir(parents=True);target.write_text(after)
 assert source.read_bytes()==raw
 report={'path':NAME,'beforeSha256':sha(raw),'afterSha256':sha(target.read_bytes()),'stagedPath':str(target),'canonicalUnchanged':True,'actualInstalled':False,'scope':'Same normal roster/detail caller and current saved native identity. Compare drawing of actual attached Drawable at actual current bounded dimensions against complete original bitmap scaled with Android filtering. Detect unproved rounded clipping/gold frame; no snapshot/rule injection. Ordinary screenshot remains separate; not PC small-family, Windows framebuffer, fullscreen timing, voice or ARM acceptance.','wholeGoalComplete':False}
 (DOC/'PORTRAIT_DRAW_CHECK289.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
