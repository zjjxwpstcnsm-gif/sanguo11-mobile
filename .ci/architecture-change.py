from pathlib import Path
import base64, hashlib, json, lzma, os, subprocess
root=Path.cwd()
def git(*args):
 return subprocess.check_output(['git',*args],text=True).strip()
def path(value):
 p=Path(value)
 assert not p.is_absolute() and '..' not in p.parts
 assert p.parts[0] in ('core','game-api','game-runtime','app','unity','scripts','docs','tools','.gitignore'), value
 return root/p
parts=[(root/f'.ci/final-transfer/{i}.b64').read_text().strip() for i in range(6)]
data=lzma.decompress(base64.b64decode(''.join(parts)))
assert hashlib.sha256(data).hexdigest()=='99fd8b4287bd1c5cfed3bc7ee9f6ab2e51e16fd7ad140fb7d66c7d7193539d7b'
payload=json.loads(data)
subprocess.run(['git','diff','--exit-code',payload['baseline'],'--','core','game-api','game-runtime','app','unity','scripts','docs/architecture','tools/architecture-client','.gitignore'],check=True)
for commit in payload['commits']:
 writes=[]; touched=set()
 for e in commit['edits']:
  old=path(e['from']) if e['from'] else None
  new=path(e['to']) if e['to'] else None
  before=old.read_bytes() if old else b''
  assert hashlib.sha256(before).hexdigest()==e['before'], e['from']
  if new:
   s=before.decode('utf-8')
   for start,end,replacement in reversed(e['splices']):
    s=s[:start]+replacement+s[end:]
   after=s.encode('utf-8')
   assert hashlib.sha256(after).hexdigest()==e['after'],e['to']
   writes.append((new,after,e['mode']))
  if old:touched.add(e['from'])
  if new:touched.add(e['to'])
 for e in commit['edits']:
  if e['from'] and e['from']!=e['to']:path(e['from']).unlink()
 for new,after,mode in writes:
  new.parent.mkdir(parents=True,exist_ok=True);new.write_bytes(after);new.chmod(0o755 if mode=='100755' else 0o644)
 subprocess.run(['git','add','-A','--',*sorted(touched)],check=True)
 subprocess.run(['git','diff','--cached','--check'],check=True)
 subprocess.run(['git','commit','-m',commit['message']],check=True)
 print('Committed',git('rev-parse','HEAD'),flush=True)
# Remove only this task's one-time transfer machinery; retain pre-existing CI triggers.
permanent=root/'.github/workflows/architecture-check.yml'
permanent.write_text((root/'.ci/architecture-check.yml').read_text())
cleanup=[p for folder in ('runtime-transfer','final-transfer') for p in (root/'.ci'/folder).rglob('*') if p.is_file()]
cleanup += [root/'.ci/architecture-change.py',root/'.ci/architecture-check.yml']
cleanup += [root/'.github/workflows/architecture-source.yml',root/'.github/workflows/architecture-apply.yml']
for p in cleanup:
 if p.exists():p.unlink()
subprocess.run(['git','add','-A','--','.ci','.github/workflows/architecture-source.yml','.github/workflows/architecture-apply.yml','.github/workflows/architecture-check.yml'],check=True)
subprocess.run(['git','diff','--cached','--check'],check=True)
subprocess.run(['git','commit','-m','ci(architecture): retain repeatable boundary checks and remove one-time transfer machinery'],check=True)
