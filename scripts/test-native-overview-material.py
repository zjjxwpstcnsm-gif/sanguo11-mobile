#!/usr/bin/env python3
"""Host-only camera LOD + extracted real material rebind; never device acceptance."""
from pathlib import Path
import hashlib, subprocess, tempfile
root=Path(__file__).resolve().parent.parent
s=(root/'app/src/main/java/game/sanguo/mobile/FilamentMapView.java').read_text()
a=s.index('        void bindTerrainMaterial(){');b=s.index('        void build(int target,MaterialInstance instance){',a)
method=s[a:b]
head='''package game.sanguo.mobile;
public class OverviewMaterialHarness {
 boolean overviewTerrain,overview;int entity=17;Source source=new Source();Engine engine=new Engine();
 Material overviewGroundMaterial=new Material(1),groundMaterial=new Material(2),overviewWaterMaterial=new Material(3),waterMaterial=new Material(4);
 static class Source {int landIndexCount;int[] indices=new int[12];}
 static class Material {final int id;Material(int id){this.id=id;}Material getDefaultInstance(){return this;}}
 static class RenderableManager {int calls;int[] slots=new int[4],materials=new int[4];int getInstance(int e){check(e==17,"same live entity");return 37;}void setMaterialInstanceAt(int i,int slot,Material m){check(i==37,"same renderable instance");slots[calls]=slot;materials[calls++]=m.id;}}
 static class Engine {final RenderableManager manager=new RenderableManager();RenderableManager getRenderableManager(){return manager;}}
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
'''
tail='''
 public static void main(String[] args){
  check(!TerrainMaterialLod.select(false,39.99f),"no premature overview");check(TerrainMaterialLod.select(false,40f),"normal coarse threshold enters overview");
  check(TerrainMaterialLod.select(true,36f),"hysteresis holds at exit");check(!TerrainMaterialLod.select(true,35.99f),"near PBR restored");
  for(float x:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})for(boolean prior:new boolean[]{false,true})check(TerrainMaterialLod.select(prior,x)==prior,"invalid span never churns resources");
  for(int land:new int[]{0,6,12})for(boolean far:new boolean[]{false,true}){
   OverviewMaterialHarness h=new OverviewMaterialHarness();h.source.landIndexCount=land;h.overviewTerrain=far;int[] original=h.source.indices;
   h.bindTerrainMaterial();RenderableManager m=h.engine.manager;
   check(h.overview==far&&h.source.indices==original&&h.entity==17,"material switch leaves geometry/identity intact");
   check(m.calls==(land==6?2:1)&&m.slots[0]==0,"land-only/water-only/mixed primitive mapping");
   check(m.materials[0]==(land==0?(far?3:4):(far?1:2)),"correct overview or original near material");
   if(land==6)check(m.slots[1]==1&&m.materials[1]==(far?3:4),"shore water retains second primitive");
  }
  boolean state=false;int switches=0;for(int i=0;i<100;i++){boolean next=TerrainMaterialLod.select(state,i%2==0?40.01f:39.99f);if(next!=state)switches++;state=next;}check(switches==1,"coarse-boundary jitter never repeatedly binds");
  for(int i=0;i<20;i++){state=TerrainMaterialLod.select(state,10);check(!state,"zoom close restores detail");state=TerrainMaterialLod.select(state,86.256714f);check(state,"zoom national restores overview");}
  System.out.println("PASS HOST extracted material binds: exact same geometry/entity, land/water slots, thresholds, invalid-span stability, 20 mode cycles (NOT installed lifecycle)");
 }
}
'''
with tempfile.TemporaryDirectory(prefix='overview-regression-') as d:
 p=Path(d);(p/'OverviewMaterialHarness.java').write_text(head+method+tail)
 subprocess.run(['javac','--release','17','-d',d,str(root/'app/src/main/java/game/sanguo/mobile/TerrainMaterialLod.java'),str(p/'OverviewMaterialHarness.java')],check=True)
 subprocess.run(['java','-cp',d,'game.sanguo.mobile.OverviewMaterialHarness'],check=True)
for name,expected in [('ground','2044ad3456b53928b49c4ffc4abea050f3f786eceb47076f8248a1a3f4af071f'),('water','007ea2d3c1391dc353cc5f28d2247ccb0a6352ef40b11bf896e984f837d0ada5')]:
 near=root/f'tools/3d/{name}.mat';far=(root/f'tools/3d/{name}-overview.mat').read_text()
 assert hashlib.sha256(near.read_bytes()).hexdigest()==expected, 'near source changed'
 assert 'shadingModel : unlit' in far and far.count('texture(')==8
 assert 'material.normal =' not in far and 'material.roughness' not in far
 for layer in ('grass','soil','sand','rock'):assert f'materialParams_{layer}Color' in far
 for preserved in ('plantTint','artTint','getUV1()','haze','macro'):assert preserved in far
assert 'int budget=8;long uploadNanos=0;int uploads=0;' in s
assert 'else {gpu.bindTerrainMaterial();lastMaterialBinds++;}' in s
assert 'gpu==null||gpu.overview!=wanted' in s
assert s.index('engine.destroyMaterial(overviewGroundMaterial)')<s.index('for(Texture texture:groundTextures)engine.destroyTexture(texture)')
print('PASS source invariants: original near assets unchanged, all eight albedo reads/shore/season retained; new programs omit PBR; bounded rebinds counted pending and borrowed textures released once.')
