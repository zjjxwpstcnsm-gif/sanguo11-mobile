#!/usr/bin/env python3
"""Build an isolated host C evaluator and compare complete source state.

This measures feasibility only. No Android, effect event binding or accepted
PC timing is implied. Uses the supplied EXE read-only, verified before copying
into the ignored probe directory; no source directory files are modified.
"""
import argparse
import gzip
import json
import os
from pathlib import Path
import platform
import subprocess
from pc_effect_machine import SourceEffectMachine, VerifiedSourceExecutable
from pc_resources import Archive, sha

ROOT = Path(__file__).resolve().parents[2]


def check(installation, unicorn_root, output, resources, steps):
    exe = (installation/'san11pk.exe').read_bytes()
    verified = VerifiedSourceExecutable(exe)
    output.mkdir(parents=True, exist_ok=True)
    # Keep native probe file access inside the project, with its original bytes.
    copied = output/'verified-source.exe'
    if not copied.is_file() or sha(copied.read_bytes()) != sha(exe):
        copied.write_bytes(exe)
    source = ROOT/'tools/content/native/pc_effect_vm_probe.c'
    library = unicorn_root/'lib'/('libunicorn.2.dylib' if platform.system()=='Darwin' else 'libunicorn.so.2')
    if not library.is_file(): raise ValueError('Explicit Unicorn2 shared library required')
    binary = output/'pc-effect-vm-probe-checked'
    build = subprocess.run(['clang','-O2','-std=c11','-Wall','-Wextra','-Werror',
        '-I'+str(unicorn_root/'include'),str(source),str(library),
        '-Wl,-rpath,'+str(library.parent),'-o',str(binary)],capture_output=True,text=True)
    (output/'build.txt').write_text(build.stdout+build.stderr)
    if build.returncode: raise RuntimeError('Native feasibility probe build failed')
    if platform.system()=='Darwin':
        subprocess.run(['codesign','-s','-','--force',str(binary)],check=True)
    archive = Archive(installation/'Media/san11pkres.bin')
    rows = []
    try:
        for resource in resources:
            raw = archive.read(resource)
            path = output/f'original-{resource}.ksef'
            path.write_bytes(raw)
            heap = output/f'{resource}-native-heap.bin'
            rng = output/f'{resource}-native-visual-rng.bin'
            env = dict(os.environ,PC_VM_PROBE_HEAP_PATH=str(heap),PC_VM_PROBE_VISUAL_RNG_PATH=str(rng))
            env.pop('PC_VM_PROBE_TRACE',None)
            run = subprocess.run([str(binary),str(copied),str(path),*[str(dt)for dt in steps]],
                env=env,capture_output=True,text=True,timeout=30)
            (output/f'{resource}-stderr.txt').write_text(run.stderr)
            if run.returncode: raise RuntimeError(f'Native resource{resource} failed: {run.returncode}')
            result = json.loads(run.stdout)
            (output/f'{resource}-native-updates.json').write_text(json.dumps(result,indent=2)+'\n')
            machine = SourceEffectMachine(verified)
            try:
                machine.load(raw);machine.start()
                for dt,frame in zip(steps,result['frames']):
                    machine.update(dt);reference=machine.snapshot()
                    if sha(bytes.fromhex(frame['root_hex']))!=reference['root_state_sha256']:
                        raise AssertionError((resource,'complete176-byte root'))
                    for field in ('source_emission_attempts','source_particle_allocations',
                                  'memory_allocation_count','memory_allocation_bytes'):
                        if frame[field]!=reference[field]:raise AssertionError((resource,field))
                if len(result['frames'])!=len(steps):raise AssertionError('Native step count')
                expected = bytes(machine.u.mem_read(machine.HEAP,0x1000000))
                expected_rng = bytes(machine.u.mem_read(0x8a5b68,2504))
                if heap.read_bytes()!=expected:raise AssertionError((resource,'complete16MiB source heap'))
                if rng.read_bytes()!=expected_rng:raise AssertionError((resource,'2504-byte visual RNG state'))
            finally:machine.close()
            compressed=output/f'{resource}-native-heap.bin.gz'
            compressed.write_bytes(gzip.compress(expected,mtime=0));heap.unlink()
            row=dict(resource_id=resource,source_sha256=sha(raw),heap_bytes=len(expected),
                heap_sha256=sha(expected),visual_rng_sha256=sha(expected_rng),
                initialization_ms=result['initialization_ms'],updates_ms=[f['native_update_ms']for f in result['frames']])
            rows.append(row);print(json.dumps(row),flush=True)
    finally:archive.close()
    report=dict(schema=1,goal_complete=False,status='PASS_HOST_NATIVE_FULL_HEAP_VISUAL_RNG_EXACT',
        resources=rows,checks=len(resources)*(len(steps)*5+2),source_executable_sha256=sha(exe),
        probe_source_sha256=sha(source.read_bytes()),unicorn_library_sha256=sha(library.read_bytes()),
        source_time_inputs=steps,runtime_effects_added=0,
        limits=['Host C feasibility only; no Android/ARM/render/event/PC timing acceptance',
            'Independent Python observer and native C execute identical supplied source code/input',
            'Five named bounded memory adapters and three named Windows query inputs retained',
            '5M instruction and5s wall cap per native call; isolated16MiB heap',
            'Native C camera provider and final draw packet geometry still pending'])
    (output/'comparison.json').write_text(json.dumps(report,indent=2)+'\n')
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation',type=Path)
    p.add_argument('--unicorn-root',type=Path,default=ROOT/'out/toolchain/pc-emulate/unicorn')
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/native-effect-feasibility')
    p.add_argument('--resources',type=int,nargs='+',default=[125,194,219,148])
    p.add_argument('--steps',type=float,nargs='+',default=[.0333333333,.5,1.0])
    a=p.parse_args()
    import math
    if any(not 125<=r<=368 for r in a.resources):p.error('Verified effect-table resource boundary125..368')
    if not a.steps or any(not math.isfinite(dt)or not 0<dt<=30 for dt in a.steps):p.error('Finite diagnostic steps in(0,30]')
    check(a.installation.resolve(),a.unicorn_root.resolve(),a.output.resolve(),a.resources,a.steps)
