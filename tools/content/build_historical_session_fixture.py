#!/usr/bin/env python3
"""Archived v33 TEST fixture only; never a gameplay baseline or working-tree replacement."""
import argparse,hashlib,io,json,os,subprocess,tarfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve()
if ROOT/'out' not in out.parents:raise ValueError('Use a fresh project out directory')
out.mkdir(parents=True,exist_ok=False);revision='9548bb350051150b21a61213f9068ffb1b7506c0';archive=subprocess.check_output(['git','archive',revision,'core/src/main'],cwd=ROOT);source=out/'archived-test-source';source.mkdir()
with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
 for m in tar.getmembers():
  if m.name.startswith('/') or '..' in Path(m.name).parts or m.issym() or m.islnk():raise ValueError('Unexpected archive member')
 tar.extractall(source)
java=Path(os.environ['JAVA_HOME'])/'bin/java';javac=java.with_name('javac');classes=out/'classes';classes.mkdir();helper=ROOT/'game-runtime/src/test/java/game/sanguo/runtime/BaselineSequence.java';writer=ROOT/'tools/content/architecture/HistoricalSessionFixtureWriter.java';files=sorted((source/'core/src/main/java').rglob('*.java'))+[helper,writer];arguments=out/'javac-inputs.txt';arguments.write_text('\n'.join('"'+str(f)+'"' for f in files)+'\n')
def run(name,args):
 with (out/(name+'.log')).open('wb') as log:subprocess.run(list(map(str,args)),check=True,stdout=log,stderr=log)
run('javac',[javac,'--release','17','-encoding','UTF-8','-d',classes,'@'+str(arguments)]);cp=os.pathsep.join(map(str,[classes,source/'core/src/main/resources']));run('historical-sequence',[java,'-Xmx768m','-cp',cp,'game.sanguo.runtime.BaselineSequence',out/'actual-9548bb35.txt']);golden=ROOT/'game-runtime/src/test/resources/architecture/baseline-9548bb35.txt';assert (out/'actual-9548bb35.txt').read_bytes()==golden.read_bytes()
run('historical-fixture',[java,'-Xmx768m','-cp',cp,'game.sanguo.runtime.HistoricalSessionFixtureWriter',out/'prepared-9548bb35-v33.sg11']);raw=(out/'prepared-9548bb35-v33.sg11').read_bytes();digest=hashlib.sha256(raw).hexdigest();assert golden.read_text().splitlines()[0]=='initial '+digest
states=[]
for number,line in enumerate(golden.read_text().splitlines()):
 file=out/'states'/('initial.sg11' if number==0 else str(number-1)+'.sg11');digest_state=hashlib.sha256(file.read_bytes()).hexdigest();assert digest_state==line.split()[-1];states.append(dict(name=file.name,sha256=digest_state,bytes=file.stat().st_size))
j=dict(scope='genuine v33 historical TEST fixture; never copied to app content, user slots or game baseline',source_commit=revision,old_seven_states_byte_equal=True,fixture_sha256=digest,fixture_bytes=len(raw),seven_saved_states=states,writer_sha256=hashlib.sha256(writer.read_bytes()).hexdigest(),historical_golden_sha256=hashlib.sha256(golden.read_bytes()).hexdigest(),helper_sha256=hashlib.sha256(helper.read_bytes()).hexdigest(),source_archive_sha256=hashlib.sha256(archive).hexdigest());(out/'manifest.json').write_text(json.dumps(j,indent=2)+'\n');print(json.dumps(j),flush=True)
