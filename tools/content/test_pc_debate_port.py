#!/usr/bin/env python3
"""Compile the complete standalone core and compare the staged port to native fixtures.

Independent output directory; does not change Gradle/APK inputs or enable the port.
"""
import argparse, hashlib, json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]

def run(java_home,output):
    output=output.resolve()
    if not output.is_relative_to(ROOT/'out/session1'):raise ValueError('Use the session1 independent cache')
    output.mkdir(parents=True,exist_ok=False);classes=output/'classes';classes.mkdir()
    sources=sorted((ROOT/'core/src/main/java').rglob('*.java'))
    sources += [ROOT/'core/src/test/java/game/sanguo/core'/name for name in ('PcDebateRulesTest.java','PcDebateStateTest.java')]
    with (output/'compile.txt').open('wb')as log:
        subprocess.run([str(java_home/'bin/javac'),'-encoding','UTF-8','-d',str(classes),*map(str,sources)],check=True,stdout=log,stderr=subprocess.STDOUT)
    classpath=':'.join(map(str,[classes,ROOT/'core/src/main/resources',ROOT/'core/src/test/resources']))
    tests=[]
    for name in ('PcDebateRulesTest','PcDebateStateTest'):
        result=subprocess.run([str(java_home/'bin/java'),'-cp',classpath,'game.sanguo.core.'+name],check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (output/(name+'.txt')).write_bytes(result.stdout);tests.append(dict(name=name,output=result.stdout.decode().strip()));print(result.stdout.decode().strip())
    digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    report=dict(schema=1,compiledSourceFiles=len(sources),sourceSha256={p.relative_to(ROOT).as_posix():digest(p)for p in sources},
                fixtureSha256={p.name:digest(p)for p in sorted((ROOT/'core/src/test/resources/pc-debate').iterdir())if p.is_file()},
                tests=tests,productionContestIntegrated=False,completeGoal=False)
    (output/'results.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();run(a.java_home,a.output)
