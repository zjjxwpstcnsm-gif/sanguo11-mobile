#!/usr/bin/env python3
"""Source-bound portable duel kernel fixtures. No normal-player acceptance claim."""
import argparse,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha
INPUT_SHA='88faf08a1cfe6574581460d9811d18ee2ea9414d8262c263b9560dcb0b5acab8'
def export(source,output,mutations=False,war=False,control=False,terminal=False,best=False,scores=False,treasures=False):
 if output.exists():raise ValueError('Preserve earlier import')
 raw=source.read_bytes();expected=sha(raw);assert expected in ({'695c688122c51277b88625988e9506ab4f6d37f8f129c971a8822d2ac1872226'}if treasures else ({'cf55f5a4e86e1e409a07131f51ca7907f2a3d1492c44950154dde133c04f25c7'}if scores else ({'99c46722b112ecfbe3e56e52316f3aed6b8a2859bd665a6aaaf4ed2fea45f3b1'}if best else ({'ea5dba2e11731e1c0224e4a43d34d06c3bfeca32c038fd38678094f5f3c49c6a'}if terminal else ({'2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'}if control else ({'91678aedc7d847b2d62d6ef1bbe14f35e987645186c0f45fe794fd212b7591a2'}if war else ({'e3334eb27b4b7b7a17c907214887fa923f6a1ebbba29a03443c768fe32454655','0818326d201c3fc09e6c405ef1b0841d9aac643a918ef54330fc569a215eadd5'}if mutations else {INPUT_SHA})))))));r=json.loads(raw);assert r['exeSha']==EXE_SHA
 lines=['# incomplete duel kernel; original source '+r['source']['scenarioId'],'# inputSHA '+expected+'; original actor pointers are only fixture references, never runtimeID arithmetic']
 if treasures:
  for x in r['cases']:
   assert x['originalRuleWorldAndRngUnchanged'];lines.append('\t'.join(map(str,['I',x['originalBonus'],','.join(str(v[0])+':'+str(v[1])for v in x['heldNativeFixture'])or'-'])))
 elif scores:
  for x in r['heldTreasures']:lines.append('\t'.join(map(str,['I',x['originalBonus'],','.join(str(v[0])+':'+str(v[1])for v in x['heldNativeItems'])or'-'])))
  for x in r['cases']:
   assert x['fullModelWorldAndRngUnchanged'];lines.append('\t'.join(map(str,['S',x['health'],x['duelWar'],x['treasureBonus'],x['score']])))
 elif best:
  for x in r['cases']:
   assert x['modelWorldUnchanged'];lines.append('\t'.join(map(str,['B',x['side'],x['active'],x['seed'],x['result'],x['nativeRng'],x['modelHex']])))
 elif terminal:
  for x in r['cases']:
   assert x['wholeSourceWorldAndRngUnchanged'];lines.append('\t'.join(map(str,['T',int(x['pending']),x['returnValue'],x['beforeHex'],x['afterHex']])))
 elif control:
  for case in r['cases']:
   before=case['modelHex'];rng=case['nativeRng']
   for row in case['trace']:
    b=bytes.fromhex(before);phase,next_phase=struct.unpack_from('<2i',b,4);effective=next_phase if 0<=next_phase<=12 else phase
    if effective in [0,2,11]:
     assert row['nativeRng']==rng;lines.append('\t'.join(map(str,['F',before,row['modelHex'],row['returnValue'],rng])))
    before=row['modelHex'];rng=row['nativeRng']
 elif war:
  for x in r['cases']:
   assert x['originalWorldAfterCacheWarmAndRngUnchanged'];lines.append('\t'.join(map(str,['W',x['nativeAbilityLowByte'],x['nativeId'],x['originalAge'],x['rawRuleOption7201990'],x['extraFlagFixture'],x['duelWar']])))
 elif mutations:
  for x in r['cases']:
   assert x['wholeWorldAndRngUnchanged'];lines.append('\t'.join(map(str,['M',x['operation'],x['side'],x['index'],x['value'],x['beforeHex'],x['afterHex']])))
 else:
  for x in r['humanSelections']:
   assert x['wholeSourceWorldAndRngUnchanged'];lines.append('\t'.join(map(str,['H',x['side'],x['selection'],int(x['accepted']),x['beforeHex'],x['afterHex']])))
  template=bytes.fromhex(r['initialModelHex']);assert len(template)==0x59c
  for x in r['legalSourceFixtures']:
   assert x['completeModelWorldAndRngUnchanged'];b=bytearray(template);struct.pack_into('<i',b,0x24+x['side']*0xec+x['fighter']*0x40+8,x['spiritFixture']);struct.pack_into('<i',b,0x10,x['roundFixture'])
   for move in x['options']:lines.append('\t'.join(map(str,['L',x['side'],x['fighter'],move['nativeMove'],move['cost'],int(move['available']),int(move['allowed']),b.hex()])))
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text('\n'.join(lines)+'\n');print('PASS native fixture',len(lines)-2,'SHA',sha(output.read_bytes()),'not whole engine ornormal acceptance')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('output',type=Path);p.add_argument('--mutations',action='store_true');p.add_argument('--war',action='store_true');p.add_argument('--control',action='store_true');p.add_argument('--terminal',action='store_true');p.add_argument('--best',action='store_true');p.add_argument('--scores',action='store_true');p.add_argument('--treasures',action='store_true');a=p.parse_args();export(a.source,a.output,a.mutations,a.war,a.control,a.terminal,a.best,a.scores,a.treasures)
