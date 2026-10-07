#!/usr/bin/env python3
"""Bounded original evaluator capacity matrix; log fault registers, never JNI."""
from pathlib import Path
import json,hashlib,subprocess,os,struct,time,argparse
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-capacity-matrix236'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--resume',action='store_true');args=parser.parse_args()
    assert OUT.exists() if args.resume else not OUT.exists()
    if not args.resume:OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';names=['pc_effect_scene_probe.c','pc_effect_vm_probe.c'];before={n:sha(source/n) for n in names}
    if not args.resume:
        for n in names:(OUT/n).write_bytes((source/n).read_bytes())
    p=OUT/'pc_effect_vm_probe.c';s=(source/'pc_effect_vm_probe.c').read_text();old='if(m->executed_code_bytes>5000000) {m->fault=1;uc_emu_stop(u);}'
    assert s.count(old)==1
    log='''if(m->executed_code_bytes>5000000) {
        fprintf(stderr,"EXISTING_BLOCK_BYTE_GUARD pc=%llx bytes=%llu eax=%x ebx=%x ecx=%x edx=%x esi=%x edi=%x ebp=%x esp=%x\\n",
            (unsigned long long)address,(unsigned long long)m->executed_code_bytes,
            reg32(m,UC_X86_REG_EAX),reg32(m,UC_X86_REG_EBX),reg32(m,UC_X86_REG_ECX),reg32(m,UC_X86_REG_EDX),
            reg32(m,UC_X86_REG_ESI),reg32(m,UC_X86_REG_EDI),reg32(m,UC_X86_REG_EBP),reg32(m,UC_X86_REG_ESP));
        unsigned char header[16];uc_err read=uc_mem_read(u,reg32(m,UC_X86_REG_ESI),header,sizeof(header));
        fprintf(stderr,"FAULT_ESI_HEADER read=%u bytes=",read);if(read==UC_ERR_OK)for(unsigned j=0;j<sizeof(header);j++)fprintf(stderr,"%02x",header[j]);fprintf(stderr,"\\n");
        m->fault=1;uc_emu_stop(u);
    }'''
    s=s.replace(old,log)
    if args.resume:assert p.read_text()==s
    else:p.write_text(s)
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    compile=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(OUT/names[0]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    if not args.resume:
        with (OUT/'compile.log').open('w') as f:r=subprocess.run(compile,stdout=f,stderr=subprocess.STDOUT)
        assert r.returncode==0,(OUT/'compile.log').read_text()
    baseline=(ROOT/'out/session-a/fire-controller-capacity230/commands.bin').read_bytes();camera=baseline[12:216];center=struct.unpack_from('<3f',camera)
    allcells=[(x,y,0.) for x in range(150,158) for y in range(80,96)]
    def cmd(kind,serial,dt,cells=None):
        b=struct.pack('<IIf',kind,serial,dt)+camera
        if cells is not None:b+=struct.pack('<I',len(cells))+b''.join(struct.pack('<IIf',*c) for c in cells)
        return b
    env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    for key in ['PC_VM_PROBE_TRACE','PC_VM_PROBE_HEAP_PATH','PC_VM_PROBE_VISUAL_RNG_PATH']:env.pop(key,None)
    rows=[]
    for count in [1,13,32,64,128]:
        case=OUT/f'cells-{count:03d}';reuse=case.exists();assert not reuse or args.resume
        if not reuse:case.mkdir()
        cells=allcells[:count]
        steps=[(.1,cells)]*30+[(0.,cells)]+[(.1,[])]*40
        payload=cmd(0,0,0.)+b''.join(cmd(4,i+1,dt,c) for i,(dt,c) in enumerate(steps))+cmd(3,len(steps)+1,0.)
        begin=time.monotonic()
        if reuse:
            assert count==1 and (case/'commands.bin').read_bytes()==payload
            # Previous tool27015 reached the zero-packet assertion *inside* the
            # returncode==0 branch. Preserve that terminal child, never rerun it.
            r=subprocess.CompletedProcess([],0,(case/'stdout.bin').read_bytes(),(case/'stderr.txt').read_bytes())
        else:
            (case/'commands.bin').write_bytes(payload)
            r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,env=env,timeout=45)
            (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr)
        offset=16;frames=[]
        while offset+32<=len(r.stdout):
            header=r.stdout[offset:offset+32];assert header[:8]==b'PCFXFR01';serial,packets=struct.unpack_from('<II',header,8);elapsed,update,draw,geometry=struct.unpack_from('<4f',header,16);offset+=32
            body=r.stdout[offset:offset+packets*184];assert len(body)==packets*184;offset+=len(body)
            frames.append(dict(serial=serial,packets=packets,elapsed=elapsed,updateMs=update,drawMs=draw,geometryMs=geometry,packetSha256=hashlib.sha256(body).hexdigest()))
        assert offset==len(r.stdout)
        row={'controllers':count,'exit':r.returncode,'framesCompleted':len(frames),'framesRequested':len(steps),'wallSeconds':None if reuse else time.monotonic()-begin,'reusedTerminalChild':reuse,'maximumPackets':max([f['packets'] for f in frames],default=0),'frames':frames,'stderr':r.stderr.decode(),'commandSha256':sha(case/'commands.bin'),'stdoutSha256':sha(case/'stdout.bin'),'stderrSha256':sha(case/'stderr.txt')}
        if r.returncode==0:
            assert len(frames)==len(steps)
            assert f'active=0 created=0 stopped={count}' in row['stderr']
            row['allCellHandlesStopped']=True
            row['residualAmbientPackets']=frames[-1]['packets']
            row['pauseExact']=frames[29]['elapsed']==frames[30]['elapsed'] and frames[29]['packetSha256']==frames[30]['packetSha256']
        rows.append(row);print(json.dumps({k:v for k,v in row.items() if k!='frames'}),flush=True)
    for n in names:assert sha(source/n)==before[n]
    report={'initialHarnessAssertionCorrected':'The source scene retains ambientSEFF packets after cell handles stop. Zero total packets was an invalid assertion; previous1-cell child exit0/71frames retained, not rerun.', 'cases':rows,'sourceBeforeAfterExact':before,'staticUnicornSha256':sha(lib),'diagnosticSourceSha256':{n:sha(OUT/n) for n in names},'compileCommand':compile,'productionBlockBudget':True,'diagnosticHeight':0,'cameraSha256':hashlib.sha256(camera).hexdigest(),'scope':'Host-only original128-capacity diagnostic at1/13/32/64/128, chosen height0, fixed camera,30 warm/1 pause/40 stop. Existing5M bytes/5s cap remains. Only guard logging in native source clone; canonical source/JNI/World/rules/save/RNG unchanged. Not normal Android/ARM/GPU/whole-goal acceptance or original terrain placement. Host time overlaps live emulator and B build.', 'wholeGoalComplete':False}
    (DOC/'FIRE_CAPACITY_MATRIX236.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
