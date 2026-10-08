#!/usr/bin/env python3
"""Actual PC new-game picker requires explicit source settings; old loads unchanged."""
from pathlib import Path
import json,ctypes,os,shutil,difflib,hashlib
from stage_serial401 import ROOT,D,sha
from read_session_state import read_session_state as read
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/required-opening433'
def main():
 assert not OUT.exists();case=ROOT/'out/session-a/combined-fire428';r=read(case/'session.json');assert r['stage']=='restored-verified' and r['passed'] and not live_case(case) and all(x['exactRegularFileSha'] for x in r['restoration'].values())
 b=read(D/'SERIAL_TEST_BUILD416.json');index=Path(b['candidateInputManifest']);assert sha(index)==b['candidateInputManifestSha256'];source=Path(b['sourcePath']);rows=read(index);OUT.mkdir();stage=OUT/'source';stage.mkdir()
 libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 for row in rows:
  src=source/row['path'];assert sha(src)==row['sha256'];dst=stage/row['path'];dst.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(src),os.fsencode(dst),0):shutil.copy2(src,dst)
  assert sha(dst)==row['sha256'] and dst.stat().st_ino!=src.stat().st_ino
 path='app/src/main/java/game/sanguo/mobile/ScenarioFactionPicker.java';p=stage/path;old=p.read_text();text=old
 text=text.replace('openingSettings.setContentDescription("选择原新局设置");','openingSettings.setContentDescription("选择原新局设置");openingSettings.setTag("pc.opening.options");')
 text=text.replace('openingFacts=facts;openingSettings.setVisibility(facts==null?View.GONE:View.VISIBLE);return this;','openingFacts=facts;openingSettings.setVisibility(facts==null?View.GONE:View.VISIBLE);refreshOpeningStart();return this;')
 text=text.replace('final Map<String,TextView> labels=new HashMap<>();','final Map<String,TextView> labels=new HashMap<>();\n        final Map<String,Map<Integer,Button>> choiceButtons=new HashMap<>();')
 needle='''            for(var group:facts.groups)labels.get(group.id).setText(group.title+"："+selectedLabel(group,draft)+(group.fixedMenuValue!=null?"（原剧本固定）":""));'''
 assert text.count(needle)==1;text=text.replace(needle,'''            for(var group:facts.groups){
                labels.get(group.id).setText(group.title+"："+selectedLabel(group,draft)+(group.fixedMenuValue!=null?"（原剧本固定）":""));
                for(var entry:choiceButtons.get(group.id).entrySet())entry.getValue().setSelected(Objects.equals(draft.get(group.id),entry.getKey()));
            }''')
 text=text.replace('LinearLayout choices=new LinearLayout(a);rows.addView(choices);','LinearLayout choices=new LinearLayout(a);rows.addView(choices);choiceButtons.put(group.id,new HashMap<>());')
 text=text.replace('option.setEnabled(group.fixedMenuValue==null||group.fixedMenuValue==choice.value);','option.setTag("pc.opening."+group.id+"."+choice.value);\n                option.setEnabled(group.fixedMenuValue==null);choiceButtons.get(group.id).put(choice.value,option);')
 text=text.replace('.setNeutralButton("沿用现有开局",(d,n)->{openingOptions=null;openingChoices=Map.of();openingSettings.setText("原新局设置：沿用现有开局");})','.setNeutralButton("清除设置",(d,n)->{openingOptions=null;openingChoices=Map.of();openingSettings.setText("原新局设置：未选择");refreshOpeningStart();})')
 text=text.replace('holder[0].show();a.trackDialog(holder[0]);','holder[0].show();a.trackDialog(holder[0]);holder[0].getButton(AlertDialog.BUTTON_POSITIVE).setTag("pc.opening.confirm");')
 text=text.replace('openingChoices=Map.copyOf(draft);openingOptions=value;openingSettings.setText("原新局设置：已选择");holder[0].dismiss();','openingChoices=Map.copyOf(draft);openingOptions=value;openingSettings.setText("原新局设置：已选择");refreshOpeningStart();holder[0].dismiss();')
 text=text.replace('start.setEnabled(selectable(selected));confirmationDialog=null;','refreshOpeningStart();confirmationDialog=null;')
 text=text.replace('if(accepted||confirming)return;','if(accepted||confirming||sourceOpening()&&openingOptions==null)return;')
 text=text.replace('start.setEnabled(selectable(side));','refreshOpeningStart();')
 needle='''    private void mapFit(){'''
 assert text.count(needle)==1;text=text.replace(needle,'''    private void refreshOpeningStart(){
        start.setEnabled(!accepted&&!confirming&&selectable(selected)&&(!sourceOpening()||openingOptions!=null));
        if(sourceOpening()&&openingOptions==null){openingSettings.setText("原新局设置：请先选择");start.setText("请先选择新局设置");}
        else start.setText("以「"+w.governance.label(selected)+"」开始新局  →");
    }
    private void mapFit(){''')
 p.write_text(text);patch=''.join(difflib.unified_diff(old.splitlines(True),text.splitlines(True),fromfile='a/'+path,tofile='b/'+path));(D/'REQUIRED_OPENING434.patch').write_text(patch)
 records=[{'path':str(f.relative_to(stage)),'sha256':sha(f),'bytes':f.stat().st_size,'mode':f.stat().st_mode&0o777} for f in sorted(stage.rglob('*')) if f.is_file()];m=OUT/'candidate-inputs.json';m.write_text(json.dumps(records,indent=2)+'\n')
 changes=[{'path':path,'beforeSha256':next(x['sha256'] for x in rows if x['path']==path),'afterSha256':sha(p),'owner':'A'}]
 (D/'REQUIRED_OPENING434.json').write_text(json.dumps({'parent':'SERIAL_TEST_BUILD416.json','candidateInputManifest':str(m),'candidateInputManifestSha256':sha(m),'sourcePath':str(stage),'completeCandidateFiles':len(records),'productionChanges':changes,'patchSha256':hashlib.sha256(patch.encode()).hexdigest(),'BProductionUnchanged':True,'oldReadsAndThreeArgFactoriesUnchanged':True,'actualInstalled':False,'scope':'Normal PC picker now requires all verified menu choices explicitly adopted, fixed choice selected/locked, no inferred defaults/old engineering fallback from this new UI. A stable tags support frozen B actual touch helpers. Reconstructed/custom openings and old Save/factory APIs unchanged; no World/RNG mutation. New independent game/test build and actual normal/cancel/save/cold/all16/fire/military/native contests/ARM required; oldce83 proofs cannot be transferred.','wholeGoalComplete':False},indent=2)+'\n');print(json.dumps({'productionChanged':path,'built':False}))
if __name__=='__main__':main()
