from pathlib import Path
import sys
root=Path(sys.argv[1])
p=root/'app/src/main/java/game/sanguo/mobile/FilamentMapView.java'
s=p.read_text()
def change(old,new):
 global s
 assert s.count(old)==1,(old[:100],s.count(old))
 s=s.replace(old,new)
change('private long frameCallbacks,beginAttempts,beginSkipped,outputCopies,lastWorkLog;',
       'private long frameCallbacks,beginAttempts,beginSkipped,outputCopies,lastWorkLog,gpuPreparationFrames;')
change('        applySeason(SeasonStyle.forMonth(next.month));\n','')
change('        syncObjects();overlay.invalidate();schedule();',
       '        // The owner accepts immutable state now; GPU updates wait for frame admission.\n        assetSyncPending=true;refreshPendingMeshes();overlay.invalidate();schedule();')
change('        if(backdropSource!=result.scenery){if(backdrop!=null){backdrop.destroy();backdrop=null;}backdropSource=result.scenery;}',
       '        // CPU mailbox delivery must not bypass renderer backpressure.\n        // Keep the old backdrop alive until an admitted frame uploads its replacement.\n        backdropSource=result.scenery;')
change('        if(result.trees!=null)woods=result.trees;pending=meshWork.pending();clampCamera();',
       '        if(result.trees!=null)woods=result.trees;clampCamera();refreshPendingMeshes();')
change('replay=e;replayFraction=CombatVisual.fraction(fraction);if(e==null)clearEffects();animateReplay();overlay.invalidate();schedule();',
       'replay=e;replayFraction=CombatVisual.fraction(fraction);if(e==null)clearEffects();overlay.invalidate();schedule();')
change('        +" frameCallbacks="+frameCallbacks+" beginAttempts="+beginAttempts+" beginSkipped="+beginSkipped+" surfaceCopies="+outputCopies',
       '        +" frameCallbacks="+frameCallbacks+" beginAttempts="+beginAttempts+" beginSkipped="+beginSkipped+" gpuPreparationFrames="+gpuPreparationFrames+" lifetimeSubmissions="+renderedFrames+" surfaceCopies="+outputCopies')
