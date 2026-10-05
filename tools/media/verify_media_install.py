#!/usr/bin/env python3
"""Install/media acceptance on exclusive5582 with exact restoration and optional immutable PCM window.

Does not restart, truncate, or configure any emulator audio recorder.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
import time
import wave

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/content'))
import verify_media_device_flow as installed


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial',choices=['emulator-5582'],default='emulator-5582')
    p.add_argument('--apk',type=Path,required=True);p.add_argument('--test-apk',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--runner',choices=['MediaBridgeInstrumentation','PortraitPixelsInstrumentation','PortraitFlowInstrumentation','PortraitPresentationInstrumentation','MusicSourceInstrumentation','MusicPlaybackInstrumentation','VoiceSourceInstrumentation','SharedMediaInstrumentation','TechniquePointsInstrumentation','TechniqueFactsInstrumentation','UiUxInstrumentation'],required=True)
    p.add_argument('--argument',action='append',default=[]);p.add_argument('--pass-marker',required=True);p.add_argument('--timeout',type=int,default=1200)
    p.add_argument('--campaign-save',type=Path);p.add_argument('--campaign-sha256')
    p.add_argument('--reuse-installed',action='store_true');p.add_argument('--wave',type=Path);p.add_argument('--cue',action='append',default=[])
    p.add_argument('--device-sha-readback',action='store_true',help='Read full installed APK SHA256 on device; preserve full byte-level user restoration')
    p.add_argument('--capture-only',action='store_true',help='Retain raw PCM window without short-cue recognition; separate media waveform proof required')
    a=p.parse_args()
    markers={'MediaBridgeInstrumentation':'MEDIA_WIRE PASS','PortraitPixelsInstrumentation':'PORTRAIT_PIXELS PASS',
        'PortraitFlowInstrumentation':'PORTRAIT_FLOW PASS','PortraitPresentationInstrumentation':'PORTRAIT_PRESENTATION PASS','MusicSourceInstrumentation':'MUSIC_SOURCE PASS',
        'MusicPlaybackInstrumentation':'MUSIC_PLAYBACK PASS','TechniquePointsInstrumentation':'PASS TECHNIQUE HUD',
        'VoiceSourceInstrumentation':'VOICE_SOURCE PASS',
        'SharedMediaInstrumentation':'SHARED_MEDIA PASS',
        'TechniqueFactsInstrumentation':'PASS TECHNIQUE FACTS','UiUxInstrumentation':'UIUX PASS'}
    if a.pass_marker!=markers[a.runner]:raise ValueError('Runner pass marker differs; device untouched')
    if a.output.exists():raise ValueError('Fresh output required')
    # Preflight before force-stop/installation. Reject another currently running instrumentation.
    adb=[str(ROOT/'out/toolchain/android-sdk/platform-tools/adb'),'-s',a.serial]
    state=subprocess.check_output(adb+['shell','dumpsys','activity','processes']).decode()
    if 'ActiveInstrumentation{' in state:raise ValueError('Device instrumentation already active; coordinate serially')
    begin=None
    if a.wave:
        processes=subprocess.check_output(['ps','-axo','pid,command']).decode().splitlines()
        owners=[line.strip().split(None,1) for line in processes if 'qemu-system' in line and ' -port 5582 ' in line]
        if len(owners)!=1:raise ValueError('Cannot bind WAV to exclusive5582 process')
        pid=owners[0][0]
        fds=subprocess.check_output(['lsof','-p',pid]).decode()
        if str(a.wave.resolve()) not in fds:raise ValueError('WAV is not opened by5582; other recorder untouched')
        with a.wave.open('rb') as f:header=f.read(44)
        if header[:4]!=b'RIFF' or header[8:12]!=b'WAVE' or header[36:40]!=b'data':raise ValueError('Unexamined WAV backend')
        encoding,channels,rate,_,alignment,bits=struct.unpack_from('<HHIIHH',header,20)
        if encoding!=1 or bits!=16 or alignment!=channels*2:raise ValueError('Unexamined PCM backend')
        begin=a.wave.stat().st_size
        if (begin-44)%alignment:raise ValueError('WAV prefix ends inside frame')
    lock=Path('/tmp/codex-sanguo-media-emulator-5582.lock')
    fd=os.open(lock,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    try:
        os.write(fd,json.dumps(dict(pid=os.getpid(),workspace=str(ROOT),serial=a.serial)).encode());os.close(fd)
        args=argparse.Namespace(**vars(a),test_package='game.sanguo.mobile.dev.test')
        installed.run(args)
        if begin is not None:
            time.sleep(.5);end=a.wave.stat().st_size;end-=(end-44)%alignment
            if end<begin:raise ValueError('Live WAV recorder replaced or truncated')
            with a.wave.open('rb') as f:f.seek(begin);pcm=f.read(end-begin)
            if len(pcm)!=end-begin:raise ValueError('Incomplete PCM snapshot')
            target=a.output/'captured-window.wav'
            with wave.open(str(target),'wb') as f:f.setnchannels(channels);f.setsampwidth(2);f.setframerate(rate);f.writeframes(pcm)
            (a.output/'pcm-provenance.json').write_text(json.dumps(dict(serial=a.serial,emulatorPid=pid,source=str(a.wave.resolve()),
                beginByte=begin,endByte=end,pcmSha256=hashlib.sha256(pcm).hexdigest(),
                scope='Actual5582 mixed PCM interval; recorder unmodified; no ARM speaker evidence'),indent=2)+'\n')
            command=[sys.executable,str(ROOT/'tools/audio/check_device_mix.py'),str(target),'--output',str(a.output/'waveform-verification.json')]
            for cue in a.cue:command.extend(['--cue',cue])
            if not a.capture_only:subprocess.run(command,check=True)
    finally:
        lock.unlink()
