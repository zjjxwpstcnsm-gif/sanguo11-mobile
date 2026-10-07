#!/usr/bin/env python3
"""Fresh-source Java fixture only; normal Android and source-art timing remain separate."""
import argparse
import json
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[4]
OWN = pathlib.Path(__file__).resolve().parent


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=pathlib.Path, required=True)
    p.add_argument('--jdk', type=pathlib.Path, required=True)
    p.add_argument('--baseline', action='store_true', help='Reproduce the preserved original blanket-override failure without Git')
    args = p.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    names = 'TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay SceneFactsPresentation PcCellFirePosition PcCellFireSet'.split()
    sources = sorted((ROOT/'core/src/main/java').rglob('*.java')) + sorted((ROOT/'game-api/src/main/java').rglob('*.java'))
    sources += [ROOT/f'app/src/main/java/game/sanguo/mobile/{name}.java' for name in names]
    if args.baseline:
        original = ROOT/'app/src/main/java/game/sanguo/mobile/PcCellFireSet.java'
        baseline = out/'PcCellFireSet.java'
        baseline.write_bytes((OWN/'cell-fire-admission-baseline89.java.txt').read_bytes())
        sources = [baseline if path == original else path for path in sources]
    sources.append(OWN/'CellFireAdmissionProbe.java')
    listing = out/'sources.txt'
    listing.write_text('\n'.join(map(str, sources))+'\n')
    classes = out/'classes'
    classes.mkdir()
    with (out/'compile.log').open('w') as log:
        subprocess.run([str(args.jdk/'bin/javac'), '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@'+str(listing)], stdout=log, stderr=subprocess.STDOUT, check=True)
    with (out/'result.txt').open('w') as log:
        result = subprocess.run([str(args.jdk/'bin/java'), '-Xmx384m', '-cp', str(classes)+':'+str(ROOT/'core/src/main/resources'), 'game.sanguo.mobile.CellFireAdmissionProbe'], stdout=log, stderr=subprocess.STDOUT)
    report = dict(exitCode=result.returncode, passed=result.returncode == 0,
                  result=(out/'result.txt').read_text(),
                  scope='Explicit detached fire fixtures / fresh full core+API and pure A source; no normal game fire creation, Android presentation, current-height original controller proof or ARM acceptance.',
                  actualAndroidAccepted=False, wholeGoalComplete=False)
    (out/'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(report, ensure_ascii=False))
    raise SystemExit(result.returncode)


if __name__ == '__main__':
    main()
