#!/usr/bin/env python3
"""Reproduce source/type metadata and adapter admission without Android fixtures."""
import argparse
import hashlib
import json
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
OWN = pathlib.Path(__file__).resolve().parent


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--installation', type=pathlib.Path, required=True)
    p.add_argument('--baseline-voice', type=pathlib.Path, required=True)
    p.add_argument('--output', type=pathlib.Path, required=True)
    p.add_argument('--java', default=str(pathlib.Path(os.environ.get('JAVA_HOME', '/usr'))/'bin/java'))
    p.add_argument('--javac', default=str(pathlib.Path(os.environ.get('JAVA_HOME', '/usr'))/'bin/javac'))
    args = p.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    classes = args.output/'classes'
    classes.mkdir()
    sources = sorted((ROOT/'core/src/main/java').rglob('*.java'))
    sources.append(OWN/'VoiceSourceMetadataProbe.java')
    source_list = args.output/'sources.txt'
    source_list.write_text('\n'.join(str(x) for x in sources)+'\n')
    commands = []

    def run(command, output=None):
        commands.append(command)
        subprocess.run(command, cwd=ROOT, stdout=output, check=True)

    run([args.javac, '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@'+str(source_list)])
    with (args.output/'source-fields.tsv').open('w') as out:
        run([args.java, '-Xmx512m', '-cp', str(classes)+':'+str(ROOT/'core/src/main/resources'),
             'game.sanguo.mobile.VoiceSourceMetadataProbe'], out)
    run([sys.executable, str(OWN/'extend_voice_identity_catalog.py'),
         '--portrait', str(ROOT/'app/src/main/assets/portraits/pc/media-manifest.json'),
         '--voice', str(args.baseline_voice.resolve()),
         '--native', str(ROOT/'docs/handoff/20261004/session2/portrait-gaiji-native-30.json.gz'),
         '--authority', str(ROOT/'docs/handoff/20261004/session2/portrait-gaiji-authority-30.json'),
         '--source-fields', str(args.output/'source-fields.tsv'),
         '--installation', str(args.installation.resolve()), '--output', str(args.output/'extension')])
    app = ROOT/'app/src/main/java/game/sanguo/mobile'
    run([args.javac, '--release', '17', '-encoding', 'UTF-8', '-d', str(classes),
         str(ROOT/'game-api/src/main/java/game/sanguo/api/StateToken.java'),
         *[str(app/n) for n in ['MediaHashes.java', 'PortraitMediaIdentity.java', 'PcVoicePolicy.java', 'PcVoiceDirective.java']],
         str(OWN/'VoiceDirectiveDomainsProbe.java')])
    with (args.output/'directive-domains.txt').open('w') as out:
        run([args.java, '-cp', str(classes), 'game.sanguo.mobile.VoiceDirectiveDomainsProbe'], out)
    raw = (args.output/'extension/voice-identities.json').read_bytes()
    expected = (ROOT/'app/src/main/assets/audio/pc/voice-identities.json').read_bytes()
    if raw != expected:
        raise ValueError('Reproduced voice identity bytes differ from staged asset')
    report = {'commands': commands, 'reproducedAssetSha256': hashlib.sha256(raw).hexdigest(),
              'stagedAssetByteEqual': True,
              'scope': 'Readonly original-source and host adapter evidence; no Android normal caller/speaker/playback/ARM acceptance',
              'wholeGoalComplete': False}
    (args.output/'reproducibility.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
