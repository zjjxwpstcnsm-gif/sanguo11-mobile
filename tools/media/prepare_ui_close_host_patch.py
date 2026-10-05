#!/usr/bin/env python3
"""Prepare a SHA-guarded cancellation-only public entry patch; never apply it."""
import argparse,difflib,hashlib,json,subprocess
from pathlib import Path

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source-repo',type=Path,required=True);p.add_argument('--source-commit',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    if a.output.exists():raise ValueError('Fresh patch directory required')
    path='app/src/main/java/game/sanguo/mobile/MainActivity.java';commit=subprocess.check_output(['git','-C',str(a.source_repo),'rev-parse',a.source_commit+'^{commit}'],text=True).strip();raw=subprocess.check_output(['git','-C',str(a.source_repo),'show',commit+':'+path]);before=raw.decode()
    anchor='    void trackDialog(AlertDialog dialog){confirmationDialog=dialog;UiTheme.dialog(dialog);fitConfirmation();}'
    if before.count(anchor)!=1 or 'setOnCancelListener' in before:raise ValueError('Existing cancellation contract drift; review before integration')
    after=before.replace(anchor,'    void trackDialog(AlertDialog dialog){confirmationDialog=dialog;dialog.setOnCancelListener(cancelled->{if(sounds!=null)sounds.cancelledDialog();});UiTheme.dialog(dialog);fitConfirmation();}',1)
    patch=''.join(difflib.unified_diff(before.splitlines(keepends=True),after.splitlines(keepends=True),fromfile='a/'+path,tofile='b/'+path)).encode()
    a.output.mkdir(parents=True);(a.output/'ui-close-host.patch').write_bytes(patch)
    (a.output/'guards.json').write_text(json.dumps({'sourceCommit':commit,'applied':False,'path':path,'beforeSha256':hashlib.sha256(raw).hexdigest(),'afterSha256':hashlib.sha256(after.encode()).hexdigest(),'patchSha256':hashlib.sha256(patch).hexdigest(),'scope':'Actual platform OnCancel only; no generic dismissal, button text, rules, metadata or commands'},indent=2)+'\n')
    print('Prepared cancellation-only entry patch',commit)
