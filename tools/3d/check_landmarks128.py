#!/usr/bin/env python3
"""Check authored v128 strict-subset, budget, UV and legacy footprint contracts.
python3 tools/3d/check_landmarks128.py [asset-root] [output-report]
Khronos validation is complementary: node tools/3d/validate_gltf.cjs.
"""
import sys,json,struct,math,hashlib
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
ASSET_ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else ROOT
REPORT=Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/'out/feedback128/blender/asset-validation.json'
folder=ASSET_ROOT/'app/src/main/assets/3d/field/v128'
report={'version':128,'checks':{},'assets':[]}
counts={}
for p in sorted(folder.glob('*.glb')):
    raw=p.read_bytes();magic,version,total=struct.unpack_from('<III',raw)
    assert magic==0x46546c67 and version==2 and total==len(raw),p
    n=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+n]);acc=doc['accessors'];start=28+n
    assert len(doc['meshes'])==1 and len(doc['meshes'][0]['primitives'])==1 and not doc.get('extensionsRequired'),p
    assert not any('uri' in x for key in ['images','buffers'] for x in doc[key]),p
    assert acc[3]['count']//3<750,p
    def arr(i):
        a=acc[i];view=doc['bufferViews'][a['bufferView']];width={'VEC2':2,'VEC3':3,'VEC4':4,'SCALAR':1}[a['type']]
        fmt='f' if a['componentType']==5126 else 'I'
        vals=struct.unpack_from('<'+fmt*(width*a['count']),raw,start+view.get('byteOffset',0)+a.get('byteOffset',0))
        return [vals[j:j+width] for j in range(0,len(vals),width)]
    pos,colors,uv,indices,normals=[arr(i) for i in range(5)]
    assert all(math.isfinite(v) for row in pos+colors+uv+normals for v in row),p
    assert all(0<=v<=1 for row in colors+uv for v in row),p
    assert all(.98<sum(t*t for t in row)<1.02 for row in normals),p
    assert all(i[0]<len(pos) for i in indices),p
    family=p.stem.rsplit('-lod',1)[0];lod=int(p.stem[-1]);counts[(family,lod)]=acc[3]['count']//3
    oldversion=127 if family.startswith('fall-') else 126
    base=ROOT/f'app/src/main/assets/3d/field/v{oldversion}/{family}-lod0.glb'
    if base.exists():
        old=base.read_bytes();nn=struct.unpack_from('<I',old,12)[0];old_acc=json.loads(old[20:20+nn])['accessors'][0]
        assert all(acc[0]['min'][i]>=old_acc['min'][i]-.0001 and acc[0]['max'][i]<=old_acc['max'][i]+.0001 for i in [0,2]),(p,acc[0],old_acc)
    if family.startswith('fall-'):
        panel=3 if family=='fall-hukou' else 5;heights=[]
        for t in [.34,.50]:
            values=[pos[i][1] for i,u in enumerate(uv) if panel/8<u[0]<(panel+1)/8 and abs(u[1]-(.06+.88*t))<.00001]
            assert values,(family,lod,t);heights.append((min(values),max(values)))
        assert heights[0][0]-heights[1][1]>.93,(family,lod,heights)
    if family=='wall-earth':
        assert any(all(abs(row[i]-[.98,.87,.69][i])<.00001 for i in range(3)) for row in colors),p
    report['assets'].append({'id':p.stem,'triangles':acc[3]['count']//3,'sha256':hashlib.sha256(raw).hexdigest(),'strict_subset':'PASS','bounds':acc[0]['min']+acc[0]['max']})
assert len(report['assets'])==20
for family,lod in counts:
    if lod==0:assert counts[(family,1)]<counts[(family,0)],family
old=Image.open(ROOT/'app/src/main/assets/3d/field/v127/scenery-atlas.png').convert('RGBA');new=Image.open(folder/'scenery-atlas.png').convert('RGBA');assert new.size==(512,64)
for i in [1,2,6,7]:assert old.crop((i*64,0,(i+1)*64,64)).tobytes()==new.crop((i*64,0,(i+1)*64,64)).tobytes(),i
assert new.getpixel((288,12))[2]<110 and new.getpixel((288,52))[2]>120
report['checks']={k:'PASS' for k in ['strict_single_primitive_subset','no_external_uris','finite_normals_uv_colors','triangles_under_750','far_LOD_smaller','atlas_512x64','panels_1_2_6_7_pixel_identical','runtime_panel4_dark_rock_pale_foam','normalized_lip_034_foot_050','wall_vertex_anchor','legacy_XZ_envelope']}
REPORT.parent.mkdir(parents=True,exist_ok=True);REPORT.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'models':len(report['assets']),'checks':report['checks']}))
