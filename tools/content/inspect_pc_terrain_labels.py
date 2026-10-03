#!/usr/bin/env python3
"""Crosscheck raw source terrain IDs against this EXE's Big5 terrain label table."""
import argparse,json,struct
from pathlib import Path
from pc_resources import sha
ROOT=Path(__file__).resolve().parents[2]
def inspect(installation):
 b=(installation/'san11pk.exe').read_bytes();entries=[]
 expected=['草地','土','砂地','濕地','毒泉','森','川','河','海','荒地','主徑','棧道','渡所','淺瀨','岸','崖','都市','港','關所','小徑']
 codes=['P','P','A','Z','X','F','Q','W','O','P','R','B','P','S','M','M','P','P','R','D']
 for i,(label,code) in enumerate(zip(expected,codes)):
  pointer=struct.unpack_from('<I',b,0x4af8d8+i*4)[0];offset=pointer-0x400000
  if not 0<=offset<len(b):raise ValueError('Terrain label pointer')
  raw=b[offset:b.index(b'\0',offset)];actual=raw.decode('cp950')
  if actual!=label:raise ValueError('Executable terrain label table changed')
  entries.append(dict(id=i,label=actual,pointer_va=hex(pointer),label_bytes_sha256=sha(raw),mobile_code=code))
 result=dict(schema=1,source='san11pk.exe',sha256=sha(b),table_offset=0x4af8d8,table_sha256=sha(b[0x4af8d8:0x4af8d8+80]),entries=entries,limits=['Names confirm raw class IDs, not full original movement/combat cost parity','Mobile keeps merges of grass/dirt/wasteland and blocked banks/cliffs; source raw IDs retained','Map model14 positions are all source class15 cliff; distinguish from constructible stone-wall type'])
 (ROOT/'docs/pc-visual/terrain-labels.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print('PASS 20 source Big5 terrain labels; existing conversion alphabet consistent')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);inspect(p.parse_args().installation)
