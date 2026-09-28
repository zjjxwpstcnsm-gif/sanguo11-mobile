#!/usr/bin/env python3
import pathlib, subprocess, tempfile, sys
checker=pathlib.Path(__file__).with_name('verify-candidate-source.py').resolve()
with tempfile.TemporaryDirectory() as temp:
    root=pathlib.Path(temp)
    def git(*args): return subprocess.check_output(['git',*args],cwd=root,text=True).strip()
    git('init','-q');git('config','user.name','test');git('config','user.email','test@example.invalid')
    (root/'source').write_text('original');git('add','.');git('commit','-qm','input');sha=git('rev-parse','HEAD')
    def check(expected,success):
        r=subprocess.run([sys.executable,str(checker),expected],cwd=root,capture_output=True,text=True)
        assert (r.returncode==0)==success,r.stdout+r.stderr
    check(sha,True);check('source-archive',False);check('0'*40,False)
    (root/'source').write_text('changed');check(sha,False);git('add','.');check(sha,False)
    git('restore','--staged','source');git('restore','source')
    (root/'new-source').write_text('new');check(sha,False)
print('PASS candidate identity: clean / invalid / wrong SHA / unstaged / staged / untracked')
