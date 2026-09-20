#!/usr/bin/env python3
from pathlib import Path
import re
p=Path('core/src/main/java/game/sanguo/core/Districts.java');s=p.read_text()
s,n=re.subn(r'(?<!public )\bString reserveError\(', 'public String reserveError(',s)
if n!=1:raise RuntimeError('Expected exactly one read-only reserve validator')
p.write_text(s)
p=Path('scripts/test-reports53.sh');s=p.read_text();anchor='java -cp "$cp_current" game.sanguo.core.Reports53Test 2>&1 | tee dist/core-checks.txt'
if s.count(anchor)!=1:raise RuntimeError('Missing report test runner')
s=s.replace(anchor,anchor+'\njava -Xmx1500m -cp "$cp_current" game.sanguo.core.Reports53NationalTest 2>&1 | tee dist/national-checks.txt')
p.write_text(s)
Path(__file__).unlink()
