"""Hand-reconstructed SAN11 geography in the catalog's 200x200 odd-r frame.

The vectors are original digitization of the publicly displayed KOEI overview,
not an extraction of the game's terrain file. See docs/GEOGRAPHY_SKILLS_V0_30.md.
"""
from math import hypot

CAPACITIES = {
    **dict.fromkeys('小沛 安定 新野 武陵 梓潼'.split(), 10),
    **dict.fromkeys('襄平 薊 平原 晉陽 北海 汝南 上庸 天水 柴桑 江夏 長沙 桂陽 零陵 永安 江州 建寧'.split(), 12),
    **dict.fromkeys('北平 南皮 壽春 濮陽 陳留 宛 武威 吳 會稽 廬江 江陵 漢中 雲南'.split(), 15),
    **dict.fromkeys('下邳 許昌 建業 襄陽 成都'.split(), 18),
    '鄴': 20, '洛陽': 22, '長安': 22,
}
# Bohai gulf, Shandong peninsula, Jiangsu coast, Yangtze mouth, Zhejiang,
# southeastern mountains. The western/southern edge follows the game theatre.
LAND = [(0,28),(15,26),(31,16),(46,12),(60,4),(81,6),(92,0),(114,0),
        (130,5),(150,4),(169,0),(191,2),(199,13),(195,20),(183,25),
        (170,28),(162,30),(151,33),(146,41),(139,47),(138,53),(149,48),
        (164,44),(174,46),(179,50),(169,55),(166,63),(162,66),(166,73),
        (174,77),(171,83),(179,88),(188,91),(189,100),(196,107),
        (193,116),(198,123),(193,132),(188,137),(186,148),(180,159),
        (169,169),(162,179),(150,187),(136,193),(117,199),(95,199),
        (69,198),(47,196),(31,199),(13,199),(0,199)]
RIVERS = {
    '黄河': (1.8,[(49,0),(48,10),(57,16),(61,23),(59,32),(61,41),(59,51),(61,61),
                    (68,66),(80,69),(94,69),(109,66),(121,64),(134,61),(144,56),(151,52)]),
    '长江': (1.9,[(0,151),(12,151),(21,156),(31,155),(36,150),(35,141),(39,134),(44,127),
                    (51,129),(58,136),(65,141),(76,146),(88,150),(99,146),(109,134),
                    (119,129),(129,127),(140,128),(147,126),(153,118),(157,110),(157,101),(163,93),(176,89),(189,91)]),
    '汉水': (1.0,[(27,96),(37,98),(45,108),(57,112),(68,111),(73,113),(80,121),(90,126),(101,126),(111,131)]),
    '淮水': (0.8,[(107,101),(118,104),(128,101),(140,104),(149,94),(164,89),(174,85)]),
    '湘水': (0.8,[(93,198),(91,185),(96,176),(103,169),(100,160),(99,150)]),
    '赣水': (0.8,[(140,190),(138,176),(140,163),(134,150),(130,137),(126,129)]),
}
MOUNTAINS = [
    (3.0,[(0,34),(15,29),(30,32),(43,39),(49,50)]),
    (3.0,[(72,9),(77,22),(78,38),(85,50),(92,57),(98,63)]),
    (2.5,[(98,10),(104,21),(112,29),(117,37)]),
    (2.8,[(0,79),(17,79),(33,82),(49,85),(65,87),(75,87)]),
    (2.8,[(35,86),(37,100),(33,109),(39,118),(45,123)]),
    (2.5,[(0,102),(8,100),(17,102),(24,110),(24,125)]),
    (2.8,[(4,164),(13,159),(19,162),(25,168),(36,179)]),
    (3.0,[(54,145),(56,154),(61,167),(62,178),(65,194)]),
    (3.8,[(149,130),(155,140),(162,153),(169,161)]),
    (3.0,[(110,176),(118,179),(130,176),(141,174),(156,174)]),
]

def inside(x,y,poly):
    hit=False
    for i,(ax,ay) in enumerate(poly):
        bx,by=poly[i-1]
        if (ay>y)!=(by>y) and x<(bx-ax)*(y-ay)/(by-ay)+ax: hit=not hit
    return hit

def distance(x,y,line):
    best=1e9
    for (ax,ay),(bx,by) in zip(line,line[1:]):
        dx,dy=bx-ax,by-ay
        t=max(0,min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)))
        best=min(best,hypot(x-ax-dx*t,y-ay-dy*t))
    return best

def hex_distance(a,b):
    aq,ar=a[0]-(a[1]-(a[1]&1))//2,a[1]
    bq,br=b[0]-(b[1]-(b[1]&1))//2,b[1]
    return max(abs(aq-bq),abs(ar-br),abs(aq+ar-bq-br))

