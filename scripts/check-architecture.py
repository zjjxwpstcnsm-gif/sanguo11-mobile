#!/usr/bin/env python3
from pathlib import Path
import json, re, hashlib
root=Path(__file__).resolve().parents[1]
def require(ok,message):
    if not ok: raise SystemExit("ARCHITECTURE: "+message)
for module in ["core","game-api"]:
    for file in (root/module/"src/main/java").rglob("*.java"):
        text=file.read_text()
        forbidden=["import android.","import com.google.android.filament.","import game.sanguo.mobile.","import game.sanguo.runtime."]
        if module=="game-api":forbidden.append("import game.sanguo.core.")
        require(not any(x in text for x in forbidden),str(file)+" forbidden dependency")
allowed=set(json.loads((root/"docs/architecture/LEGACY_CORE_ALLOWLIST.json").read_text()))
actual={str(p.relative_to(root)) for p in (root/"app/src/main/java").rglob("*.java") if "game.sanguo.core" in p.read_text()}
require(actual<=allowed,"new direct core consumers need reviewed migration boundary: "+str(sorted(actual-allowed)))
text=(root/"app/src/main/java/game/sanguo/mobile/bridge/AndroidGameBridge.java").read_text()
require("MainActivity" not in text,"JNI adapter must not call Activity business methods")
text=(root/"app/src/main/java/game/sanguo/mobile/FilamentMapView.java").read_text()
require(not re.search(r"\bWorld\b",text),"Filament renderer cannot accept or query World")
bootstrap=(root/"unity/Assets/Sanguo/Bootstrap/TrialEntry.cs").read_text()
require(len(bootstrap.splitlines())<65,"bootstrap must remain composition, not state/rules/transport")
assemblies={}
for p in (root/"unity/Assets/Sanguo").rglob("*.asmdef"):
    v=json.loads(p.read_text());require(v['name'] not in assemblies,"duplicate assembly");assemblies[v['name']]=v
    require(not(v.get('includePlatforms') and v.get('excludePlatforms')),"conflicting platforms")
def visit(name,trail):
    require(name in assemblies,"missing assembly "+name)
    require(name not in trail,"assembly cycle "+str(trail))
    for dep in assemblies[name].get('references',[]):visit(dep,trail+[name])
for name in assemblies:visit(name,[])
for name in ['Sanguo.Contracts','Sanguo.Client']:require(assemblies[name]['noEngineReferences'],"pure contract/client must not reference UnityEngine")
for p in (root/"unity/Assets/Sanguo").rglob("*.cs"):
    text=p.read_text();path=str(p.relative_to(root))
    require("AndroidJava" not in text or '/Transport/Android/' in path,"JNI scattered: "+path)
    if '/Contracts/' in path or '/Client/' in path:require('UnityEngine' not in text,"pure client references UnityEngine: "+path)
guids={}
for p in (root/"unity/Assets").rglob('*.meta'):
    m=re.search(r'^guid: (\w+)$',p.read_text(),re.M)
    require(m is not None,"missing meta GUID: "+str(p));g=m.group(1)
    require(g not in guids,"duplicate GUID: "+g);guids[g]=str(p.relative_to(root))
for path,digest in {'unity/Assets/Sanguo/Bootstrap/TrialEntry.cs.meta': 'e4905cce44043964544a26875204eb9e18b7ce75962e96ef0356bfe596ae38eb', 'unity/Assets/Sanguo/Editor/ExportAndroid.cs.meta': '0a2a1272ecfcf30a428f1a8a173fed94f757233c3dd7d3aa08fceb4992d443bd', 'unity/Assets/Sanguo/Bootstrap.meta': 'fa671069c25a6c10769b90830b53bf62fd41aaa955b24097950daf4bd25c0e16', 'unity/Assets/Sanguo/Editor.meta': '711262dfb857fa26c5b9cdcda179e2f757c96861e2faaf2a38da2c7f8a7d2d24'}.items():
    require(hashlib.sha256((root/path).read_bytes()).hexdigest()==digest,"original moved meta changed: "+path)
print("Architecture boundary PASS: reviewed legacy consumers, pure modules, JNI location, assembly DAG and preserved meta identities")
