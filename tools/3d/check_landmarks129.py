#!/usr/bin/env python3
"""Independent strict-subset, indexed geometry and footprint checks for v129.
python3 tools/3d/check_landmarks129.py [asset-root] [output-report]
Complement with Khronos validator and the real Java SiteGlb parser benchmark.
"""
import sys,json,struct,math,hashlib
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];AR=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else ROOT
REPORT=Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/'out/feedback129/blender/asset-validation.json'
folder=AR/'app/src/main/assets/3d/field/v129';manifest=json.loads((AR/'docs/native-pc-visual/feedback-v129-blender-assets.json').read_text())
report={'version':129,'checks':{},'assets':[]};counts={}
def read(p):
 raw=p.read_bytes();magic,ver,total=struct.unpack_from('<III',raw);assert (magic,ver,total)==(0x46546c67,2,len(raw))
 n=struct.unpack_from('<I',raw,12)[0];d=json.loads(raw[20:20+n]);a=d['accessors'];start=28+n
 def arr(i):
  x=a[i];v=d['bufferViews'][x['bufferView']];w={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[x['type']];fmt='f' if x['componentType']==5126 else 'I';vals=struct.unpack_from('<'+fmt*w*x['count'],raw,start+v.get('byteOffset',0)+x.get('byteOffset',0));return [vals[j:j+w] for j in range(0,len(vals),w)]
 return raw,d,[arr(i) for i in range(5)]
for p in sorted(folder.glob('*.glb')):
 raw,d,arrays=read(p);acc=d['accessors'];pos,colors,uv,ix,normals=arrays;indices=[x[0] for x in ix]
 assert len(d['meshes'])==1 and len(d['meshes'][0]['primitives'])==1 and not d.get('extensionsRequired')
 assert not any('uri' in x for key in ['images','buffers'] for x in d[key]);assert len(pos)==len(colors)==len(uv)==len(normals)
 family=p.stem.rsplit('-lod',1)[0];lod=int(p.stem[-1]);tris=len(indices)//3;counts[family,lod]=tris
 assert tris <= (manifest['budgets_lod0'][family] if lod==0 else 850)
 assert len(pos)<30000 and len(raw)<2_000_000 and len(indices)%3==0
 assert all(math.isfinite(v) for row in pos+colors+uv+normals for v in row)
 assert all(0<=v<=1 for row in colors+uv for v in row)
 assert all(.98<sum(t*t for t in row)<1.02 for row in normals)
 assert all(0<=i<len(pos) for i in indices)
 areas=[]
 for i in range(0,len(indices),3):
  a,b,c=[pos[j] for j in indices[i:i+3]];ab=[b[k]-a[k] for k in range(3)];ac=[c[k]-a[k] for k in range(3)];cross=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]];areas.append(math.sqrt(sum(x*x for x in cross))*.5)
 assert min(areas)>1e-12,(p,min(areas))
 oldversion=127 if family.startswith('fall-') else 126;bases=list((ROOT/f'app/src/main/assets/3d/field/v{oldversion}').glob(family+'-lod*.glb'))
 if not bases:bases=list((ROOT/'app/src/main/assets/3d/field/v128').glob(family+'-lod*.glb'))
 ba=[read(f)[1]['accessors'][0] for f in bases]
 for axis in [0,2]:assert acc[0]['min'][axis]>=min(a['min'][axis] for a in ba)-.0001 and acc[0]['max'][axis]<=max(a['max'][axis] for a in ba)+.0001,(p,axis,acc[0],ba)
 if family.startswith('fall-'):
  panel=3 if family=='fall-hukou' else 5;heights=[]
  for t in [.34,.50]:
   values=[pos[i][1] for i,u in enumerate(uv) if panel/8<u[0]<(panel+1)/8 and abs(u[1]-(.06+.88*t))<.00001];assert values,(family,lod,t);heights.append([min(values),max(values)])
  assert heights[0][0]-heights[1][1]>.93
  # Rock/foam panel4 uses intentionally separated UV rows, never inverted.
  for i,u in enumerate(uv):
   if 4/8<u[0]<5/8:assert u[1]<.65 or u[1]>.79,(family,lod,i,u)
 if family=='wall-earth':assert any(all(abs(row[i]-[.98,.87,.69][i])<.00001 for i in range(3)) for row in colors)
 oldraw,od,oa=read(ROOT/f'app/src/main/assets/3d/field/v128/{p.name}');oldtris=od['accessors'][3]['count']//3;oldvertices=od['accessors'][0]['count']
 oldcpu=oldvertices*52+oldtris*12;cpu=len(pos)*52+tris*12
 report['assets'].append({'id':p.stem,'triangles':tris,'vertices':len(pos),'glb_bytes':len(raw),'mesh_cpu_bytes':cpu,'sha256':hashlib.sha256(raw).hexdigest(),'bounds':acc[0]['min']+acc[0]['max'],'minimum_triangle_area':min(areas),'before_v128':{'triangles':oldtris,'vertices':oldvertices,'glb_bytes':len(oldraw),'mesh_cpu_bytes':oldcpu},'delta':{'triangles':tris-oldtris,'glb_bytes':len(raw)-len(oldraw),'mesh_cpu_bytes':cpu-oldcpu}})
