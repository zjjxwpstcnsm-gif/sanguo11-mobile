from pathlib import Path
import hashlib, sys
root=Path(sys.argv[1]); p=root/'app/src/main/java/game/sanguo/mobile/FilamentMapView.java'; s=p.read_text()
assert hashlib.sha256(p.read_bytes()).hexdigest()=='480a436b3cc829f6930be8c04f6e2d2ecd3f1071f5c949e103dcdef7a5a94eaf', 'live renderer changed; reconcile, never overwrite'
def change(old,new):
 global s
 assert s.count(old)==1,(old[:80],s.count(old));s=s.replace(old,new)
change('    private Material waterMaterial;','    private Material waterMaterial;\n    private Material overviewGroundMaterial,overviewWaterMaterial;\n    private boolean overviewTerrain;\n    private int lastMaterialBinds;')
change('            loadWaterMaterial(context);','            loadWaterMaterial(context);\n            loadOverviewMaterials(context);')
change('    private Texture loadAtlas(Context context,String path)throws java.io.IOException {', '''    /** Camera-LOD programs use the same texture owners and unchanged world geometry.
     * No normal/PBR shader variants at national scale; detailed programs remain intact. */
    private void loadOverviewMaterials(Context context)throws java.io.IOException {
        overviewGroundMaterial=loadOverviewMaterial(context,"ground-overview");
        overviewWaterMaterial=loadOverviewMaterial(context,"water-overview");
        TextureSampler sampler=new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT);
        String[] layers={"grass","soil","sand","rock"};
        for(int i=0;i<layers.length;i++){
            Texture texture=groundTextures.get(i*2); // Borrowed, never duplicated or destroyed here.
            overviewGroundMaterial.getDefaultInstance().setParameter(layers[i]+"Color",texture,sampler);
            overviewWaterMaterial.getDefaultInstance().setParameter(layers[i]+"Color",texture,sampler);
        }
        overviewWaterMaterial.getDefaultInstance().setParameter("waveTime",0f);
    }
    private Material loadOverviewMaterial(Context context,String name)throws java.io.IOException {
        byte[] bytes;
        try(java.io.InputStream in=context.getAssets().open("3d/terrain/"+name+".filamat");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);bytes=out.toByteArray();}
        ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();
        return new Material.Builder().payload(payload,bytes.length).build(engine);
    }
    private Texture loadAtlas(Context context,String path)throws java.io.IOException {''')
change('        waterMaterial.getDefaultInstance().setParameter("waterTint",next.waterR,next.waterG,next.waterB);', '''        waterMaterial.getDefaultInstance().setParameter("waterTint",next.waterR,next.waterG,next.waterB);
        EnvironmentProfile.pigment(overviewGroundMaterial.getDefaultInstance(),next,true);
        EnvironmentProfile.pigment(overviewWaterMaterial.getDefaultInstance(),next,true);
        overviewWaterMaterial.getDefaultInstance().setParameter("waterTint",next.waterR,next.waterG,next.waterB);''')
change('        +" shadows="+environmentShadows+', '        +" terrainMaterialLod="+(overviewTerrain?"OVERVIEW":"DETAIL")+" lastAdmittedMaterialBinds="+lastMaterialBinds+" shadows="+environmentShadows+')
change('+(siteMaterial==null?0:1))+','+(siteMaterial==null?0:1)+(overviewGroundMaterial==null?0:1)+(overviewWaterMaterial==null?0:1))+')
change('" ground_fragment_samples="+(quality==SceneQuality.LOW?8:12)+','" ground_fragment_samples="+(overviewTerrain?8:quality==SceneQuality.LOW?8:12)+')
change('            if(backdropSource!=null&&(backdrop==null||backdrop.source!=backdropSource))remaining++;\n            for(SceneMesh chunk:chunks)if(inView(chunk.x,chunk.z,chunk.radius)&&!terrain.containsKey(chunk))remaining++;', '''            boolean wanted=TerrainMaterialLod.select(overviewTerrain,camera.span);
            if(backdropSource!=null&&(backdrop==null||backdrop.source!=backdropSource||backdrop.overview!=wanted))remaining++;
            for(SceneMesh chunk:chunks)if(inView(chunk.x,chunk.z,chunk.radius)){
                GpuMesh gpu=terrain.get(chunk);if(gpu==null||gpu.overview!=wanted)remaining++;
            }''')
change('    private void loadVisible(){\n        if(snapshot==null)return;', '''    private void loadVisible(){
        if(snapshot==null)return;
        overviewTerrain=TerrainMaterialLod.select(overviewTerrain,camera.span);
        overviewWaterMaterial.getDefaultInstance().setParameter("waveTime",(float)(waterSeconds%4096));
        lastMaterialBinds=0;
        // One shared bound for new GPU meshes and material switches. A single driver
        // operation is non-preemptible; no meshes/textures are recreated on an LOD switch.
        int budget=8;long uploadNanos=0;int uploads=0;
        if(backdrop!=null&&backdrop.source==backdropSource&&backdrop.overview!=overviewTerrain){
            long started=System.nanoTime();backdrop.bindTerrainMaterial();uploadNanos+=System.nanoTime()-started;budget--;lastMaterialBinds++;
        }''')
