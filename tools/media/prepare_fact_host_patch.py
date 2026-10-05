#!/usr/bin/env python3
"""Prepare, never apply, the sequential public host media patch from committed source only."""
import argparse
import difflib
import hashlib
import json
from pathlib import Path
import subprocess

def once(source,before,after):
    if source.count(before)!=1:raise ValueError('Source contract drift: '+before[:100])
    return source.replace(before,after,1)
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source-repo',type=Path,required=True)
    p.add_argument('--source-commit',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    commit=subprocess.check_output(['git','-C',str(a.source_repo),'rev-parse',a.source_commit+'^{commit}'],text=True).strip()
    if a.output.exists():raise ValueError('Fresh patch output required')
    a.output.mkdir(parents=True);rows=[];patch=[]
    for name in ['MainActivity','MapHost','TurnPlayback']:
        path='app/src/main/java/game/sanguo/mobile/'+name+'.java'
        raw=subprocess.check_output(['git','-C',str(a.source_repo),'show',commit+':'+path]);before=raw.decode();after=before
        if name=='MainActivity':
            after=once(after,'    private void sessionChanged(GameEvent event){\n',
                '    private void sessionChanged(GameEvent event){\n        if(techniqueHud!=null)techniqueHud.committedFacts(event,world.player);\n')
            after=once(after,'        techniqueHud.update(legacyView.state,world.player,world.campaign.points(world.player),techniqueDeferred);',
                '        if(techniqueHud.mediaNeedsResync())techniqueHud.resynchronizeFacts(legacyView.state,world.player,world.campaign.points(world.player));\n'
                '        else techniqueHud.syncFactsBaseline(legacyView.state,world.player,world.campaign.points(world.player));')
            after=once(after,'        map.setTechniqueFeedback(techniqueHud::releasePending);',
                '        map.setTechniqueFeedback(techniqueHud::releasePresentation,techniqueHud::skipPresentation,techniqueHud::discardPending,techniqueHud::pauseFacts);')
        elif name=='MapHost':
            after=once(after,'    private Runnable techniqueFeedback;\n    void setTechniqueFeedback(Runnable feedback){techniqueFeedback=feedback;}',
                '    private Consumer<String> techniqueFeedback,techniqueSkip;\n'
                '    private Runnable techniqueDiscard;\n    private Consumer<Boolean> techniquePause;\n'
                '    private List<TurnJournal.Event> techniqueCommandEvents=Collections.emptyList();\n'
                '    void setTechniqueFeedback(Consumer<String> feedback,Consumer<String> skip,Runnable discard,Consumer<Boolean> pause){techniqueFeedback=feedback;techniqueSkip=skip;techniqueDiscard=discard;techniquePause=pause;}\n'
                '    private void techniquePhase(TurnJournal.Event event){\n'
                '        if(event==null)return;\n'
                '        if(resumed&&replayCommitted&&renderGate.active()&&techniqueFeedback!=null)techniqueFeedback.accept(event.id);\n    }\n'
                '    private void techniqueFinished(TurnJournal.Event event){\n'
                '        if(event==null)return;\n'
                '        if(resumed&&replayCommitted&&renderGate.active())techniquePhase(event);\n'
                '        else if(techniqueSkip!=null)techniqueSkip.accept(event.id);\n    }\n'
                '    void discardTechniqueMedia(){if(techniqueDiscard!=null)techniqueDiscard.run();}')
            after=once(after,'void pauseEffects(boolean paused){if(spatial!=null)',
                'void pauseEffects(boolean paused){if(techniquePause!=null)techniquePause.accept(paused);if(spatial!=null)')
            after=once(after,'void finishReplay(TurnJournal.Event event){completionSound(event);',
                'void finishReplay(TurnJournal.Event event){techniqueFinished(event);completionSound(event);')
            after=once(after,'        cancelCommandEffects();replayCommitted=true;for(var event:events)constructionSound(event);',
                '        cancelCommandEffects();replayCommitted=true;techniqueCommandEvents=new ArrayList<>(events);for(var event:events)constructionSound(event);\n'
                '        if(events.size()>CombatSequence.CAPACITY){for(var event:events){if(techniqueSkip!=null)techniqueSkip.accept(event.id);combatLedger().finish(event);}techniqueCommandEvents=Collections.emptyList();return;}')
            after=once(after,'            if(techniqueFeedback!=null)techniqueFeedback.run();',
                '            techniqueCommandEvents=Collections.emptyList();')
            after=once(after,'        if(commandEffects!=null&&techniqueFeedback!=null)techniqueFeedback.run();',
                '        if(commandEffects!=null){for(var event:techniqueCommandEvents)if(techniqueSkip!=null)techniqueSkip.accept(event.id);}\n'
                '        techniqueCommandEvents=Collections.emptyList();')
            after=once(after,'        commandEffects.advance(wallElapsed,this::replayVisible);commandEffectTime=now;',
                '        commandEffects.advance(wallElapsed,this::replayVisible);commandEffectTime=now;\n'
                '        if(!commandEffects.paused())for(var event:techniqueCommandEvents)if(combatLedger().completed(event))techniqueFinished(event);')
            after=once(after,'if(e!=null&&fraction>=.35f&&replayCommitted&&resumed&&techniqueFeedback!=null)techniqueFeedback.run();',
                'if(e!=null&&fraction>=.35f)techniquePhase(e);')
        else:
            after=once(after,'void detach(){detached=true;', 'void detach(){map.discardTechniqueMedia();detached=true;')
            after=once(after,'void skip(){work.skipAnimations=true;', 'void skip(){map.discardTechniqueMedia();work.skipAnimations=true;')
            after=once(after,'        if(work.fastForward()){\n', '        if(work.fastForward()){\n            map.discardTechniqueMedia();\n')
        encoded=after.encode();target=a.output/'candidate'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(encoded)
        rows.append(dict(path=path,sourceCommit=commit,beforeSha256=hashlib.sha256(raw).hexdigest(),afterSha256=hashlib.sha256(encoded).hexdigest()))
        patch.extend(difflib.unified_diff(before.splitlines(keepends=True),after.splitlines(keepends=True),fromfile='a/'+path,tofile='b/'+path))
    body=''.join(patch).encode();(a.output/'fact-host.patch').write_bytes(body)
    (a.output/'guards.json').write_text(json.dumps(dict(sourceCommit=commit,applied=False,
        purpose='Sequential media callbacks only; metadata/page changes inherited from committed session1 source',
        patchSha256=hashlib.sha256(body).hexdigest(),files=rows),indent=2)+'\n')
    print('Prepared unapplied patch from committed source',commit,'files=',len(rows))
if __name__=='__main__':main()
