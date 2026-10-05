#!/usr/bin/env python3
"""Bound a single installed audio acceptance run in an existing emulator WAV backend.
The PCM interval is copied verbatim; only a new WAV container is written. Never edits
or stops the live device/mixer. Installation/restoration is delegated to the proven tool.
Requires numpy for the independent waveform recognition child process.
"""
import argparse,hashlib,json,struct,subprocess,sys,time,wave
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser(description=__doc__)
for name in ('serial','apk','test-apk','wave','output'):p.add_argument('--'+name,required=True,type=str)
a=p.parse_args();source=Path(a.wave);output=Path(a.output);output.mkdir(parents=True,exist_ok=False)
with source.open('rb') as f:
 header=f.read(44)
assert header[:4]==b'RIFF' and header[8:12]==b'WAVE' and header[12:16]==b'fmt ' and header[36:40]==b'data','Expected PCM emulator backend, not arbitrary WAV layout'
encoding,channels,rate,byte_rate,block_align,bits=struct.unpack_from('<HHIIHH',header,20)
assert encoding==1 and bits==16 and block_align==channels*2
begin=source.stat().st_size;assert (begin-44)%block_align==0
started=time.monotonic()
with (output/'install.log').open('wb') as log:
 result=subprocess.run([sys.executable,str(ROOT/'tools/android/verify_pure3d_install.py'),'--serial',a.serial,'--apk',a.apk,'--test-apk',a.test_apk,'--output',str(output/'installed'),'--runner','UiUxInstrumentation','--argument','suite=audio','--argument','run=isolatedaudio','--pass-marker','UIUX PASS'],stdout=log,stderr=subprocess.STDOUT)
# Include final already submitted buffers without causing playback or changing PCM.
time.sleep(.5);end=source.stat().st_size;end-=(end-44)%block_align
with source.open('rb') as f:f.seek(begin);pcm=f.read(end-begin)
assert len(pcm)==end-begin
raw=output/'captured-window.pcm';raw.write_bytes(pcm)
container=output/'captured-window.wav'
with wave.open(str(container),'wb') as f:f.setnchannels(channels);f.setsampwidth(2);f.setframerate(rate);f.writeframes(pcm)
meta=dict(serial=a.serial,scope='one pinned installed audio run; emulator WAV backend, not an ARM physical speaker',source_live_wave=str(source.resolve()),begin_byte=begin,end_byte=end,pcm_sha256=hashlib.sha256(pcm).hexdigest(),wall_seconds=time.monotonic()-started,rate=rate,channels=channels,installer_exit_code=result.returncode)
(output/'provenance.json').write_text(json.dumps(meta,indent=2)+'\n')
if result.returncode:sys.exit(result.returncode)
subprocess.run([sys.executable,str(ROOT/'tools/audio/check_device_mix.py'),str(container),'--output',str(output/'waveform-verification.json')],check=True)
print('PASS one installed-run PCM interval; original bytes preserved')
