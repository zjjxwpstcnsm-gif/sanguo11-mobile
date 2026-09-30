#!/usr/bin/env python3
"""Blender BVH geometry-change check. Pure subdivision of an unchanged surface
would have zero point-to-old-surface distance (within numeric roundoff).
This is evidence of changed form, not a substitute for visual/art acceptance.
blender -b --python-exit-code 1 --python tools/3d/analyze_landmarks129.py -- OUTPUT
"""
import bpy,struct,json,math,sys
from pathlib import Path
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [];OUT=Path(args[0]).resolve() if args else ROOT
folder=OUT/'app/src/main/assets/3d/field';records=[]
def mesh(path):
 raw=path.read_bytes();n=struct.unpack_from('<I',raw,12)[0];d=json.loads(raw[20:20+n]);start=28+n
 def array(i,fmt,width):
  a=d['accessors'][i];v=d['bufferViews'][a['bufferView']];vals=struct.unpack_from('<'+fmt*width*a['count'],raw,start+v.get('byteOffset',0));return [vals[k:k+width] for k in range(0,len(vals),width)]
 p=array(0,'f',3);ix=[x[0] for x in array(3,'I',1)];return p,[ix[k:k+3] for k in range(0,len(ix),3)]
for path in sorted((folder/'v129').glob('*-lod0.glb')):
 new,nt=mesh(path);old,ot=mesh(folder/'v128'/path.name);bvh=BVHTree.FromPolygons(old,ot,all_triangles=True)
 points=set(new);dist=sorted(bvh.find_nearest(p)[3] for p in points);diag=math.sqrt(sum((max(p[k] for p in new)-min(p[k] for p in new))**2 for k in range(3)))
 ratio=sum(d>diag*.003 for d in dist)/len(dist)
 assert ratio>.10,(path.name,ratio)
 records.append({'family':path.stem.removesuffix('-lod0'),'old_triangles':len(ot),'new_triangles':len(nt),'new_unique_positions':len(points),'surface_distance_mean':sum(dist)/len(dist),'surface_distance_p90':dist[int(len(dist)*.9)],'bounding_diagonal':diag,'fraction_positions_over_0_3_percent_diagonal_from_old_surface':ratio,'unchanged_surface_subdivision_rejected':'PASS'})
report={'kind':'BVH_POINT_TO_PREVIOUS_SURFACE_NOT_VISUAL_ACCEPTANCE','blender':bpy.app.version_string,'scope':'LOD0 geometry; pure subdivision would remain on old surface. Changed silhouette/crevices are verified by authored source and offline renders, not this metric alone.','assets':records}
p=OUT/'out/feedback129/blender/geometry-change.json';p.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
