#!/usr/bin/env python3
"""Bound staged map-effect vertex uploads without mutating active production."""
from pathlib import Path
import json,hashlib,difflib,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/map-effect-upload258'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 path='app/src/main/java/game/sanguo/mobile/PcMapEffects.java';source=ROOT/path;before=source.read_text();after=before
 needle='    private int capacity,shown,serial;'
 replacement='''    private int capacity,shown,serial;
    private static final int MAX_QUADS=32768;
    private static final class UploadSlot { FloatBuffer buffer; boolean busy; }
    private final UploadSlot[] uploadSlots={new UploadSlot(),new UploadSlot(),new UploadSlot()};
    private final android.os.Handler uploadHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private float[] vertexOutput=new float[0];
    private UploadSlot availableUpload(int floats) {
        for(UploadSlot slot:uploadSlots)if(!slot.busy) {
            if(slot.buffer==null||slot.buffer.capacity()<floats) {
                int size=slot.buffer==null?16*36:slot.buffer.capacity();
                while(size<floats)size*=2;
                slot.buffer=ByteBuffer.allocateDirect(size*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            return slot;
        }
        return null;
    }'''
 assert after.count(needle)==1;after=after.replace(needle,replacement)
 needle='''            ensureCapacity(frame.count);
            if(frame.count>0) {
                float[] output=new float[frame.count*4*9];
                for(int i=0;i<frame.count;i++)for(int v=0;v<4;v++)PcEffectCoordinates.vertex(data,i*184,v,originX,originZ,output,(i*4+v)*9);
                FloatBuffer buffer=ByteBuffer.allocateDirect(output.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();buffer.put(output).flip();
                vertices.setBufferAt(engine,0,buffer);
            }'''
 replacement='''            if(frame.count<0||frame.count>MAX_QUADS)throw new IllegalArgumentException("Original map packet count out of bounds");
            int floats=frame.count*36;
            UploadSlot slot=frame.count==0?null:availableUpload(floats);
            if(frame.count>0&&slot==null)return;
            ensureCapacity(frame.count);
            if(frame.count>0) {
                if(vertexOutput.length<floats)vertexOutput=new float[slot.buffer.capacity()];
                for(int i=0;i<frame.count;i++)for(int v=0;v<4;v++)PcEffectCoordinates.vertex(data,i*184,v,originX,originZ,vertexOutput,(i*4+v)*9);
                FloatBuffer buffer=slot.buffer;buffer.clear();buffer.put(vertexOutput,0,floats).flip();
                slot.busy=true;
                try { vertices.setBufferAt(engine,0,buffer,0,0,uploadHandler,()->{slot.busy=false;if(closed)slot.buffer=null;}); }
                catch(RuntimeException failure){slot.busy=false;throw failure;}
            }'''
 assert after.count(needle)==1;after=after.replace(needle,replacement)
 needle='''        hide();for(int entity:entities)''';assert after.count(needle)==1
 after=after.replace(needle,'''        vertexOutput=new float[0];for(UploadSlot slot:uploadSlots)if(!slot.busy)slot.buffer=null;
        hide();for(int entity:entities)''')
 target=OUT/path;target.parent.mkdir(parents=True);target.write_text(after)
 patch=DOC/'MAP_EFFECT_UPLOAD258.patch';patch.write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+path,tofile='b/'+path)))
 java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java');android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar');base=ROOT/'out/session-a/native-opening-stage224/compile';dep=ROOT/'out/session-a/native-opening-stage224/dependencies';cp=':'.join(map(str,[android,base/'filament.jar',base/'classes']+sorted(dep.glob('*.jar'))));classes=OUT/'classes';classes.mkdir();cmd=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(target)]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text()
 replay=OUT/'readback';copy=replay/path;copy.parent.mkdir(parents=True);copy.write_text(before);subprocess.run(['git','apply','--no-index',str(patch)],cwd=replay,check=True);assert sha(copy)==sha(target) and source.read_text()==before
 report={'path':path,'beforeSha256':sha(source),'afterSha256':sha(target),'patchSha256':sha(patch),'compiled':True,'patchReadbackExact':True,'maxPackets':32768,'maxDirectSlots':3,'maxDirectVertexBytes':32768*144*3,'maxJavaVertexBytes':32768*144,'actualInstalled':False,'canonicalUnchanged':True,'scope':'Three callback-owned direct upload slots; no slot reused before actual Filament completion. Backpressure returns with pending source frame retained; same original vertex conversion/order/count/materials/time/state. One bounded reusable Java vertex array. Close frees idle slots; completion clears busy references after close. No rules/RNG/JNI/old snapshots changed. Declared bounds require actual Android allocation/slow GPU/rotation/close acceptance; no claim of original OOM root cause or runtime performance.','wholeGoalComplete':False};(DOC/'MAP_EFFECT_UPLOAD258.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
