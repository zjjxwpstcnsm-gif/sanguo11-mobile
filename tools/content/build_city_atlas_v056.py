#!/usr/bin/env python3
"""Original procedural historical-strategy buildings. No reference image pixels are copied.
Requires Pillow. Deterministic PNG assets, alpha-safe downsampling and a checked atlas manifest.
"""
from __future__ import annotations
import hashlib,json,math,random
from pathlib import Path
from PIL import Image,ImageDraw,ImageFilter
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'app/src/main/assets/map/cities-v056'
SIZE=512

class Painter:
    def __init__(self,seed):
        self.im=Image.new('RGBA',(1024,1024));self.d=ImageDraw.Draw(self.im);self.r=random.Random(seed)
    def p(self,u,v,z=0):return (round((256+(u-v)*3.15)*2),round((285+(u+v)*1.82-z*3.15)*2))
    def polygon(self,pts,color,line=None):
        xy=[self.p(*p) for p in pts];self.d.polygon(xy,fill=color)
        if line:self.d.line(xy+[xy[0]],fill=line,width=2)
    def plane(self,u,v,w,h,z,color,line=None):self.polygon([(u,v,z),(u+w,v,z),(u+w,v+h,z),(u,v+h,z)],color,line)
    def line(self,pts,color,width=1):self.d.line([self.p(*p) for p in pts],fill=color,width=width*2)
    def box(self,u,v,w,h,z,stone=False):
        left=(125,112,90,255) if stone else (172,155,121,255)
        right=(91,88,76,255) if stone else (117,109,91,255)
        top=(159,146,119,255) if stone else (198,177,136,255)
        self.polygon([(u,v,z),(u+w,v,z),(u+w,v+h,z),(u,v+h,z)],top)
        self.polygon([(u,v+h,0),(u+w,v+h,0),(u+w,v+h,z),(u,v+h,z)],left)
        self.polygon([(u+w,v,0),(u+w,v+h,0),(u+w,v+h,z),(u+w,v,z)],right)
        for lev in range(2,int(z),2):
            self.line([(u,v+h,lev),(u+w,v+h,lev),(u+w,v,lev)],(62,67,61,90))
        if not stone:
            for t in (.23,.68):
                self.plane(u+w*t,v+h+.01,w*.12,.01,z*.36,(63,65,56,255))
                self.line([(u+w*t,v+h+.03,1),(u+w*t,v+h+.03,z-1)],(102,83,63,255))
    def roof(self,u,v,w,h,z,tier=False):
        e=.85;u-=e;v-=e;w+=2*e;h+=2*e;peak=min(3.8,h*.4)
        a=(u,v,z);b=(u+w,v,z);c=(u+w,v+h,z);d=(u,v+h,z)
        r1=(u+w*.17,v+h*.5,z+peak);r2=(u+w*.83,v+h*.5,z+peak)
        self.polygon([a,b,r2,r1],(100,111,108,255))
        self.polygon([a,r1,d],(111,120,111,255))
        self.polygon([b,c,r2],(49,66,67,255))
        self.polygon([d,r1,r2,c],(68,88,89,255))
        self.line([a,b,c,d,a],(37,51,52,255))
        for i in range(1,max(2,int(w*1.5))):
            x=u+w*i/max(2,int(w*1.5))
            self.line([(x,v+h,z+.1),(x,v+h*.5,z+peak+.1)],(132,146,139,115))
        for i in range(1,4):
            f=i/4;self.line([(u+w*.17*f,v+h*(1-f*.5),z+peak*f),(u+w-w*.17*f,v+h*(1-f*.5),z+peak*f)],(32,54,58,100))
        self.line([r1,r2],(167,165,139,255))
        if tier:
            self.line([(u,v,z+.6),(u+.6,v+.5,z)],(119,127,113,255))
            self.line([(u+w,v+h,z+.6),(u+w-.6,v+h-.5,z)],(132,140,120,255))
    def house(self,u,v,w,h,height=4,grand=False):
        self.box(u,v,w,h,height)
        if grand:
            self.roof(u-.4,v-.4,w+.8,h+.8,height)
            self.box(u+1,v+1,w-2,h-2,height+4)
            self.roof(u+.9,v+.9,w-1.8,h-1.8,height+4,True)
        else:self.roof(u,v,w,h,height)
    def wall(self,a,b,height=5):
        u,v=a;u2,v2=b
        self.polygon([(u,v,0),(u2,v2,0),(u2,v2,height),(u,v,height)],(124,118,94,255),(69,76,65,255))
        self.line([(u,v,height),(u2,v2,height)],(193,179,144,255),2)
        distance=math.hypot(u2-u,v2-v);n=max(1,int(distance/2.5))
        for i in range(n):
            f=(i+.2)/n;f2=(i+.65)/n
            self.polygon([(u+(u2-u)*f,v+(v2-v)*f,height),(u+(u2-u)*f2,v+(v2-v)*f2,height),(u+(u2-u)*f2,v+(v2-v)*f2,height+1.3),(u+(u2-u)*f,v+(v2-v)*f,height+1.3)],(170,161,128,255))
        for z in (1.5,3.1):self.line([(u,v,z),(u2,v2,z)],(79,84,69,120))
    def tree(self,u,v,height=5):
        self.line([(u,v,0),(u,v,height)],(111,91,63,255),1)
        x,y=self.p(u,v,height)
        for ox,oy,r in [(-5,0,6),(4,-2,7),(0,-8,6)]:
            self.d.ellipse((x+ox*2-r*2,y+oy*2-r*2,x+ox*2+r*2,y+oy*2+r*2),fill=(55+self.r.randrange(12),81+self.r.randrange(15),59,245))
    def ground(self,polygon):
        mask=Image.new('RGBA',self.im.size);md=ImageDraw.Draw(mask)
        xy=[self.p(u+2.8,v+3,0) for u,v in polygon];md.polygon(xy,fill=(41,48,32,70));mask=mask.filter(ImageFilter.GaussianBlur(13));self.im.alpha_composite(mask)
        self.d=ImageDraw.Draw(self.im)
        self.polygon([(u,v,0) for u,v in polygon],(128,128,93,215))
        for _ in range(450):
            u=self.r.uniform(-30,30);v=self.r.uniform(-30,30);x,y=self.p(u,v)
            self.d.ellipse((x,y,x+2,y+2),fill=self.r.choice([(176,158,110,120),(63,89,56,95),(203,185,146,100)]))
    def finish(self):return self.im.convert('RGBa').resize((SIZE,SIZE),Image.Resampling.LANCZOS).convert('RGBA')

