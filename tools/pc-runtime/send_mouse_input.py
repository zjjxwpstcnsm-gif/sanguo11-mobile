#!/usr/bin/env python3
"""Bounded original UI mouse input in the owned private Wine process.

Requires pc-mouse-input.dll. Alters only imported OS mouse polling results;
gameplay, camera and presentation must respond through the original UI loop.
Success means acknowledged input, never a claim that a menu action succeeded.
"""
import argparse,json,struct,time,os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
COPY=ROOT/'out/pc-visual/pc-runtime-copy'

def send(x,y,button,enabled):
    sequence=time.monotonic_ns()&0xffffffff
    temp=COPY/'pc-mouse-input.tmp';target=COPY/'pc-mouse-input.bin'
    temp.write_bytes(struct.pack('<7I',0x534d4350,sequence,x,y,button,enabled,0));temp.replace(target)
    end=time.monotonic()+5
    while time.monotonic()<end:
        try:
            data=(COPY/'pc-mouse-ack.bin').read_bytes()
            if len(data)==28:
                ack=struct.unpack('<7I',data)
                if ack[:2]==(0x534d4350,sequence):return dict(sequence=sequence,screen=list(ack[2:4]),enabled=ack[4],cursor_reads=ack[5],key_reads=ack[6])
        except FileNotFoundError:pass
        time.sleep(.02)
    raise TimeoutError('Private input adapter did not acknowledge within5s')

def click(x,y,messages=False):
    rows=[send(x,y,0,1)];time.sleep(.25)
    try:
        if messages:rows.append(send(x,y,1,1))
        # Deliver the original window's standard mouse messages as well as
        # polling state. Menus and the map use both input paths.
        (COPY/'pc-ui-command.txt').write_text(f'{"message" if messages else "click"} {x} {y}\n')
        env=dict(os.environ,WINEPREFIX=str(ROOT/'out/pc-visual/pc-runtime-prefix'),WINEDLLOVERRIDES='d3d9=b',WINEDEBUG='-all')
        subprocess.run([str(ROOT/'out/toolchain/wine/Wine Devel.app/Contents/Resources/wine/bin/wine'),'g:\\pc-ui-command.exe'],cwd=COPY,env=env,check=True,timeout=20)
    finally:
        rows.append(send(x,y,0,1))
    time.sleep(.25)
    return rows

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('action',choices=['click','message-click','move','disable']);p.add_argument('x',type=int);p.add_argument('y',type=int);args=p.parse_args()
    if not 0<=args.x<4096 or not 0<=args.y<4096:raise ValueError('Bounded nonnegative client position')
    records=click(args.x,args.y,args.action=='message-click') if args.action in ('click','message-click') else [send(args.x,args.y,0,int(args.action!='disable'))]
    print(json.dumps(dict(action=args.action,client=[args.x,args.y],acknowledgements=records,menu_action_accepted=False)))
