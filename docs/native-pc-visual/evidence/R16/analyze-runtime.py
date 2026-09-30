#!/usr/bin/env python3
"""Summarize original installed samples without turning submissions into presented FPS."""
import pathlib,re,csv,json,math,sys,hashlib
root=pathlib.Path(sys.argv[1]);rows=[]
for mode in ['local','national']:
 p=root/mode;files=list(p.rglob('r16-'+mode+'.txt'));log=files[0].read_text() if files else ''
 instrumentation=(p/'instrumentation.txt').read_text()
 pss=[int(v) for v in re.findall(r'pss_kib=(\d+)',log)];raw={}
 for f in p.rglob('r16-*-frames-*.csv'):
  for row in csv.DictReader(f.open()):
   if 'attempt' in row:raw[int(row['attempt'])]={k:int(v) for k,v in row.items()}
 vals=list(raw.values())
 def quantile(v,f):return sorted(v)[math.ceil(len(v)*f)-1]/1e6 if v else None
 counts=re.findall(r'cpuChunks=(\d+) pending=(\d+) submitted=(\d+).*beginAttempts=(\d+) beginSkipped=(\d+)',instrumentation)
 item={'mode':mode,'status':'PASS_SCOPED' if 'PASS R16 '+mode in instrumentation else 'FAIL','firstReadyElapsedMs':re.findall(r'FIRST_READY_MS=(\d+)',log),'failureCounters':list(map(int,counts[0])) if counts else None,'failureCounterOrder':['cpuChunks','pending','submitted','beginAttempts','beginSkipped'],'pssSamples':len(pss),'pssMiBMin':min(pss)/1024 if pss else None,'pssMiBMax':max(pss)/1024 if pss else None,'uniqueOwnerSamples':len(vals),'admittedSamples':sum(v['admitted'] for v in vals),'ownerWallP95Ms':quantile([r['owner_wall_ns'] for r in vals],.95),'ownerCpuP95Ms':quantile([r['owner_cpu_ns'] for r in vals],.95),'uploadOver2ms':sum(r['mesh_upload_wall_ns']>2000000 for r in vals),'largestMeshUploadWallMs':max([r['mesh_upload_wall_ns'] for r in vals],default=0)/1e6 if vals else None,'measurement':'loading/route attempts incl rejected, NOT presented FPS/steady phone performance','presentIntervalP95Ms':None,'gpuDurationMs':None}
 trace=p/'trace.perfetto-trace'
 if trace.exists():
  data=trace.read_bytes();item['trace']={'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'ownerMarkerStringFound':b'R16.ownerFrame' in data,'mapProjectionMarkerStringFound':b'R16.mapProjection' in data,'frameTimelineStringFound':b'FrameTimeline' in data,'scope':'raw scheduler/gfx trace; string scan not semantic analysis'}
 rows.append(item)
print(json.dumps(rows,indent=2))
