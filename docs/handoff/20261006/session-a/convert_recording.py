#!/usr/bin/env python3
"""Remux real adb screenrecord H264; no generated or edited game content."""
import hashlib,json,pathlib,subprocess,sys
ROOT=pathlib.Path(__file__).resolve().parents[4]
sys.path.insert(0,str(ROOT/'out/session-a/video-tools'))
import imageio_ffmpeg
source=ROOT/'out/session-a/touch-flow-05.h264';target=source.with_suffix('.mp4')
ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
command=[ffmpeg,'-y','-r','25','-i',str(source),'-c','copy','-movflags','+faststart',str(target)]
r=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True)
log=ROOT/'out/session-a/touch-flow-05-remux.txt';log.write_bytes(r.stderr)
reader=imageio_ffmpeg.read_frames(str(target));metadata=next(reader);frames=sum(1 for _ in reader)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
result={'run':'resume-install-05','coverage':'Actual encoded frames from a first180s recording attempt, NOT the full16-source sequence. Elementary H264 lost wall-clock packet timestamps. This preview explicitly assigns25fps; temporal/lifecycle acceptance must use logs and screenshots, NOT the preview playback pace.','apkSha256':'daecedbb0436e5eebd5423b4b607d7f8926b544ce411409edb4448c8c6719d1e','source':str(source),'sourceSha256':sha(source),'video':str(target),'videoSha256':sha(target),'method':'FFmpeg stream copy, no game frame edits/synthetic content','command':command,'ffmpegVersion':imageio_ffmpeg.get_ffmpeg_version(),'metadata':metadata,'decodedFrames':frames,'originalTimingVerified':False,'previewAssignedFps':25,'log':str(log)}
(ROOT/'docs/handoff/20261006/session-a/VIDEO_EVIDENCE.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
