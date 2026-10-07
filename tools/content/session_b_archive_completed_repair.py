#!/usr/bin/env python3
"""Archive exact completed repair parent, overlays, JNI and compiler inputs."""
import gzip
import hashlib
import io
import json
import subprocess
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = 'aa9bdf6de39b521548b436aeedf81118f82e7ad0'
STAGE = 'out/session-b/completed-repair-stage-v4'
OUTPUT = ROOT / 'out/session-b/completed-repair-54-source-v2.tar.gz'
MANIFEST = ROOT / 'out/session-b/completed-repair-54-source-v2-manifest.json'
OVERLAYS = ['core/src/main/java/game/sanguo/core/Fieldworks.java',
            'app/src/main/java/game/sanguo/mobile/FieldworkUi.java',
            'app/src/androidTest/java/game/sanguo/mobile/SessionBFieldworksInstrumentation.java',
            'core/src/test/java/game/sanguo/core/SessionBFieldworksTest.java']
TOOLS = ['docs/handoff/20261006/session-b/completed-repair.init.gradle',
         'tools/content/session_b_stage_completed_repair.py',
         'tools/content/session_b_freeze_completed_repair.py',
         'tools/content/session_b_verify_fieldworks_ui.py',
         'tools/content/session_b_archive_completed_repair.py']


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def completed_tool(name, raw):
    if name != 'tools/content/session_b_verify_fieldworks_ui.py':
        return raw
    text = raw.decode()
    if "'items'" not in text:
        compile(text, name, 'exec')
        return raw
    for fragment in ["'items':'game.sanguo.mobile.SessionBDebateInstrumentation',", "'items':'PASS SESSION B ITEMS',", "'items':'actual source0 ordinary native book/newmenu/cancel/confiscate-award/current inventory/save/fullWorld/RNG/cold/fullturn; hidden/latent/fullDuel/ARM pending',"]:
        if fragment not in text:
            raise ValueError('Optional verifier mode changed; audit again')
        text = text.replace(fragment, '')
    text = text.replace("['search','direct','items']", "['search','direct']")
    text = text.replace("'search','direct','items'", "'search','direct'")
    assert "'items'" not in text
    compile(text, name, 'exec')
    return text.encode()


def file_sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def main():
    if OUTPUT.exists() or MANIFEST.exists():
        raise ValueError('Preserve earlier source archive')
    report = json.loads((ROOT / 'out/session-b/fieldwork-repair-ui-54/results.json').read_text())
    if not report.get('passed'):
        raise ValueError('Actual APK54 acceptance and full device restoration required')
    stage = json.loads((ROOT / STAGE / 'manifest.json').read_text())
    assert stage['base'] == BASE
    for row in stage['compiled']:
        assert file_sha(ROOT / row['path']) == row['compiledSha256']
        if row['overlay']:
            assert file_sha(ROOT / row['logical']) == row['compiledSha256']
    jni = json.loads((ROOT / 'docs/handoff/20261006/session-b/INHERITANCE.json').read_text())['jni']
    for row in jni:
        assert file_sha(ROOT / row['path']) == row['sha256']
    extras = set(TOOLS + [r['path'] for r in jni])
    for prefix in [STAGE, 'out/session-b/readonly-theme-dependencies']:
        extras.update(str(p.relative_to(ROOT)) for p in (ROOT / prefix).rglob('*') if p.is_file())
    rows = []
    parent_files = set(subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', '-z', BASE], cwd=ROOT).decode().split('\0')) - {''}
    parent = subprocess.Popen(['git', 'archive', '--format=tar', BASE], cwd=ROOT, stdout=subprocess.PIPE)
    try:
        with OUTPUT.open('xb') as stream, gzip.GzipFile(fileobj=stream, mode='wb', filename='', mtime=0, compresslevel=1) as compressed, tarfile.open(fileobj=compressed, mode='w|', format=tarfile.PAX_FORMAT) as dest, tarfile.open(fileobj=parent.stdout, mode='r|') as source:
            found = set()
            for member in source:
                if member.isdir():
                    continue
                if not member.isfile() or member.name.startswith('/') or '..' in Path(member.name).parts:
                    raise ValueError('Unsupported parent archive entry: ' + member.name)
                before = source.extractfile(member).read()
                current = ROOT / member.name
                raw = current.read_bytes() if member.name in OVERLAYS + TOOLS else before
                raw = completed_tool(member.name, raw)
                member.size = len(raw)
                dest.addfile(member, io.BytesIO(raw))
                rows.append(dict(path=member.name, bytes=len(raw), sha256=sha(raw), mode=member.mode, parentSha256=sha(before), origin='repair-overlay' if member.name in OVERLAYS else 'repair-tool' if member.name in TOOLS else 'completed-parent'))
                found.add(member.name)
            if found != parent_files or not set(OVERLAYS).issubset(found):
                raise ValueError('Parent export omitted tracked inputs')
            for name in sorted(extras - found):
                path = ROOT / name
                raw = completed_tool(name, path.read_bytes())
                member = tarfile.TarInfo(name)
                member.mode = path.stat().st_mode & 0o777
                member.size = len(raw)
                dest.addfile(member, io.BytesIO(raw))
                rows.append(dict(path=name, bytes=len(raw), sha256=sha(raw), mode=member.mode, origin='frozen-compiler-input-or-repair-tool'))
        if parent.wait() != 0:
            raise ValueError('Parent git archive failed')
    finally:
        parent.stdout.close()
        if parent.poll() is None:
            parent.terminate()
            parent.wait()
    expected = {r['path']: r for r in rows}
    seen = set()
    with tarfile.open(OUTPUT, 'r|gz') as check:
        for member in check:
            row = expected[member.name]
            assert member.isfile() and member.mode == row['mode']
            assert sha(check.extractfile(member).read()) == row['sha256']
            assert member.name not in seen
            seen.add(member.name)
    assert seen == set(expected)
    MANIFEST.write_text(json.dumps(dict(base=BASE, rows=rows, fileCount=len(rows), archiveSha256=file_sha(OUTPUT), bytes=OUTPUT.stat().st_size, everyEntryAndModeReadbackVerified=True, productionOverlays=OVERLAYS[:2], acceptedApkSha=report['installed']['game.sanguo.mobile.dev'], frozenACommit='47326188fbc43837c8d52caf0fa76051390faaf4', jni=jni, limits=['Completed repair batch only; all other own Duel/raw-loyalty/item WIP excluded', 'Device original user backups are excluded', 'Reproduce with existing JDK17/Android SDK and own completed-repair init; SDK is not distributed']), ensure_ascii=False, indent=2) + '\n')
    print('PASS completed source archive', len(rows), file_sha(OUTPUT), OUTPUT.stat().st_size)


if __name__ == '__main__':
    main()
