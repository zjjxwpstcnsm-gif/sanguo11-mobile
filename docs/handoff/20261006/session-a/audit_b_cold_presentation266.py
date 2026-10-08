#!/usr/bin/env python3
"""Read-only exact B r12 APK/restoration/recovery-image provenance."""
from pathlib import Path
import json,tarfile,hashlib,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';B=Path('/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile');CASE=B/'out/session-b/native-duel-apk61-r12-acceptance-v1'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def members(path):
 with tarfile.open(path) as t:return {m.name:hashlib.sha256(t.extractfile(m).read()).hexdigest() for m in t if m.isfile()}
def main():
 output=DOC/'B_COLD_PRESENTATION_GAP266.json';assert not output.exists();r=json.loads((CASE/'results.json').read_text());assert r['passed'] and r['coldPassed'] and r['terminalColdPassed'] and r['serial']=='emulator-5582';apks={}
 for name,h in r['apks'].items():path=B/name;assert sha(path)==h;apks[str(path)]=h
 restore={}
 for domain in ['internal','external']:
  before=members(CASE/f'{domain}-before.tar');after=members(CASE/f'{domain}-restored.tar');assert before==after;restore[domain]={'files':len(before),'everyPathAndShaExact':True,'beforeTarSha256':sha(CASE/f'{domain}-before.tar'),'restoredTarSha256':sha(CASE/f'{domain}-restored.tar')}
 guards=[]
 for name in ['internal-before.tar','internal-post-test.tar','internal-restored.tar']:
  with tarfile.open(CASE/name) as t:
   matches=[m for m in t if m.isfile() and m.name.endswith('shared_prefs/map-renderer.xml')];assert len(matches)==1;raw=t.extractfile(matches[0]).read();xml=ET.fromstring(raw);values={x.attrib['name']:x.attrib.get('value',x.text) for x in xml};guards.append({'archive':name,'path':matches[0].name,'sha256':hashlib.sha256(raw).hexdigest(),'values':values})
 assert all(g['values']['nativeFailure']=='true' and g['values']['nativeSession']=='true' for g in guards)
 screenshots={name:sha(CASE/'evidence/native-duel-cold'/name) for name in ['human-terminal.png','normal-settled.png']}
 report={'case':str(CASE),'resultSha256':sha(CASE/'results.json'),'apks':apks,'restoration':restore,'guards':guards,'screenshots':screenshots,'ownMapHostSha256':sha(ROOT/'app/src/main/java/game/sanguo/mobile/MapHost.java'),'actualViewedNormalSettledShowsRecovery':True,'actualNativeCrashOrJavaOomStackProvided':False,'newFailureCannotBeInferredFromRecoveryAlone':True,'beforeHadSameFailureAndSessionBooleans':True,'threeDimensionalColdAcceptance':False,'nativeDuelOriginalPresentationAcceptance':False,'canonicalOrBChanges':False,'scope':'Read-only B r12 actual menu/rules/save receipts and guarded APK hashes/restoration. Inspected normal-settled pixels show recovery with real retained campaign. Before archive already has both health booleans true; post true lacks renderer timing/allocation/crash provenance. A does not equate rule PASS/recovery with renderer stability, infer a new OOM, clear user prefs, replace guards or merge Native WIP. Cold retry lifecycle and original duel display still need current-host real output acceptance.','wholeGoalComplete':False};output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['guards','apks']}))
if __name__=='__main__':main()
