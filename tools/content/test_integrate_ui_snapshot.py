import json,subprocess,tempfile,unittest
from pathlib import Path
from types import SimpleNamespace
from integrate_ui_snapshot import main

class IntegrationTest(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name);self.ui=self.root/'ui';self.project=self.root/'project';self.ui.mkdir();self.project.mkdir()
  self.git('init','-q');self.git('config','user.email','fixture@example.invalid');self.git('config','user.name','Fixture')
  self.app='app/src/main/java/game/sanguo/mobile/A.java';self.allow='docs/architecture/LEGACY_CORE_ALLOWLIST.json';self.rules='docs/architecture/MODULE_RULES.md'
  self.files={self.app:'base\n',self.allow:json.dumps([self.app]),self.rules:'base rules\n','progress.md':'base progress\n'}
  for name,text in self.files.items():self.put(self.ui,name,text);self.put(self.project,name,text)
  self.base=self.commit()
 def git(self,*args):return subprocess.check_output(['git','-C',str(self.ui),*args]).decode().strip()
 def put(self,root,name,text):p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 def commit(self):self.git('add','.');self.git('commit','-qm','fixture');return self.git('rev-parse','HEAD')
 def run_merge(self,target,review=False,apply=True,output='result'):
  main(SimpleNamespace(ui_repo=self.ui,project=self.project,base=self.base,target=target,output=self.root/output,apply=apply,include_architecture_review=review))
 def snapshot(self):return {str(p.relative_to(self.project)):p.read_bytes() for p in self.project.rglob('*') if p.is_file()}
 def test_conflict_aborts_entire_batch(self):
  self.put(self.ui,self.app,'ui\n');self.put(self.ui,'app/B.java','new\n');target=self.commit();self.put(self.project,self.app,'local\n');before=self.snapshot()
  with self.assertRaisesRegex(ValueError,'conflict'):self.run_merge(target)
  self.assertEqual(before,self.snapshot())
 def test_frozen_blob_not_ui_wip_and_progress_append(self):
  self.put(self.ui,self.app,'frozen\n');self.put(self.ui,'progress.md','base progress\nUI delta\n');target=self.commit();self.put(self.ui,self.app,'WIP\n');self.put(self.project,'progress.md','base progress\ncore delta\n')
  self.run_merge(target);self.assertEqual('frozen\n',(self.project/self.app).read_text());self.assertEqual('base progress\ncore delta\nUI delta\n',(self.project/'progress.md').read_text())
 def test_reviewed_additions_preserve_local_architecture(self):
  extra='app/src/main/java/game/sanguo/mobile/B.java';local='app/src/main/java/game/sanguo/mobile/Local.java'
  self.put(self.ui,self.allow,json.dumps([self.app,extra]));self.put(self.ui,self.rules,'base rules\nUI review\n');target=self.commit()
  self.put(self.project,self.allow,json.dumps([self.app,local]));self.put(self.project,self.rules,'base rules\ncore review\n');before=self.snapshot()
  with self.assertRaisesRegex(ValueError,'Out-of-scope'):self.run_merge(target)
  self.assertEqual(before,self.snapshot());self.run_merge(target,review=True)
  self.assertEqual({self.app,extra,local},set(json.loads((self.project/self.allow).read_text())));self.assertEqual('base rules\ncore review\nUI review\n',(self.project/self.rules).read_text())
  self.put(self.project,self.rules,(self.project/self.rules).read_text()+'later local\n');before=self.snapshot();self.run_merge(target,review=True,output='repeat');self.assertEqual(before,self.snapshot())
 def test_architecture_removal_and_wildcard_rejected(self):
  for i,values in enumerate([[],[self.app,'app/src/main/java/game/sanguo/mobile/*.java']]):
   self.put(self.ui,self.allow,json.dumps(values));target=self.commit();before=self.snapshot()
   with self.assertRaises(ValueError):self.run_merge(target,review=True,output='bad'+str(i))
   self.assertEqual(before,self.snapshot())
 def test_preflight_writes_no_project_file(self):
  self.put(self.ui,self.app,'frozen\n');target=self.commit();before=self.snapshot();self.run_merge(target,apply=False);self.assertEqual(before,self.snapshot())

if __name__=='__main__':unittest.main()
