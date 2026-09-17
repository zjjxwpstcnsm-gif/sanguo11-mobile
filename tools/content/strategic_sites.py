"""Authored gate/port positions on our reconstructed terrain, with stable catalog IDs.
Unverified port coordinates in the source table are deliberately not used.
"""
# id, parent city id, x, y. Parent controls opening ownership only; sites are independent thereafter.
GATES=[(20042,20005,97,59),(20043,20015,90,77),(20044,20017,65,72),
       (20045,20015,71,78),(20046,20017,63,89),(20047,20036,27,83),
       (20048,20037,23,102),(20049,20037,20,105),(20050,20037,14,97),(20051,20039,11,119)]
PORTS=[(20052,20000,169,28),(20053,20004,127,62),(20054,20005,60,49),
       (20055,20011,118,65),(20056,20007,172,49),(20057,20007,146,55),
       (20058,20008,167,80),(20059,20010,168,90),(20060,20010,152,107),
       (20061,20006,111,64),(20062,20012,102,69),(20063,20015,80,70),
       (20064,20015,74,66),(20065,20017,62,65),(20066,20017,62,52),
       (20067,20018,60,110),(20068,20022,157,104),(20069,20022,153,117),
       (20070,20023,179,92),(20071,20024,192,125),(20072,20025,142,126),
       (20073,20026,133,129),(20074,20026,116,134),(20075,20026,134,139),
       (20076,20026,136,156),(20077,20027,111,130),(20078,20028,83,120),
       (20079,20029,74,114),(20080,20030,98,144),(20081,20030,91,125),
       (20082,20030,84,148),(20083,20031,102,163),(20084,20032,96,152),
       (20085,20032,75,148),(20086,20035,49,130)]

def neighbors(x,y):
    return [(x-1,y),(x+1,y),(x-(1-y%2),y-1),(x+y%2,y-1),(x-(1-y%2),y+1),(x+y%2,y+1)]

def layout(terrain,city_positions):
    occupied=set(city_positions.values());result=[]
    for key,parent,x,y in GATES:
        assert terrain[y][x] not in 'WOV',(key,'gate on water')
        terrain[y][x]='P';occupied.add((x,y));result.append((key,parent,x,y,'GATE'))
    for key,parent,x,y in PORTS:
        candidates=[]
        for yy in range(max(1,y-12),min(199,y+13)):
            for xx in range(max(1,x-12),min(199,x+13)):
                if terrain[yy][xx] not in 'PF' or (xx,yy) in occupied:continue
                around=[terrain[ny][nx] for nx,ny in neighbors(xx,yy)]
                if any(t in 'WO' for t in around) and sum(t in 'PF' for t in around)>=2:
                    candidates.append(((xx-x)**2+(yy-y)**2,yy,xx))
        assert candidates,('no port bank',key)
        _,yy,xx=min(candidates);occupied.add((xx,yy));result.append((key,parent,xx,yy,'PORT'))
    return result
