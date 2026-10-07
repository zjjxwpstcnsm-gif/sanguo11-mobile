#!/usr/bin/env python3
"""Stage completed parent plus exact military repair delta; exclude own Duel WIP.

Read-only compilation exports preserve every real production/A/JNI path.
"""
import hashlib
import argparse
import io
import json
import subprocess
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = 'aa9bdf6de39b521548b436aeedf81118f82e7ad0'
OVERLAYS = {'core/src/main/java/game/sanguo/core/Fieldworks.java',
            'app/src/main/java/game/sanguo/mobile/FieldworkUi.java',
            'app/src/androidTest/java/game/sanguo/mobile/SessionBFieldworksInstrumentation.java'}
TEST_FILES = {'app/src/androidTest/java/game/sanguo/mobile/SessionBFieldworksInstrumentation.java',
              'app/src/androidTest/java/game/sanguo/mobile/UiUxInstrumentation.java',
              'app/src/androidTest/java/game/sanguo/mobile/SessionProbe.java',
              'app/src/androidTest/java/game/sanguo/mobile/MapTap57Probe.java',
              'app/src/androidTest/java/game/sanguo/mobile/MapTap57Harness.java',
              'app/src/androidTest/java/game/sanguo/mobile/PcTacticLifecycleChecks.java',
              'app/src/androidTest/java/game/sanguo/core/UiPcCriticalsFixture.java'}
PREFIXES = ['core/src/main/java', 'core/src/main/resources', 'game-api/src/main/java',
            'game-runtime/src/main/java', 'app/src/main/java', 'core/src/testFixtures/java', 'app/src/androidTest/java']


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def main(label):
    if subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip() != BASE:
        raise ValueError('Re-audit changed completed parent')
    if label not in ['completed-repair-stage', 'completed-repair-stage-v2', 'completed-repair-stage-v3', 'completed-repair-stage-v4']:
        raise ValueError('Explicit registered stage required')
    dest = ROOT / 'out/session-b' / label
    dest.mkdir(exist_ok=False)
    data = subprocess.check_output(['git', 'archive', '--format=tar', BASE, *PREFIXES, *sorted(TEST_FILES)], cwd=ROOT)
    rows, found = [], set()
    with tarfile.open(fileobj=io.BytesIO(data)) as archive:
        for member in archive:
            if member.isdir():
                continue
            if not member.isfile() or member.name.startswith('/') or '..' in Path(member.name).parts:
                raise ValueError('Unsafe parent export')
            logical = member.name
            before = archive.extractfile(member).read()
            raw = (ROOT / logical).read_bytes() if logical in OVERLAYS else before
            output = dest / logical
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(raw)
            rows.append(dict(logical=logical, path=str(output.relative_to(ROOT)), parentSha256=sha(before), compiledSha256=sha(raw), overlay=logical in OVERLAYS))
            found.add(logical)
    if not OVERLAYS <= found or sum(row['overlay'] for row in rows) != 3:
        raise ValueError('Exact repair overlay missing')
    if any('PcDuel' in row['logical'] or 'PcNativeItemPolicy' in row['logical'] for row in rows):
        raise ValueError('Unfinished contest code entered completed source stage')
    report = dict(base=BASE, compiled=rows, overlays=sorted(OVERLAYS), excludedWip='all other uncommitted core/API/runtime/B page changes and new Duel/resources/raw loyalty/item policies', actualApkAccepted=False, limits=['Only exact three completed-parent source overlays in compilation', 'Current existing app assets/168 fixed resources/four JNI remain guarded and unchanged', 'Read-only six A frozen compile dependencies remain separately staged'])
    (dest / 'manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('Staged completed repair', len(rows), 'paths; exact three overlays; no Duel WIP')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--label', default='completed-repair-stage')
    main(parser.parse_args().label)
