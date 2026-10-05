#!/usr/bin/env python3
"""Extend the frozen v35 merchant flow to explicit native37 and whole-save commit comparison."""
import argparse
import hashlib
import json
from pathlib import Path
from build_native_merchant_ui_probe import ROOT, build

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--output', required=True, type=Path)
a = p.parse_args()
out = a.output.resolve()
if ROOT/'out' not in out.parents:
    raise ValueError('Output must be inside project out/')
out.mkdir(parents=True, exist_ok=False)
source = ROOT/'tools/content/android/NativeMerchantUiInstrumentation.java'
original = source.read_text(encoding='utf-8')
assert hashlib.sha256(original.encode()).hexdigest() == 'f2a336a513a53783d49306fa9e0c27b75db5525d26f7d1e4a3689d92a63bf032', 'Frozen legacy flow changed; review instead of silently rebuilding'
changes = [
    ('check(((original[7]&255)==35)&&world().merchantMarket.enabled(),"actual normal v35 campaign loaded");',
     'check(((original[7]&255)==37)&&world().merchantMarket.enabled()&&world().pcProduction.enabled()&&world().pcTechniquePoints.enabled(),"actual normal native37 campaign loaded with explicit market/production/technique policies");'),
    ('"normal load restores full v35 price/ability/RNG state"', '"normal load restores full native37 price/ability/RNG state"'),
    ('        World after=world();check(activity.deploymentState().revision==revision+1,',
     '        World control=SaveCodec.decode(original);World.Result controlTrade=control.campaign.trade(city,actor,true,quantity);check(controlTrade.ok,"independent normal merchant command succeeds");check(Arrays.equals(SaveCodec.encode(control),capture()),"real merchant commit matches every save byte and RNG from independent normal command");\n        World after=world();check(activity.deploymentState().revision==revision+1,')
]
changed = original
for before, after in changes:
    assert changed.count(before) == 1
    changed = changed.replace(before, after)
inverse = changed
for before, after in reversed(changes):
    assert inverse.count(after) == 1
    inverse = inverse.replace(after, before)
assert inverse == original
generated = out/source.name
generated.write_text(changed, encoding='utf-8')
build(out/'probe', 'NativeMerchantUiInstrumentation', [generated])
(out/'extension-audit.json').write_text(json.dumps(dict(
    frozen_original_source_path=str(source.relative_to(ROOT)),
    frozen_original_sha256=hashlib.sha256(original.encode()).hexdigest(),
    extended_sha256=hashlib.sha256(changed.encode()).hexdigest(), inverse_byte_equal=True,
    original_quantity_minimum_maximum_purity_cancel_double_commit_xp_ap_and_save_assertions_preserved=True,
    additional_scope='explicit new37 policies and whole-save normal command comparison',
    original_v35_source_changed=False, production_apk_changed=False), indent=2)+'\n')
