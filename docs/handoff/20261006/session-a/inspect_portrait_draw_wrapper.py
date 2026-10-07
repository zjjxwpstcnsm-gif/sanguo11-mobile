#!/usr/bin/env python3
"""Readonly exact original wrapper bytes; no UI role or FCE group inference."""
import argparse
import hashlib
import json
import pathlib
import struct
from capstone import Cs, CS_ARCH_X86, CS_MODE_32, __version__


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--executable', type=pathlib.Path, required=True)
    p.add_argument('--output', type=pathlib.Path, required=True)
    args = p.parse_args()
    assert not args.output.exists()
    assert args.output.resolve().is_relative_to(pathlib.Path(__file__).resolve().parents[4])
    raw = args.executable.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    assert digest == '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
    decoder = Cs(CS_ARCH_X86, CS_MODE_32)
    windows = []
    for address, length in [(0x584e80, 106), (0x589780, 32), (0x443790, 6), (0x588de0, 320)]:
        body = raw[address-0x400000:address-0x400000+length]
        instructions = [dict(va=hex(i.address), bytes=i.bytes.hex(), op=i.mnemonic, args=i.op_str)
                        for i in decoder.disasm(body, address)]
        windows.append(dict(startVa=hex(address), bytes=len(body), rawHex=body.hex(),
                            sha256=hashlib.sha256(body).hexdigest(), instructions=instructions))
    assert raw[0x443790-0x400000:0x443796-0x400000] == bytes.fromhex('b830367900c3')
    matrix = raw[0x793630-0x400000:0x793630-0x400000+64]
    values = list(struct.unpack('<16f', matrix))
    assert values == [float(i//4 == i%4) for i in range(16)]
    assert any(row['va']=='0x588e0b' and row['op']=='rep movsd' for row in windows[-1]['instructions'])
    assert hashlib.sha256(args.executable.read_bytes()).hexdigest() == digest
    report = dict(sourceExecutable=str(args.executable.resolve()), sourceExecutableSha256=digest,
                  capstoneVersion=__version__, sourceReadOnly=True, wineUsed=False, windows=windows,
                  descriptorVa='0x793630', descriptorRawHex=matrix.hex(), descriptorFloats=values,
                  observedIdentity4x4Matrix=True,
                  staticStackInterpretation='589780 receives two stack arguments. It keeps argument2 and literal1 on stack, obtains identity-matrix pointer from443790, then passes argument1/matrix/literal1/argument2 to588de0. 588de0 copies16 dwords from its matrix argument; direct branch stores packet pointer and0x12 then calls vtable+0x10. Both consumer branches retained in raw320-byte window.',
                  unknowns=['Live caller/object/vtable reachability and semantic type0x12','FCE image-group binding: literal1 is not proven a portrait group','Ordinary versus fullscreen UI role, output rect/crop/color/lifecycle','Native dynamic-slot+0x83 route does not itself establish actual Android caller parity'],
                  ordinarySmallFormCallerBindingAccepted=False,
                  fullScreenCallerCropTimingAccepted=False, wholeGoalComplete=False)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(dict(readOnlySha=digest, decodedWindows=len(windows), identityMatrixObserved=True,
                          ordinaryOrFullScreenUiBindingAccepted=False)))


if __name__ == '__main__':
    main()
