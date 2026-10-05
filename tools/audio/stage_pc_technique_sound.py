#!/usr/bin/env python3
"""Stage only the native-proved HUD33 sample. Other mobile compositions stay labelled inherited."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('banks',type=Path);p.add_argument('--bindings',type=Path,required=True)
    a=p.parse_args();binding=json.loads(a.bindings.read_text());manifest=json.loads((a.banks/'manifest.json').read_text())
    if binding['hud33']['status']!='ORIGINAL_SAMPLE_SELECTOR_AND_NATIVE_PCM_FORMAT_VERIFIED_PLAYBACK_PENDING':raise ValueError('Original HUD33 evidence missing')
    if manifest['bindingEvidenceSha256']!=hashlib.sha256(a.bindings.read_bytes()).hexdigest():raise ValueError('Binding evidence differs')
    matches=[r for r in manifest['entries'] if 33 in r['soundIds']]
    if len(matches)!=1:raise ValueError('Ambiguous original HUD33 sample')
    r=matches[0]
    if (r['bank'],r['slot'])!=(1,19) or r['status']!='ORIGINAL_PCM_BYTES_AND_NATIVE_FORMAT_VERIFIED_PLAYBACK_PENDING':raise ValueError('Original PCM proof missing')
    data=(a.banks/r['asset']).read_bytes()
    if hashlib.sha256(data).hexdigest()!=r['wavSha256']:raise ValueError('Sample guard')
    asset='audio/pc/technique-33.wav';target=ROOT/'app/src/main/assets'/asset;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
    pc=dict(schema=1,sourceExecutableSha256=binding['sourceExecutableSha256'],sourceArchiveSha256=manifest['sourceSha256'],
            nativeSoundId=33,bank=r['bank'],slot=r['slot'],headerResource=r['headerResource'],waveResource=r['waveResource'],
            sampleRate=r['sampleRate'],channels=r['channels'],samples=r['samples'],durationSeconds=r['durationSeconds'],
            sourcePcmSha256=r['pcmS16leSha256'],wavSha256=r['wavSha256'],nativeFormatHex=r['nativeFormatHex'],
            cues={name:asset for name in ['technique_gain','technique_loss']},
            sourceDispatch='Original6318ab pushes33; original4d0570 capture; original6f2c30 PCM format',
            status='ORIGINAL_SAMPLE_STAGED_INSTALLED_PLAYBACK_PENDING',
            limits=['One original sample serves the original changed-value HUD call regardless of sign.',
                    'Inherited other nine cues remain original mobile compositions, not PC restoration.',
                    'Native scalar HUD still consumes legacy commit values until ordered-fact entry patches are sequentially integrated.',
                    'No BGM, actor voice or full event matrix acceptance claimed.'])
    (ROOT/'app/src/main/assets/audio/pc-media-manifest.json').write_text(json.dumps(pc,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(asset=asset,wavSha256=r['wavSha256'],durationSeconds=r['durationSeconds'])))
