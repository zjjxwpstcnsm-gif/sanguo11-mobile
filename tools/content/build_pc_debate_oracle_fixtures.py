#!/usr/bin/env python3
"""Freeze original unhooked rules outputs into host regression fixtures."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import json_bytes,sha,EXE_SHA
ROOT=Path(__file__).resolve().parents[2]
def build(output):
 p=ROOT/'docs/handoff/20261004/session1/debate-rules-native.json.gz';raw=p.read_bytes()
 if sha(raw)!='bf60b64a209a2c147b11ab2711760ebddea0a6d167ec3999e451518f2d6450e8':raise ValueError('Original oracle changed')
 r=json.loads(gzip.decompress(raw))
 if r['sourceExecutableSha256']!=EXE_SHA:raise ValueError('Original executable differs')
 output.mkdir(parents=True,exist_ok=False);rows=[]
 for v in r['capacity']:rows.append('capacity\t%d\t%d'%(v['currentIntelligence'],v['slotsIncludingReconsider']))
 for v in r['cards']:rows.append('card\t%d\t%d\t%d'%(v['nativeCard'],v['topic'],v['sizeZeroBased']))
 for v in r['comparison']:rows.append('compare\t%d\t%d\t%d\t-1\t0\t-1\t0\t%d'%(v['topic'],v['left'],v['right'],v['winner']))
 for v in r['furyComparisons']:rows.append('compare\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d'%(v['topic'],v['left'],v['right'],v['leftTemper'],v['leftFury'],v['rightTemper'],v['rightFury'],v['winner']))
 for v in r['damage']:rows.append('damage\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d'%(v['topic'],v['card'],v['temper'],v['fury'],v['modifier'],v['seed'],v['damage'],v['finalNativeRng']))
 for v in r['clamps']:rows.append('clamp\t%d\t%d\t%d'%(v['input'],v['health'],v['anger']))
 data=('\n'.join(rows)+'\n').encode();(output/'original-rules.tsv').write_bytes(data)
 flow_raw=(ROOT/'docs/handoff/20261004/session1/debate-flow-native.json.gz').read_bytes()
 if sha(flow_raw)!='193ca13a6bb92a3c1d389934166249611536150a50733ba4fd183fe786a5fca7':raise ValueError('Original initial oracle changed')
 initial=[]
 for v in json.loads(gzip.decompress(flow_raw))['cases']:
  state=bytes.fromhex(v['initialStateHex']);row=v['currentIntelligence']+v['nativePersonality']+v['nativeTalkMasks']+[v['seed'],struct.unpack_from('<i',state,0x168)[0],v['trace'][0]['nativeRng']]
  # Native initialization consumes all deck/hand RNG before frame0. Frame0
  # is independently checked to leave this RNG unchanged by the flow oracle.
  for side in range(2):
   base=0x10+side*0xa0
   offsets=[4,8,12,16]+list(range(0x14,0x30,4))+[0x30]+list(range(0x34,0x7c,4))+[0x7c]+list(range(0x80,0x94,4))+[0x94]
   row.extend(struct.unpack_from('<i',state,base+offset)[0]for offset in offsets)
  initial.append('\t'.join(map(str,row)))
 initial_data=('\n'.join(initial)+'\n').encode();(output/'original-initial.tsv').write_bytes(initial_data)
 effects_raw=(ROOT/'docs/handoff/20261004/session1/debate-effects-native.json.gz').read_bytes()
 if sha(effects_raw)!='37922ad1c3529fcefe81ad34b5e49dd67dbb800d86d5e5a719582616fee96dbc':raise ValueError('Original effects oracle changed')
 effects=json.loads(gzip.decompress(effects_raw));effect_rows=[]
 for v in effects['legality']:effect_rows.append('legal\t'+'\t'.join(map(str,v)))
 for v in effects['effects']:effect_rows.append('effect\t'+'\t'.join(map(str,v)))
 for v in effects['ordinaryAnger']:effect_rows.append('anger\t'+'\t'.join(map(str,v)))
 effects_data=('\n'.join(effect_rows)+'\n').encode();(output/'original-effects.tsv').write_bytes(effects_data)
 ui_raw=(ROOT/'docs/handoff/20261004/session1/debate-ui-counters-native.json.gz').read_bytes()
 if sha(ui_raw)!='bf1d77cfce20c2a857869e60177ab06384fc01e33d01653b1e5b2ef67da339b8':raise ValueError('Original counter queue oracle changed')
 ui_rows=[]
 for v in json.loads(gzip.decompress(ui_raw))['cases']:
  after=bytes.fromhex(v['afterStateHex']);row=[v['temper'],v['counter'],v['side'],v['seed'],v['nativeRng'],v['originalUiRandomDraws']]
  for side in range(2):row.extend(struct.unpack_from('<7i',after,0x24+0xa0*side))
  choices=[struct.unpack_from('<i',bytes.fromhex(q['rawHex']),0x100)[0]for q in v['queue']if q['type']==15]
  if len(choices)!=v['originalUiRandomDraws']:raise ValueError('Original counter animation record count differs')
  row.extend(choices+([-1]if len(choices)==1 else []))
  ui_rows.append('\t'.join(map(str,row)))
 ui_data=('\n'.join(ui_rows)+'\n').encode();(output/'original-ui-counters.tsv').write_bytes(ui_data)
 selected_raw=(ROOT/'docs/handoff/20261004/session1/debate-selected-effects-native.json.gz').read_bytes()
 if sha(selected_raw)!='b7de906f06356e0f2ef348113644f35a1e6470c9364e32ce664f779a5284a25e':raise ValueError('Original selected-card effects oracle changed')
 selected=json.loads(gzip.decompress(selected_raw));selected_data=('\n'.join('\t'.join(map(str,v))for v in selected['cases'])+'\n').encode()
 (output/'original-selected-effects.tsv').write_bytes(selected_data)
 fury_raw=(ROOT/'docs/handoff/20261004/session1/debate-fury-model-native.json.gz').read_bytes()
 if sha(fury_raw)!='096bae747645353e0744505f5e27632c44dec67496e89387af9ce2fa0ecb1ba1':raise ValueError('Original fury oracle changed')
 fury=json.loads(gzip.decompress(fury_raw));fury_rows=['fury\t'+'\t'.join(map(str,v))for v in fury['cases']]+['war-fury\t'+'\t'.join(map(str,v))for v in fury['warCases']]+['stage\t'+'\t'.join(map(str,v))for v in fury['stages']]
 fury_data=('\n'.join(fury_rows)+'\n').encode();(output/'original-fury-model.tsv').write_bytes(fury_data)
 (output/'provenance.json').write_bytes(json_bytes(dict(sourceExecutableSha256=EXE_SHA,oraclePackedSha256=sha(raw),fixtureSha256=sha(data),records=len(rows),initialOraclePackedSha256=sha(flow_raw),initialFixtureSha256=sha(initial_data),initialCases=len(initial),effectsOraclePackedSha256=sha(effects_raw),effectsFixtureSha256=sha(effects_data),effectsRecords=len(effect_rows),uiCountersOraclePackedSha256=sha(ui_raw),uiCountersFixtureSha256=sha(ui_data),uiCounterCases=len(ui_rows),selectedEffectsOraclePackedSha256=sha(selected_raw),selectedEffectsFixtureSha256=sha(selected_data),selectedEffectCases=len(selected['cases']),furyOraclePackedSha256=sha(fury_raw),furyFixtureSha256=sha(fury_data),furyCases=len(fury['cases']),warFuryCases=len(fury['warCases']),stageCases=len(fury['stages']),fullContestEngineIntegrated=False)));print(json.dumps(dict(records=len(rows),sha256=sha(data),initialSha256=sha(initial_data),effectsSha256=sha(effects_data),uiCountersSha256=sha(ui_data),selectedEffectsSha256=sha(selected_data),furySha256=sha(fury_data))))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();build(a.output)
