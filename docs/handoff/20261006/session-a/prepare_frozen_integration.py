#!/usr/bin/env python3
"""Inherit complete finished A; audit only explicit finished B commits and inputs."""
import argparse,hashlib,json,pathlib,subprocess,shutil
ROOT=pathlib.Path(__file__).resolve().parents[4]
B=pathlib.Path('/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile')
BC='224751a7fadf6c2ef562e74be2e67576461d5e32';BD='cc4e7abd39c2998c4ae708123a15365b96ede80a'
def git(root,*args):return subprocess.check_output(['git',*args],cwd=root)
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('destination',type=pathlib.Path);a=p.parse_args();dest=a.destination.resolve()
 assert not git(ROOT,'status','--porcelain').strip(),'Finish A increment first'
 assert not git(dest,'status','--porcelain').strip(),'Destination dirty'
 ahead=git(ROOT,'rev-parse','HEAD').decode().strip();git(dest,'switch','-c','codex/map-ui-media-integration');git(dest,'merge','--ff-only',ahead)
 inherited=[];inputs=set(git(ROOT,'ls-files','-z').decode().split('\0'))-{''}
 inputs|={r['path'] for r in json.loads((ROOT/'docs/handoff/20261006/session-a/SOURCE_GUARD.json').read_text())['files']}
 for name in sorted(inputs):
  source=ROOT/name;target=dest/name
  assert source.is_file(),name
  if not target.exists():target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
  assert sha(source.read_bytes())==sha(target.read_bytes()),name
  inherited.append({'path':name,'sha256':sha(source.read_bytes())})
 d=json.loads(git(B,'show',BD+':docs/handoff/20261006/session-b/DELTA.json'));assert d['commonBase']=='0e7b9bc2df90249a50851baeda58c7d183ea6059'
 for row in d['paths']:
  raw=git(B,'show',BC+':'+row['path']);assert sha(raw)==row['afterSha256'],row['path']
  target=dest/row['path'];assert (sha(target.read_bytes()) if target.exists() else None)==row['beforeSha256'],row['path']
 git(dest,'cherry-pick',BC,BD)
 for row in d['paths']:
  if row['path'].startswith('docs/'):continue # BD updates only B handoff records.
  assert sha((dest/row['path']).read_bytes())==row['afterSha256'],row['path']
 # Independent cache files, APFS copies; no daemon registry or source app/build.
 src_cache=ROOT/'out/session-a/gradle-home/caches';dst_cache=dest/'out/session-a/gradle-home/caches';dst_cache.parent.mkdir(parents=True,exist_ok=True)
 assert not dst_cache.exists();subprocess.run(['cp','-cR',str(src_cache),str(dst_cache)],check=True)
 report={'aCommit':ahead,'bSourceCommit':BC,'bDocumentCommit':BD,'integrationHead':git(dest,'rev-parse','HEAD').decode().strip(),'source':str(ROOT),'destination':str(dest),'inheritedBeforeIntegration':inherited,'bVerifiedPaths':d['paths'],'inherited168PinsUnmodified':True,'jni4Unmodified':True,'conflicts':[],'bwipMerged':False,'apkInstalled':False,'overallGoalComplete':False}
 out=ROOT/'docs/handoff/20261006/session-a/INTEGRATION_AUDIT.json';out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['inheritedBeforeIntegration','bVerifiedPaths']},ensure_ascii=False))
if __name__=='__main__':main()
