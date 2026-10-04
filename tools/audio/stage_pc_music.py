#!/usr/bin/env python3
"""Stage every original music Ogg plus independent media manifest; no scene assignment guesses."""
import argparse
import hashlib
import json
from pathlib import Path

def stage(source,assets):
    manifest=json.loads((source/'manifest.json').read_text());rows=[]
    if manifest['sourceSha256']!='e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a':raise ValueError('Re-audit changed source archive')
    for row in manifest['entries']:
        if not 2237<=row['resourceId']<=2266:continue
        data=(source/row['asset']).read_bytes()
        if hashlib.sha256(data).hexdigest()!=row['oggSha256'] or row['decodedSamples']!=row['endGranule']:raise ValueError('Original music timeline/hash differs')
        asset='audio/pc/music/'+str(row['resourceId'])+'.ogg';target=assets/asset;target.parent.mkdir(parents=True,exist_ok=True)
        if target.exists() and target.read_bytes()!=data:raise ValueError('Existing music asset differs; explicit replacement required')
        target.write_bytes(data)
        rows.append(dict(musicId=row['resourceId']-2237,resourceId=row['resourceId'],asset=asset,bytes=len(data),oggSha256=row['oggSha256'],
            originalKovsSha256=row['sourceSha256'],sourceOffset=row['sourceOffset'],sourceBytes=row['sourceBytes'],
            sampleRate=row['sampleRate'],channels=row['channels'],frames=row['decodedSamples'],referencePcmSha256=row['pcmS16leSha256'],
            loopStartFrame=row['loopStartSample'],loopEndFrame=row['loopEndSample'],loopBasis=row['loopBasis'],
            nativeResourceBinding='4cf9b0 -> resource2237+musicId',sceneBinding=None,status='ORIGINAL_MUSIC_RESOURCE_VERIFIED_SCENE_UNBOUND'))
    if len(rows)!=30 or {row['musicId'] for row in rows}!=set(range(30)):raise ValueError('Missing original music resource')
    report=dict(schema=1,sourceArchive='Media/san11pkres.bin',sourceArchiveSha256=manifest['sourceSha256'],
        sourceExecutableSha256='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb',
        normalization='NONE_ORIGINAL_OGG_BYTES',tracks=rows,totalBytes=sum(row['bytes'] for row in rows),
        limits=['All30 original resources, not30 bound scenes. No menu/map assignment until caller inputs verified.',
            'Three source partial loops retained; other tracks require explicit original repeat directive to loop from0.',
            'PCM reference is deterministic PyAV16.1 decode; Android codec output must be measured separately.'])
    target=assets/'audio/pc/music-manifest.json';target.write_text(json.dumps(report,indent=2)+'\n');print('Staged original music30 bytes=',report['totalBytes'])
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--assets',type=Path,required=True)
    a=p.parse_args();stage(a.source,a.assets)
