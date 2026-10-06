#!/usr/bin/env python3
"""Remux real adb screenrecord H264; no generated or edited game content."""
import hashlib,json,pathlib,subprocess,sys
ROOT=pathlib.Path(__file__).resolve().parents[4]
sys.path.insert(0,str(ROOT/'out/session-a/video-tools'))
import imageio_ffmpeg
source=pathlib.Path(sys.argv[1]).resolve() if len(sys.argv)>1 else ROOT/'out/session-a/touch-flow-05.h264';target=source.with_suffix('.mp4')
assert source.is_relative_to(ROOT/'out')
apk_sha=sys.argv[2] if len(sys.argv)>2 else 'daecedbb0436e5eebd5423b4b607d7f8926b544ce411409edb4448c8c6719d1e'
assert len(apk_sha)==64 and all(c in '0123456789abcdef' for c in apk_sha)
run=sys.argv[3] if len(sys.argv)>3 else 'resume-install-05'
ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
command=[ffmpeg,'-y','-r','25','-i',str(source),'-c','copy','-movflags','+faststart',str(target)]
r=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True)
log=source.with_name(source.stem+'-remux.txt');log.write_bytes(r.stderr)
reader=imageio_ffmpeg.read_frames(str(target));metadata=next(reader);frames=sum(1 for _ in reader)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
result={'run':run,'coverage':'Actual encoded normal gesture frames from the named installed APK, not the full source/gameplay sequence. Elementary H264 lost wall-clock packet timestamps. This preview explicitly assigns25fps; temporal/lifecycle acceptance must use logs and screenshots, NOT the preview playback pace.','apkSha256':apk_sha,'source':str(source),'sourceSha256':sha(source),'video':str(target),'videoSha256':sha(target),'method':'FFmpeg stream copy, no game frame edits/synthetic content','command':command,'ffmpegVersion':imageio_ffmpeg.get_ffmpeg_version(),'metadata':metadata,'decodedFrames':frames,'originalTimingVerified':False,'previewAssignedFps':25,'log':str(log)}
report=ROOT/'docs/handoff/20261006/session-a/VIDEO_EVIDENCE.json';old=json.loads(report.read_text());records=old.get('records',[old]);records=[r for r in records if r.get('run')!=run]+[result];report.write_text(json.dumps({'records':records},indent=2)+'\n');print(json.dumps(result))