assert len(report['assets'])==20
for family,lod in counts:
 if lod==0:assert counts[family,1]<counts[family,0]*.45
assert (folder/'scenery-atlas.png').read_bytes()==(ROOT/'app/src/main/assets/3d/field/v128/scenery-atlas.png').read_bytes()
assert Image.open(folder/'scenery-atlas.png').size==(512,64)
legacy=ROOT/'out/feedback129/blender/preexisting-assets.json'
if legacy.exists():
 inventory=json.loads(legacy.read_text())
 for name,digest in inventory.items():assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest,name
 report['preexisting_asset_files_byte_identical']=len(inventory)
# Honest dense repeated-cliff bounds, not a GPU/FPS measurement. Counts reflect
# merged buffer duplication; visible chunks and actual placements vary by map.
cliffs=[a for a in report['assets'] if a['id'].startswith('cliff-') and a['id'].endswith('lod0')]
report['dense_repeated_cliff_cost']={}
for chunks in [1,4,9,16]:
 instances=15*chunks
 report['dense_repeated_cliff_cost'][str(chunks)+'_8x8_chunks']={'assumed_cliffs_per_chunk':15,'instances':instances,'near_triangles_min':instances*min(a['triangles'] for a in cliffs),'near_triangles_max':instances*max(a['triangles'] for a in cliffs),'merged_near_cpu_bytes_min':instances*min(a['mesh_cpu_bytes'] for a in cliffs),'merged_near_cpu_bytes_max':instances*max(a['mesh_cpu_bytes'] for a in cliffs),'before_near_triangles_min':instances*min(a['before_v128']['triangles'] for a in cliffs),'before_near_triangles_max':instances*max(a['before_v128']['triangles'] for a in cliffs),'before_merged_near_cpu_bytes_min':instances*min(a['before_v128']['mesh_cpu_bytes'] for a in cliffs),'before_merged_near_cpu_bytes_max':instances*max(a['before_v128']['mesh_cpu_bytes'] for a in cliffs),'scope':'cliffs only; excludes terrain, trees, units, draw overhead, array object headers and far buffers; scenarios, not observed map placement counts'}
report['totals']={key:sum(a[key] for a in report['assets']) for key in ['triangles','vertices','glb_bytes','mesh_cpu_bytes']}
report['before_v128_totals']={key:sum(a['before_v128'][key] for a in report['assets']) for key in ['triangles','vertices','glb_bytes','mesh_cpu_bytes']}
report['checks']={k:'PASS' for k in ['strict_single_primitive_subset','no_external_uris','finite_normals_uv_colors','family_reasoned_triangle_budgets','actual_indices_not_vertex_count','nonzero_triangle_area','far_LOD_under_45_percent','atlas_byte_identical_v128','normalized_lip_034_foot_050','wall_vertex_anchor','legacy_XZ_envelope']}
REPORT.parent.mkdir(parents=True,exist_ok=True);REPORT.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'models':len(report['assets']),'checks':report['checks'],'totals':report['totals'],'before':report['before_v128_totals']}))
