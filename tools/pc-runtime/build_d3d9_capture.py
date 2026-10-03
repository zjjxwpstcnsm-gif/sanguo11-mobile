#!/usr/bin/env python3
"""Build a readback observer in the PRIVATE copy, never the PC installation."""
import argparse
import shutil
import subprocess
import struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def build(wine,lld):
    destination=ROOT/'out/pc-visual/pc-d3d9-build';destination.mkdir(parents=True,exist_ok=True)
    copy=ROOT/'out/pc-visual/pc-runtime-copy'
    definition=destination/'kernel32.def'
    symbols=['LoadLibraryA@4','GetProcAddress@8','CreateFileA@28','WriteFile@20','CloseHandle@4','GetFileAttributesA@4','DeleteFileA@4']
    definition.write_text('LIBRARY kernel32.dll\nEXPORTS\n'+'\n'.join('_'+n+'='+n.split('@')[0] for n in symbols)+'\n')
    lib=destination/'kernel32.lib'
    subprocess.run([str(lld),'-flavor','link','/dll','/noentry','/machine:x86','/def:'+str(definition),'/out:'+str(destination/'kernel32.dll'),'/implib:'+str(lib)],check=True)
    # COFF short import NAME_UNDECORATE resolves x86 stdcall's _name@bytes to
    # kernel32's undecorated export, keeping the decorated linker symbols.
    data=bytearray(lib.read_bytes());offset=8;patched=0
    while offset<len(data):
        size=int(data[offset+48:offset+58]);start=offset+60
        if data[start:start+4]==b'\x00\x00\xff\xff':
            flags=struct.unpack_from('<H',data,start+18)[0]
            struct.pack_into('<H',data,start+18,(flags&~28)|12);patched+=1
        offset=start+size+(size&1)
    if patched!=len(symbols):raise ValueError('Unexpected import library layout')
    lib.write_bytes(data)
    obj=destination/'d3d9-capture.obj'
    subprocess.run(['clang','-target','i686-pc-windows-msvc','-O1','-ffreestanding','-fno-stack-protector','-c',str(ROOT/'tools/pc-runtime/d3d9_capture.c'),'-o',str(obj)],check=True)
    exports=destination/'d3d9.def';exports.write_text('LIBRARY d3d9.dll\nEXPORTS\nDirect3DCreate9=_Direct3DCreate9@4\n')
    subprocess.run([str(lld),'-flavor','link','/dll','/noentry','/nodefaultlib','/machine:x86','/timestamp:0','/def:'+str(exports),'/out:'+str(copy/'d3d9.dll'),str(obj),str(lib)],check=True)
    shutil.copyfile(wine/'lib/wine/i386-windows/d3d9.dll',copy/'pc-real-d3d9.dll')
    print(copy/'d3d9.dll')
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wine',type=Path,default=ROOT/'out/toolchain/wine/Wine Devel.app/Contents/Resources/wine')
    parser.add_argument('--lld',type=Path,default=ROOT/'out/toolchain/android-sdk/build-tools/35.0.0/lld')
    args=parser.parse_args();build(args.wine,args.lld)
