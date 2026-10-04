#!/usr/bin/env python3
"""Original dynamic portraits for every approved identity; independent media pins.

No public map-release manifest writes, source activation, resampling or AI art.
Compact lookup preserves exact source identities and native age boundaries.
"""
import argparse,hashlib,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from pc_resources import Archive,wftx

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
ARCHIVE_SHA='e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a'

def sha(raw):return hashlib.sha256(raw).hexdigest()
def file_sha(path):
    digest=hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''):digest.update(chunk)
    return digest.hexdigest()
def text(value):
    raw=value.encode('utf-8');return struct.pack('>H',len(raw))+raw

def stage(installation,portrait_manifest,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Executable changed')
    archive_path=installation/'Media/san11pkres.bin'
    if file_sha(archive_path)!=ARCHIVE_SHA:raise ValueError('Original resource archive changed')
    source=portrait_manifest.read_bytes();m=json.loads(source)
    if m['schema']!=1 or m['identityJoins']!=10656:raise ValueError('Complete approved portrait source required')
    identities=m['identities'];selectors=sorted({r['dynamicSelector'] for r in identities}|{b['dynamicSelector'] for r in identities for b in r['ageBoundaries']})
    if selectors!=list(range(131,193)):raise ValueError('Original dynamic selector coverage changed')
    variants=sorted({(r['sourceVariant'],r['sourcePath'],r['sourceSha256']) for r in identities});index={row:i for i,row in enumerate(variants)}
    lookup=bytearray(b'PCDY0001'+struct.pack('>II',len(variants),len(identities)))
    for variant,path,source_sha in variants:lookup.extend(text(variant)+text(path)+bytes.fromhex(source_sha))
    for r in identities:
        old=next(b['dynamicSelector'] for b in r['ageBoundaries'] if b['age']>=r['ageThreshold']);key=(r['sourceVariant'],r['sourcePath'],r['sourceSha256'])
        lookup.extend(struct.pack('>iiHihBHH',r['officerId'],r['nativeId'],index[key],r['faceId'],r['birth'],r['ageThreshold'],r['dynamicSelector'],old)+bytes.fromhex(r['recordSha256']))
    output.mkdir(parents=True);(output/'dynamic-lookup.pcd').write_bytes(lookup);rows=[]
    archive=Archive(archive_path)
    try:
        common_raw=archive.read(124);common=wftx(common_raw)[32].convert('RGBA')
        if common.size!=(512,512):raise ValueError('Source dynamic atlas extent changed')
        for selector in selectors:
            template,top=struct.unpack_from('<2I',exe,0x3774e0+selector*8)
            if template!=115:raise ValueError('Additional source template requires original reconstruction')
            raw=archive.read(369+selector);images=wftx(raw)
            if len(images)!=1:raise ValueError('Ambiguous original selector image')
            image=images[0].convert('RGBA');atlas=common.copy()
            if image.width>512 or top+image.height>512:raise ValueError('Original raw replacement rectangle out of bounds')
            atlas.paste(image,(0,top));p=output/f'selector-{selector}.png';atlas.save(p,optimize=True)
            rows.append(dict(selector=selector,template=115,resourceId=369+selector,sourceSha256=sha(raw),sourceDimensions=list(image.size),destination=[0,top,image.width,top+image.height],sourceRgbaSha256=sha(image.tobytes()),asset='3d/pc-presentations/'+p.name,pngSha256=sha(p.read_bytes()),rgbaSha256=sha(atlas.tobytes()),width=512,height=512))
    finally:archive.close()
    manifest=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceArchiveSha256=ARCHIVE_SHA,approvedPortraitManifestSha256=sha(source),commonTextureResource=124,commonTextureSourceSha256=sha(common_raw),selectors=rows,
        compactLookup=dict(asset='portraits/pc/dynamic-lookup.pcd',sha256=sha(lookup),bytes=len(lookup),identityCount=len(identities),sourceVariantCount=len(variants),recordEndian='BIG_ENDIAN',sourceRows='All10656 approved rows plus unchanged native age selectors'),
        limits=['Original atlas replacement pixels; no normal Android fullscreen integration/playback claim.','Exact source identity/record SHA required; no name/roster-ID inference or replacement of custom portraits.','Only supplied primary source; external active MOD precedence remains unknown.'])
    (output/'dynamic-media-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');print(json.dumps({'selectors':len(rows),'identities':len(identities),'lookupBytes':len(lookup),'lookupSha256':sha(lookup)}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--portrait-manifest',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();stage(a.installation,a.portrait_manifest,a.output)
