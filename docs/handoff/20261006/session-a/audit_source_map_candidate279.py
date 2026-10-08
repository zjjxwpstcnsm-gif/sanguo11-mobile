#!/usr/bin/env python3
"""Current candidate source-map gates and explicit presentation gap evidence."""
from pathlib import Path
import json,hashlib,re
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';STAGE=ROOT/'out/session-a/map-fire-upload-build269/source'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 output=DOC/'SOURCE_MAP_CANDIDATE279.json';assert not output.exists();build=json.loads((DOC/'MAP_FIRE_UPLOAD_BUILD269.json').read_text());markers=re.compile(r'\bpcMap\b|\bpcImported\b|\bsourceVisuals\s*\(|\bpcGround\b|\bpcUnit\b|\bpcSite\b|\bpcFacility\b');rows=[];files={}
 for path in sorted((STAGE/'app/src/main/java').rglob('*.java')):
  lines=path.read_text().splitlines();found=[]
  for i,line in enumerate(lines):
   if markers.search(line):found.append({'line':i+1,'text':line.strip(),'context':lines[max(0,i-2):i+3]})
  if found:
   name=str(path.relative_to(STAGE));files[name]=sha(path);rows.extend({'path':name,**row} for row in found)
 required={
 'oldFireSuppression':('FilamentMapView.java','if(snapshot.ground.pcMap!=null){clearEffects();return;}','Legacy authored fire remains rejected; actual source controller13 runs through animatePcMapEffects and persistent drawSourceFireState.197 is old165 evidence, new269 installed result still pending272.'),
 'legacyFacilityOverlaySuppression':('FilamentMapView.java','if(snapshot!=null&&snapshot.ground.pcMap!=null)return "";','Source PcFacilities model states and PcFacilityRigs remain. Authored scaffold/fire cannot be relabelled original. Whole construction/damage/complete/demolition/fireball/fireboat chains still need finalB normal acceptance.'),
 'legacyCriticalPortraitSuppression':('FilamentMapView.java','if(snapshot!=null&&snapshot.ground.pcMap!=null){hit=null;portrait=null;}','Original PcPresentationStage/PcPresentations is separate; no legacy fallback claimed original. Allage/caller/encoded-color/fullscreen normal acceptance still partial.'),
 'legacyFlagSuppression':('FilamentMapView.java','if(snapshot.ground.pcMap==null&&(item.site!=null','Legacy flag proxy is suppressed; original model labels/palette remain. Does not establish every original source flag/fill or all faction display pixels.'),
 'fireCapacityAndPaintGuard':('PcCellFireSet.java','surface.overrides.containsKey(f.hex)','128 admitted native controllers and local painted-height guard may omit original emitters. Persistent actual state rings/labels cover all visible live snapshot fires independently; source omitted emitter list is not closed by host128 pass.'),
 'facilityClimateProvisional':('PcFacilities.java','Climate 0 is explicit provisional region selection.','Converted original winter sheets exist, but default region climate selection is provisional. No blanket full PC seasonal/model parity claim.'),
 'nativeStartupHealthMarker':('MapHost.java','putBoolean("nativeSession",true).putBoolean("nativeFailure",true)','Marker is cleared only by actual current-host output. B r12 before already had true/true266; recovery alone does not prove new OOM. Cold native contests without actual ready map remain unaccepted; lifecycle startup while unattached/unfocused requires measured follow-up, no speculative guard clearing.'),
 'normalHintFix':('FilamentMapView.java','地图载入中','Candidate235 removes engineering hint and provides opaque readable normal HUD. Actual new269 Android pixels/input acceptance still pending; old165 PNG does not transfer.')}
 important={}
 for key,(filename,needle,limit) in required.items():
  paths=list((STAGE/'app/src/main/java').rglob(filename));assert len(paths)==1;path=paths[0];lines=path.read_text().splitlines();matches=[i+1 for i,s in enumerate(lines) if needle in s];assert matches,(key,needle);important[key]={'path':str(path.relative_to(STAGE)),'sha256':sha(path),'matchingLines':matches,'findingAndAcceptanceBoundary':limit}
 report={'candidateApks':build['apks'],'candidateBaseRevision':build['canonicalBaseRevision'],'lexicalMarkerLines':len(rows),'filesWithMarkers':len(files),'sourceFileSha256':files,'rows':rows,'importantConditions':important,'sourceAndCanonicalUnchanged':True,'actualInstalled269':False,'allMarkersAreSemanticBranches':False,'scope':'Fresh immutable269 full-app lexical marker inventory plus targeted gate alternatives/unknowns. Marker list includes declarations/material/geometry lines and is not proof of complete semantic source-path coverage. Static alternatives do not replace normal visible operation, memory or original model/material/status/flags/caller/effect acceptance. No B file or source-map rule change. All original user paths/sixJNI/APK remain unchanged.','wholeGoalComplete':False}
 output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['lexicalMarkerLines','filesWithMarkers','actualInstalled269','wholeGoalComplete']}))
if __name__=='__main__':main()
