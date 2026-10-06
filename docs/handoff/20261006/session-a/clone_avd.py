#!/usr/bin/env python3
"""Quiesce exclusively owned5554, SHA-copy full AVD, flatten private live disk,
and enlarge only its independent copy. Original AVD and all resources retained.
"""
import hashlib,json,pathlib,subprocess,time,os,re,sys
ROOT=pathlib.Path(__file__).resolve().parents[4]
SDK=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk')
ORIGINAL=pathlib.Path('/Users/paopao/.android/avd/san11-pc-map.avd')
HOME=ROOT/'out/session-a/avd-home';CLONE=HOME/'san11-session-a-repair.avd'
LOCK=pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock')
def cmd(*args):return subprocess.run(list(map(str,args)),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE).stdout
def adb(*args):return cmd(SDK/'platform-tools/adb','-s','emulator-5554',*args)
def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):h.update(b)
 return h.hexdigest()
def main():
 if '--resume-private-grow' in sys.argv:
  owner=json.loads((LOCK/'owner.json').read_text());assert owner['root']==str(ROOT) and owner.get('purpose')=='full AVD clone/resize; only5554'
  assert b'emulator-5554' not in cmd(SDK/'platform-tools/adb','devices')
  assert (CLONE/'userdata-qemu.img').stat().st_size==16*1024**3
  audit=ROOT/'docs/handoff/20261006/session-a/AVD_CLONE.json';report=json.loads(audit.read_text());report['requiresGuestResize']=True;finish(report,audit);return
 originalRunning=subprocess.run(['kill','-0','93106'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0
 if originalRunning:
  assert adb('emu','avd','name').decode().splitlines()[0]=='san11-pc-map'
  assert 'game.sanguo.mobile.dev' not in adb('shell','ps','-A').decode(),'Active application; no AVD action'
 else:
  assert b'emulator-5554' not in cmd(SDK/'platform-tools/adb','devices'),'Another5554 process active'
 assert json.loads((ROOT/'out/session-a/resume-install-02/session.json').read_text())['stage']=='restored-verified'
 assert not CLONE.exists()
 if LOCK.exists():
  previous=json.loads((LOCK/'owner.json').read_text());assert previous['root']==str(ROOT) and previous.get('purpose')=='full AVD clone/resize; only5554'
 else:LOCK.mkdir()
 HOME.mkdir(parents=True,exist_ok=True)
 (LOCK/'owner.json').write_text(json.dumps({'root':str(ROOT),'pid':os.getpid(),'output':str(HOME),'purpose':'full AVD clone/resize; only5554'}))
 argv=cmd('ps','-p','93106','-o','command=').decode().strip() if originalRunning else 'previously inspected san11-pc-map5554; console/TERM did not exit; guest sync then avd stop, exact PID KILL'
 if originalRunning:
  assert '-avd san11-pc-map -port 5554' in argv
  adb('shell','sync');adb('emu','kill')
 began=time.monotonic();deadline=began+60;signalled=False
 while time.monotonic()<deadline:
  if subprocess.run(['kill','-0','93106'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:break
  if time.monotonic()-began>=10 and not signalled:
   # Only the exact inspected A-owned emulator PID; no broad process match.
   cmd('kill','-TERM','93106');signalled=True
  time.sleep(1)
 else:raise RuntimeError('Emulator did not stop; no copy attempted')
 files={str(p.relative_to(ORIGINAL)):sha(p) for p in ORIGINAL.rglob('*') if p.is_file() and not p.is_symlink()}
 cmd('cp','-cR',ORIGINAL,CLONE)
 assert all(sha(CLONE/p)==digest for p,digest in files.items()),'Full AVD copy SHA differs'
 report={'original':str(ORIGINAL),'clone':str(CLONE),'originalCommand':argv,'originalFileSha256':files,'copiedAllRegularFilesShaEqual':True,'originalRetained':True,'userResourcesDeleted':False,'serial':'emulator-5554','onlyPrivateDiskResized':True,'completeGoal':False}
 audit=ROOT/'docs/handoff/20261006/session-a/AVD_CLONE.json';audit.write_text(json.dumps(report,indent=2)+'\n')
 # Flatten the committed live qcow2 layer. Its original backing file is read
 # only; the booting clone must not use a shared writable userdata overlay.
 qemu=SDK/'emulator/qemu-img';live=CLONE/'userdata-live-flat.img'
 cmd(qemu,'convert','-O','raw',ORIGINAL/'userdata-qemu.img.qcow2',live)
 (CLONE/'userdata-qemu.img').rename(CLONE/'inherited-userdata-base.img')
 (CLONE/'userdata-qemu.img.qcow2').rename(CLONE/'inherited-userdata-overlay.qcow2')
 live.rename(CLONE/'userdata-qemu.img')
 cmd(qemu,'resize',CLONE/'userdata-qemu.img','16G')
 # Source already contains a valid mounted ext4 filesystem. Grow the private
 # offline copy before boot; retain stdout/stderr for exact diagnostics.
 with (HOME/'resize.txt').open('wb') as log:
  for args in [(SDK/'emulator/bin64/e2fsck','-f','-p',CLONE/'userdata-qemu.img'),(SDK/'emulator/bin64/resize2fs',CLONE/'userdata-qemu.img')]:
   r=subprocess.run(list(map(str,args)),stdout=log,stderr=subprocess.STDOUT)
   if r.returncode not in (0,1):report['requiresGuestResize']=True;break
 finish(report,audit)
def finish(report,audit):
 config=CLONE/'config.ini';text=config.read_text();text=re.sub(r'^disk.dataPartition.size\s*=.*$','disk.dataPartition.size = 17179869184',text,flags=re.M)
 text=re.sub(r'^AvdId\s*=.*$','AvdId = san11-session-a-repair',text,flags=re.M);text=re.sub(r'^avd.ini.displayname\s*=.*$','avd.ini.displayname = san11-session-a-repair',text,flags=re.M);config.write_text(text)
 for name in ['hardware-qemu.ini.lock','multiinstance.lock']:
  p=CLONE/name
  if p.exists():p.rename(CLONE/(name+'.inherited'))
 (HOME/'san11-session-a-repair.ini').write_text('avd.ini.encoding=UTF-8\npath='+str(CLONE)+'\ntarget=android-29\n')
 report['originalStillShaEqual']=all(sha(ORIGINAL/p)==digest for p,digest in report['originalFileSha256'].items());assert report['originalStillShaEqual']
 env=dict(os.environ,ANDROID_AVD_HOME=str(HOME),ANDROID_HOME=str(SDK))
 command=[str(SDK/'emulator/emulator'),'-avd','san11-session-a-repair','-port','5554','-gpu','host','-no-window','-no-snapshot-load','-no-snapshot-save','-no-boot-anim','-memory','2048','-cores','4','-audio','wav','-partition-size','16384']
 with (HOME/'emulator.log').open('wb') as log:proc=subprocess.Popen(command,env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 report.update(cloneCommand=command,clonePid=proc.pid,cloneStarted=True);audit.write_text(json.dumps(report,indent=2)+'\n');print('Independent16GiB clone started',proc.pid,flush=True)
if __name__=='__main__':main()
