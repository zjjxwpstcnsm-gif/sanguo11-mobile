#!/usr/bin/env python3
"""Original hand-shaped landscape families, Blender 4.3+, strict one-primitive bake.
Run: blender -b --python-exit-code 1 --python tools/3d/refine_blender128.py -- OUTPUT
Y up, +Z downstream. Preview material flips V to match runtime PNG row-zero UV.
No geometry/textures from the cited photographic references are incorporated.
"""
import bpy, math, json, hashlib, sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
OUT=Path(args[0]).resolve() if args else ROOT
ns={'__file__':str(ROOT/'tools/3d/refine_blender121.py')}
source=(ROOT/'tools/3d/refine_blender121.py').read_text().split('# Original authored swept')[0]
exec(compile(source.replace('original CC0 v121 subset bake','original CC0 v128 subset bake'),'existing strict subset bake','exec'),ns)
ns['OUT']=OUT  # The delegated exporter must use the same resolved output root.
ATLAS=OUT/'app/src/main/assets/3d/field/v128/scenery-atlas.png'
ATLAS.parent.mkdir(parents=True,exist_ok=True)
ART=OUT/'out/feedback128/blender';ART.mkdir(parents=True,exist_ok=True)
BASE=ROOT/'app/src/main/assets/3d/field/v127/scenery-atlas.png'
image=bpy.data.images.load(str(BASE));w,h=image.size
pixels=list(image.pixels[:]);original=pixels[:]
for y in range(h):
    for panel in [0,3,4,5]:
        for x in range(w//8):
            u=x/(w//8-1);v=(h-1-y)/(h-1)
            grain=math.sin(u*153+v*91)*math.sin(u*63-v*147)
            strata=math.sin(v*55+math.sin(u*12)*.5+math.sin(u*29)*.16)
            if panel==0:
                # Fine irregular earthen/mineral laminations: no brick rectangles.
                seam=max(0,math.cos(v*57+math.sin(u*10)*.40))**18
                c=(.59-.037*seam+.015*grain,.58-.035*seam+.014*grain,.525-.032*seam+.013*grain)
            elif panel==4:
                if v<.65:
                    c=(.29+.021*strata+.016*grain,.305+.021*strata+.014*grain,.26+.019*strata+.011*grain)
                else:
                    fleck=.03*grain+.045*math.sin(u*23+v*29)*math.sin(u*41-v*33)
                    c=(.70+fleck,.765+fleck,.71+fleck)
            else:
                curve=u*39+math.sin(v*24+u*11)*1.6+math.sin(v*53)*.3
                vein=(.5+.5*math.sin(curve))**10
                broken=.20+.80*(.5+.5*math.sin(v*43+u*19))
                foam=.34*vein*broken+.021*grain
                curtain=math.exp(-(((v-.06)/.88-.42)/.078)**4)
                base=(.47,.325,.145) if panel==3 else (.18+.40*curtain,.32+.35*curtain,.32+.34*curtain)
                c=tuple(min(.93,max(.02,a+foam)) for a in base)
            i=(y*w+panel*w//8+x)*4;pixels[i:i+4]=[*c,1]
for panel in [1,2,6,7]:
    for y in range(h):
        a=(y*w+panel*w//8)*4;b=a+w//8*4
        assert pixels[a:b]==original[a:b],f'unchanged atlas panel {panel}'
image.pixels=pixels;image.filepath_raw=str(ATLAS);image.file_format='PNG';image.save()

class Sculpt:
    def __init__(self): self.p=[];self.f=[];self.c=[];self.uv=[];self.sm=[]
    def v(self,x,y,z,col=(1,1,1),panel=0,u=.5,v=.5):
        self.p.append((x,y,z));self.c.append((*col,1));self.uv.append(((panel+.06+.88*u)/8,.06+.88*v));return len(self.p)-1
    def face(self,ids,smooth=False):self.f.append(tuple(ids));self.sm.append(smooth)
    def poly(self,pts,col=(1,1,1),panel=0,uv=None,smooth=False):
        ids=[self.v(*pt,col,panel,*(uv[k] if uv else (.12+k*.17,.3))) for k,pt in enumerate(pts)]
        self.face(ids,smooth)
    def block(self,cx,cz,sx,sz,y0,y1,col,panel=0,seed=0,taper=.78):
        # Broken/chamfered rectilinear outline, uneven eroded upper edge.
        plan=[(-.80,-1),(.53,-.96),(1,-.53),(.93,.73),(.42,1),(-.87,.87),(-1,.24),(-1,-.64)]
        rows=[]
        for level,(y,scale) in enumerate([(y0,1),(y1,taper)]):
            row=[]
            for k,(x,z) in enumerate(plan):
                yy=y if not level else y+.025*(y1-y0)*math.sin(k*2.1+seed)
                shade=1-.055*math.sin(k*2+seed)
                row.append(self.v(cx+x*sx*.5*scale,yy,cz+z*sz*.5*scale,tuple(a*shade for a in col),panel,k/8,.18 if not level else .53))
            rows.append(row)
        for k in range(8):self.face([rows[0][k],rows[0][(k+1)%8],rows[1][(k+1)%8],rows[1][k]])
        center=self.v(cx+.07*sx,y1*.998,cz-.05*sz,col,panel,.4,.52)
        for k in range(8):self.face([rows[1][k],rows[1][(k+1)%8],center])
    def lobe(self,cx,cz,sx,sz,height,col,panel=0,seed=1,lod=0,base=0,smooth=False):
        # Author through actual Blender icosphere mesh, then edit each vertex.
        bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2 if lod==0 else 1,radius=1)
        ob=bpy.context.object;start=len(self.p)
        for j,vert in enumerate(ob.data.vertices):
            q=vert.co;noise=1+.09*math.sin(j*2.13+seed)+.045*math.cos(q.z*11+seed)
            yy=max(0,(q.z+.55)/1.55)*height
            moss=yy/height>.66 and math.sin(j*1.9+seed)>.0
            color=tuple(a*(.91+.075*q.z) for a in col) if not moss else (col[0]*.77,col[1]*.90,col[2]*.72)
            self.v(cx+q.x*sx*.5*noise,base+yy,cz+q.y*sz*.5*noise,color,panel,(q.x+1)/2,(q.z+1)*.30)
        for f in ob.data.polygons:self.face([start+i for i in f.vertices],smooth)
        bpy.data.objects.remove(ob,do_unlink=True)
    def object(self,name):
        ob=ns['mesh_object'](name,self.p,self.f,self.c,self.uv)
        for f,sm in zip(ob.data.polygons,self.sm):f.use_smooth=sm
        return ob

def waterfall(name,lod):
    s=Sculpt();hukou=name=='fall-hukou';b=.34 if name=='fall-narrow' else .46;panel=3 if hukou else 5
    cols=10 if lod==0 else 6
    rows=[0,.12,.25,.32,.34,.37,.405,.445,.48,.50,.62,.79,1] if lod==0 else [0,.22,.34,.39,.45,.50,.72,1]
    def half(t):
        if hukou:return b*(.43-.24*math.exp(-((t-.37)/.20)**2)+.055*t)
        return b*(.47-.055*math.sin(t*math.pi))
    def waterpoint(t,u):
        drop=1 if t<=.34 else max(.015,1-(t-.34)/.16)
        bulge=math.sin(math.pi*max(0,min(1,(t-.34)/.16)))
        edge=1+.032*math.sin(t*38+u*11)+.018*math.sin(t*67+u*3)
        x=(2*u-1)*half(t)*edge+b*.018*math.sin(t*18+u*7)*math.sin(u*math.pi)
        # Overlapping continuous convex lobes, with NO parallel open slits.
        y=drop+.002*math.sin(u*27+t*29)+.026*bulge*math.sin(u*27+t*17)
        z=t+.026*bulge*math.sin(u*19+.7)
        if t<.12:z+=.035*(1-t/.12)*(.55+.45*math.sin(u*23+.3))
        if t>.79:z-=(t-.79)/.21*(.03+.068*(.5+.5*math.cos(u*16+.6)))
        return x,y,z
    # A continuous but asymmetric headwater feeds independently broken curtains.
    for t in rows:
        for k in range(cols+1):
            u=k/cols;col=(.91,.88,.79) if hukou else (.90,.985,1)
            if .34<t<.5:
                pale=.07*(.5+.5*math.sin(k*2.1+t*17));col=tuple(min(1,a+pale) for a in col)
            s.v(*waterpoint(t,u),col,panel,u,t)
    for j in range(len(rows)-1):
        for k in range(cols):
            a=j*(cols+1)+k;s.face([a,a+1,a+cols+2,a+cols+1],True)
    # Carved solid bed: full-height support, irregular concave cut face behind water.
    zs=[0,.10,.21,.32] if lod==0 else [0,.17,.32];nc=7 if lod==0 else 4
    start=len(s.p)
    for j,z in enumerate(zs):
        extent=b*(.49+.10*math.sin(z/.32*math.pi))
        for k in range(nc+1):
            u=k/nc;x=(u*2-1)*extent
            y=.976+.010*math.sin(k*1.8+j)
            s.v(x,y,z,(.95,.85,.67) if hukou else (.83,.91,.80),4,u,.12+.29*j/len(zs))
    for j in range(len(zs)-1):
        for k in range(nc):
            a=start+j*(nc+1)+k;s.face([a,a+1,a+nc+2,a+nc+1])
    # Ground-to-lip buttress walls, with split fracture facets rather than a cylinder.
    boundary=[start+k for k in range(nc+1)]
    boundary += [start+j*(nc+1)+nc for j in range(1,len(zs))]
    boundary += [start+(len(zs)-1)*(nc+1)+k for k in range(nc-1,-1,-1)]
    boundary += [start+j*(nc+1) for j in range(len(zs)-2,0,-1)]
    stone=(.99,.82,.52) if hukou else (.83,.91,.79)
    for k,a in enumerate(boundary):
        c=boundary[(k+1)%len(boundary)];pa=s.p[a];pc=s.p[c]
        # Mismatched diagonal breaks and battered lower facets, all grounded.
        def cut(pt,number,level):
            jig=math.sin(number*2.13)
            scale=(1.045+.035*jig) if level else 1.055
            yy=.37+.095*math.sin(number*1.7) if level else 0
            zz=max(0,min(.335,pt[2]+(.010*math.cos(number*2.3) if level else 0)))
            tint=tuple(v*(.84+.12*(.5+.5*math.sin(number*2.6))) for v in stone)
            return s.v(pt[0]*scale,yy,zz,tint,4,number/len(boundary),.17+.15*level)
        midA=cut(pa,k,1);midC=cut(pc,k+1,1);botA=cut(pa,k,0);botC=cut(pc,k+1,0)
        s.face([a,c,midA]);s.face([c,midC,midA]);s.face([midA,midC,botC]);s.face([midA,botC,botA])
    # Grounded oblique shelf outcrops, broad at the foot instead of cuboid pillars.
    for i,(x,z,sx,sz,hh) in enumerate([(-b*.29,.30,b*.36,.14,.43),(b*.30,.30,b*.34,.13,.30)]):
        s.lobe(x,z,sx,sz,hh,(.93,.74,.44) if hukou else (.78,.86,.71),4,10+i,1)
        # Rock UV must stay in panel4's stone rows; generic lobe already v<=.588.
    # Three overlapping, low sculpted froth lobes make a coherent impact cluster.
    # Their bottom is sunk into the stream and their smooth relief is only .03;
    # unlike flat shards they have an irregular rounded volume in side view.
    foam=(.99,.88,.66) if hukou else (.90,.985,.99)
    for i,(x,z,sx,sz,hh) in enumerate([(-.24,.547,.35,.09,.029),(.005,.565,.44,.12,.036),(.27,.547,.32,.085,.025)]):
        start=len(s.p)
        s.lobe(x*b,z,sx*b,sz,hh,foam,4,6+i,lod,.013,True)
        for k in range(start,len(s.p)):
            t=max(0,min(1,(s.p[k][1]-.013)/hh));tint=.87+.13*t
            s.c[k]=(*tuple(v*tint for v in foam),1)
            s.uv[k]=((4.10+.70*(.5+.5*math.sin(k*1.7)))/8,.79+.09*t)
    return s

def wall(lod):
    s=Sculpt();n=18 if lod==0 else 9;layers=6 if lod==0 else 3
    for side in [-1,1]:
        start=len(s.p)
        for j in range(layers+1):
            t=j/layers
            for k in range(n+1):
                z=k/n;crest=.194+.014*math.sin(z*9)+.009*math.sin(z*31)-.019*math.exp(-((z-.71)/.055)**2)
                x=side*(.052-.019*t+.0018*math.sin(j*2.4+z*19))+.004*math.sin(z*9)
                col=(.98,.87,.69) if j==0 and k==0 else (.96-.065*(j%2),.76-.055*(j%2),.48-.032*(j%2))
                s.v(x,t*crest,z,col,0,z,t)
        for j in range(layers):
            for k in range(n):
                a=start+j*(n+1)+k;ids=[a,a+1,a+n+2,a+n+1];s.face(ids if side==1 else ids[::-1])
    stride=(layers+1)*(n+1)
    for k in range(n):
        a=layers*(n+1)+k;b=a+stride;s.face([a,b,b+1,a+1])
    for k in [0,n]:s.face([j*(n+1)+k for j in range(layers+1)]+[stride+j*(n+1)+k for j in reversed(range(layers+1))])
    if lod==0:
        for i,z in enumerate([.13,.49,.84]):s.block(.048,z,.028,.052,0,.029,(.85,.68,.45),0,i)
    return s

def beacon(lod):
    s=Sculpt()
    # Continuous rammed-earth battered body, not eight superimposed tower boxes.
    levels=7 if lod==0 else 4;plan=[(-1,-.73),(-.70,-1),(.74,-.97),(1,-.65),(.92,.74),(.58,1),(-.78,.94),(-1,.61)]
    for j in range(levels+1):
        t=j/levels;span=.111-.051*t
        for k,(x,z) in enumerate(plan):
            y=.338*t+.002*math.sin(k*1.7)*t
            c=(.98-.055*(j%2),.76-.035*(j%2),.48-.025*(j%2))
            s.v(x*span+.0014*math.sin(j+k),y,z*span,c,0,k/8,t)
    for j in range(levels):
        for k in range(8):a=j*8+k;b=j*8+(k+1)%8;s.face([a,b,b+8,a+8])
    s.face([levels*8+k for k in range(8)])
    s.block(0,0,.103,.101,.337,.343,(.29,.26,.20),4)
    # Two broken low rim remnants and an earth lip; deliberately no merlons.
    for side in [-1,1]:s.block(side*.055,.008,.016,.108,.337,.375 if side==1 else .388,(.94,.74,.46),0,side,.9)
    s.block(0,-.052,.104,.016,.336,.364,(.89,.70,.44),0,3,.9)
    if lod==0:
        for j in range(3):s.block((j-1)*.018,.004,.012,.060,.342,.353,(.30,.25,.17),0,j,1)
    return s

def granite(lod):
    s=Sculpt();nx=8 if lod==0 else 5;nz=6 if lod==0 else 4
    # A broad irregular heightfield outcrop. Ridge runs diagonally, with two
    # unequal angular shoulders; no radial ring construction or central bowl.
    for j in range(nz+1):
        v=j/nz;z=(v-.5)*.36
        for k in range(nx+1):
            u=k/nx;x=(u-.5)*.51
            shoulder=.94-.46*abs(2*v-1)-.22*abs(2*u-1)
            ridge=.24+.13*(1-abs(u-(.29+.24*v))*1.1)-.035*abs(v-.48)
            y=max(.065,ridge*shoulder)+.010*math.sin(k*2.7+j*4.1)
            # Rounded plan corners are chipped at the perimeter, not a pot rim.
            xx=x*(1-.12*abs(2*v-1)**3);zz=z*(1-.15*abs(2*u-1)**3)
            shade=.89+.065*math.sin(k*1.7+j*2.3)
            col=(.80*shade,.865*shade,.87*shade)
            if (k+j)%7==0:col=(.64,.72,.61)
            s.v(xx,y,zz*.98,col,0,u,.38+.035*v)
    for j in range(nz):
        for k in range(nx):
            a=j*(nx+1)+k
            if (k+j)%2:s.face([a,a+1,a+nx+1]);s.face([a+1,a+nx+2,a+nx+1])
            else:s.face([a,a+1,a+nx+2]);s.face([a,a+nx+2,a+nx+1])
    edge=[k for k in range(nx+1)]+[j*(nx+1)+nx for j in range(1,nz+1)]+[nz*(nx+1)+k for k in range(nx-1,-1,-1)]+[j*(nx+1) for j in range(nz-1,0,-1)]
    for k,a in enumerate(edge):
        b=edge[(k+1)%len(edge)];pa=s.p[a];pb=s.p[b]
        c=s.v(pb[0]*1.01,0,pb[2]*1.01,(.65,.73,.73),0,k/len(edge),.39)
        d=s.v(pa[0]*1.01,0,pa[2]*1.01,(.71,.79,.78),0,k/len(edge),.43);s.face([a,b,c,d])
    # Grounded unequal fractured buttresses break the regular field outline.
    for i,(x,z,sx,sz,hh) in enumerate([(-.155,.060,.20,.15,.21),(.14,-.080,.20,.19,.18),(.15,.085,.17,.13,.14)]):
        s.block(x,z,sx,sz,0,hh,(.73,.79,.78),0,i,taper=.65)
    # Fracture seams follow actual exposed cliff faces, unlike painted brickwork.
    if lod==0:
        for x,z,hgt,lean in [(-.08,-.157,.22,.025),(.12,-.152,.18,-.019),(.239,.00,.21,-.016)]:
            if x>.2:s.poly([(x,0,z),(x+.001,hgt*.68,z+lean),(x+.001,hgt,z+lean*.7),(x+.002,hgt*.75,z+lean+.003),(x+.002,0,z+.005)],(.29,.37,.36),0)
            else:s.poly([(x,0,z),(x+lean,hgt*.7,z-.001),(x+lean*.7,hgt,z-.001),(x+lean+.005,hgt*.73,z-.002),(x+.006,0,z-.002)],(.31,.38,.37),0)
    return s

def sandstone(lod):
    s=Sculpt();n=10 if lod==0 else 7;layers=7 if lod==0 else 4
    outline=[(-.25,-.11),(-.16,-.18),(.06,-.18),(.22,-.11),(.27,.025),(.19,.15),(.015,.19),(-.17,.15),(-.255,.064),(-.26,-.022)]
    if n==7:outline=[outline[k] for k in [0,1,3,4,5,7,8]]
    for j in range(layers+1):
        t=j/layers;scale=1-.30*t+.025*math.sin(j*2.0)
        for k,(x,z) in enumerate(outline):
            yy=.29*t+.007*math.sin(k*1.8)*t
            color=(.97-.06*(j%2),.78-.055*(j%2),.55-.04*(j%2))
            s.v(x*scale+.025*t,yy,z*scale,color,0,k/n,t)
    for j in range(layers):
        for k in range(n):a=j*n+k;b=j*n+(k+1)%n;s.face([a,b,b+n,a+n])
    center=s.v(.025,.288,0,(.98,.85,.65),0,.45,.55)
    for k in range(n):s.face([layers*n+k,layers*n+(k+1)%n,center])
    if lod==0:
        s.block(-.14,.102,.12,.10,0,.086,(.87,.70,.48),0,1)
        s.block(.15,.09,.14,.10,0,.079,(.91,.72,.50),0,3)
    return s

def karst(lod):
    s=Sculpt()
    # Interlocking, unequal limestone lobes and mossy horizontal shelves.
    for i,(x,z,sx,sz,hh) in enumerate([(-.095,-.025,.28,.27,.455),(.12,-.035,.25,.24,.345),(-.02,.091,.32,.22,.26)]):
        s.lobe(x,z,sx,sz,hh,(.84,.89,.77),0,4+i,lod,0,False)
    for i,(x,z,sx,sz,hh) in enumerate([(-.06,.038,.33,.24,.12),(.03,-.035,.32,.26,.23)]):
        s.block(x,z,sx,sz,max(0,hh-.04),hh,(.66,.80,.58),0,i,.96)
    if lod==0:s.lobe(.19,.078,.12,.15,.14,(.71,.81,.67),0,8,1)
    return s

def shore_rock(lod):
    s=Sculpt()
    for i,(x,z,sx,sz,hh) in enumerate([(-.075,.005,.26,.20,.075),(.092,-.021,.22,.16,.055),(.03,.079,.13,.11,.040)]):
        s.lobe(x,z,sx,sz,hh,(.83,.92,.81),0,i+2,lod,0,False)
    return s

def reeds(lod):
    s=Sculpt();count=13 if lod==0 else 7
    for i in range(count):
        a=i*2.39996;r=.020+.09*((i*5+3)%count)/count;cx=math.cos(a)*r;cz=math.sin(a)*r*.65
        h=.069+.055*(.5+.5*math.sin(i*2.7));dx=.018*math.sin(i*1.7);dz=.018*math.cos(i*1.3)
        # Three-sided bent reeds, sparse and opaque, avoiding alpha sorting.
        seg=3 if lod==0 else 2;start=len(s.p)
        for j in range(seg+1):
            t=j/seg;rad=.0014*(1-.65*t)
            for k in range(3):
                ang=k*math.tau/3;s.v(cx+dx*t*t+rad*math.cos(ang),h*t,cz+dz*t*t+rad*math.sin(ang),(.59+.10*t,.73-.07*t,.37-.03*t),6,.32,.42)
        for j in range(seg):
            for k in range(3):a0=start+j*3+k;b=start+j*3+(k+1)%3;s.face([a0,b,b+3,a0+3])
        for side in [-1,1]:
            t=.36 if side==1 else .61;y=h*t;x=cx+dx*t*t;z=cz+dz*t*t
            tip=(x+side*.030,y+.018,z+side*.012)
            s.poly([(x,y,z),(x+side*.014,y+.014,z+.004),tip,(x+side*.015,y+.008,z-.003)],(.62,.77,.39),6,[(.33,.42)]*4)
        if lod==0 and i%3==0:
            s.block(cx+dx,cz+dz,.006,.005,h-.019,h+.001,(.72,.66,.40),6,i,.62)
    return s

families={'fall-narrow':waterfall,'fall-wide':waterfall,'fall-hukou':waterfall,'wall-earth':wall,'beacon-han':beacon,'cliff-sandstone':sandstone,'cliff-granite':granite,'cliff-karst':karst,'shore-reeds':reeds,'shore-rock':shore_rock}
objects=[]
for family,fn in families.items():
    last=None
    for lod in [0,1]:
        sculpt=fn(family,lod) if family.startswith('fall-') else fn(lod)
        # Retain the existing footprint envelope for all legacy landmark families.
        if family=='cliff-karst':sculpt.p=[(x,y,z*.985) for x,y,z in sculpt.p]
        if family=='cliff-sandstone':sculpt.p=[(x,y,z*.985) for x,y,z in sculpt.p]
        obj=sculpt.object(family+'-lod'+str(lod));objects.append(obj)
        desc='original Blender-authored '+family+'; irregular supported natural landform; geographic interpretation, exact PC reference unavailable'
        ns['export'](obj,OUT/f'app/src/main/assets/3d/field/v128/{family}-lod{lod}.glb',ATLAS,desc)
        count=ns['REPORT'][-1]['triangles'];assert count<750,(family,lod,count)
        assert last is None or count<last,(family,count,last);last=count
        obj.hide_render=True
# Preview material exactly matches runtime atlas orientation and vertex colors.
mat=bpy.data.materials.new('v128 atlas x authored vertex color (runtime V orientation)');mat.use_nodes=True
nt=mat.node_tree;nt.nodes.clear();out=nt.nodes.new('ShaderNodeOutputMaterial');bs=nt.nodes.new('ShaderNodeBsdfPrincipled');bs.inputs['Roughness'].default_value=.91
tex=nt.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Linear'
uv=nt.nodes.new('ShaderNodeTexCoord');sep=nt.nodes.new('ShaderNodeSeparateXYZ');comb=nt.nodes.new('ShaderNodeCombineXYZ');flip=nt.nodes.new('ShaderNodeMath');flip.operation='SUBTRACT';flip.inputs[0].default_value=1
nt.links.new(uv.outputs['UV'],sep.inputs[0]);nt.links.new(sep.outputs['X'],comb.inputs['X']);nt.links.new(sep.outputs['Y'],flip.inputs[1]);nt.links.new(flip.outputs[0],comb.inputs['Y']);nt.links.new(comb.outputs[0],tex.inputs['Vector'])
vc=nt.nodes.new('ShaderNodeVertexColor');vc.layer_name='Color';mul=nt.nodes.new('ShaderNodeMixRGB');mul.blend_type='MULTIPLY';mul.inputs[0].default_value=1
nt.links.new(tex.outputs['Color'],mul.inputs[1]);nt.links.new(vc.outputs['Color'],mul.inputs[2]);nt.links.new(mul.outputs[0],bs.inputs['Base Color']);nt.links.new(bs.outputs[0],out.inputs[0])
for o in objects:o.data.materials.append(mat)
# Portrait-like orthographic studio inspections of actual source geometry, not APK.
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=32;scene.cycles.use_denoising=False
scene.render.resolution_x=1000;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.world.color=(.20,.20,.20);scene.view_settings.view_transform='Standard';scene.view_settings.look='Medium High Contrast' if 'Medium High Contrast' in [] else 'None'
scene.view_settings.exposure=0;scene.view_settings.gamma=1
bpy.ops.object.light_add(type='AREA',location=(-3,-4,7));key=bpy.context.object;key.name='Preview only broad key';key.data.energy=600;key.data.shape='DISK';key.data.size=5
bpy.ops.object.light_add(type='AREA',location=(4,2,5));fill=bpy.context.object;fill.name='Preview only soft fill';fill.data.energy=380;fill.data.size=4
bpy.ops.object.camera_add();camera=bpy.context.object;camera.name='Preview camera';scene.camera=camera;camera.data.type='ORTHO'
# Scene saved with raw source objects at origin and a render rig. Export ignores
# presentation transforms; keep editable raw meshes available by collection.
rawcoll=bpy.data.collections.new('Runtime meshes Y-up (all twenty editable sources)');scene.collection.children.link(rawcoll)
for o in objects:
    for col in list(o.users_collection):col.objects.unlink(o)
    rawcoll.objects.link(o)
preview=[]
for family in families:
    for lod in [0,1]:
        original_obj=bpy.data.objects[family+'-lod'+str(lod)]
        proxy=original_obj.copy();proxy.data=original_obj.data.copy();scene.collection.objects.link(proxy);proxy.hide_render=False
        proxy.name='PREVIEW '+family+' LOD'+str(lod);proxy.rotation_euler.x=math.pi/2
        if family.startswith('fall-'):
            # Normalize presentation to a typical landscape silhouette. Runtime
            # terrain fitting chooses the actual scale; this is a source preview.
            proxy.scale=(2.3,.53 if family=='fall-hukou' else .76,1.3)
        bpy.context.view_layer.update()
        pts=[proxy.matrix_world @ Vector(p) for p in proxy.bound_box]
        mn=Vector(tuple(min(p[k] for p in pts) for k in range(3)));mx=Vector(tuple(max(p[k] for p in pts) for k in range(3)));center=(mn+mx)*.5
        size=max(mx-mn);camera.location=center+Vector((size*1.25,-size*1.65,size*1.2));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.ortho_scale=size*1.58
        scene.render.image_settings.file_format='PNG';scene.render.film_transparent=False
        scene.render.filepath=str(ART/(family+'-lod'+str(lod)+'.png'));bpy.ops.render.render(write_still=True)
        preview.append({'family':family,'lod':lod,'path':str(Path(scene.render.filepath).relative_to(OUT)),'kind':'BLENDER_SOURCE_PREVIEW_NOT_APK','presentation_scale':list(proxy.scale)})
        bpy.data.objects.remove(proxy,do_unlink=True)
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(ART/'landmarks-v128.blend'))
report={'version':128,'blender':bpy.app.version_string,'generator':'tools/3d/refine_blender128.py','generator_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),'license':'CC0-1.0 original geometry and new atlas panels; preserved project atlas panels 1/2/6/7','assets':ns['REPORT'],'atlas':{'path':str(ATLAS.relative_to(OUT)),'sha256':hashlib.sha256(ATLAS.read_bytes()).hexdigest(),'bytes':ATLAS.stat().st_size,'dimensions':[w,h],'color_space':'sRGB','runtime_uv':'PNG row zero = UV.v zero; Blender preview explicitly flips V','retained_panels':[1,2,6,7],'changed_panels':[0,3,4,5]},'previews':preview,'editable_source':str((ART/'landmarks-v128.blend').relative_to(OUT)),'reference_links':['https://photo.china.com.cn/2024-08/21/content_117378600.shtml','https://whc.unesco.org/en/list/438','https://english.igsnrr.cas.cn/ecg/naturalscenery/lakes/202011/t20201119_251411.html','https://whc.unesco.org/en/list/437','https://whc.unesco.org/en/tentativelists/6380/'],'reference_status':'Geographic form/color references; no downloaded assets used; exact licensed PC reference unavailable','verification':'Original source renders only; APK and physical-device visual review are separate parent work'}
path=OUT/'docs/native-pc-visual/feedback-v128-blender-assets.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'status':'EXPORTED','models':len(objects),'triangles':sum(x['triangles'] for x in ns['REPORT']),'artifact_dir':str(ART)}))
