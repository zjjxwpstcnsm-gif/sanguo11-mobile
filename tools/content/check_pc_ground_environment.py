#!/usr/bin/env python3
"""Java production SENV arithmetic against original441d30 and actual c26/c27.

Original x86 routines execute in isolated memory with observed control0x007f.
Does not claim raster fidelity or Android GPU arithmetic acceptance.
"""
import argparse,json,os,struct,subprocess
from pathlib import Path
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_FPCW,UC_X86_REG_EIP
from pc_resources import Archive,sha
from import_pc_environment import environment
from inspect_pc_effect_bindings import EXE_SHA
ROOT=Path(__file__).resolve().parents[2]
def f32(x):return struct.unpack('<f',struct.pack('<f',x))[0]
def check(source,output):
    exe=(source/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
    a=Archive(source/'Media/san11pkres.bin')
    try:raw=a.read(4799)
    finally:a.close()
    assert raw==(ROOT/'app/src/main/assets/3d/pc-environment/environment.bin').read_bytes()
    states=environment(raw);u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    base=0x10000000;stop=base+0x4000;stack=base+0x3000;camera=base+0x1000;dest=base+0x2000;u.mem_map(base,0x5000)
    cases=[]
    for season in range(4):
        s=states[season*6]
        for near,far in [(0,1),(16,2048),(3.125,731.75),(16,f32(697.0001831054688))]:
            u.mem_write(camera,bytes(512));u.mem_write(camera+0x28,struct.pack('<2f',near,far));u.mem_write(camera+0x1dd,bytes([s['renderer_byte_1dd']]))
            u.mem_write(stack,struct.pack('<3I',stop,dest,camera));u.reg_write(UC_X86_REG_ESP,stack);u.reg_write(UC_X86_REG_FPCW,0x007f)
            u.emu_start(0x441d30,stop,count=1000);assert u.reg_read(UC_X86_REG_EIP)==stop
            fade=bytes(u.mem_read(dest,8));delta=f32(far-near)
            start=f32(near+f32(f32(s['fog_start_percent']*delta)*f32(.01)));end=f32(near+f32(f32(s['fog_end_percent']*delta)*f32(.01)))
            reciprocal=f32(1/f32(end-start));density=min(255,int(f32(s['fog_density_percent']*f32(2.55))))
            fog=struct.pack('<3f',reciprocal,f32(reciprocal*end),f32((255-density)*f32(1/255)))
            cases.append(dict(season=season,month=season*3+1,near=near,far=far,expected=(fog+fade).hex(),fade_oracle='original441d30 observed24bit'))
    work=output.parent/'environment-host';work.mkdir(parents=True,exist_ok=True)
    cases_file=work/'cases.tsv';cases_file.write_text('\n'.join(f"{c['month']}\t{c['near']}\t{c['far']}"for c in cases)+'\n')
    java=work/'PcGroundEnvironmentProbe.java';java.write_text('''package game.sanguo.mobile;
import java.io.*;import java.nio.*;import java.nio.file.*;
public class PcGroundEnvironmentProbe {
 public static void main(String[] args)throws Exception {
  PcEnvironment e=new PcEnvironment(Files.newInputStream(Path.of(args[0])));
  for(String line:Files.readAllLines(Path.of(args[1]))){String[] c=line.split("\\t");int month=Integer.parseInt(c[0]);float[] value=e.groundFog(month,Float.parseFloat(c[1]),Float.parseFloat(c[2]));ByteBuffer b=ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);for(float v:value)b.putFloat(v);System.out.println(java.util.HexFormat.of().formatHex(b.array()));if(month==7||month==10){ByteBuffer d=ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);for(float v:e.baseDirection(month))d.putFloat(v);System.err.println(month+":"+java.util.HexFormat.of().formatHex(d.array()));}}
 }
}''')
    home=Path(os.environ['JAVA_HOME']);subprocess.run([str(home/'bin/javac'),'--release','17','-d',str(work),str(ROOT/'app/src/main/java/game/sanguo/mobile/PcEnvironment.java'),str(java)],check=True)
    run=subprocess.run([str(home/'bin/java'),'-cp',str(work),'game.sanguo.mobile.PcGroundEnvironmentProbe',str(ROOT/'app/src/main/assets/3d/pc-environment/environment.bin'),str(cases_file)],text=True,capture_output=True,check=True);actual=run.stdout.splitlines();directions={int(line.split(':')[0]):line.split(':')[1]for line in run.stderr.splitlines()};assert len(actual)==len(cases)
    for c,line in zip(cases,actual):assert line==c['expected'],(c,line)
    live=[]
    for season,name in [(2,'source-xinye-texture-state-draw.bin'),(3,'source-xinye-winter-state-draw.bin')]:
        b=(ROOT/'out/pc-visual/v156/pc-reference'/name).read_bytes();o=16+11*8540+64
        original=b[o+26*16:o+26*16+12]+b[o+27*16:o+27*16+8]
        c=next(c for c in cases if c['season']==season and c['near']==16 and c['far']<700)
        assert original.hex()==c['expected'],(season,original.hex(),c)
        assert b[o+18*16:o+18*16+12].hex()==directions[season*3+1], ('actual source c18',season,directions)
        outline=16+24*8540+64
        assert struct.unpack_from('<4f',b,outline+3*16)==(f32(1.3),f32(1.3),f32(1.3),1)
        packet=json.loads((ROOT/'out/pc-visual/v156'/('source-texture-state-draw.json' if season==2 else 'source-winter-state-draw.json')).read_text())
        assert packet['records'][24]['draw']==24 and packet['records'][24]['draw_state']['vertex_shader']['sha256']=='7cec5ac0ddeb335ec0be19570aa788f966b17ea89e35db30e36826782cb38319'
        live.append(dict(season=season,c26_c27=original.hex(),capture_sha256=sha(b),draw=11,outline_draw=24,outline_extrusion_source=1.3,c18_direction_hex=directions[season*3+1]))
    output.write_text(json.dumps(dict(status='PASS',cases=len(cases),actual_source_draws=len(live),goal_complete=False,
        executable_sha256=EXE_SHA,environment_sha256=sha(raw),records=cases,actual_pc=live,
        limits=['Host production Java arithmetic only; GPU and PC raster comparisons separate','Only SENV base variants; source1500ms weather transition remains unbound']),indent=2)+'\n')
    print('PASS original441d30/Java',len(cases),'actual source c26/c27',len(live))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();check(a.installation,a.output)
