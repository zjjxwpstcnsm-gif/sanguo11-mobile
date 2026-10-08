#!/usr/bin/env python3
from pathlib import Path
import json,subprocess
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/capture-queue362'
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);r=json.loads((D/'CAPTURE_QUEUE361.json').read_text());source=Path(r['paths'][1]['stagedPath']);assert sha(source)==r['paths'][1]['afterSha256'];jdk=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin');a=subprocess.run([str(jdk/'javac'),'-d',str(OUT),str(source),str(D/'CaptureQueueRegression362.java')],capture_output=True,text=True);assert a.returncode==0,a.stderr;b=subprocess.run([str(jdk/'java'),'-cp',str(OUT),'game.sanguo.mobile.CaptureQueueRegression362'],capture_output=True,text=True,timeout=30);assert b.returncode==0,b.stdout+b.stderr;report={'actualJavaStdout':b.stdout,'exitCode':b.returncode,'stagedWriterSha256':sha(source),'androidAccepted':False,'scope':'Actual bounded writer byte order/reused buffer/slow disk overflow/exception/partial frame/terminal thread tests; not Android recording/whole music acceptance.','wholeGoalComplete':False};(D/'CAPTURE_QUEUE362.json').write_text(json.dumps(report,indent=2)+'\n');print(b.stdout)
if __name__=='__main__':main()
