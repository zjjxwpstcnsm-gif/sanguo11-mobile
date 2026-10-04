#!/usr/bin/env python3
"""Stage the proved original negative-close sample; retain fixture/Android scope."""
import argparse,hashlib,json,wave,io
from pathlib import Path

def stage(banks, native_close, output):
    if output.exists():raise ValueError('Fresh staging directory required')
    proof=json.loads(native_close.read_bytes());m=json.loads((banks/'manifest.json').read_bytes())
    if proof['sourceExecutableSha256']!='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb' or proof['checks']!=69:raise ValueError('Source close evidence missing')
    negative=[r for r in proof['rows'] if r['closeResults']==[-2]]
    if not negative or any(len(r['dispatchHex'])!=1 or bytes.fromhex(r['dispatchHex'][0])[:3]!=bytes([0,0,1]) for r in negative):raise ValueError('Source close sample differs')
    r=next(x for x in m['entries'] if x['soundIds']==[1]);data=(banks/r['asset']).read_bytes()
    if (r['bank'],r['slot'])!=(0,1) or hashlib.sha256(data).hexdigest()!=r['wavSha256']:raise ValueError('Source sample identity differs')
    with wave.open(io.BytesIO(data)) as w:
        if (w.getnframes(),w.getframerate(),w.getnchannels())!=(r['samples'],r['sampleRate'],r['channels']) or hashlib.sha256(w.readframes(w.getnframes())).hexdigest()!=r['pcmS16leSha256']:raise ValueError('Source PCM differs')
    asset='audio/pc/ui-close-1.wav'
    manifest={'schema':1,'sourceExecutableSha256':proof['sourceExecutableSha256'],'sourceArchiveSha256':m['sourceSha256'],'source':r,'asset':asset,
        'nativeBinding':{'negativeReturnRaw':-2,'nativeSoundId':1,'callers':['4dd8c0 flags8 without flags2','63b270 child1237'],'originalDispatch':'4d0570 -> 6e98b0 bank0 slot1'},
        'androidBinding':'Actual AlertDialog OnCancel callback; BACK/outside cancellation only, no ordinary dismiss or rule failure inference',
        'status':'SOURCE_CLOSE_BRANCH_AND_PCM_VERIFIED_ANDROID_INSTALL_PENDING',
        'limits':['Explicit native child lookup and window callback fixture boundaries; original OS routing not emulated.','Other generic UI interactions retain mobile composition; no complete PC UI sound matrix claim.']}
    output.mkdir(parents=True);(output/'ui-close-1.wav').write_bytes(data);(output/'ui-close-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('PASS source close sample',r['wavSha256'])

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--banks',type=Path,required=True);p.add_argument('--native-close',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();stage(a.banks,a.native_close,a.output)
