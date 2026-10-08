#!/usr/bin/env python3
"""Bounded1sec actual source-format playback reserve; immutable staged product delta."""
from pathlib import Path
import json,difflib
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/music-buffer368';P='app/src/main/java/game/sanguo/mobile/PcMusicStreamPlayer.java'
def main():
 assert not OUT.exists();base=ROOT/'out/session-a/capture-queue-build363/source'/P;before=base.read_text();old='.setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Math.max(minimum,65536)).build();';new='''// Original stereo PCM stays exact. Keep one source-second of bounded
                // reserve for measured scheduling stalls, rather than only371ms at44.1kHz.
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Math.max(minimum,
                    Math.multiplyExact(track.sampleRate,Math.multiplyExact(track.channels,2)))).build();''';assert before.count(old)==1;after=before.replace(old,new);target=OUT/P;target.parent.mkdir(parents=True);target.write_text(after)
 patch=''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+P,tofile='b/'+P));(D/'MUSIC_BUFFER368.patch').write_text(patch)
 result={'paths':[{'path':P,'beforeSha256':sha(base),'afterSha256':sha(target),'stagedPath':str(target)}],'requestedSourceSeconds':1,'original44100StereoReserveBytesBefore':65536,'requestedBytesAfter':176400,'additionalRequestedBytes':110864,'observedPriorFirstLoopUnderruns':{'342':145,'349':0,'358':61},'scope':'Unbuilt product candidate reserve for real intermittent player starvation. One source-second, actual AudioTrack.bufferFrames must be measured after install. Original codec/PCM/loop/timing/focus/pause/cancel/identity/Save/RNG unchanged. Does not repair capture/mixer scheduling or prove unique cause; zero-underrun349 still failed0.995. Queue366 outcome must precede new product build and new full normal acceptance. Not installed/accepted, canonical source unchanged.','wholeGoalComplete':False};(D/'MUSIC_BUFFER368.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
