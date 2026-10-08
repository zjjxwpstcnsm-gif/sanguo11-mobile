#!/usr/bin/env python3
"""Pinned original AI selector corpus exporter, never an answer-tape implementation."""
import argparse,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha
def export(source,output,expected):
 if output.exists():raise ValueError('Preserve earlier import')
 raw=source.read_bytes();assert sha(raw)==expected;r=json.loads(raw);assert r['exeSha']==EXE_SHA
 support=bool(r['cases'])and'individualSupport'in r['cases'][0]
 damage=bool(r['cases'])and'damage'in r['cases'][0]
 exchange=bool(r['cases'])and'operation'in r['cases'][0]
 settlement=exchange and'outputs'in r['cases'][0]
 matrix=exchange and r['cases'][0]['operation']=='pair'
 injury=exchange and'warByInjury'in r
 special_effect='sourceInjuryProtection'in r
 retreat='sourceProtection'in r
 selector='retreat'if retreat else'special-effect'if special_effect else'injury-lifecycle'if injury else'matchup-matrix'if matrix else'normal-settlement'if settlement else'normal-exchange'if exchange else'5084a0'if damage else'50b270/508820/508890'if support else r.get('originalSelector')
 if selector is None:
  assert expected=='3b320ca10370698ff67091cf687983e46c513180015ad4d97df6d7419f82dfc8'
  selector='4fb510' # Exact earlier receipt predates the selectable CLI mode.
 mode={'4fb510':'X','4fb8f0':'Y','4fafa0':'Q','5099a0':'A','4fbcb0':'P','50b270/508820/508890':'U','5084a0':'D','normal-exchange':'K','normal-settlement':'J','matchup-matrix':'G','injury-lifecycle':'O','special-effect':'V','retreat':'C'}[selector]
 lines=['# Original '+selector+'; whole duel/normal campaign incomplete','# source SHA '+expected]
 for x in r['cases']:
  if retreat:
   assert x['worldUnchanged'];facts=','.join(':'.join(map(str,[p['nativeId'],int(protection)]+war))for p,protection,war in zip(r['people'],r['sourceProtection'],r['warByInjury']));lines.append('\t'.join(map(str,['C',x['operation'],x['side'],x['slot'],x['seed'],x['nativeRng'],x['result'],x['beforeHex'],x['afterHex'],facts])));continue
  if special_effect:
   assert x['worldUnchanged'];facts=','.join(':'.join(map(str,[p['nativeId'],int(protection)]+war))for p,protection,war in zip(r['people'],r['sourceInjuryProtection'],r['warByInjury']));lines.append('\t'.join(map(str,['V',x['seed'],x['nativeRng'],x['returnValue'],int(r['difficultyValid']),r['difficulty'],x['beforeHex'],x['afterHex'],facts])));continue
  if injury:
   assert x['worldAndRngUnchanged'];facts=','.join(':'.join(map(str,[p['nativeId']]+war))for p,war in zip(r['people'],r['warByInjury']));lines.append('\t'.join(map(str,['O',x['operation'],x['side'],x['slot'],x['value'],x['beforeHex'],x['afterHex'],facts])));continue
  if matrix:
   assert x.get('modelWorldAndRngUnchanged',x.get('worldAndRngUnchanged',False));wars=x['wars']if x['operation']=='pair'else[v for side in x['wars']for v in side];lines.append('\t'.join(map(str,['G',x['operation'],','.join(map(str,wars)),x.get('ratio','-'),x.get('beforeHex','-'),x.get('afterHex','-')])));continue
  if settlement:
   assert x['worldUnchanged'];facts=','.join(':'.join(map(str,[f['nativeId'],int(f['valid'])]))for f in x['facts']);lines.append('\t'.join(map(str,['J',x['operation'],','.join(map(str,x['args']))or'-',x['returnValue'],','.join(map(str,x['outputs']))or'-',x['seed'],x['nativeRng'],int(r['difficultyValid']),r['difficulty'],x['beforeHex'],x['afterHex'],facts])));continue
  if exchange:
   assert x['worldUnchanged'];facts=','.join(':'.join(map(str,[f['nativeId'],f['personality'],f['duelWar'],int(f['valid']),f['originalAge']]))for f in x['facts']);lines.append('\t'.join(map(str,['K',x['operation'],','.join(map(str,x['args']))or'-',x['seed'],x['returnValue'],x['nativeRng'],int(r['lifeValid']),r['lifeOption'],x['beforeHex'],x['afterHex'],facts])));continue
  if damage:
   assert x['modelDeclaredWorldAndRngUnchanged'];lines.append('\t'.join(map(str,['D',x['side'],x['slot'],x['move'],x['difficulty'],int(x['difficultyValid']),':'.join(map(str,x['activeNativeIds'])),x['damage'],x['modelHex']])));continue
  assert x['modelWorldUnchanged'];facts=[]
  for f in x['facts']:
   values=[int(f['valid']),f['threshold'],int(f['p4887d0']),int(f['p488790']),int(f['p4889e0Other']),int(f['p4889e0Own']),int(f['p488910']),int(f['p48bb70']),int(f['ownStatusZero']),int(f['ownOwnerValid']),int(f['sameOwner']),f['rawLoyalty'],int(f['rawE4Equal']),f['affinityGapLowByte']]if support else[f['nativeId'],f['personality'],f['duelWar'],f['treasureBonus'],int(f['valid']),int(bool(f['predicate488c00']))]
   facts.append(':'.join(map(str,values)))
  fields=[mode,x['side'],x['seed'],x['result'],x['nativeRng'],x['modelHex'],','.join(facts)]
  if support:fields.append(','.join(':'.join(map(str,[int(f['eligible']),one[0],one[1]]))for f,one in zip(x['facts'],x['individualSupport'])))
  lines.append('\t'.join(map(str,fields)))
 output.write_text('\n'.join(lines)+'\n');print('PASS export original AI',len(lines)-2,sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('output',type=Path);p.add_argument('--expected-sha',required=True);a=p.parse_args();export(a.source,a.output,a.expected_sha)
