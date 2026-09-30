#!/usr/bin/env python3
"""Original authored high-detail landmark meshes; Blender 4.3+, strict GLB bake.
Run blender -b --python-exit-code 1 --python tools/3d/refine_blender129.py -- OUTPUT
Real eroded surfaces, crevices, battered support, continuous water and bent reeds.
No generic subdivision modifier; near topology samples the actual sculpted form.
Y up/+Z downstream. Atlas is byte-identical v128; PNG row0 means runtime UV.v=0.
"""
import bpy,math,json,hashlib,sys,shutil,time,struct
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]).resolve() if args else ROOT
ART=OUT/'out/feedback129/blender';ART.mkdir(parents=True,exist_ok=True)
ATLAS=OUT/'app/src/main/assets/3d/field/v129/scenery-atlas.png';ATLAS.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/'app/src/main/assets/3d/field/v128/scenery-atlas.png',ATLAS)
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
source=(ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0]
exec(compile(source.replace('original CC0 v121 subset bake','original CC0 v129 subset bake'),'strict subset bake','exec'),ns)
ns['OUT']=OUT
# Reuse the basic authoring container, with its original coarse source as a named
# baseline only. The family constructors below replace its actual geometry.
old=(ROOT/'tools/3d/refine_blender128.py').read_text()
exec(compile(old[old.index('class Sculpt:'):old.index('families=')],'v128 low-level authoring helpers','exec'))
REPORT=[]

