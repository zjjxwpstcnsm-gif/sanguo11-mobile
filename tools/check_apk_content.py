#!/usr/bin/env python3
"""Assert test code/content are absent from the actual distributable, not just its menu."""
import sys, zipfile
from pathlib import Path
with zipfile.ZipFile(sys.argv[1]) as apk:
    names=set(apk.namelist())
    for line in Path('core/src/main/resources/scenarios/index.txt').read_text().splitlines():
        if line and not line.startswith('#'):
            assert 'scenarios/'+line.split()[0]+'.properties' in names,line
    for n in names:
        assert not n.startswith('test-scenarios/'),n
        assert not (n.startswith('scenarios/') and ('drill' in n or 'm0-skirmish' in n or 'regional-sandbox' in n or 'river-siege-sandbox' in n)),n
    for n in names:
        if n.endswith('.dex'):
            data=apk.read(n)
            for symbol in [b'Lgame/sanguo/core/battle/',b'Lgame/sanguo/core/DemoScenario;',b'Lgame/sanguo/core/TestScenarios;',b'Lgame/sanguo/core/ArchitectureFixture;']:
                assert symbol not in data,(n,symbol)
print('PASS: production scenarios packaged; nine test scenarios, Demo and isolated battle engine absent.')
