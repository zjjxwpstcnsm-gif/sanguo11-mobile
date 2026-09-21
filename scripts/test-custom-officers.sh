#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/custom-officers
find core/src/main/java -name '*.java' > core/build/custom-officers-sources.txt
printf '%s\n' core/src/test/java/game/sanguo/core/CustomOfficerTest.java >> core/build/custom-officers-sources.txt
javac -encoding UTF-8 --release 17 -d core/build/custom-officers @core/build/custom-officers-sources.txt
java -cp core/build/custom-officers:core/src/main/resources game.sanguo.core.CustomOfficerTest
python3 - <<'PY'
from pathlib import Path
import re
root=Path('core/src/main/java/game/sanguo/core')
manifest=dict(line.split('\t',1) for line in Path('core/src/main/resources/rules/skill-support.tsv').read_text().splitlines() if line and not line.startswith('#'))
for name,id in re.findall(r'^    (\w+)\("(san11[^\"]+)"',(root/'Skill.java').read_text(),re.M):
    assert id in manifest and manifest[id],id
    for module in manifest[id].split(', '):
        assert re.search(r'\b'+name+r'\b',(root/(module+'.java')).read_text()), (id,module)
print('SKILL MANIFEST PASS: all enabled IDs have audited production rule references')
PY
