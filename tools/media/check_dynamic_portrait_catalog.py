#!/usr/bin/env python3
"""Production Java compact lookup against every independently recorded native age output."""
import argparse,hashlib,json,subprocess
from pathlib import Path

HARNESS=r'''package game.sanguo.mobile;
import java.io.*;import java.nio.file.*;import java.util.*;
public final class DynamicPortraitProbe {
 public static void main(String[] args)throws Exception{
  PcDynamicPortraitCatalog c=PcDynamicPortraitCatalog.read(Files.newInputStream(Path.of(args[0])));
  int checks=0;Set<Integer> selectors=new TreeSet<>();
  for(String line:Files.readAllLines(Path.of(args[1]))){String[] v=line.split("\t");
   PortraitMediaIdentity id=new PortraitMediaIdentity(Integer.parseInt(v[0]),Integer.parseInt(v[1]),v[2],v[3],v[4],v[5]);
   int year=Integer.parseInt(v[6]),expected=Integer.parseInt(v[7]);
   if(c.selector(id,year)!=expected)throw new AssertionError("Native age output differs "+id.key()+" year="+year);checks++;selectors.add(expected);
   for(int bad=0;bad<4;bad++){
    String changed="0".repeat(64);if(changed.equals(id.recordSha)||changed.equals(id.sourceSha))changed="1".repeat(64);
    PortraitMediaIdentity unknown=new PortraitMediaIdentity(id.officerId,id.nativeId,id.sourceVariant+(bad==0?"-unknown":""),id.sourcePath+(bad==1?"-unknown":""),bad==2?changed:id.sourceSha,bad==3?changed:id.recordSha);
    if(c.selector(unknown,year)!=-1)throw new AssertionError("Unknown source borrowed pixels");checks++;
   }
  }
  if(c.size()!=10656||selectors.size()!=62||c.selector(null,184)!=-1)throw new AssertionError("Coverage or unknown lookup");checks+=3;
  byte[] raw=Files.readAllBytes(Path.of(args[0]));
  for(byte[] broken:List.of(Arrays.copyOf(raw,raw.length-1),Arrays.copyOf(raw,raw.length+1),new byte[16])){
   try{PcDynamicPortraitCatalog.read(new ByteArrayInputStream(broken));throw new AssertionError("Corrupt catalog accepted");}catch(IOException expected){checks++;}
  }
  System.out.println("DYNAMIC_PORTRAITS PASS checks="+checks+" identities="+c.size()+" selectors="+selectors.size());
 }
}'''

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--stage',type=Path,required=True);p.add_argument('--manifest',type=Path,required=True);p.add_argument('--jdk',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    if a.output.exists():raise ValueError('Fresh evidence output required')
    a.output.mkdir(parents=True);root=Path(__file__).resolve().parents[2];m=json.loads(a.manifest.read_bytes());stage=json.loads((a.stage/'dynamic-media-manifest.json').read_bytes())
    lookup=a.stage/'dynamic-lookup.pcd';digest=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
    if digest(a.manifest)!=stage['approvedPortraitManifestSha256'] or digest(lookup)!=stage['compactLookup']['sha256']:raise ValueError('Staged original source pins differ')
    rows=[]
    for r in m['identities']:
        for b in r['ageBoundaries']:
            rows.append('\t'.join(map(str,[r['officerId'],r['nativeId'],r['sourceVariant'],r['sourcePath'],r['sourceSha256'],r['recordSha256'],r['birth']+b['age']-1,b['dynamicSelector']])))
    vectors=a.output/'native-age-vectors.tsv';vectors.write_text('\n'.join(rows)+'\n');harness=a.output/'DynamicPortraitProbe.java';harness.write_text(HARNESS);classes=a.output/'classes';classes.mkdir()
    production=root/'app/src/main/java/game/sanguo/mobile';files=[production/(name+'.java') for name in ['MediaHashes','PortraitMediaIdentity','PcDynamicPortraitCatalog']]
    subprocess.run([str(a.jdk/'bin/javac'),'-d',str(classes),*map(str,files),str(harness)],check=True)
    result=subprocess.check_output([str(a.jdk/'bin/java'),'-cp',str(classes),'game.sanguo.mobile.DynamicPortraitProbe',str(lookup),str(vectors)],text=True)
    report=dict(result=result.strip(),nativeAgeVectors=len(rows),sourceLookupSha256=digest(lookup),productionSources={str(f.relative_to(root)):digest(f) for f in files},normalAndroidIntegration=False,limits=['Production Java resolver only; no installed normal fullscreen rendering or memory claim.','Native age vectors inherited from independently executed original source lookup; unknown variants and changed source records reject.'])
    (a.output/'result.json').write_text(json.dumps(report,indent=2)+'\n');print(result,end='')