def generate_geography(cities):
    terrain=[]
    for y in range(200):
        row=[]
        for x in range(200):
            if not inside(x,y,LAND):
                row.append('O' if x>130 else 'V');continue
            mountain=min(distance(x,y,line)/width for width,line in MOUNTAINS)
            noise=(x*17+y*31+x*y*7)%101
            t='M' if mountain<0.7 else 'F' if mountain<1.8 or noise<13 else 'P'
            if any(distance(x,y,line)<=width for width,line in RIVERS.values()):t='W'
            if ((x-99)/5)**2+((y-155)/9)**2<1 or ((x-133)/4)**2+((y-143)/10)**2<1:t='W'
            row.append(t)
        terrain.append(row)
    positions={c['id']:(int(c['rawX']),int(c['rawY'])) for c in cities}
    # Preserve original city positions. Clear only a compact city footprint, never
    # flatten a five-by-five square through every river as the former generator did.
    for x,y in positions.values():
        for yy in range(max(0,y-1),min(200,y+2)):
            for xx in range(max(0,x-1),min(200,x+2)):
                if hex_distance((x,y),(xx,yy))<=1:terrain[yy][xx]='P'
    # Connect the theatre through mountain passes. River cells stay water: crossing
    # uses the existing embark/ship rules. Never lay an artificial straight land bridge.
    from heapq import heappush, heappop
    roads=set()
    edges=[]
    for key,p in positions.items():
        for other in sorted((k for k in positions if k!=key),key=lambda k:hex_distance(p,positions[k]))[:3]:
            edge=tuple(sorted((key,other)))
            if edge not in edges:edges.append(edge)
    for a,b in edges:
        start,end=positions[a],positions[b];queue=[(0,start)];cost={start:0};prev={}
        while queue:
            n,p=heappop(queue)
            if n!=cost[p]:continue
            if p==end:break
            x,y=p
            for xx,yy in ((x-1,y),(x+1,y),(x-(1-y%2),y-1),(x+y%2,y-1),(x-(1-y%2),y+1),(x+y%2,y+1)):
                if not 0<=xx<200 or not 0<=yy<200:continue
                t=terrain[yy][xx]
                if t in 'OV':continue
                step=10 if t=='M' else 5 if t=='W' else 3 if t=='F' else 1
                new=n+step;v=(xx,yy)
                if new<cost.get(v,10**9):cost[v]=new;prev[v]=p;heappush(queue,(new,v))
        assert end in cost, ('isolated city',a,b)
        p=end
        while p!=start:
            roads.add(p)
            if terrain[p[1]][p[0]] not in 'WOV':terrain[p[1]][p[0]]='P'
            p=prev[p]
    # Explicit northern mountain corridors: city-nearest roads could route around
    # Qinling instead of opening the actual north exits. Carve only mountain land.
    routes=[[(27,89),(27,83),(32,79),(40,73),(53,73)],
            [(16,110),(20,105),(23,102),(25,96),(27,89)],
            [(10,71),(12,77),(20,82),(27,83)],
            [(10,132),(11,119),(16,110)],[(14,97),(20,99),(23,102)]]
    for route in routes:
        for start,end in zip(route,route[1:]):
            p=start
            while True:
                x,y=p
                for yy in range(max(0,y-1),min(200,y+2)):
                    for xx in range(max(0,x-1),min(200,x+2)):
                        if hex_distance(p,(xx,yy))<=1 and terrain[yy][xx] not in 'WOV':terrain[yy][xx]='P';roads.add((xx,yy))
                if p==end:break
                from strategic_sites import neighbors
                p=min(neighbors(x,y),key=lambda h:(hex_distance(h,end),h))
    # Luoyang basin must not be bypassable by generic nearest-city roads. The
    # Yellow River seals the north; mountain walls seal the south and flanks,
    # leaving the authored western/eastern gate corridors (Tong/Hangu and Hulao).
    luoyang=(79,76)
    for yy in range(68,86):
        for xx in range(68,91):
            d=hex_distance(luoyang,(xx,yy))
            if 9<=d<=12 and terrain[yy][xx] not in 'WOV':terrain[yy][xx]='M'
    def carve_corridor(points):
        for start,end in zip(points,points[1:]):
            p=start
            while True:
                x,y=p
                for yy in range(max(0,y-1),min(200,y+2)):
                    for xx in range(max(0,x-1),min(200,x+2)):
                        if hex_distance(p,(xx,yy))<=1 and terrain[yy][xx] not in 'WOV':terrain[yy][xx]='P';roads.add((xx,yy))
                if p==end:break
                from strategic_sites import neighbors
                p=min(neighbors(x,y),key=lambda h:(hex_distance(h,end),h))
    carve_corridor([(79,76),(84,77),(90,77)])       # Hulao east exit
    carve_corridor([(79,76),(71,78),(65,72)])       # Hangu/Tong west exit
    for xx in range(71,88):
        if terrain[87][xx] not in 'WOV':terrain[87][xx]='M'

    from strategic_sites import layout
    extra_sites=layout(terrain,positions)
    # Two or three compact farming districts, offset from the city, with at least
    # one clear exit. Exact capacity is researched; parcel coordinates are authored.
    occupied=set(positions.values())|{(x,y) for _,_,x,y,_ in extra_sites};parcels={}
    for c in cities:
        x,y=positions[c['id']];need=CAPACITIES[c['name']]
        centers=[(x-4,y-2),(x+4,y+1),(x,y+5)]
        candidates=[]
        for yy in range(max(0,y-8),min(200,y+9)):
            for xx in range(max(0,x-8),min(200,x+9)):
                p=(xx,yy);dist=hex_distance((x,y),p)
                if dist<2 or dist>8 or p in occupied or p in roads or terrain[yy][xx] in 'WOVM':continue
                if min((hex_distance(p,pos),key) for key,pos in positions.items())[1]!=c['id']:continue
                score=min(hex_distance(p,center) for center in centers)
                candidates.append((score,dist,yy,xx))
        chosen=[(xx,yy) for _,_,yy,xx in sorted(candidates)[:need]]
        assert len(chosen)==need,(c['name'],len(chosen),need)
        parcels[c['id']]=chosen;occupied.update(chosen)
        for xx,yy in chosen:terrain[yy][xx]='P'
    return terrain,parcels