def city(variant,seed):
    p=Painter(seed);r=p.r
    outline=[(-31,-25),(-25,-31),(25,-31),(31,-25),(31,25),(25,31),(-25,31),(-31,25)]
    if variant=='mountain':outline=[(-29,-19),(-20,-31),(23,-29),(31,-15),(29,25),(17,31),(-25,29),(-32,17)]
    p.ground([(u*1.14,v*1.14) for u,v in outline])
    # Continuous streets inside a single enclosing city, not seven tinted blocks.
    p.plane(-29,-2.6,58,5.2,0,(178,166,135,235));p.plane(-2.5,-29,5,58,0,(190,176,142,235))
    for i in range(-26,28,3):
        p.line([(i,-2.4,.01),(i,2.4,.01)],(121,119,95,110))
        p.line([(-2.3,i,.01),(2.3,i,.01)],(121,119,95,110))
    edges=list(zip(outline,outline[1:]+outline[:1]))
    for a,b in edges:
        if sum(a)+sum(b)<0:p.wall(a,b,6 if variant in ('luoyang','changan','capital') else 5)
    buildings=[]
    for u in [-24,-15,7,17]:
        for v in [-24,-14,7,17]:
            if abs(u)<17 and v<-5:continue
            if variant=='river' and u>10 and v>10:continue
            w=6+r.random()*2;h=5+r.random()*3
            buildings.append((u,v,w,h,3.5+r.random()*1.5,False))
    if variant=='luoyang':
        buildings += [(-12,-21,10,10,7,True),(4,-16,10,9,6,True),(-9,8,8,7,5,True)]
        for i in (-20,-7,6,20):p.plane(i,2,1,27,0,(109,116,85,190))
    elif variant=='changan':
        buildings += [(-14,-22,18,12,7,True),(8,-15,9,8,5,True)]
        for v in (-8,5,15):p.plane(-28,v,55,1.2,0,(182,171,143,210))
    elif variant=='capital':buildings += [(-12,-21,22,13,7,True)]
    elif variant=='large':buildings += [(-10,-19,15,10,6,True),(-16,10,8,7,5,True)]
    elif variant=='mountain':
        p.box(-14,-21,28,16,3,True);buildings += [(-10,-18,15,11,8,True)]
    elif variant=='southern':buildings += [(-10,-18,15,11,5,True)]
    else:buildings += [(-10,-18,15,11,6,True)]
    for b in sorted(buildings,key=lambda b:b[0]+b[1]):p.house(*b)
    # Courtyard pillars and trees give depth at city scale, with no faction tint.
    for u,v in [(-25,-2),(-7,13),(13,-7),(24,-4)]:p.tree(u,v,4)
    for a,b in edges:
        if sum(a)+sum(b)>=0:p.wall(a,b,6 if variant in ('luoyang','changan','capital') else 5)
    for u,v in [(-29,-23),(23,-29),(28,22),(-22,28)]:
        p.box(u-2,v-2,4,4,8,True);p.roof(u-2.8,v-2.8,5.6,5.6,8)
    # Foreground gate: dark arch/door framed in pale stone with a roof above.
    p.box(-3,27,6,5,7,True);p.roof(-4,26,8,7,7)
    p.polygon([(-1.1,32.01,0),(1.1,32.01,0),(1.1,32.01,3.5),(-1.1,32.01,3.5)],(43,50,46,255))
    p.line([(-1.3,32.05,0),(-1.3,32.05,3.8),(0,32.05,4.5),(1.3,32.05,3.8),(1.3,32.05,0)],(176,164,132,255))
    if variant=='southern':
        for u,v in [(32,-15),(-31,0),(10,35),(-16,33)]:p.tree(u,v,7)
    if variant=='river':
        p.polygon([(29,7,0),(38,1,0),(40,31,0),(26,39,0)],(82,117,124,150))
        for i in range(4):p.line([(30+i*2,6,0),(30+i*2,30,0)],(174,188,165,130))
    return p.finish()

