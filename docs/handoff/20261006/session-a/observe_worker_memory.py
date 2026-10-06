#!/usr/bin/env python3
"""Read only actual source-child smaps_rollup. Does not request a process GC."""
import argparse,json,pathlib,subprocess,time,re
ADB='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
def source_child(command):return any(name in command for name in ['libpc_effect_worker.so','libpc_effect_fire_worker.so'])
def shell(*args):return subprocess.check_output([ADB,'-s','emulator-5554','shell',*args],timeout=20,text=True)
p=argparse.ArgumentParser();p.add_argument('--session',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
a.output.parent.mkdir(parents=True,exist_ok=True);started=time.monotonic()
with a.output.open('x') as out:
 while time.monotonic()-started<4500:
  state=json.loads(a.session.read_text())
  if state['stage']=='restored-verified':break
  record={'time':time.time(),'stage':state['stage'],'sourceChildren':[],'scope':'per-child smaps_rollup only, not Java/native allocator bytes or GPU VRAM; independent samples'}
  try:
   for row in shell('ps','-A','-o','PID,PPID,ARGS').splitlines()[1:]:
    fields=row.split(None,2)
    if len(fields)!=3 or not source_child(fields[2]):continue
    pid,parent,cmd=fields
    if not pid.isdigit() or not parent.isdigit():continue
    # Avoid PID reuse during read. Exact executable filename in both snapshots.
    before=shell('cat','/proc/'+pid+'/cmdline').replace('\0',' ').strip()
    if not source_child(before):continue
    raw=shell('cat','/proc/'+pid+'/smaps_rollup')
    after=shell('cat','/proc/'+pid+'/cmdline').replace('\0',' ').strip()
    if before!=after:continue
    stats={m.group(1):int(m.group(2)) for m in re.finditer(r'^([A-Za-z_]+):\s+(\d+) kB$',raw,re.M)}
    record['sourceChildren'].append({'pid':int(pid),'parentPid':int(parent),'command':before,'KiB':stats})
  except Exception as error:record['unavailable']=str(error)
  out.write(json.dumps(record)+'\n');out.flush();time.sleep(2)