change('siteLod=nextLod;syncObjects();}int budget=8;long uploadNanos=0;visibleChunks=0;', 'siteLod=nextLod;syncObjects();}visibleChunks=0;')
change('            if(shown){visibleChunks++;if(gpu==null){if(budget>0&&uploadNanos<4_000_000L){long started=System.nanoTime();gpu=new GpuMesh(chunk);terrain.put(chunk,gpu);uploadNanos+=System.nanoTime()-started;budget--;}else pending++;}}', '''            if(shown){visibleChunks++;
                if(gpu==null||gpu.overview!=overviewTerrain){
                    if(budget>0&&uploadNanos<4_000_000L){
                        long started=System.nanoTime();
                        if(gpu==null){gpu=new GpuMesh(chunk);terrain.put(chunk,gpu);uploads++;}
                        else {gpu.bindTerrainMaterial();lastMaterialBinds++;}
                        uploadNanos+=System.nanoTime()-started;budget--;
                    }else pending++;
                }
            }''')
change('vegetation.put(chunk,gpu);uploadNanos+=System.nanoTime()-started;budget--;', 'vegetation.put(chunk,gpu);uploads++;uploadNanos+=System.nanoTime()-started;budget--;')
change('        lastMeshUploads=8-budget;lastMeshUploadNanos=uploadNanos;', '        lastMeshUploads=uploads;lastMeshUploadNanos=uploadNanos;')
change('        if(waterMaterial!=null)engine.destroyMaterial(waterMaterial);', '''        if(overviewWaterMaterial!=null)engine.destroyMaterial(overviewWaterMaterial);
        if(overviewGroundMaterial!=null)engine.destroyMaterial(overviewGroundMaterial);
        if(waterMaterial!=null)engine.destroyMaterial(waterMaterial);''')
change('material=null;waterMaterial=null;groundMaterial=null;', 'material=null;waterMaterial=null;groundMaterial=null;overviewGroundMaterial=null;overviewWaterMaterial=null;')
change('final SceneMesh source;boolean shown;', 'final SceneMesh source;boolean shown,overview;')
change('            int land=source.landIndexCount,water=source.indices.length-land;\n            RenderableManager.Builder', '''            overview=overviewTerrain;
            int land=source.landIndexCount,water=source.indices.length-land;
            RenderableManager.Builder''')
change('if(land>0)b.material(slot,groundMaterial.getDefaultInstance())', 'if(land>0)b.material(slot,(overview?overviewGroundMaterial:groundMaterial).getDefaultInstance())')
change('if(water>0)b.material(slot,waterMaterial.getDefaultInstance())', 'if(water>0)b.material(slot,(overview?overviewWaterMaterial:waterMaterial).getDefaultInstance())')
change('        void build(int target,MaterialInstance instance){build(target,instance,1);}', '''        void bindTerrainMaterial(){
            // Only called by loadVisible inside an admitted owner frame.
            RenderableManager manager=engine.getRenderableManager();int instance=manager.getInstance(entity),slot=0;
            if(source.landIndexCount>0)manager.setMaterialInstanceAt(instance,slot++,(overviewTerrain?overviewGroundMaterial:groundMaterial).getDefaultInstance());
            if(source.landIndexCount<source.indices.length)manager.setMaterialInstanceAt(instance,slot,(overviewTerrain?overviewWaterMaterial:waterMaterial).getDefaultInstance());
            overview=overviewTerrain;
        }
        void build(int target,MaterialInstance instance){build(target,instance,1);}''')
p.write_text(s)
(root/'app/src/main/java/game/sanguo/mobile/TerrainMaterialLod.java').write_text('''package game.sanguo.mobile;

/** Material detail follows the normal coarse geometry threshold, with hysteresis.
 * This changes neither geometry nor picking, and is independent of tests/device/OS. */
final class TerrainMaterialLod {
    private TerrainMaterialLod() {}
    static boolean select(boolean overview,float span){
        if(!Float.isFinite(span))return overview;
        return overview?span>=36f:span>=40f;
    }
}
''')
# Derive camera-LOD programs while leaving every existing source/asset byte intact.
for name in ('ground','water'):
 p=root/f'tools/3d/{name}.mat'; t=p.read_text(); t=t.replace('shadingModel : lit','shadingModel : unlit').replace('ContinuousGroundR04','NationalGroundV110').replace('ContinuousWaterR05','NationalWaterV110')
 if name=='ground':
  t=t.replace('        { type : sampler2d, name : rockColor },','        { type : sampler2d, name : rockColor }')
  a=t.index('        { type : sampler2d, name : grassNormal }'); b=t.index('\n    ]',a);t=t[:a]+t[b:]
  a=t.index('        float4 nr =');b=t.index('        float wet =',a);t=t[:a]+t[b:]
  t='\n'.join(line for line in t.split('\n') if not any(x in line for x in ('material.normal =','material.roughness =','material.metallic =','material.reflectance =')))
 else:
  t=t.replace('        { type : float, name : waveTime },\n        { type : float, name : waveStrength }','        { type : float, name : waveTime }')
  t='\n'.join(line for line in t.split('\n') if not any(x in line for x in ('float strength =','material.normal =','material.roughness=','material.reflectance=','material.metallic =','float wave2 =')))
 t='// Camera overview specialization: original albedo/shore/season and geometry retained.\n// No new texture uploads; near PBR source and binary are deliberately unchanged.\n'+t
 (root/f'tools/3d/{name}-overview.mat').write_text(t)
p=root/'version.properties';t=p.read_text();assert t=='versionCode=109\nversionName=0.109.0-native-frame-admission\n';p.write_text('versionCode=110\nversionName=0.110.0-native-overview-material\n')
print('Applied bounded national material LOD; unchanged core/map/near assets; compile/register exactly two new binaries before committing.')
