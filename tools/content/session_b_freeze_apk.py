#!/usr/bin/env python3
"""Record exact effective build inputs before build; freeze only unchanged successful artifacts."""
import argparse,hashlib,json,subprocess,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def paths():
 names=set(p.decode()for p in subprocess.check_output(['git','ls-files','-z','--cached','--others','--exclude-standard'],cwd=ROOT).split(b'\0')if p)
 names.update(str(p.relative_to(ROOT))for p in (ROOT/'out/pc-native-runtime/jniLibs').rglob('*')if p.is_file())
 names.update(str(p.relative_to(ROOT))for p in (ROOT/'out/session-b/readonly-theme-dependencies').rglob('*')if p.is_file())
 return sorted(name for name in names if (ROOT/name).is_file())
def main(a):
 out=ROOT/'out/session-b'/a.label
 if a.mode=='capture':
  out.mkdir(parents=True,exist_ok=False)
  rows=[dict(path=name,bytes=(ROOT/name).stat().st_size,sha256=sha(ROOT/name))for name in paths()]
  report=dict(head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip(),branch=subprocess.check_output(['git','branch','--show-current'],cwd=ROOT).decode().strip(),files=rows)
  (out/'build-inputs.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('captured',len(rows),'effective inherited+B+A-frozen inputs');return
 report=json.loads((out/'build-inputs.json').read_text());bad=[r['path']for r in report['files']if not (ROOT/r['path']).is_file()or sha(ROOT/r['path'])!=r['sha256']]
 if bad:raise ValueError('Inputs changed during build: '+repr(bad))
 if set(paths())!={r['path']for r in report['files']}:raise ValueError('Effective input path set changed during build')
 frozen=out/'frozen';frozen.mkdir(exist_ok=False);artifacts=[]
 for rel in ['app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  source=ROOT/rel;target=frozen/source.name;shutil.copy2(source,target);assert sha(source)==sha(target)
  artifacts.append(dict(path=str(target),bytes=target.stat().st_size,sha256=sha(target)))
 guard={r['path']:r['sha256']for r in report['files']if r['path'].startswith(('app/src/main/','core/src/main/','game-api/src/main/','game-runtime/src/main/','out/pc-native-runtime/','out/session-b/readonly-theme-dependencies/'))or r['path']in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']}
 (frozen/'source-guard.json').write_text(json.dumps(guard,indent=2)+'\n');report['artifacts']=artifacts;report['inputsUnchanged']=True;report['compiledThemeDependencies']=json.loads((ROOT/'out/session-b/readonly-theme-dependencies/manifest.json').read_text())
 (out/'frozen-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(artifacts,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('mode',choices=['capture','freeze']);p.add_argument('label');a=p.parse_args()
 if not a.label.replace('-','').isalnum():raise ValueError('Simple unique label required')
 main(a)
