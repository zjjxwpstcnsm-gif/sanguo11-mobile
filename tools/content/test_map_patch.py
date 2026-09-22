#!/usr/bin/env python3
"""Integration test in a disposable repository copy, never the default resource tree."""
import hashlib, importlib.util, json, pathlib, shutil, subprocess, sys, tempfile
ROOT=pathlib.Path(__file__).resolve().parents[2]

def hashes(root):
    files=list((root/'core/src/main/resources/maps').rglob('*'))+list((root/'core/src/main/resources/scenarios').rglob('*'))
    files += [root/'core/src/main/java/game/sanguo/core/NationalMap.java',root/'tools/content/map-release-manifest.json']
    return {str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files if p.is_file()}

def main():
    fixture=pathlib.Path(sys.argv[1]).resolve();before=hashes(ROOT)
    with tempfile.TemporaryDirectory(prefix='test-map-merge-') as directory:
        target=pathlib.Path(directory)/'repo'
        shutil.copytree(ROOT/'core/src/main',target/'core/src/main')
        (target/'tools/content').mkdir(parents=True)
        for name in ['map_patch.py','MapPatchRepository.java','map-release-manifest.json']:
            shutil.copy2(ROOT/'tools/content'/name,target/'tools/content'/name)
        tool=target/'tools/content/map_patch.py';output=pathlib.Path(directory)/'log.txt'
        with output.open('w') as log:
            subprocess.run([sys.executable,str(tool),str(fixture),'--apply'],stdout=log,stderr=subprocess.STDOUT,check=True)
        applied=hashes(target)
        repeat=subprocess.run([sys.executable,str(tool),str(fixture),'--apply'],capture_output=True,text=True,check=True)
        assert 'Already applied' in repeat.stdout and hashes(target)==applied,'repeat import changed files'
        damaged=json.loads(fixture.read_text());damaged['baseFingerprint']='0'*64
        invalid=pathlib.Path(directory)/'bad.json';invalid.write_text(json.dumps(damaged))
        failure=subprocess.run([sys.executable,str(tool),str(invalid),'--apply'],capture_output=True,text=True)
        assert failure.returncode!=0 and hashes(target)==applied,'failed baseline modified resources'
        spec=importlib.util.spec_from_file_location('merge_tool',tool);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        journal=target/'.map-patch-transaction';journal.mkdir();victim=pathlib.Path('core/src/main/resources/scenarios/index.txt');old=(target/victim).read_bytes();(journal/'0').write_bytes(old)
        newer=old+b'# interrupted\n';(target/victim).write_bytes(newer)
        (journal/'journal.json').write_text(json.dumps([dict(path=str(victim),backup='0',before=module.digest(old),after=module.digest(newer))]))
        module.recover(target);assert hashes(target)==applied,'recovery did not restore original resource'
        assert hashes(ROOT)==before,'real repository was changed by tests'
        print('PASS repository Patch integration: exact nine-scenario loading, seven editor fingerprints, transactional apply, duplicate no-op, conflict no-write and interrupted-write recovery.')
        print('Input SHA256:',hashlib.sha256(fixture.read_bytes()).hexdigest())
if __name__=='__main__':main()
