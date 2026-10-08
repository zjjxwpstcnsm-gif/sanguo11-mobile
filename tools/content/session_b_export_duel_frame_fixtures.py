#!/usr/bin/env python3
"""Original whole-frame differential inputs and independent source binding facts."""
import argparse,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def export(corpus,bindings,output,binding_sha,phases,exchange_source=None,settlement_source=None):
 if output.exists():raise ValueError('Preserve earlier fixture import')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);raw=bindings.read_bytes();assert sha(raw)==binding_sha;b=json.loads(raw);assert r['exeSha']==b['exeSha']==EXE_SHA and r['source']==b['source']and r['people']==b['people']and b['worldAndRngPureAfterCacheWarm']
 relations={(x['side'],x['ownSlot'],x['otherSlot'],x['slot']):x['values']for x in b['relations']};lines=['# Original complete frame corpus '+CORPUS_SHA,'# Original source binding SHA '+binding_sha+'; incomplete full engine, not ordinary APK proof']
 settings=None
 if exchange_source is not None:
  raw=exchange_source.read_bytes();assert sha(raw)=='95f15237ab7c85c0c97abe96017c5e9e57008089bf3a1467bd3a953cf7345484';settings=json.loads(raw);assert settings['exeSha']==EXE_SHA and settings['source']==r['source']
 if 6 in phases:assert settings is not None
 numeric_settings=None
 if settlement_source is not None:
  raw=settlement_source.read_bytes();assert sha(raw)=='46de388a09e8814d88c456ac45ddd1cca34f9f5abe9c8c18f8d44f4737da41b3';numeric_settings=json.loads(raw);assert numeric_settings['exeSha']==EXE_SHA and numeric_settings['source']==r['source']and settings is not None
 if 9 in phases:assert numeric_settings is not None
 for case in r['cases']:
  assert case['wholeSourceWorldUnchanged'];before=case['modelHex'];rng=case['nativeRng']
  for row in case['trace']:
   state=bytes.fromhex(before);phase,next_phase=struct.unpack_from('<2i',state,4);effective=next_phase if 0<=next_phase<=12 else phase
   if effective in phases:
    af=[];sf=[];active=[struct.unpack_from('<i',state,0xe8+side*0xec)[0]for side in range(2)];assert all(0<=v<3 for v in active)
    for side in range(2):
     for slot in range(3):
      a=b['actors'][side*3+slot];offset=0x24+side*0xec+slot*64;assert struct.unpack_from('<I',state,offset)[0]==a['pointer'];injury=struct.unpack_from('<i',state,offset+12)[0];assert 0<=injury<4
      af.append(':'.join(map(str,[a['nativeId'],a['personality'],a['warByInjury'][injury],a['treasureBonus'],int(a['valid']),int(bool(a['predicate488c00']))])))
      sf.append(':'.join(map(str,relations[(side,active[side],active[1-side],slot)])))
    fields=['R',before,row['modelHex'],row['returnValue'],rng,row['nativeRng'],','.join(af),','.join(sf)]
    if settings is not None:fields.extend([int(settings['lifeValid']),settings['lifeOption']])
    if numeric_settings is not None:fields.extend([int(numeric_settings['difficultyValid']),numeric_settings['difficulty']])
    lines.append('\t'.join(map(str,fields)))
   before=row['modelHex'];rng=row['nativeRng']
 output.write_text('\n'.join(lines)+'\n');print('PASS whole-frame source fixtures',len(lines)-2,'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('corpus',type=Path);p.add_argument('bindings',type=Path);p.add_argument('output',type=Path);p.add_argument('--binding-sha',required=True);p.add_argument('--phases',type=int,nargs='+',required=True);p.add_argument('--exchange-source',type=Path);p.add_argument('--settlement-source',type=Path);a=p.parse_args();export(a.corpus,a.bindings,a.output,a.binding_sha,a.phases,a.exchange_source,a.settlement_source)
