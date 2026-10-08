#!/usr/bin/env python3
"""Independent full original postload court observations across all16 sources."""
import argparse,concurrent.futures,json
from pathlib import Path
from session_b_pc_duel_ruler_context import inspect
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def one(args):
 installation,root,index=args;path=root/('duel-court-source-%02d-v1.json'%index)
 if path.exists():raise ValueError('Preserve original earlier court evidence')
 inspect(installation,path,index,False);r=json.loads(path.read_text());return dict(index=index,path=str(path),sha256=sha(path.read_bytes()),source=r['source'],root14=r['root14'],person=r.get('root14Person'),actualPlace=r['actual49cdf0PlaceNative'],holders=[x['nativeId']for x in r['forces']if x['original481910']],worldAndRngPure=r['fullSourceWorldAndRngQueryPure'])
def main(installation,root,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier corpus')
 rows=[]
 with concurrent.futures.ProcessPoolExecutor(max_workers=2)as pool:
  futures=[pool.submit(one,(installation,root,i))for i in range(16)]
  for future in concurrent.futures.as_completed(futures):
   row=future.result();rows.append(row);print('SOURCE',row['index'],'court',row['root14'],'home',row['actualPlace'],'holders',row['holders'],flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,sources=sorted(rows,key=lambda r:r['index']),limits=['Actual full original loaders/date/geography/493400, no GUI opening proof','Raw NPC reference and admin home preserved; no project officer ID assignment','All source differences retained; no current campaign/Save/APK completion']),indent=2)+'\n');print('PASS court16',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--root',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();main(a.installation,a.root,a.output)
