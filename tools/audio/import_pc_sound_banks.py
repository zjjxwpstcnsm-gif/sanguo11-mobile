#!/usr/bin/env python3
"""Preserve original paired sound banks and structurally decode their PCM sample slices.

Runtime gain/pitch/loop descriptor fields are retained raw until their reader is verified.
No implicit normalizing, synthetic replacement or guessed event role.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import sys
import wave
import numpy as np
sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'content'))
from pc_resources import Archive


def sha(data):return hashlib.sha256(data).hexdigest()


def convert(installation, binding_path, output):
    if output.exists() or installation.resolve() in output.resolve().parents:
        raise ValueError('Fresh output outside read-only source required')
    bindings = json.loads(binding_path.read_text())
    native_formats={(r['bank'],r['slot']):r for r in bindings['pcmFormats']}
    if sha((installation/'san11pk.exe').read_bytes()) != bindings['sourceExecutableSha256']:
        raise ValueError('Original executable guard')
    output.mkdir(parents=True)
    a = Archive(installation/'Media/san11pkres.bin')
    rows, banks = [], []
    try:
        for bank, (header_id, wave_id) in enumerate(zip(bindings['soundBankHeaderResources'],bindings['soundBankWaveResources'])):
            header, raw = a.read(header_id), a.read(wave_id)
            flag, version, count, size = struct.unpack_from('<BBHI',header)
            if flag not in (0,1) or version != 50 or size != len(raw) or len(header)<8+4*count:
                raise ValueError('Unexamined bank header/data size')
            p = output/'original';p.mkdir(exist_ok=True)
            (p/f'{header_id}.bank').write_bytes(header);(p/f'{wave_id}.pcm').write_bytes(raw)
            banks.append(dict(bank=bank,headerResource=header_id,waveResource=wave_id,slotCount=count,
                              headerSha256=sha(header),waveSha256=sha(raw),sourceHeaderHex=header[:8].hex()))
            offsets = list(struct.unpack_from('<'+'I'*count,header,8))
            nonempty = [at for at in offsets if at]
            if sorted(nonempty) != list(range(8+4*count,len(header),40)):
                raise ValueError('Unexamined bank descriptor table')
            for slot, at in enumerate(offsets):
                events = [r['soundId'] for r in bindings['effectLookups'] if r['bank']==bank and r['slot']==slot]
                row = dict(bank=bank,slot=slot,headerResource=header_id,waveResource=wave_id,soundIds=events,
                           eventRole='UNVERIFIED',status='SOURCE_EMPTY')
                rows.append(row)
                if not at:continue
                desc = header[at:at+40]
                rate = int.from_bytes(desc[4:7],'little')
                alignment, codec = struct.unpack_from('<HH',desc,8)
                sample_count, start, length = struct.unpack_from('<III',desc,16)
                channels = alignment//2
                if codec!=1 or alignment not in (2,4) or rate not in (22050,44100) or sample_count*alignment!=length or start+length>len(raw):
                    raise ValueError('Unexamined bank PCM slice')
                pcm = raw[start:start+length]
                native=native_formats[(bank,slot)]
                if (sha(desc),sha(pcm),rate,channels,alignment)!=(native['descriptorSha256'],native['rawPcmSha256'],native['sampleRate'],native['channels'],native['blockAlign']):
                    raise ValueError('Original native PCM reader evidence differs')
                values = np.frombuffer(pcm,dtype='<i2').astype(np.float64)
                rms = np.sqrt(np.mean(values*values))/32768
                rel=f'wav/bank-{bank:02d}-slot-{slot:03d}.wav';target=output/rel;target.parent.mkdir(exist_ok=True)
                with wave.open(str(target),'wb') as sink:
                    sink.setnchannels(channels);sink.setsampwidth(2);sink.setframerate(rate);sink.writeframes(pcm)
                # Verify the output payload is exactly the original PCM, never a decoder-generated approximation.
                with wave.open(str(target),'rb') as sink:
                    if sink.readframes(sample_count) != pcm:raise ValueError('WAV changed original sample bytes')
                row.update(status='ORIGINAL_PCM_BYTES_AND_NATIVE_FORMAT_VERIFIED_PLAYBACK_PENDING',
                           descriptorOffset=at,descriptorHex=desc.hex(),descriptorSha256=sha(desc),
                           rawSampleOffset=start,rawSampleBytes=length,pcmS16leSha256=sha(pcm),
                           sampleRate=rate,channels=channels,samples=sample_count,durationSeconds=sample_count/rate,
                           asset=rel,wavSha256=sha(target.read_bytes()),peakS16=int(np.abs(values).max(initial=0)),
                           rmsDbfs=float(20*np.log10(rms)) if rms else None,
                           sampleFlagsRaw=desc[0],loopFlagRaw=desc[32]&1,loopStartRaw=struct.unpack_from('<I',desc,28)[0],
                           loopStartSample=None,loopEndSample=None,
                           loopStatus='ORIGINAL_LOOP_FIELDS_RETAINED_COMPLETE_PLAY_POLICY_PENDING',gainPitchRawHex=desc[28:40].hex(),
                           nativeFormatHex=native['nativeFormatHex'])
    finally:a.close()
    manifest=dict(schema=1,sourcePolicy='READ_ONLY_NO_WINE',sourceFile='Media/san11pkres.bin',
                  sourceSha256=sha((installation/'Media/san11pkres.bin').read_bytes()),
                  bindingEvidenceSha256=sha(binding_path.read_bytes()),banks=banks,entries=rows,
                  decodedSamples=sum('pcmS16leSha256' in r for r in rows),verifiedEventRoles=0,
                  limits=['PCM rate/channels/block alignment/bits independently verified by original6f2c30 for every nonempty sample.',
                          'Original source PCM payload is retained and WAV round-trip is exact.',
                          'Sound ID table is native verified; complete original command triggers, gain/pitch/loops and installed playback are separate.'])
    (output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(banks=len(banks),decodedSamples=manifest['decodedSamples'],verifiedEventRoles=0)))


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--bindings',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();convert(a.installation,a.bindings,a.output)
