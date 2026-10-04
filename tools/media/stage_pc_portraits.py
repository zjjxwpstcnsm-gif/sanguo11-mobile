#!/usr/bin/env python3
"""Stage corrected original face pixels and approved source joins. Never infer an officer from a face/name."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import struct

def sha(data):return hashlib.sha256(data).hexdigest()
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--pixels',type=Path,required=True);p.add_argument('--join',type=Path,required=True);p.add_argument('--decoded',type=Path,required=True);p.add_argument('--fce',type=Path,required=True);p.add_argument('--assets',type=Path,required=True);a=p.parse_args()
    pixels=json.loads(gzip.decompress(a.pixels.read_bytes()));join=json.loads(gzip.decompress(a.join.read_bytes()));fce=a.fce.read_bytes()
    if sha(fce)!=pixels['sourceSha256'] or join['sourceFaceSha256']!=pixels['sourceSha256'] or pixels['indexLayout'].find('small1,small2')<0:raise ValueError('Correct original registry/source required')
    _,_,start,count,_=struct.unpack_from('<5I',fce);flags=list(struct.unpack_from('<'+str(count)+'I',fce,20));images=[];total=0
    for row in pixels['entries']:
        if not row['bytes']:continue
        raw=(a.decoded/row['asset']).read_bytes()
        if sha(raw)!=row['pngSha256']:raise ValueError('Original corrected pixel output differs')
        asset='portraits/pc/'+row['asset'];target=a.assets/asset;target.parent.mkdir(parents=True,exist_ok=True)
        if target.exists() and target.read_bytes()!=raw:raise ValueError('Existing original pixels differ')
        target.write_bytes(raw);total+=len(raw)
        images.append({k:row[k] for k in ['faceId','imageGroup','sourceRegistryIndex','pngSha256','rgbaSha256','width','height','alphaMin','alphaMax','nonOpaquePixels']}|dict(asset=asset,sourceWftxSha256=row['sourceSha256']))
    identities=[];keys=set()
    for row in join['entries']:
        if row['officerId'] is None:continue
        key=(row['officerId'],row['nativeId'],row['sourceVariant'])
        if key in keys:raise ValueError('Duplicate approved media join')
        keys.add(key);identities.append({k:row[k] for k in ['officerId','nativeId','sourceVariant','sourcePath','sourceSha256','recordSha256','identityStatus','faceId','sexRaw','birth','ageThreshold','dynamicSelector','voiceTypeRaw']})
    report=dict(schema=1,sourceFaceSha256=pixels['sourceSha256'],sourceFaceFile=pixels['sourceFile'],metadataCommit=join['metadataCommit'],metadataRequestSha256=join['metadataRequestSha256'],
        registryLayout=pixels['indexLayout'],flagStartFace=start,faceFlags=flags,images=images,identities=identities,decodedImages=len(images),identityJoins=len(identities),pngBytes=total,
        limits=['SourceInfo missing remains unknown; no name or roster-ID inference.', 'Native flags/age selection retained; role-specific imageGroup callers require independent proof.', 'Only supplied primary FCE present; external active override priority is unresolved.'])
    target=a.assets/'portraits/pc/media-manifest.json.gz';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(gzip.compress(json.dumps(report,separators=(',',':')).encode(),mtime=0))
    print('Staged corrected original portrait images=',len(images),'approved joins=',len(identities),'PNG bytes=',total)
if __name__=='__main__':main()
