#!/usr/bin/env python3
"""Exact linked original capture endpoint; not original ordinary trigger proof."""
import argparse,json,hashlib
from pathlib import Path
def export(source,output):
 raw=source.read_bytes();receipt=hashlib.sha256(raw).hexdigest();assert receipt in ['0bc0c09d491ff8b76cf483a7dd1ee90800fc8e04b1258051bae26fd78ca84114','824b36c8e982e055976b8e428bd16f933dcd0ea7481854cf90571b937d8eed9e','5823072e12a8d862abeecdc48b8f466d8277a16675f12fbdf289f344aebdfcbb','96ee3383a55f6b46aa003823088fd1b8b69635463457a97830f71a029dce2416'];r=json.loads(raw);assert r['linkedOriginalMembership']and r['seed']==24
 if output.exists():raise ValueError('Preserve import')
 natives=[p['nativeId']for p in r['crew']];
 if r.get('soloRightFixture'):natives[4:]=[-1,-1]
 lines=['# Exact original linked '+('declared EXECUTE'if r.get('declaredDispositionInput')==3 else 'declared RELEASE'if r.get('declaredDispositionInput')==2 else 'capture')+' SHA'+receipt,'MANAGER\t'+r['managerHex'],'MODEL\t'+r['modelHex'],'NATIVES\t'+','.join(map(str,natives)),'SEED\t'+str(r['rngBefore']),'CAPTURE\t558','FINAL_RNG\t'+str(r['rngAfter'])]
 output.write_text('\n'.join(lines)+'\n');print('PASS linked capture import',hashlib.sha256(output.read_bytes()).hexdigest())
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args();export(a.source,a.output)
