#!/usr/bin/env python3
"""Readonly supplied original EXE call candidates; no inferred UI form binding."""
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
    if args.output.exists() or args.executable.resolve() in args.output.resolve().parents:
        raise ValueError('Fresh output outside original source required')
    raw = args.executable.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb':
        raise ValueError('Supplied original executable SHA differs')
    # The existing executed portrait/source probes map this original prefix
    # directly at0x400000. Byte candidates still do not prove executable reach.
    code = raw[:0x500000]
    targets = {0x48a450: 'normal face getter', 0x48a5b0: 'dynamic slot getter'}
    md = Cs(CS_ARCH_X86, CS_MODE_32)
    candidates = []
    offset = 0
    while True:
        offset = code.find(b'\xe8', offset)
        if offset < 0 or offset+5 > len(code):
            break
        destination = (0x400000+offset+5+struct.unpack_from('<i', code, offset+1)[0]) & 0xffffffff
        if destination in targets:
            tail = code[offset:offset+112]
            instructions = [{'va': hex(i.address), 'bytes': i.bytes.hex(), 'op': i.mnemonic, 'args': i.op_str}
                            for i in md.disasm(tail, 0x400000+offset)]
            candidates.append({'callVa': hex(0x400000+offset), 'target': hex(destination),
                'targetEvidenceRole': targets[destination], 'rawForward112Sha256': hashlib.sha256(tail).hexdigest(),
                'rawBefore48Hex': code[max(0, offset-48):offset].hex(), 'forwardInstructions': instructions,
                'originalUiCallerRole': None, 'fceImageGroup': None,
                'scope': 'Byte displacement and forward decode only; code/data alignment, reachability, UI role, geometry/crop and runtime lifetime not established'})
        offset += 1
    if hashlib.sha256(args.executable.read_bytes()).hexdigest() != digest:
        raise ValueError('Original executable changed during readonly inspection')
    report = {'sourceExecutable': str(args.executable.resolve()), 'sourceExecutableSha256': digest,
              'capstoneVersion': __version__, 'sourceReadOnly': True, 'wineUsed': False,
              'candidateCount': len(candidates), 'candidates': candidates,
              'ordinarySmallFormCallerBindingAccepted': False,
              'fullScreenCallerCropTimingAccepted': False, 'wholeGoalComplete': False}
    args.output.write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps({'candidateCount': len(candidates), 'counts': {hex(k): sum(int(r['target'], 16) == k for r in candidates) for k in targets}}))


if __name__ == '__main__':
    main()
