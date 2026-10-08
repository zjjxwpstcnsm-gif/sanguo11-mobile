#!/usr/bin/env python3
"""Pinned actual natural terminal import; preserves native outcome and seed."""
import argparse,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha
def pack(source,output):
 if output.exists():raise ValueError('Preserve import')
 raw=source.read_bytes();assert sha(raw)=='4436f4ad74f967f775ad63aaf9c45a504058eee17e71c28c781a8d5e47145655';r=json.loads(raw);e=r['selected'];assert r['exeSha']==EXE_SHA and r['wholeWorldAndRngRestored']and not r['completeGoal'];assert e['seed']==14 and e['frames']==657 and e['outcomes']==[0,2,0,0,0,0]and r['natives']==[365,116,466,558,14,517];assert list(struct.unpack_from('<6i',bytes.fromhex(e['managerHex']),0x64))==e['outcomes']
 lines=['# Actual full657-frame original natural death/callback SHA'+sha(raw),'MODEL\t'+e['modelHex'],'MANAGER\t'+e['managerHex'],'NATIVES\t'+','.join(map(str,r['natives'])),'OPTIONS\t0,2,0','SEED\t'+str(e['terminalRng']),'FINAL_RNG\t'+str(e['rngAfter']),'DEATH\t'+str(e['deadNative'])]
 output.write_text('\n'.join(lines)+'\n');print('PASS pinned natural-death import',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args();pack(a.source,a.output)
