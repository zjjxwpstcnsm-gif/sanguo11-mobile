#!/usr/bin/env python3
"""Complete named-predicate lexical inventory plus explicit suppression review in actual369."""
from pathlib import Path
import json,re
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent

def main():
 out=D/'MAP_GATES389.json';assert not out.exists();b=read(D/'MUSIC_RESERVE_BUILD369.json');m=Path(b['candidateInputManifest']);assert sha(m)==b['candidateInputManifestSha256'];expected={x['path']:x['sha256'] for x in read(m)};base=m.parent/'source';owners={x['path']:x['owner'] for x in read(ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json')['files']};pattern=re.compile(r'\bpcMap\b|\bsourceVisuals\b|\bsourceMap\b');inventory=[]
 for p in sorted((base/'app/src/main/java').rglob('*.java')):
  text=p.read_text();lines=text.splitlines();hits=[]
  for i,line in enumerate(lines):
   if pattern.search(line) and not line.strip().startswith(('//','*','/*')):hits.append({'line':i+1,'code':line,'context':'\n'.join(lines[max(0,i-3):min(len(lines),i+5)]),'kind':'predicate-or-dispatch' if re.search(r'\bif\b|\?|&&|\|\|',line) else 'reference-definition-or-diagnostic'})
  if not hits:continue
  path=str(p.relative_to(base));assert sha(p)==expected[path];inventory.append({'path':path,'sha256':sha(p),'owner':owners.get(path,'A-registered-or-other'),'allNamedNonCommentMarkers':hits})
 def method(file,marker,need):
  p=base/'app/src/main/java/game/sanguo/mobile'/file;text=p.read_text();start=text.index(marker);brace=text.index('{',start);depth=1;i=brace+1
  while depth and i<len(text):
   if text[i]=='{':depth+=1
   elif text[i]=='}':depth-=1
   i+=1
  block=text[start:i]
  for n in need:assert n in block,(file,marker,n)
  return {'path':str(p.relative_to(base)),'sha256':sha(p),'line':text[:start].count('\n')+1,'methodCode':block}
 reviews=[
 ('legacy combat particles','FilamentMapView.java','private void animateEffects(',['if(snapshot.ground.pcMap!=null){clearEffects();return;}'],'Suppresses authored mobile fire/smoke. Continuous native cell fire is separate; projectile/tactic/fireball/seed/ship chains and other original emitters still missing normal caller/sequence evidence.'),
 ('legacy facility overlay','FilamentMapView.java','private String facilityOverlay(',['if(snapshot!=null&&snapshot.ground.pcMap!=null)return "";'],'Suppresses authored scaffold/fire decal; original facility body variants and independent work/HP/fire labels exist. All-kind construction/damage/upgrade/stop/repair/complete/demolish original transitions not accepted.'),
 ('legacy critical flash','MainActivity.java','private void showCritical(',['map.sourceVisuals()'],'Suppresses old CriticalFlash on source map. PcPresentationPlan/Stage is separate, limited to verified original bindings; unsupported identities/events remain missing rather than replacing with authored flash.'),
 ('legacy critical portrait field','FilamentMapView.java','void critical(',['hit=null;portrait=null;'],'Source map clears old portrait slot. Source critical stage must have actual caller/age/actor/source profile evidence; all16 and unknown profiles not closed.'),
 ('legacy critical projection','MapHost.java','void criticalFrame(',['if(sourceVisuals()){hit=null;'],'Blocks legacy projection on source map. Source criticalEvent drives source stage. Existing known bindings do not prove complete normal fullscreen pixels/timing/lifetime.'),
 ('source object without asset','FilamentMapView.java','private GpuMesh shape(',['nativeUnit==null&&nativeSite==null&&!nativeFacility','pc-unresolved'],'Unsupported PC object omitted and explicitly unresolved; original acquisition/binding still needed. No mobile stand-in or full16 unsupported-object normal inventory acceptance.'),
 ('source native cells admission','FilamentMapView.java','private void animatePcMapEffects(',['closePcMapEffects()','outputVerified'],'Native grid-fire path exists, waits actual terrain/output admission and consumes copied facts. New369 full fire lifecycle remains separate from old296 accepted subset.'),
 ('source fire facts drawing','FilamentMapView.java','private void drawSourceFireState(',['snapshot.ground.pcMap==null'],'PC facts labels provide burning-state recognition independently of native animation. No authority mutation/creation; native budget and all normal expiry/low-quality/pause cases still require exact new package.'),
 ('source scenery legacy block','Vegetation.java','static List<SceneMesh> buildWindow(', ['PC map requires source scenery'],'Rejects mobile placement in source map. Original PcScenery route exists; exact original density/climate/wind/frame and all16 proof incomplete.'),
 ]
 result=[]
 for title,file,marker,need,assessment in reviews:
  record=method(file,marker,need);record.update({'gate':title,'assessment':assessment,'actualNew369Complete':False});result.append(record)
 report={'actual369Apks':b['apks'],'exactInputManifestSha256':sha(m),'namedMarkerFiles':len(inventory),'namedMarkerCount':sum(len(x['allNamedNonCommentMarkers']) for x in inventory),'inventory':inventory,'explicitSuppressionsReviewed':result,'separateRoutingAssessment':{'SceneMesh/TerrainSurface/SceneCamera':'Source height/grid/water/mesh/perspective geometry routing; no original fire removal proved by presence of geometry branch. Vertex/index byte parity315/325 scoped only tested windows.','WaterVisualField/TerrainMaterialField':'Legacy flow drift disabled for PC; original source plane/mask/clock route is separate. Full original water pixels/timing not accepted.','PcFacilities/PcFacilityRigs/PcSites/PcWalls/PcDams':'Original-only binding filters and completed-rig constraints. Facility climate0 caller322 remains a gap; no rule reconstruction or RNG.','MapHost/TurnPlayback':'Source prelude duration/submission gates separate from legacy cues. Could suppress fallback when original cue unavailable; all real events/speakers/voices not closed.','MapSceneSnapshot/PcCellFireSet':'Source scene/StateToken/accepted facts checks; stale or absent facts rejected. Missing facts do not authorize renderer-generated fire.'},'completeNamedLexicalInventory':True,'completeGlobalSemanticOrAliasAudit':False,'scope':'Read-only every non-comment pcMap/sourceVisuals/sourceMap named line in actual369 app Java, exact file SHA/context plus9 specific suppression methods verified. Does not cover aliases(pcGround/pcUnit/local booleans), shaders/C# or every dynamic caller; no blanket124/all sourceMap closure. Sources0-10 current380 normal, fire/media/caller/ARM/finalB still pending.','wholeGoalComplete':False};out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'namedMarkerFiles':report['namedMarkerFiles'],'namedMarkerCount':report['namedMarkerCount'],'explicitSuppressionReviews':len(result),'globalSemanticComplete':False}))
if __name__=='__main__':main()
