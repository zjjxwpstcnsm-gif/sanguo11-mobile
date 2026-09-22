#!/usr/bin/env python3
"""Offline geometry inspection only: not an installed game screenshot."""
import json,struct,numpy as np
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];assets=ROOT/'app/src/main/assets/3d/field';out=ROOT/'app/build/field-review';out.mkdir(parents=True,exist_ok=True)
tex=np.array(Image.open(assets/'atlas.png'))/255
for family,files in [('facilities',sorted(assets.glob('*-1-lod0.glb'))),('units',sorted(assets.glob('unit-*-lod0.glb')))]:
 cols=5;rows=(len(files)+cols-1)//cols;fig=plt.figure(figsize=(18,rows*3.2),facecolor='#20272c')
 for n,file in enumerate(files):
  b=file.read_bytes();jl=struct.unpack_from('<I',b,12)[0];j=json.loads(b[20:20+jl]);start=28+jl;arrays=[]
  for a in j['accessors']:
   v=j['bufferViews'][a['bufferView']];w={'VEC3':3,'VEC4':4,'VEC2':2,'SCALAR':1}[a['type']]
   arrays.append(np.frombuffer(b,dtype='<f4' if a['componentType']==5126 else '<u4',count=a['count']*w,offset=start+v['byteOffset']).reshape(-1,w))
  p,c,uv,idx=arrays;idx=idx.reshape(-1,3);t=uv[idx].mean(axis=1);colors=tex[np.clip((t[:,1]*64).astype(int),0,63),np.clip((t[:,0]*512).astype(int),0,511)]*c[idx,:3].mean(axis=1)
  ax=fig.add_subplot(rows,cols,n+1,projection='3d',facecolor='#20272c');ax.add_collection3d(Poly3DCollection(p[idx][:,:,[0,2,1]],facecolors=np.clip(colors,0,1),linewidths=0))
  span=max(np.ptp(p[:,0]),np.ptp(p[:,2]),np.ptp(p[:,1]))*.6
  ax.set_xlim(-span,span);ax.set_ylim(-span,span);ax.set_zlim(0,span*1.6);ax.view_init(28,-58);ax.set_box_aspect((1,1,.8));ax.set_axis_off();ax.set_title(file.stem.replace('-lod0','').replace('domestic-','').replace('military-','').replace('unit-',''),color='#eee5cf',fontsize=11)
 fig.suptitle('OFFLINE GLB geometry inspection / original CC0 assets — NOT game screenshots',color='white',fontsize=14);fig.tight_layout();fig.savefig(out/(family+'.png'),dpi=130);plt.close(fig)
print(out)
