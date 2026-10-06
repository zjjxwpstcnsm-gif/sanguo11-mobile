#!/usr/bin/env python3
import pathlib,subprocess,json,hashlib,collections,sys
ROOT=pathlib.Path(__file__).resolve().parents[4];OUT=ROOT/'out/session-a/media-identity';OUT.mkdir(parents=True,exist_ok=True)
saved=pathlib.Path(sys.argv[1]).resolve() if len(sys.argv)>1 else None
COMMON=ROOT/'out/session-a/android-java/classes';java=ROOT/'docs/handoff/20261006/session-a/MediaIdentityProbe.java'
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',str(COMMON),'-d',str(OUT),str(java)],check=True)
with (OUT/'latest-source-identities.tsv').open('w') as f:subprocess.run(['java','-Xmx384m','-cp',str(OUT)+':'+str(COMMON)+':'+str(ROOT/'core/src/main/resources'),'game.sanguo.mobile.MediaIdentityProbe']+([str(saved)] if saved else []),stdout=f,check=True)
path=ROOT/'app/src/main/assets/portraits/pc/media-manifest.json';manifest=json.loads(path.read_text());join={(x['officerId'],x['nativeId'],x['sourceVariant']):x for x in manifest['identities']};images={(x['faceId'],x['imageGroup']):x for x in manifest['images']};flags=manifest['faceFlags'];start=manifest['flagStartFace'];rows=collections.defaultdict(lambda:dict(officers=0,sourceKnown=0,approvedMediaJoin=0,normalImage=0,unknowns=[],gaiji=[]));allimages=set()
for line in (OUT/'latest-source-identities.tsv').read_text().splitlines():
 v=line.split('\t');source,oid,name,year=v[:4];oid=int(oid);year=int(year);row=rows[source];row['officers']+=1
 if v[4]=='unknown':row['unknowns'].append(dict(officerId=oid,name=name,reason='saved SourceInfo missing'));continue
 native=int(v[4]);row['sourceKnown']+=1;person=join.get((oid,native,v[5]))
 if not person or [person['sourcePath'],person['sourceSha256'],person['recordSha256']]!=v[6:9]:row['unknowns'].append(dict(officerId=oid,name=name,reason='exact media provenance join rejected'));continue
 row['approvedMediaJoin']+=1;face=person['faceId'];age=year-person['birth']+1
 if 0<=face<1000 and age>=person['ageThreshold']:face+=1000
 flag=flags[face-start] if start<=face<start+len(flags) else 0
 if not ((flag>>24)&2):face=2100 if person['sexRaw']==1 else 2000
 image=images.get((face,0))
 if image:row['normalImage']+=1;allimages.add(image['asset'])
 else:row['unknowns'].append(dict(officerId=oid,name=name,reason='resolved normal image missing',faceId=face))
 if oid in [156234,844857,598828,850922]:row['gaiji'].append(dict(officerId=oid,nativeId=native,currentName=name,normalFaceId=face,dynamicSelector=person['dynamicSelector'],recordSha256=person['recordSha256']))
for image in manifest['images']:
 p=ROOT/'app/src/main/assets'/image['asset'];assert hashlib.sha256(p.read_bytes()).hexdigest()==image['pngSha256'],p
report={'scope':('actual installed16 normal source captures' if saved else 'host latest16 source previews')+' saved-media projection and exact manifest join/assetSHA; not all Android caller/pixels/lifecycle/age acceptance','actualInstalledCapturesUsed':saved is not None,'capturedSourceDirectory':str(saved) if saved else None,'sources':dict(rows),'sourceCount':len(rows),'sourceOfficerJoins':sum(x['approvedMediaJoin'] for x in rows.values()),'sourceUnknowns':sum(len(x['unknowns']) for x in rows.values()),'uniqueNormalAssetsResolved':len(allimages),'allManifestPngShaVerified':len(manifest['images']),'mediaManifestSha256':hashlib.sha256(path.read_bytes()).hexdigest(),'old652And4CountReused':False,'normalAndroidValidated':False,'allCallerCoverage':'pending','completeGoal':False}
(ROOT/'docs/handoff/20261006/session-a/MEDIA_IDENTITY_AUDIT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='sources'},ensure_ascii=False))