def gate():
    p=Painter(5608);p.ground([(-27,-11),(-14,-24),(25,-17),(30,14),(12,26),(-26,16)])
    for u,v in [(-26,-11),(-18,7),(24,-10),(21,14)]:
        p.polygon([(u-7,v+4,0),(u+8,v+5,0),(u+3,v-5,9),(u-4,v-3,13)],(117,119,99,245))
        p.polygon([(u+3,v-5,9),(u+8,v+5,0),(u+13,v-4,0)],(84,96,83,245))
    p.plane(-3,-30,6,60,0,(185,163,121,230))
    p.wall((-23,4),(-4,4),9);p.wall((4,4),(24,4),9)
    p.box(-6,-3,12,9,11,True);p.roof(-7,-4,14,11,11)
    p.polygon([(-2.8,6.01,0),(2.8,6.01,0),(2.8,6.01,6),(-2.8,6.01,6)],(42,49,42,255))
    p.house(-19,1,6,6,12,False);p.house(15,1,6,6,12,False)
    return p.finish()

def port():
    p=Painter(5609);p.ground([(-31,-23),(22,-23),(30,18),(19,33),(-25,27),(-33,6)])
    p.polygon([(-2,-24,0),(32,-17,0),(35,28,0),(16,36,0),(-2,23,0)],(76,111,120,178))
    for i in range(19):
        v=-16+i*2.5;p.line([(3,v,0),(26,v-2,0)],(145,170,159,125))
    p.plane(-9,-23,6,50,.6,(166,147,110,255))
    for v in (-13,1,15):
        p.plane(-6,v,25,3,1.3,(153,131,91,255))
        for u in range(-4,19,2):p.line([(u,v,1.5),(u,v+3,1.5)],(77,82,64,210))
        for u in (-4,16):p.line([(u,v,0),(u,v,3)],(97,88,61,255))
    p.house(-24,-19,13,10,5);p.house(-24,-1,12,10,5);p.house(-23,15,11,7,4)
    for u,v in [(16,-7),(14,8),(19,24)]:
        p.polygon([(u-3,v-7,1),(u+3,v-5,1),(u+3,v+5,1),(u,v+8,1),(u-3,v+5,1)],(91,78,55,255),(57,66,56,255))
        p.plane(u-1.5,v-2,3,6,2.6,(153,133,90,255))
        p.line([(u,v,2),(u,v,12)],(103,91,63,255));p.polygon([(u,v,11),(u,v+6,8),(u,v+6,5),(u,v,5)],(213,196,151,255))
    return p.finish()

def generate():
    OUT.mkdir(parents=True,exist_ok=True)
    variants=['capital','large','standard','mountain','river','southern','luoyang','changan','gate','port']
    catalog={'schema':1,'map_revision':56,'license':'Original procedural artwork generated by repository source; no reference image pixels.','light':'upper-left neutral daylight','variants':{},'decoded_bytes':0}
    audit=[]
    for seed,key in enumerate(variants):
        base=gate() if key=='gate' else port() if key=='port' else city(key,5600+seed)
        catalog['variants'][key]={}
        for lod,n in [('near',512),('mid',256),('far',96)]:
            im=base if n==512 else base.convert('RGBa').resize((n,n),Image.Resampling.LANCZOS).convert('RGBA')
            name=key+'-'+lod+'.png';path=OUT/name;im.save(path,optimize=True)
            alpha=im.getchannel('A');bounds=alpha.getbbox();assert bounds and bounds[0]>=1 and bounds[1]>=1 and bounds[2]<n and bounds[3]<n,(name,bounds)
            coverage=sum(v>0 for v in alpha.getdata())/(n*n);assert 0.05<coverage<0.90,(name,coverage)
            sha=hashlib.sha256(path.read_bytes()).hexdigest();world_width=72 if key=='port' else 68 if key=='gate' else 136
            entry={'file':name,'rect':[0,0,n,n],'pivot':[n*.5,n*285/512],'world_width':world_width,'bounds':list(bounds),'alpha_coverage':round(coverage,6),'sha256':sha,'bytes':path.stat().st_size}
            catalog['variants'][key][lod]=entry;catalog['decoded_bytes']+=n*n*4;audit.append({'variant':key,'lod':lod,**entry})
    (OUT/'catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
    folder=ROOT/'data/map/reference-v056';folder.mkdir(parents=True,exist_ok=True)
    (folder/'art-audit.json').write_text(json.dumps({'source':'original procedural renderer','assets':audit,'decoded_bytes':catalog['decoded_bytes']},indent=2)+'\n')
    print('Generated',len(audit),'real RGBA PNGs;',catalog['decoded_bytes'],'decoded bytes;',sum(x['bytes'] for x in audit),'PNG bytes')
if __name__=='__main__':generate()
