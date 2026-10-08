#!/usr/bin/env python3
"""Pinned original commander comparator and current-person fields."""
import argparse,json,hashlib
from pathlib import Path
def export(source,output):
 raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()=='ec5766662df1d5914a2b2a65d92a93725dc8da3a662d02f22af7145fc0a79726';r=json.loads(raw);assert r['worldAndRngPure']and len(r['rows'])==36
 if output.exists():raise ValueError('Preserve import')
 lines=['# Actual original4cf160 source0 comparator SHAec5766662df1d5914a2b2a65d92a93725dc8da3a662d02f22af7145fc0a79726']
 for p in r['persons']:lines.append('\t'.join(map(str,['P',p['native'],p['status'],p['capacity'],p['leadership'],p['war'],p['merit']])))
 for p in r['rows']:lines.append('\t'.join(map(str,['R',p['left'],p['right'],int(p['less'])])))
 output.write_text('\n'.join(lines)+'\n');print('PASS original comparator export',hashlib.sha256(output.read_bytes()).hexdigest())
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args();export(a.source,a.output)