def export_indexed(obj,path,desc):
    """Exact-value weld full attribute tuples, not positional welding.
    Hard fracture normals and color/UV seams remain distinct. GLB still uses
    the established accessor order, FLOAT attributes, uint32 indices, 1 draw.
    """
    started=time.process_time();bpy.context.view_layer.objects.active=obj
    dg=bpy.context.evaluated_depsgraph_get();e=obj.evaluated_get(dg);m=e.to_mesh();m.calc_loop_triangles()
    pos=[];colors=[];uvs=[];normals=[];indices=[];seen={}
    attr=m.color_attributes['Color'];layer=m.uv_layers.active
    for tr in m.loop_triangles:
        if tr.area<1e-12:continue
        for li in tr.loops:
            vi=m.loops[li].vertex_index;v=m.vertices[vi]
            p=tuple(v.co);c=tuple(min(1,max(0,x)) for x in attr.data[vi].color)
            uv=tuple(layer.data[li].uv);n=tuple((v.normal if m.polygons[tr.polygon_index].use_smooth else tr.normal).normalized())
            key=struct.pack('<12f',*(p+c+uv+n))
            idx=seen.get(key)
            if idx is None:
                idx=len(pos);seen[key]=idx;pos.append(p);colors.append(c);uvs.append(uv);normals.append(n)
            indices.append(idx)
    data=b'';views=[];access=[]
    for arr,fmt,typ,shape in [(pos,'f',5126,'VEC3'),(colors,'f',5126,'VEC4'),(uvs,'f',5126,'VEC2'),(indices,'I',5125,'SCALAR'),(normals,'f',5126,'VEC3')]:
        flat=arr if shape=='SCALAR' else [x for row in arr for x in row]
        block=struct.pack('<'+fmt*len(flat),*flat);views.append(dict(buffer=0,byteOffset=len(data),byteLength=len(block)));data+=block
        a=dict(bufferView=len(views)-1,componentType=typ,count=len(arr),type=shape)
        if not access:a.update(min=[min(p[k] for p in pos) for k in range(3)],max=[max(p[k] for p in pos) for k in range(3)])
        access.append(a)
    png=ATLAS.read_bytes();views.append(dict(buffer=0,byteOffset=len(data),byteLength=len(png)));data+=png;data+=b'\0'*(-len(data)%4)
    doc=dict(asset=dict(version='2.0',generator='Blender '+bpy.app.version_string+' / original CC0 v129 strict indexed bake'),scene=0,scenes=[dict(nodes=[0])],nodes=[dict(mesh=0)],meshes=[dict(primitives=[dict(attributes={'POSITION':0,'COLOR_0':1,'TEXCOORD_0':2,'NORMAL':4},indices=3,material=0,mode=4)])],buffers=[dict(byteLength=len(data))],bufferViews=views,accessors=access,images=[dict(bufferView=5,mimeType='image/png')],textures=[dict(source=0)],materials=[dict(doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=0,roughnessFactor=1))])
    js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4)
    raw=struct.pack('<III',0x46546c67,2,28+len(js)+len(data))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(data),0x004e4942)+data
    path.write_bytes(raw);e.to_mesh_clear()
    REPORT.append(dict(path=str(path.relative_to(OUT)),id=path.stem,sha256=hashlib.sha256(raw).hexdigest(),bytes=len(raw),triangles=len(indices)//3,vertices=len(pos),primitives=1,bounds=access[0]['min']+access[0]['max'],source=desc,tool='Blender '+bpy.app.version_string,license='CC0-1.0 original project geometry; unchanged v128 atlas',axes='+Y up / +Z forward',pivot=[0,0,0],export_cpu_seconds=time.process_time()-started,unindexed_vertex_bytes=len(indices)*48,indexed_vertex_bytes=len(pos)*48,index_bytes=len(indices)*4))

def blend(a,b,t):return a*(1-t)+b*t
def tint(c,k):return tuple(min(1,max(.02,a*k)) for a in c)
def g(x,w):return math.exp(-(x/w)**2)

def stone(s,cx,cz,sx,sz,h,col=(.81,.87,.76),panel=0,seed=0,lod=0,base=0,kind='rock',sides=None,shallow=False):
    """Five unequal bevel/cleavage bands and an off-centre broken crown.
    Superellipse shoulders, inset vertical joints and buried broad footing are
    authored at every ring; these are not a subdivided unchanged primitive.
    """
    n=sides or (16 if lod==0 else 8)
    bands=[(0,.91),(.11,1),(.33,.92),(.59,.85),(.82,.65),(1,.25)] if lod==0 else [(0,.92),(.28,1),(.70,.79),(1,.24)]
    if shallow and not lod:bands=[(0,.91),(.24,1),(.65,.78),(1,.22)]
    start=len(s.p)
    for j,(t,r) in enumerate(bands):
        for k in range(n):
            a=k*math.tau/n;ca=math.cos(a);sa=math.sin(a)
            xx=math.copysign(abs(ca)**.66,ca);zz=math.copysign(abs(sa)**.69,sa)
            split=.11*g(math.sin(a-.8-seed*.5),.14)*math.sin(math.pi*t)**.5
            r1=r*(1+.065*math.sin(a*3+seed)+.025*math.sin(a*7+seed*2))-split
            y=base+h*t*(.96+.035*math.sin(a*3+seed))
            x=cx+sx*.5*xx*r1+sx*.08*t*math.sin(seed);z=cz+sz*.5*zz*r1+sz*.05*t*math.cos(seed)
            c=tint(col,.90+.085*math.sin(a*2+seed)+.06*t)
            s.v(x,y,z,c,panel,k/n,.11+.40*t)
    for j in range(len(bands)-1):
        for k in range(n):
            a=start+j*n+k;b=start+j*n+(k+1)%n;s.face([a,b,b+n,a+n],kind=='round')
    center=s.v(cx+sx*.04,base+h*.985,cz-sz*.03,tint(col,1.05),panel,.45,.49)
    for k in range(n):s.face([start+(len(bands)-1)*n+k,start+(len(bands)-1)*n+(k+1)%n,center],kind=='round')

# Authored meshed cliff shelf under waterfall: it is broad, worn and battered,
# with genuine recessed strata. Side face leans from a narrow lip to talus foot.
def waterfall(name,lod):
    s=Sculpt();hukou=name=='fall-hukou';b=.34 if name=='fall-narrow' else .46;panel=3 if hukou else 5
    cols=18 if lod==0 else 7
    rows=[0,.065,.13,.205,.27,.315,.34,.355,.371,.389,.409,.430,.452,.475,.50,.532,.575,.64,.72,.81,.90,1] if lod==0 else [0,.15,.27,.34,.39,.445,.50,.58,.77,1]
    def half(t):return b*((.43-.24*g(t-.37,.20)+.055*t) if hukou else (.47-.055*math.sin(t*math.pi)))
    for t in rows:
        fall=max(0,min(1,(t-.34)/.16));drop=1 if t<=.34 else max(.015,1-fall)
        for k in range(cols+1):
            u=k/cols;edge=1+.037*math.sin(t*27)+.018*math.sin(t*59+1)
            x=(2*u-1)*half(t)*edge+b*.014*math.sin(t*25+u*7)*math.sin(u*math.pi)
            # Physical folds swell out from curtain then meet a continuous foot.
            wave=math.sin(math.pi*fall);fold=.5+.5*math.sin(u*36+t*13)
            y=drop+.0018*math.sin(u*23+t*31)+.020*wave*(fold-.35)
            z=t+.036*wave*(.35+.65*fold)
            if t<.13:z+=.019*(1-t/.13)*(.5+.5*math.sin(u*21))
            if t>.81:z-=(t-.81)/.19*(.025+.053*(.5+.5*math.cos(u*16+.6)))
            c=(.93,.86,.72) if hukou else (.88,.98,1)
            s.v(x,y,z,tint(c,.95+.05*fold),panel,u,t)
    for j in range(len(rows)-1):
        for k in range(cols):
            a=j*(cols+1)+k;s.face([a,a+1,a+cols+2,a+cols+1],True)
    # Top bed and perimeter share vertices, eliminating disconnected pillars.
    nx=10 if lod==0 else 5;nz=4 if lod==0 else 2;start=len(s.p)
    for j in range(nz+1):
        z=.005+.324*j/nz
        for k in range(nx+1):
            u=k/nx;ext=b*(.48+.095*math.sin(j/nz*math.pi));x=(2*u-1)*ext
            s.v(x,.975+.008*math.sin(u*11+j*.6),z,(.99,.85,.61) if hukou else (.88,.93,.81),4,u,.13+.20*j/nz)
    for j in range(nz):
        for k in range(nx):
            a=start+j*(nx+1)+k;s.face([a,a+1,a+nx+2,a+nx+1])
    edge=[start+k for k in range(nx+1)]+[start+j*(nx+1)+nx for j in range(1,nz+1)]+[start+nz*(nx+1)+k for k in range(nx-1,-1,-1)]+[start+j*(nx+1) for j in range(nz-1,0,-1)]
    rings=[1,.88,.77,.65,.54,.41,.26,.11,0] if lod==0 else [1,.70,.38,0]
    top=[s.p[a] for a in edge];nn=len(edge);wallstart=len(s.p);col=(.96,.77,.47) if hukou else (.82,.89,.73)
    for j,t in enumerate(rings):
        for k,(x,yy,z) in enumerate(top):
            # Tall face is narrower above and lower terraces flare organically.
            broad=1.10-.24*t+.055*math.sin(t*math.pi*4+k*.45)*(1-t)
            xx=x*broad;zz=z+(.119*(1-t)**1.4 if z>.30 else -.023*(1-t))
            joint=.041*g(math.sin(k*.62+t*.7),.32)*math.sin(math.pi*t)
            xx*=1-joint*3
            if z>.30:zz-=joint
            y=yy*t+.011*math.sin(k*1.37+t*15)*math.sin(math.pi*t)
            c=tint(col,.83+.13*t-.08*math.sin(k*.65+t*9)**2)
            s.v(xx,y,zz,c,4,k/nn,.10+.43*t)
    # Connect bed outer edge to the inset lip as a chipped, shallow bevel.
    for k in range(nn):s.face([edge[k],edge[(k+1)%nn],wallstart+(k+1)%nn,wallstart+k])
    for j in range(len(rings)-1):
        for k in range(nn):
            a=wallstart+j*nn+k;c=wallstart+j*nn+(k+1)%nn;s.face([a,c,c+nn,a+nn])
    # Unequal foot boulders visually root the shelf without exceeding footprint.
    for i,(x,z,sx,sz,hh) in enumerate([(-.36,.326,.30,.17,.28),(.37,.329,.28,.17,.22),(-.20,.38,.23,.14,.115),(.23,.40,.19,.13,.085)]):
        if lod and i>1:continue
        stone(s,x*b,z,sx*b,sz,hh,col,4,8+i,1 if lod or i>1 else 0)
    # Two unequal blade-like bedrock splinters break the otherwise planar sides.
    for i,(x,z,hh) in enumerate([(-.49,.15,.63),(.48,.205,.48)]):
        stone(s,x*b,z,b*.24,.155,hh,col,4,i+11,1)
    # Solid foam is five shallow overlapping organic lobes, not vertical spikes.
    foam=(.99,.90,.71) if hukou else (.93,.99,1)
    for i,(x,z,sx,sz,hh) in enumerate([(-.29,.540,.28,.080,.021),(-.095,.566,.38,.11,.030),(.17,.550,.37,.09,.026),(.33,.580,.25,.083,.019),(.03,.624,.36,.090,.015)]):
        if lod and i in [0,3]:continue
        at=len(s.p);stone(s,x*b,z,sx*b,sz,hh,foam,4,i,1 if lod else 0,base=.010,kind='round',sides=8 if lod else 12,shallow=True)
        for k in range(at,len(s.p)):
            t=max(0,min(1,(s.p[k][1]-.010)/hh));s.uv[k]=((4.10+.70*(.5+.5*math.sin(k*1.7)))/8,.80+.075*t);s.c[k]=(*tint(foam,.90+.10*t),1)
    return s

def wall(lod):
    s=Sculpt();n=26 if lod==0 else 10;layers=9 if lod==0 else 3
    def crest(z):return .201+.010*math.sin(z*9)+.006*math.sin(z*37)-.027*g(z-.71,.037)-.014*g(z-.22,.025)
    for side in [-1,1]:
        start=len(s.p)
        for j in range(layers+1):
            t=j/layers
            for k in range(n+1):
                z=k/n
                fiss=sum(.006*g(z-(c+.018*math.sin(t*4+q)),.014) for q,c in enumerate([.17,.45,.73,.89]))*math.sin(math.pi*t)
                buttress=.007*g(z-.35,.07)*(1-t)**2
                x=side*(.053-.022*t-fiss+buttress+.0017*math.sin(t*40+z*5)*math.sin(math.pi*t))+.003*math.sin(z*9)
                y=t*crest(z)+.0015*math.sin(z*41+t*6)*math.sin(math.pi*t)
                c=(.98,.87,.69) if j==0 and k==0 else tint((.96,.78,.51),.94+.055*math.sin(t*33+z*3)-fiss*11)
                s.v(x,y,z,c,0,z,t)
        for j in range(layers):
            for k in range(n):
                a=start+j*(n+1)+k;ids=[a,a+1,a+n+2,a+n+1];s.face(ids if side==1 else ids[::-1],True)
    stride=(layers+1)*(n+1)
    for k in range(n):a=layers*(n+1)+k;b=a+stride;s.face([a,b,b+1,a+1],True)
    for k in [0,n]:s.face([j*(n+1)+k for j in range(layers+1)]+[stride+j*(n+1)+k for j in reversed(range(layers+1))])
    for i,z in enumerate([.16,.37,.70,.88] if not lod else [.16,.70]):stone(s,.049,z,.024,.055,.018+.009*(i%2),(.89,.72,.49),0,2+i,1)
    return s

def beacon(lod):
    s=Sculpt();n=28 if not lod else 12;levels=12 if not lod else 4
    # Squared battered rammed earth shell. Four corners remain broad chamfers;
    # rain channels and compressed earth bands are geometric recesses.
    for j in range(levels+1):
        t=j/levels;span=.108-.049*t
        for k in range(n):
            a=math.tau*k/n;ca=math.cos(a);sa=math.sin(a)
            xx=math.copysign(abs(ca)**.36,ca);zz=math.copysign(abs(sa)**.36,sa)
            seam=.04*g(math.sin(a*4+.3+t*.5),.22)*math.sin(math.pi*t)
            rad=1-seam+.015*math.sin(t*41+a*2)*math.sin(math.pi*t)
            y=.338*t+.0018*math.sin(a*5)*t
            s.v(span*xx*rad+.002*t*math.sin(a),y,span*zz*rad,tint((.98,.79,.52),.93+.055*math.sin(t*42+a)-seam),0,k/n,t)
    for j in range(levels):
        for k in range(n):a=j*n+k;b=j*n+(k+1)%n;s.face([a,b,b+n,a+n],True)
    # Visible inset top basin: ramped inner lip, soot bed and collapsed rim.
    start=len(s.p)
    for j,(rad,y) in enumerate([(.060,.338),(.047,.328),(.037,.327)]):
        for k in range(n):
            a=math.tau*k/n;ca=math.cos(a);sa=math.sin(a)
            s.v(rad*math.copysign(abs(ca)**.36,ca),y,rad*math.copysign(abs(sa)**.36,sa),(.56,.49,.35) if j else (.91,.72,.47),0,k/n,.40)
    for j in range(2):
        for k in range(n):a=start+j*n+k;b=start+j*n+(k+1)%n;s.face([a,b,b+n,a+n])
    s.face([start+2*n+k for k in range(n)])
    for i,(x,z,sx,sz,hh) in enumerate([(-.056,-.003,.018,.11,.048),(.055,.018,.017,.079,.040),(.005,-.053,.10,.018,.031)]):stone(s,x,z,sx,sz,hh,(.97,.76,.48),0,i,1 if lod else 0,.336)
    for i in range(3 if not lod else 1):stone(s,(i-1)*.013,.005,.010,.052,.009,(.32,.28,.19),0,i,1,.331)
    return s

def granite(lod):
    s=Sculpt();nx=24 if not lod else 8;nz=16 if not lod else 5
    for j in range(nz+1):
        v=j/nz
        for k in range(nx+1):
            u=k/nx;x=(u-.5)*.46;z=(v-.5)*.325
            # Unequal overlapping shoulders, carved crossing fracture gullies.
            ridge=.225+.111*g(u-(.29+.23*v),.36)+.035*g(u-.80,.20)
            edge=1-.32*abs(2*v-1)**2-.18*abs(2*u-1)**2
            cut=.067*g(u-(.53+.050*math.sin(v*8)),.035)+.036*g(v-(.31+.07*u),.032)
            y=max(.075,ridge*edge-cut+.008*math.sin(u*21+v*9)*math.sin(v*13))
            xx=x*(1-.21*abs(2*v-1)**3);zz=z*(1-.23*abs(2*u-1)**3)
            col=tint((.82,.86,.81),.87+.10*y/.34-.22*min(1,cut/.08))
            if cut>.026:col=(col[0]*.84,col[1]*.98,col[2]*.80)
            s.v(xx,y,zz,col,0,u,.32+.18*v)
    for j in range(nz):
        for k in range(nx):
            a=j*(nx+1)+k;s.face([a,a+1,a+nx+2,a+nx+1],True)
    edge=[k for k in range(nx+1)]+[j*(nx+1)+nx for j in range(1,nz+1)]+[nz*(nx+1)+k for k in range(nx-1,-1,-1)]+[j*(nx+1) for j in range(nz-1,0,-1)]
    prev=edge;top=[s.p[i] for i in edge];nn=len(edge)
    for j,t in enumerate([.75,.47,.23,0] if not lod else [.42,0]):
        row=[]
        for k,(x,y,z) in enumerate(top):
            scale=1+.10*(1-t);crevice=.006*g(math.sin(k*.47+t*2),.21)*math.sin(math.pi*t)
            row.append(s.v(x*scale-crevice*math.copysign(1,x),y*t,z*scale,(.73,.79,.72),0,k/nn,.15+.33*t))
        for k in range(nn):s.face([prev[k],prev[(k+1)%nn],row[(k+1)%nn],row[k]])
        prev=row
    for i,(x,z,sx,sz,hh) in enumerate([(-.18,.08,.135,.13,.13),(.17,-.08,.145,.14,.14),(.16,.11,.14,.10,.095)]):stone(s,x,z,sx,sz,hh,(.76,.80,.72),0,i,1)
    return s

def sandstone(lod):
    s=Sculpt();n=40 if not lod else 12;layers=14 if not lod else 4
    outline=[(-.25,-.11),(-.16,-.175),(.06,-.175),(.22,-.11),(.262,.025),(.19,.15),(.015,.183),(-.17,.15),(-.250,.064),(-.255,-.022)]
    for j in range(layers+1):
        t=j/layers
        for k in range(n):
            q=k/n*len(outline);i=int(q);f=q-i;x=blend(outline[i][0],outline[(i+1)%len(outline)][0],f);z=blend(outline[i][1],outline[(i+1)%len(outline)][1],f)
            cleft=.110*g(math.sin(k/n*math.tau*3+t*.4),.21)*math.sin(math.pi*t)
            ledge=(.013+.008*math.sin(k/n*17))*math.sin(t*math.pi*9+k*.11)+.008*math.sin(t*math.pi*19)
            scale=1-.26*t+ledge-cleft
            y=.285*t+.012*math.sin(k/n*19+t*1.5)*t
            s.v(x*scale+.021*t,y,z*scale,tint((.98,.82,.60),.91+.06*math.sin(t*44)-cleft),0,k/n,t)
    for j in range(layers):
        for k in range(n):a=j*n+k;b=j*n+(k+1)%n;s.face([a,b,b+n,a+n])
    # Three inward terraces, not a radial flat cap.
    prev=[layers*n+k for k in range(n)]
    for r in [.72,.39]:
        row=[]
        for k in range(n):
            x,y,z=s.p[layers*n+k];row.append(s.v(x*r+.012*(1-r),.287+.007*math.sin(k/n*15)*r,z*r,(.97,.84,.64),0,k/n,.50))
        for k in range(n):s.face([prev[k],prev[(k+1)%n],row[(k+1)%n],row[k]],True)
        prev=row
    center=s.v(.012,.287,0,(.94,.80,.60),0,.5,.5)
    for k in range(n):s.face([prev[k],prev[(k+1)%n],center],True)
    for i,(x,z,sx,sz,hh) in enumerate([(-.15,.10,.13,.13,.085),(.16,.07,.16,.13,.09),(-.17,-.095,.12,.09,.075)]):stone(s,x,z,sx,sz,hh,(.91,.75,.52),0,i,1)
    return s

def karst(lod):
    s=Sculpt();n=18 if not lod else 8;levels=12 if not lod else 4
    for seed,(cx,cz,sx,sz,h) in enumerate([(-.095,-.030,.255,.250,.450),(.112,-.025,.255,.238,.342),(-.020,.098,.315,.188,.255)]):
        start=len(s.p)
        for j in range(levels+1):
            t=j/levels;profile=1-.31*t-.48*t**6
            for k in range(n):
                a=k/n*math.tau;flute=.230*g(math.sin(a*3+seed+t*.55),.34)*math.sin(math.pi*t)
                r=profile*(1+.045*math.sin(a*3+seed+t*2))-flute+.033*g(t-.31,.043)
                x=cx+math.cos(a)*sx*.5*r+.018*t*math.sin(seed+1);z=cz+math.sin(a)*sz*.5*r
                y=h*t*(.98+.020*math.sin(a*3+seed))
                shade=.94+.045*math.cos(a*3)-1.35*flute
                col=tint((.83,.88,.76),shade)
                if .23<t<.35 or .64<t<.75:col=tint((.65,.78,.57),shade)
                s.v(x,y,z,col,0,k/n,.10+.46*t)
        for j in range(levels):
            for k in range(n):a=start+j*n+k;b=start+j*n+(k+1)%n;s.face([a,b,b+n,a+n],k%3!=0)
        crown=s.v(cx+.014*math.sin(seed+1),h,cz,(.83,.88,.76),0,.5,.51)
        for k in range(n):s.face([start+levels*n+k,start+levels*n+(k+1)%n,crown],True)
    for i,(x,z,sx,sz,hh) in enumerate([(-.09,.075,.19,.16,.083),(.16,.085,.17,.13,.13)]):stone(s,x,z,sx,sz,hh,(.71,.81,.63),0,i,1)
    return s

def shore_rock(lod):
    s=Sculpt()
    for i,(x,z,sx,sz,hh) in enumerate([(-.075,.005,.260,.20,.075),(.092,-.021,.220,.16,.055),(.03,.079,.13,.11,.04),(-.13,-.058,.095,.077,.025),(.148,.053,.084,.073,.027)]):
        stone(s,x,z,sx,sz,hh,(.83,.91,.79),0,i+2,1 if lod else 0,kind='round')
    return s

def reeds(lod):
    s=Sculpt();count=19 if not lod else 8
    for i in range(count):
        a=i*2.39996;r=.015+.087*((i*7+3)%count)/count;cx=math.cos(a)*r;cz=math.sin(a)*r*.67
        h=.062+.060*(.5+.5*math.sin(i*2.7));dx=.017*math.sin(i*1.7);dz=.016*math.cos(i*1.3)
        sides=4 if not lod else 3;seg=5 if not lod else 2;start=len(s.p)
        for j in range(seg+1):
            t=j/seg;rad=.0015*(1-.78*t)
            for k in range(sides):
                aa=k*math.tau/sides;s.v(cx+dx*t*t+rad*math.cos(aa),h*t,cz+dz*t*t+rad*math.sin(aa),(.61+.11*t,.74-.07*t,.37-.03*t),6,.32,.42)
        for j in range(seg):
            for k in range(sides):
                a0=start+j*sides+k;bb=start+j*sides+(k+1)%sides;s.face([a0,bb,bb+sides,a0+sides],True)
        # Bent lanceolate leaves have a midrib ridge, curl and narrowing tip.
        for side in [-1,1]:
            t=.32 if side==1 else .59;y=h*t;x=cx+dx*t*t;z=cz+dz*t*t;prev=None
            steps=4 if not lod else 2
            for j in range(steps+1):
                q=j/steps;wide=.0038*math.sin(math.pi*q)+.00015
                xx=x+side*.029*q;yy=y+.023*math.sin(q*1.9);zz=z+side*.012*q
                row=[s.v(xx,yy,zz-wide,(.59,.74,.36),6,.32,.42),s.v(xx,yy+.0015*math.sin(math.pi*q),zz,(.72,.81,.40),6,.32,.42),s.v(xx,yy,zz+wide,(.58,.73,.34),6,.32,.42)]
                if prev:
                    s.face([prev[0],row[0],row[1],prev[1]],True);s.face([prev[1],row[1],row[2],prev[2]],True)
                prev=row
        if i%4==0:
            stone(s,cx+dx,cz+dz,.0045,.0045,.017,(.72,.62,.36),6,i,1,base=h-.017,kind='round')
    return s

families={'fall-narrow':waterfall,'fall-wide':waterfall,'fall-hukou':waterfall,'wall-earth':wall,'beacon-han':beacon,'cliff-sandstone':sandstone,'cliff-granite':granite,'cliff-karst':karst,'shore-reeds':reeds,'shore-rock':shore_rock}
budgets={'fall-narrow':2500,'fall-wide':2500,'fall-hukou':2500,'wall-earth':1300,'beacon-han':1600,'cliff-sandstone':1800,'cliff-granite':1600,'cliff-karst':1600,'shore-reeds':1700,'shore-rock':1100}
details={'fall-narrow':'18-column continuous folded water; battered nine-band bedrock; inset fractures; four foot boulders plus two side splinters; five shallow volumetric foam lobes','fall-wide':'18-column continuous folded water; battered nine-band bedrock; inset fractures; four foot boulders plus two side splinters; five shallow volumetric foam lobes','fall-hukou':'pinched sediment-laden channel; 18-column folded curtain; nine-band stratified eroded bedrock; shallow opaque froth relief','wall-earth':'26 longitudinal samples, nine compressed-earth courses, raincut gullies and broken crest; four grounded spalls','beacon-han':'28-sided squared battered body with twelve eroded earth bands; inset hearth basin; broken uneven rim and fuel bed','cliff-sandstone':'forty-point irregular plan; fourteen undercut sedimentary courses; recessed vertical fissures; terrace cap; detached talus','cliff-granite':'24x16 continuous unequal ridge surface with deep crossing gullies; four battered perimeter courses and broken foot blocks','cliff-karst':'three unequal limestone shoulders with twelve authored flute bands and rounded eroded summits; moss ledges and talus','shore-reeds':'19 bent tapered stems; thirty-eight curved ridged leaves; five seed heads; opaque one-primitive mesh','shore-rock':'five asymmetric water-rounded rocks with inset joints, bevel shoulders and buried contact bands'}
objects=[];started=time.process_time();envelope_fits={}
def assetbounds(path):
    raw=path.read_bytes();n=struct.unpack_from('<I',raw,12)[0];a=json.loads(raw[20:20+n])['accessors'][0];return a['min'],a['max']

for family,fn in families.items():
    counts=[]
    sculpts=[fn(family,lod) if family.startswith('fall-') else fn(lod) for lod in [0,1]]
    oldversion=127 if family.startswith('fall-') else 126
    baselines=list((ROOT/f'app/src/main/assets/3d/field/v{oldversion}').glob(family+'-lod*.glb'))
    if not baselines:baselines=list((ROOT/'app/src/main/assets/3d/field/v128').glob(family+'-lod*.glb'))
    bounds=[assetbounds(p) for p in baselines]
    scales=[1.,1.,1.]
    for axis in [0,2]:
        lo=min(x[0][axis] for x in bounds);hi=max(x[1][axis] for x in bounds)
        mn=min(p[axis] for s in sculpts for p in s.p);mx=max(p[axis] for s in sculpts for p in s.p)
        if mn<lo and mn<0:scales[axis]=min(scales[axis],lo/mn)
        if mx>hi and mx>0:scales[axis]=min(scales[axis],hi/mx)
    envelope_fits[family]=scales
    for lod,sculpt in enumerate(sculpts):
        sculpt.p=[tuple(p[k]*scales[k] for k in range(3)) for p in sculpt.p]
        obj=sculpt.object(family+'-lod'+str(lod));objects.append(obj)
        export_indexed(obj,ATLAS.parent/(family+'-lod'+str(lod)+'.glb'),details[family]);counts.append(REPORT[-1]['triangles'])
        assert counts[-1] <= (budgets[family] if not lod else 850),(family,lod,counts[-1]);obj.hide_render=True
    assert counts[1]<counts[0]*.45,(family,counts)
# Preview material only: runtime PNG row convention requires explicit V reversal
# in Blender, because Blender images internally use a lower-left pixel origin.
mat=bpy.data.materials.new('v129 atlas x authored vertex color (runtime V orientation)');mat.use_nodes=True
nt=mat.node_tree;nt.nodes.clear();out=nt.nodes.new('ShaderNodeOutputMaterial');bs=nt.nodes.new('ShaderNodeBsdfPrincipled');bs.inputs['Roughness'].default_value=.90
tex=nt.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(ATLAS));tex.interpolation='Linear'
uv=nt.nodes.new('ShaderNodeTexCoord');sep=nt.nodes.new('ShaderNodeSeparateXYZ');comb=nt.nodes.new('ShaderNodeCombineXYZ');flip=nt.nodes.new('ShaderNodeMath');flip.operation='SUBTRACT';flip.inputs[0].default_value=1
nt.links.new(uv.outputs['UV'],sep.inputs[0]);nt.links.new(sep.outputs['X'],comb.inputs['X']);nt.links.new(sep.outputs['Y'],flip.inputs[1]);nt.links.new(flip.outputs[0],comb.inputs['Y']);nt.links.new(comb.outputs[0],tex.inputs['Vector'])
vc=nt.nodes.new('ShaderNodeVertexColor');vc.layer_name='Color';mul=nt.nodes.new('ShaderNodeMixRGB');mul.blend_type='MULTIPLY';mul.inputs[0].default_value=1
nt.links.new(tex.outputs['Color'],mul.inputs[1]);nt.links.new(vc.outputs['Color'],mul.inputs[2]);nt.links.new(mul.outputs[0],bs.inputs['Base Color']);nt.links.new(bs.outputs[0],out.inputs[0])
for o in objects:o.data.materials.append(mat)
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=False
scene.render.resolution_x=1000;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.world.color=(.20,.20,.20);scene.view_settings.view_transform='Standard';scene.view_settings.look='None';scene.view_settings.exposure=0;scene.view_settings.gamma=1
bpy.ops.object.light_add(type='AREA',location=(-3,-4,7));key=bpy.context.object;key.name='Preview only broad key';key.data.energy=600;key.data.shape='DISK';key.data.size=5
bpy.ops.object.light_add(type='AREA',location=(4,2,5));fill=bpy.context.object;fill.name='Preview only soft fill';fill.data.energy=380;fill.data.size=4
bpy.ops.object.camera_add();camera=bpy.context.object;camera.name='Preview camera';scene.camera=camera;camera.data.type='ORTHO'
rawcoll=bpy.data.collections.new('Runtime meshes Y-up (all twenty editable sources)');scene.collection.children.link(rawcoll)
for o in objects:
    for col in list(o.users_collection):col.objects.unlink(o)
    rawcoll.objects.link(o)
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(ART/'landmarks-v129.blend'))
oldmanifest=json.loads((ROOT/'docs/native-pc-visual/feedback-v128-blender-assets.json').read_text())
report={'version':129,'blender':bpy.app.version_string,'generator':'tools/3d/refine_blender129.py','generator_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),'license':'CC0-1.0 original geometry; byte-identical existing v128 atlas','assets':REPORT,'source_anchor_preserving_envelope_scale':envelope_fits,'atlas':{'path':str(ATLAS.relative_to(OUT)),'sha256':hashlib.sha256(ATLAS.read_bytes()).hexdigest(),'bytes':ATLAS.stat().st_size,'dimensions':[512,64],'color_space':'sRGB','runtime_uv':'PNG row zero = UV.v zero; Blender preview explicitly flips V','byte_identical_to_v128':True},'budgets_lod0':budgets,'budget_reason':'User explicitly requested substantive higher-detail geometry; near landmarks generally 1400-2500 triangles, shoreline stones lower, far LOD kept <=850 and less than 45% near. Actual per-family counts below. No arbitrary subdivision modifier. Indexed full-attribute dedup limits CPU/GPU bytes without dissolving normal/UV seams. Device performance remains unmeasured.','editable_source':str((ART/'landmarks-v129.blend').relative_to(OUT)),'reference_links':oldmanifest['reference_links']+['https://www.gamecity.ne.jp/sangokushi/11/playreport/image04/img06.htm','https://www.gamecity.ne.jp/sangokushi/11/playreport/image04/06.jpg','https://store.steampowered.com/app/628070/_/?l=schinese'],'reference_status':'Inspected official KOEI original PC San11 play-report screenshot (480x360): strongly broken gray-brown mountain cliffs, recessed vertical joints, irregular feet. Region/camera UNKNOWN; stylistic/detail-scale reference only. Exact same-camera comparison REFERENCE_MISSING. Commercial screenshot is research only, not included in runtime or redistributable deliverables; no extracted game assets.','verification':'OFFLINE Blender mesh/source evidence; real APK and ARM64 performance require separate validation','generation_export_cpu_seconds':time.process_time()-started}
path=OUT/'docs/native-pc-visual/feedback-v129-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'status':'EXPORTED','models':len(objects),'triangles':sum(x['triangles'] for x in REPORT),'assets':[(x['id'],x['triangles'],x['bytes']) for x in REPORT],'artifact_dir':str(ART)}))
