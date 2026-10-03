#!/usr/bin/env python3
"""Build a fixed-source Win32 observer/helper in the private PC clone.

Original resources remain readonly. Import libraries use documented Win32
stdcall declarations; no DLL is renamed to fake a Wine builtin module.
"""
from pathlib import Path
import hashlib,json,struct,subprocess,argparse
ROOT=Path(__file__).resolve().parents[2]
PIN='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
def build(only=None):
    copy=ROOT/'out/pc-visual/pc-runtime-copy';out=ROOT/'out/pc-visual/pc-readback-build';out.mkdir(parents=True,exist_ok=True)
    if hashlib.sha256((copy/'san11pk.exe').read_bytes()).hexdigest()!=PIN:raise ValueError('Observer requires supplied fixed-base source EXE')
    lld=ROOT/'out/toolchain/android-sdk/build-tools/35.0.0/lld'
    imports={
      'kernel32':['CreateFileA@28','ReadFile@20','WriteFile@20','CloseHandle@4','ExitProcess@4','GetFileAttributesA@4','DeleteFileA@4',
                  'CreateThread@24','GetModuleHandleA@4','Sleep@4','VirtualQuery@12','OpenProcess@12','VirtualAllocEx@20',
                  'WriteProcessMemory@20','ReadProcessMemory@20','CreateRemoteThread@28','GetProcAddress@8','WaitForSingleObject@8','GetExitCodeThread@8','VirtualProtect@16','GetTickCount@0','GetCurrentProcessId@0','GetCurrentThreadId@0'],
      'user32':['EnumWindows@8','IsWindowVisible@4','GetWindowTextW@12','GetWindowThreadProcessId@8',
                'GetClientRect@8','ClientToScreen@8','SetForegroundWindow@4','SetCursorPos@8','GetCursorPos@4','GetForegroundWindow@0','SendMessageTimeoutA@28','mouse_event@20','keybd_event@16']}
    libraries=[]
    for dll,symbols in imports.items():
        definition=out/(dll+'.def');definition.write_text('LIBRARY '+dll+'.dll\nEXPORTS\n'+'\n'.join('_'+n+'='+n.split('@')[0]for n in symbols)+'\n')
        lib=out/(dll+'.lib')
        subprocess.run([str(lld),'-flavor','link','/dll','/noentry','/machine:x86','/def:'+str(definition),'/out:'+str(out/(dll+'.dll')),'/implib:'+str(lib)],check=True)
        data=bytearray(lib.read_bytes());offset=8;patched=0
        while offset<len(data):
            size=int(data[offset+48:offset+58]);start=offset+60
            if data[start:start+4]==b'\0\0\xff\xff':
                flags=struct.unpack_from('<H',data,start+18)[0];struct.pack_into('<H',data,start+18,(flags&~28)|12);patched+=1
            offset=start+size+(size&1)
        if patched!=len(symbols):raise ValueError('Unexpected Win32 import layout')
        lib.write_bytes(data);libraries.append(str(lib))
    outputs=[]
    for name,dest,extra in [('readback_observer','pc-readback-observer.dll',['/dll','/entry:DllMain@12']),('inject_capture','pc-inject-capture.exe',['/entry:entry','/subsystem:console']),('inject_mouse','pc-inject-mouse.exe',['/entry:entry','/subsystem:console']),('inject_fpu','pc-inject-fpu.exe',['/entry:entry','/subsystem:console']),('inject_sampler','pc-inject-sampler.exe',['/entry:entry','/subsystem:console']),('sampler_observer','pc-sampler-observer.dll',['/dll','/entry:DllMain@12']),('fpu_observer','pc-fpu-observer.dll',['/dll','/entry:DllMain@12']),('mouse_input','pc-mouse-input.dll',['/dll','/entry:DllMain@12']),('ui_command','pc-ui-command.exe',['/entry:entry','/subsystem:console']),('read_camera','pc-read-camera.exe',['/entry:entry','/subsystem:console']),('read_regions','pc-read-regions.exe',['/entry:entry','/subsystem:console']),('read_portraits','pc-read-portraits.exe',['/entry:entry','/subsystem:console'])]:
        if only is not None and name!=only:continue
        obj=out/(name+'.obj');source=ROOT/'tools/pc-runtime'/('inject_capture.c' if name in ('inject_mouse','inject_fpu','inject_sampler') else name+'.c')
        defines=['-DPC_MOUSE_INPUT'] if name=='inject_mouse' else ['-DPC_FPU_OBSERVER'] if name=='inject_fpu' else ['-DPC_SAMPLER_OBSERVER'] if name=='inject_sampler' else []
        subprocess.run(['clang','-target','i686-pc-windows-msvc','-O1','-ffreestanding','-fno-stack-protector',*defines,'-c',str(source),'-o',str(obj)],check=True)
        output=copy/dest
        subprocess.run([str(lld),'-flavor','link','/machine:x86','/nodefaultlib','/timestamp:0',*extra,'/out:'+str(output),str(obj),*libraries],check=True)
        outputs.append(dict(path=str(output.relative_to(ROOT)),bytes=output.stat().st_size,sha256=hashlib.sha256(output.read_bytes()).hexdigest(),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest()))
    report=dict(goal_complete=False,status='BUILT_PC_PRIVATE_COPY_READBACK_OBSERVER_NOT_CAPTURE_ACCEPTANCE',source_exe_sha256=PIN,device_slot='0x6ed6f74',source_device_slot_evidence='native/pc_effect_vm_probe.c original source quad boundary',outputs=outputs)
    if only=='read_portraits':
        report.pop('device_slot',None);report.pop('source_device_slot_evidence',None)
        report['status']='BUILT_READONLY_PC_PORTRAIT_DESCRIPTOR_READER_NOT_LIVE_EVIDENCE'
        report['visual_table']={'address':'0x6fae8b8','entries':2400,'stride':4,'source_lookup':'480320;48a5b0','snapshot':'two equal ReadProcessMemory reads required'}
    if only is not None and not outputs:raise ValueError('Unknown requested helper')
    (out/(('build-'+only+'.json') if only else 'build.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--only');build(parser.parse_args().only)
