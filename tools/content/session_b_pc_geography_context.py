"""Load installation SHEX4791 through original484090 before native postload.
This certifies geography input decoding, not full menus/opening events/settings.
"""
import struct
from pc_resources import Archive
from audit_pc_restoration_sources import EXE_SHA,sha
SHEX_SHA='c726989df91d44c99ea3c3f613d41e7e725c43002a9775dfac2e43516f5c6112'
def load_geography(world,installation):
 if sha(world.exe)!=EXE_SHA:raise ValueError('Original EXE changed')
 archive=Archive(installation/'Media/san11pkres.bin')
 try:raw=archive.read(4791)
 finally:archive.close()
 if len(raw)!=440008 or sha(raw)!=SHEX_SHA:raise ValueError('Original SHEX changed')
 buffer=0xc200000;world.u.mem_map(buffer,0x6c000);world.u.mem_write(buffer,raw)
 before=bytes(world.u.mem_read(0x7200000,0x300000));rng=bytes(world.u.mem_read(0x8a5d44,4))
 if world.call(0x484090,buffer,len(raw),receiver=0x6fb0e68,count=50000000)!=1:raise ValueError('Complete original SHEX loader failed')
 decoded=bytes(world.u.mem_read(0x6fb0e68,40000*20))
 for cell in range(40000):
  if ((struct.unpack_from('<I',decoded,cell*20+4)[0]>>5)&127)!=(raw[8+cell*11+1]&127):raise ValueError('Native decoded region differs from source')
 if before!=bytes(world.u.mem_read(0x7200000,0x300000))or rng!=bytes(world.u.mem_read(0x8a5d44,4)):raise ValueError('Geography loader changed world/RNG')
 return dict(shexResourceId=4791,shexSha=sha(raw),originalSHEXLoader='0x484090',originalSHEXLoaderCodeSha=sha(world.exe[0x84090:0x84342]),decodedGeographySha=sha(decoded),verifiedRegionCells=40000,worldAndRngUnchanged=True,completeOpening=False)
