#!/usr/bin/env python3
"""Execute source KSEF startup/emission/update; no Android acceptance claim."""
import argparse
import json
from pathlib import Path

from pc_effect_machine import SourceEffectMachine,VerifiedSourceExecutable
from pc_resources import Archive, sha

ROOT = Path(__file__).resolve().parents[2]


def inspect(installation, resources, steps, output):
    exe = (installation/'san11pk.exe').read_bytes()
    verified=VerifiedSourceExecutable(exe)
    archive = Archive(installation/'Media/san11pkres.bin')
    results = []
    try:
        for resource in resources:
            raw = archive.read(resource)
            item = dict(resource_id=resource, bytes=len(raw), sha256=sha(raw),
                        stages=[], status='unresolved', android_output=None, runtime_binding=None)
            machine = SourceEffectMachine(verified)
            stage = 'load'
            try:
                machine.load(raw)
                item['stages'].append(dict(stage=stage, **machine.snapshot()))
                stage = 'source-matrix-start'
                machine.start()
                item['stages'].append(dict(stage=stage, **machine.snapshot()))
                for dt in steps:
                    stage = 'update'
                    machine.update(dt)
                    item['stages'].append(dict(stage=stage, dt_source_units=dt,
                                               **machine.snapshot()))
                item['status'] = 'source-execution-complete-not-render-acceptance'
            except Exception as error:
                item.update(unresolved_stage=stage, reason=str(error),
                            source_trace=list(machine.trace), **machine.snapshot())
            results.append(item)
            print(json.dumps(dict(resource=resource, status=item['status'],
                                  source_particle_allocations=machine.successes,
                                  unresolved_stage=item.get('unresolved_stage'))), flush=True)
            machine.close()
    finally:
        archive.close()
    report = dict(schema=1, goal_complete=False, executable_sha256=sha(exe),
                  source_archive='Media/san11pkres.bin', resources=results,
                  native_CRT_table=dict(va='89d0a4', functions=['73bc80', '73bd20'],
                                        sha256=sha(exe[0x49d0a4:0x49d0ac])),
                  native_FPU_startup=dict(function='707075(1)', precision_function='70da68',
                      CPU_reset_input='FNINIT', verified_control_word='023f (all exceptions masked,53bit precision)',
                      previous_probe_limit='Unicorn initialCW0000 incorrectly caused64 RaiseException calls'),
                  memory_adapters=[hex(x) for x in SourceEffectMachine.MEMORY],
                  platform_inputs=[dict(iat=hex(iat), name=name, value=value, argument_count=argc)
                                   for iat, name, value, argc in SourceEffectMachine.PLATFORM],
                  runtime_effects_added=0,
                  native_visual_RNG=dict(initial_index=625, native_auto_seed=4357,
                      functions=['444150','444040'], source_static_va='8a5b68',
                      sha256=sha(exe[0x4a5b68:0x4a5b74])),
                  limits=['Original emission/controller/matrix arithmetic executes; no replacement artwork',
                          'No PC rasterization, original duration calibration or Android event/render binding',
                          'Source time steps are diagnostic inputs, not seconds/FPS acceptance',
                          'Absent optional DLL/registry overrides and deterministic zeroed heap are explicit oracle inputs',
                          'Native visual RNG runs in isolated source memory, without game rules/save/RNG access',
                          'Source effect semantic names and MOD runtime precedence require further evidence'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--resources', type=int, nargs='+', default=[148])
    p.add_argument('--steps', type=float, nargs='+', default=[1/30, .1, .5, 1, 2, 4, 10, 30])
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/effect-runtime-source.json')
    a = p.parse_args()
    if not a.steps or any(not 0 < dt <= 30 for dt in a.steps):
        p.error('Source diagnostic steps must be in (0,30]')
    inspect(a.installation, a.resources, a.steps, a.output)
