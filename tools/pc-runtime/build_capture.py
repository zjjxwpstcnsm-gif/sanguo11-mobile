#!/usr/bin/env python3
"""Build a tiny Win64 capture helper with local clang/Android SDK LLD.

No downloaded headers/libraries or CRT; import libraries are generated from
the documented Windows API declarations in capture_window.c.
"""
import argparse
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
IMPORTS = {
    'kernel32': ['CreateFileA','WriteFile','CloseHandle','ExitProcess','WideCharToMultiByte','GetLastError'],
    'user32': ['EnumWindows','IsWindowVisible','GetWindowTextW','GetClientRect','GetDC','ReleaseDC','PrintWindow'],
    'gdi32': ['CreateCompatibleDC','CreateDIBSection','SelectObject','BitBlt','DeleteDC','DeleteObject'],
}
def build(lld):
    destination = ROOT / 'out/pc-visual/pc-capture-build'
    destination.mkdir(parents=True, exist_ok=True)
    libraries = []
    for name, symbols in IMPORTS.items():
        definition=destination / (name+'.def')
        definition.write_text('LIBRARY '+name+'.dll\nEXPORTS\n'+'\n'.join(symbols)+'\n')
        library=destination / (name+'.lib')
        subprocess.run([str(lld),'-flavor','link','/dll','/noentry','/machine:x64','/def:'+str(definition),
            '/out:'+str(destination / (name+'.dll')),'/implib:'+str(library)],check=True)
        libraries.append(str(library))
    obj=destination/'capture.obj'
    subprocess.run(['clang','-target','x86_64-pc-windows-msvc','-O1','-ffreestanding','-fno-stack-protector',
        '-c',str(ROOT/'tools/pc-runtime/capture_window.c'),'-o',str(obj)],check=True)
    output=ROOT/'out/pc-visual/pc-runtime-copy/pc-capture.exe'
    subprocess.run([str(lld),'-flavor','link','/machine:x64','/entry:entry','/subsystem:console','/nodefaultlib',
        '/timestamp:0','/out:'+str(output),str(obj),*libraries],check=True)
    print(output)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--lld',type=Path,default=ROOT/'out/toolchain/android-sdk/build-tools/35.0.0/lld')
    build(parser.parse_args().lld)
