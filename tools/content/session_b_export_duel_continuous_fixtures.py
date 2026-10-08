#!/usr/bin/env python3
"""Continuous original trace expected values; source facts never frame answers."""
import argparse,json,hashlib,struct
from pathlib import Path
def read(path,expected):
 raw=path.read_bytes();assert hashlib.sha256(raw).hexdigest()==expected;return json.loads(raw)
def export(root,output,human=None,human_sha=None):
 if output.exists():raise ValueError('Preserve earlier import')
 r=read(human,human_sha)if human is not None else read(root/'duel-crew-full-source0.json','2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426')
 b=read(root/'duel-frame-bindings-source0.json','14223034a5deb854b7b22a823c5edc17af6b86f31e4741b8047c28d92912aa7a')
 o=read(root/'duel-opening-source0.json','00bcdc67edb1bdfbe6918b7be9a39a513d41576c0b8a404a196d79920bdd3bbf')
 t=read(root/'duel-terminal-phase-source0.json','64bddaec923f1d132ea0543ae9d3bef58d0157d142c2f2b879d04c3341fbb884')
 assert all(x['source']==r['source']and x['people']==r['people']for x in [b,o,t]);af=[]
 for a,f,protection in zip(b['actors'],o['facts'],t['protection']):
  assert a['nativeId']==f['nativeId'];af.append(':'.join(map(str,[a['nativeId'],a['personality'],a['treasureBonus'],int(a['valid']),int(bool(a['predicate488c00'])),f['age'],f['raw489080'],int(f['virtual48Value']),int(protection)]+a['warByInjury'])))
 sf=','.join(':'.join(map(str,[x['side'],x['ownSlot'],x['otherSlot'],x['slot']]+x['values']))for x in b['relations']);relations=','.join(str(int(v))for row in t['relations']for v in row)
 lines=['# continuous original model/RNG; no normal campaign proof']
 for n,x in enumerate(r['cases']):
  before=bytes.fromhex(x['modelHex']);model=struct.unpack_from('<i',before,0x23c)[0];vtable=struct.unpack_from('<i',before,0)[0]
  lines.append('\t'.join(map(str,['START',n,x['managerHex'],vtable,model,x['modelHex'],x['nativeRng'],','.join(af),sf,relations,int(o['settingsValid']),o['life'],o['difficulty'],int(t['settingValid']),t['setting']])))
  for row in x['trace']:
   fields=['FRAME',n,row['frame'],row['modelHex'],row['nativeRng'],row['returnValue']]
   if human is not None:
    fields.extend([row['inputKind']or'-',row['pose'],row['selected']])
    if 'swap'in row:fields.append(row['swap'])
   lines.append('\t'.join(map(str,fields)))
  lines.append('\t'.join(map(str,['END',n,x['finalManagerHex'],int(x['terminal'])])))
 output.write_text('\n'.join(lines)+'\n');print('PASS continuous export',len(r['cases']),len(lines),'SHA',hashlib.sha256(output.read_bytes()).hexdigest())
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('root',type=Path);p.add_argument('output',type=Path);p.add_argument('--human-corpus',type=Path);p.add_argument('--human-sha');a=p.parse_args();export(a.root,a.output,a.human_corpus,a.human_sha)
