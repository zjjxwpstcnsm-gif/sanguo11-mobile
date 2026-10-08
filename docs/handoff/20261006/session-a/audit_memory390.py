#!/usr/bin/env python3
"""Actual380 log checkpoints only, independent memory estimates and process-bound lifecycle."""
from pathlib import Path
import json,re,time
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from run_music_reserve378 import live_case
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/reserve-map380'
def main():
 out=D/'MEMORY_LIFETIME390.json';assert not out.exists()
 while True:
  r=read(CASE/'session.json')
  if r['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 assert all(x['exactRegularFileSha'] for x in r['restoration'].values());log=CASE/'logcat.txt';text=log.read_text(errors='replace');rows=[]
 for number,line in enumerate(text.splitlines(),1):
  if 'Sanguo3D:' not in line or 'javaHeapUsedBytes=' not in line:continue
  pid=re.match(r'^\d+-\d+ \S+\s+(\d+)\s+',line);assert pid,line
  values={k:int(v) for k,v in re.findall(r'\b(javaHeapUsedBytes|javaHeapLimitBytes|nativeHeapAllocatedBytes|retainedMeshPayloadBytes)=(\d+)',line)}
  assert len(values)==4;rows.append({'pid':int(pid[1]),'line':number,'deviceTime':line[:18],**values})
 assert rows;byPid={}
 for row in rows:
  p=str(row['pid']);summary=byPid.setdefault(p,{'checkpointCount':0,'javaCheckpointMaximumBytes':0,'nativeAllocatorCheckpointMaximumBytes':0,'retainedMeshArraysCheckpointMaximumBytes':0,'heapLimitsBytes':[]})
  summary['checkpointCount']+=1
  for k,field in [('javaCheckpointMaximumBytes','javaHeapUsedBytes'),('nativeAllocatorCheckpointMaximumBytes','nativeHeapAllocatedBytes'),('retainedMeshArraysCheckpointMaximumBytes','retainedMeshPayloadBytes')]:summary[k]=max(summary[k],row[field])
  if row['javaHeapLimitBytes'] not in summary['heapLimitsBytes']:summary['heapLimitsBytes'].append(row['javaHeapLimitBytes'])
 oom=[{'line':i,'text':s} for i,s in enumerate(text.splitlines(),1) if 'OutOfMemoryError' in s or 'FATAL EXCEPTION' in s];video=read(CASE/'video/video.json');parts=video['parts']
 for p in parts:
  assert p['sha256']==p['deviceSha256']==p['deviceSha256AfterPull'];assert sha(Path(p['path']))==p['sha256']
 report={'actual369Apks':r['apks'],'actualSessionStage':r['stage'],'wholeNormal16AndColdPassed':bool(r.get('passed')),'javaNativeCheckpointSamples':len(rows),'byProcess':byPid,'actualRawCheckpoints':rows,'rawOomOrFatalLines':oom,'fatalLinesAreNotAssumedTargetOnly':True,'rawVideoParts':len(parts),'videoIndexSha256':sha(CASE/'video/video.json'),'wholeLogSha256':sha(log),'nativeChildObservationPath':r['workerObservation']['path'],'nativeChildObservationSha256':sha(Path(r['workerObservation']['path'])),'wholeInstantaneousMemoryPeakProven':False,'gpuAttributablePeakKnown':False,'nativeRiseIsNotInferredLeak':True,'scope':'Actual Sanguo3D emitted Java/native allocator/owner mesh-array checkpoints by real PID, not250ms sampler, not whole instantaneous peaks. Values independently measured, never added to PSS/child/GPU. No target-only FATAL attribution without actual PID stack, no original ARM354832 root proof. Native allocator highwater/caches can rise without persistent ownership; new390 does not prove every release. Every original game file restored and original raw video SHA checked.','wholeGoalComplete':False};out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'checkpoints':len(rows),'byProcess':byPid,'rawVideoParts':len(parts)}))
if __name__=='__main__':main()
