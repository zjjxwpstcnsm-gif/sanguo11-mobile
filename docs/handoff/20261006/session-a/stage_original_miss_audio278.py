#!/usr/bin/env python3
"""Original58 raw PCM and source-rate playback stage, no invented outcome trigger."""
from pathlib import Path
import json,gzip,hashlib,io,wave,sys,subprocess,difflib
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/original-miss-audio278';sys.path.insert(0,str(ROOT/'tools/content'))
from pc_resources import Archive
SOURCE=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for chunk in iter(lambda:f.read(1048576),b''):h.update(chunk)
 return h.hexdigest()
def digest(raw):return hashlib.sha256(raw).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);authority=ROOT/'docs/handoff/20261004/session2/sound-banks-manifest.json.gz';a=json.loads(gzip.decompress(authority.read_bytes()));rows=[r for r in a['entries'] if 58 in r.get('soundIds',[])];assert len(rows)==1;row=rows[0];bank=next(b for b in a['banks'] if b['bank']==row['bank']);source=SOURCE/a['sourceFile'];assert sha(source)==a['sourceSha256'];reader=Archive(source)
 try:header=reader.read(row['headerResource']);body=reader.read(row['waveResource'])
 finally:reader.file.close()
 assert digest(header)==bank['headerSha256'] and digest(body)==bank['waveSha256'];descriptor=header[row['descriptorOffset']:row['descriptorOffset']+len(bytes.fromhex(row['descriptorHex']))];assert descriptor.hex()==row['descriptorHex'] and digest(descriptor)==row['descriptorSha256'];pcm=body[row['rawSampleOffset']:row['rawSampleOffset']+row['rawSampleBytes']];assert digest(pcm)==row['pcmS16leSha256'] and len(pcm)==row['samples']*2 and row['sampleRate']==22050 and row['channels']==1 and row['loopFlagRaw']==0
 buffer=io.BytesIO()
 with wave.open(buffer,'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(22050);w.writeframes(pcm)
 raw=buffer.getvalue();assert digest(raw)==row['wavSha256'];asset='app/src/main/assets/audio/pc/tactic-58.wav';target=OUT/asset;target.parent.mkdir(parents=True);target.write_bytes(raw)
 with wave.open(io.BytesIO(raw),'rb') as w:assert w.getnframes()==33553 and w.readframes(w.getnframes())==pcm
 changes=[];texts={};name='app/src/main/java/game/sanguo/mobile/PcTacticPcmPlayer.java';before=(ROOT/name).read_text();after=before.replace('Six worker-prepared original49/78 PCM tracks','Nine worker-prepared original49/78/58 PCM tracks').replace('IDS={49,78},FRAMES={45350,45644}','IDS={49,78,58},FRAMES={45350,45644,33553},RATES={44100,44100,22050}');needle='"d62f28032fce9e132a9930565770d4e5a7a2a607623354c8c568a45cb616c638"';assert after.count(needle)==1;after=after.replace(needle,needle+',"'+row['wavSha256']+'"').replace('header.getInt(24)!=44100','header.getInt(24)!=RATES[source]').replace('.setSampleRate(44100)','.setSampleRate(RATES[source])').replace('slots.size()==6','slots.size()==IDS.length*3').replace('sound!=49&&sound!=78','sound!=49&&sound!=78&&sound!=58');assert after!=before;texts[name]=(before,after)
 name='app/src/main/java/game/sanguo/mobile/SoundEffects.java';before=(ROOT/name).read_text();needle='if(sound!=49&&sound!=78)throw new IllegalArgumentException("Unverified source tactic ID");';assert before.count(needle)==1;after=before.replace(needle,'if(sound!=49&&sound!=78&&sound!=58)throw new IllegalArgumentException("Unverified source tactic ID");');texts[name]=(before,after)
 # No policy or MapHost change: the original hit policy still never treats
 # missing Strike/HP delta or human text as an authoritative miss.
 patches=[];targets=[]
 for name,(before,after) in texts.items():
  target=OUT/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(after);targets.append(target);changes.append({'path':name,'beforeSha256':sha(ROOT/name),'afterSha256':sha(target)});patches.append(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+name,tofile='b/'+name)))
 manifest_name='app/src/main/assets/audio/pc/tactic-sound-manifest.json';manifest_before=(ROOT/manifest_name).read_text();m=json.loads(manifest_before);assert [e['nativeSoundId'] for e in m['entries']]==[49,78]
 m['entries'].append({'nativeSoundId':58,'original':row,'originalAsset':'audio/pc/tactic-58.wav','asset':'audio/pc/tactic-58.wav','sampleRate':22050,'channels':1,'samples':33553,'wavBytes':len(raw),'wavSha256':digest(raw),'pcmSha256':digest(pcm),'conversion':'Original native-rate PCM preserved; no resample/gain/trim/pad','normalProducerBound':False})
 manifest_after=json.dumps(m,indent=2)+'\n';manifest_target=OUT/manifest_name;manifest_target.parent.mkdir(parents=True,exist_ok=True);manifest_target.write_text(manifest_after);texts[manifest_name]=(manifest_before,manifest_after);changes.append({'path':manifest_name,'beforeSha256':sha(ROOT/manifest_name),'afterSha256':sha(manifest_target)});patches.append(''.join(difflib.unified_diff(manifest_before.splitlines(True),manifest_after.splitlines(True),fromfile='a/'+manifest_name,tofile='b/'+manifest_name)))
 patch=DOC/'ORIGINAL_MISS_AUDIO278.patch';patch.write_text(''.join(patches));base=ROOT/'out/session-a/map-fire-upload-build269/source/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';old=ROOT/'out/session-a/native-opening-stage224/compile';dep=ROOT/'out/session-a/native-opening-stage224/dependencies';android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar');java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java');cp=':'.join(map(str,[android,old/'filament.jar',base]+sorted(dep.glob('*.jar'))));classes=OUT/'classes';classes.mkdir();command=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,targets)]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text();replay=OUT/'readback'
 for name,(before,_) in texts.items():target=replay/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(before)
 subprocess.run(['git','apply','--no-index',str(patch)],cwd=replay,check=True)
 for row2 in changes:assert sha(replay/row2['path'])==row2['afterSha256'] and sha(ROOT/row2['path'])==row2['beforeSha256']
 assert sha(source)==a['sourceSha256'];report={'originalSource':str(source),'originalSourceSha256BeforeAfter':a['sourceSha256'],'bankAuthoritySha256':sha(authority),'originalDescriptor':row,'stageAsset':str(OUT/asset),'assetSha256':digest(raw),'pcmSha256':digest(pcm),'sourceSampleRate':22050,'channels':1,'frames':33553,'nativeRateNoResamplingGainTrimPadding':True,'changes':changes,'patchSha256':sha(patch),'compiled':True,'compileCommand':command,'compileLogSha256':sha(OUT/'compile.log'),'patchReadbackExact':True,'maxPreparedTracks':9,'extraRawPcmTrackBytes':3*len(pcm),'current49And78AssetsUnchanged':True,'normalMissProducerBound':False,'canonicalUnchanged':True,'apk269Unchanged':True,'actualInstalled':False,'scope':'Original nativeSound58 bank4/slot24 PCM and native22050 mono source-rate AudioTrack stage; first49/78 sources/rates/frames unchanged. Three bounded static tracks added;100000-byte per-WAV extent remains. No MapHost/policy/API/B rules change or fake trigger. Needs explicit committed miss/state/event identity fromB and new actual APK playback/fullSaveRNGToken/PCM/lifecycle/ARM acceptance. Nine engineering WAV labels stay synthetic.','wholeGoalComplete':False};(DOC/'ORIGINAL_MISS_AUDIO278.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['compileCommand','originalDescriptor']}))
if __name__=='__main__':main()
