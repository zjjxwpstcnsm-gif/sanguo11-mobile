#!/usr/bin/env python3
"""Additional normal-UI music-only condition; keep existing mixed-flow fixture unchanged."""
from pathlib import Path
import json,difflib
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/clean-menu384';P='app/src/androidTest/java/game/sanguo/mobile/UiUxInstrumentation.java'
def main():
 assert not OUT.exists();base=ROOT/'out/session-a/music-reserve-build369/source'/P;before=base.read_text();s=before
 needle='        nav("菜单");awaitText("军政菜单");awaitMenuMusic(sounds,status->status.resourceId==2238'
 assert s.count(needle)==1
 s=s.replace(needle,'        boolean cleanWhole="true".equals(arguments.getString("menuCleanWhole","false"));\n        if(cleanWhole){\n            nav("地图");mapTool("音效与音量");\n            SeekBar effects=(SeekBar)await(v->v instanceof SeekBar&&v.getTag()==null);\n            setMediaSlider(effects,0f);\n            setMediaSlider((SeekBar)tag("media.music-volume"),.65f);\n            setMediaSlider((SeekBar)tag("media.voice-volume"),.30f);\n            check(sounds.volume()==0&&sounds.musicVolume()>=55&&sounds.musicVolume()<=70&&sounds.voiceVolume()>=20&&sounds.voiceVolume()<=40,"actual ordinary settings isolate music from UI effects before scene entry");\n            text("完成");\n        }\n'+needle)
 start=s.index('        mapTool("音效与音量");SeekBar music=',s.index('private void menuMusicFlow'))
 end=s.index('        long originalMenuFrames=',start)
 block=s[start:end]
 assert 'int musicGain=sounds.musicVolume();' in block
 block=block.replace('int musicGain=sounds.musicVolume();','musicGain=sounds.musicVolume();')
 s=s[:start]+'        int musicGain=sounds.musicVolume();\n        if(!cleanWhole){\n'+block+'        }\n'+s[end:]
 needle='        MapHost host=(MapHost)field(activity,"map");backgroundMap(host);'
 assert s.count(needle)==2
 s=s.replace(needle,'        if(cleanWhole){\n            mapTool("音效与音量");SeekBar effects=(SeekBar)await(v->v instanceof SeekBar&&v.getTag()==null);setMediaSlider(effects,.75f);\n            check(sounds.volume()>=70&&sounds.volume()<=80&&sounds.musicVolume()==musicGain,"actual ordinary effects restore after complete music head without changing music gain");text("完成");\n        }\n'+needle,1)
 needle='.put("normalMenuOnly",true).put("noTestPlaybackDirective",true)'
 assert s.count(needle)==1
 s=s.replace(needle,needle+'.put("musicOnlyUsingOrdinarySettings",cleanWhole).put("effectGainDuringOriginalWholeTrack",cleanWhole?0:75).put("gainControlsBeforeOriginalScene",cleanWhole)')
 target=OUT/P;target.parent.mkdir(parents=True);target.write_text(s);patch=''.join(difflib.unified_diff(before.splitlines(True),s.splitlines(True),fromfile='a/'+P,tofile='b/'+P));(D/'CLEAN_MENU384.patch').write_text(patch)
 report={'paths':[{'path':P,'beforeSha256':sha(base),'afterSha256':sha(target),'stagedPath':str(target)}],'game369Unchanged':True,'defaultMixedMenuFlowUnchanged':True,'newOptInArgument':'menuCleanWhole=true','actualOrdinarySettingsOnly':True,'effectsGainDuringWhole':0,'wholeOriginalFramesUnchanged':3904512,'waveformThresholdUnchanged':.995,'scope':'Additional diagnostic condition: normal actual sliders on map before entering established menu, effect0/music65/voice30, entire original first track before effects75 restoration, then same Home/mute/noisy/reentry/exit Save/RNG/Token checks. No test play directive/PCM edits/forced snapshot. Keeps old mixed-flow0.511 failure; not evidence that mix caused it, not yet compiled/installed/accepted, no substitute for full-media/default-output or ARM.','wholeGoalComplete':False};(D/'CLEAN_MENU384.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