start=s.index('    @Override public void doFrame(long time){')
end=s.index('    /** Check actual display output',start)
s=s[:start]+'''    @Override public void doFrame(long time){
        frameCallbacks++;queued=false;if(released||!resumed||swap==null)return;
        if(!pacer.due(time,thermal.fps(quality))){schedule();return;}
        long cpuStart=System.nanoTime();
        lastMeshUploads=0;lastMeshUploadNanos=0;
        try{
            // Bounded CPU delivery is independent of GPU admission. A rejected frame
            // must not strand the producer on its one-slot result mailbox.
            meshWork.drain(this::acceptMeshes,e->{pending=0;failure.accept(e);});
            if(released)return;
            if(assetWork.drain())assetSyncPending=true;
            refreshPendingMeshes();
            if(lastFrame!=0)callbackMillis=(time-lastFrame)/1e6;lastFrame=time;
            boolean begun=false;
            if(bufferWidth>0&&bufferHeight>0){beginAttempts++;begun=renderer.beginFrame(swap,time);if(!begun)beginSkipped++;}
            if(begun){
                try{
                    // Filament's beginFrame(false) is backpressure, not permission
                    // to enqueue uploads/transforms and only skip render().
                    gpuPreparationFrames++;assetUploadBudget=2;
                    if(snapshot!=null)applySeason(SeasonStyle.forMonth(snapshot.month));
                    if(assetSyncPending)syncObjects();
                    double aspect=camera.width/(double)camera.height;
                    lens.setProjection(Camera.Projection.ORTHO,-camera.span*aspect,camera.span*aspect,-camera.span,camera.span,.1,1000);
                    lens.lookAt(camera.x+camera.backX()*300*camera.cos(),300*camera.sin(),camera.z+camera.rightX()*300*camera.cos(),camera.x,0,camera.z,0,1,0);
                    if(waterLastTick!=0&&UiMotion.enabled())waterSeconds+=Math.min(.1,(time-waterLastTick)/1e9);
                    waterLastTick=time;waterMaterial.getDefaultInstance().setParameter("waveTime",(float)(waterSeconds%4096));
                    animationTick=time/1_000_000;animateReplay();loadVisible();animateUnits();animateEffects();
                    renderer.render(view);
                }finally{renderer.endFrame();}
                renderedFrames++;surfaceFrames++;
                if(surfaceFrames==1)android.util.Log.i("Sanguo3D","First submission (not visibility proof): "+startupReport());
                checkSurfaceOutput();
            }
            long now=android.os.SystemClock.uptimeMillis();
            if(!outputVerified&&now-lastWorkLog>=1000){lastWorkLog=now;android.util.Log.i("Sanguo3D","Load progress "+startupReport());}
            // HWUI labels/progress and the next opportunity remain live even when
            // the separate Filament Surface cannot accept another frame.
            overlay.invalidate();schedule();
            cpuSamples[cpuCursor++%cpuSamples.length]=System.nanoTime()-cpuStart;cpuCount=Math.min(cpuSamples.length,cpuCount+1);
        }catch(RuntimeException|LinkageError|OutOfMemoryError e){cancelFrame();failure.accept(e);}
    }
'''+s[end:]
change('    private void loadVisible(){\n        if(snapshot==null)return;\n        if(backdrop==null&&backdropSource!=null){backdrop=new GpuMesh(backdropSource);backdrop.show(true);}',
'''    /** Count real visible CPU results not resident on the GPU, even when frame
     * admission is rejected. No GPU writes, synthetic READY or ignored mailbox. */
    private void refreshPendingMeshes(){
        int remaining=meshWork.pending();
        if(snapshot!=null){
            if(backdropSource!=null&&(backdrop==null||backdrop.source!=backdropSource))remaining++;
            for(SceneMesh chunk:chunks)if(inView(chunk.x,chunk.z,chunk.radius)&&!terrain.containsKey(chunk))remaining++;
            for(SceneMesh source:woods){
                SceneMesh chunk=quality!=SceneQuality.LOW&&camera.span<14?source:source.distant;
                if(chunk.indices.length>0&&inView(chunk.x,chunk.z,chunk.radius)&&!vegetation.containsKey(chunk))remaining++;
            }
        }
        pending=remaining;
    }
    private void loadVisible(){
        if(snapshot==null)return;
        if(backdropSource!=null&&(backdrop==null||backdrop.source!=backdropSource)){
            GpuMesh replacement=new GpuMesh(backdropSource);replacement.show(true);
            GpuMesh old=backdrop;backdrop=replacement;if(old!=null)old.destroy();
        }''')
p.write_text(s)
v=root/'version.properties';text=v.read_text();assert 'versionCode=108' in text;text=text.replace('versionCode=108','versionCode=109').replace('0.108.0-native-ground-stream','0.109.0-native-frame-admission');v.write_text(text)
p=root/'app/src/androidTest/java/game/sanguo/mobile/NativeColdStartInstrumentation.java';text=p.read_text();old='   check((Boolean)field(nativeView,"distantTerrain"),"first national preview uses coarse terrain");';assert text.count(old)==1
text=text.replace(old,old+'''
   long preparations=(Long)field(nativeView,"gpuPreparationFrames"),submissions=(Long)field(nativeView,"renderedFrames");
   long attempts=(Long)field(nativeView,"beginAttempts"),skipped=(Long)field(nativeView,"beginSkipped");
   note("FRAME_ADMISSION preparations="+preparations+" lifetimeSubmissions="+submissions+" attempts="+attempts+" rejected="+skipped);
   check(preparations>0&&preparations==submissions&&preparations==attempts-skipped,"GPU preparation occurs only in successful, balanced native frames");''');p.write_text(text)
