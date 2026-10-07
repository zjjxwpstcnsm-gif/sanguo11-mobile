#!/usr/bin/env python3
"""Read original image-bank arguments and branches; preserve unknown UI roles."""
import argparse
import hashlib
import json
import pathlib
import struct
from capstone import Cs, CS_ARCH_X86, CS_MODE_32, __version__

ROOT=pathlib.Path(__file__).resolve().parents[4]


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--executable',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args()
    assert a.output.parent.resolve()==pathlib.Path(__file__).resolve().parent and not a.output.exists()
    raw=a.executable.read_bytes();digest=hashlib.sha256(raw).hexdigest()
    assert digest=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
    pe=struct.unpack_from('<I',raw,0x3c)[0]
    assert raw[pe:pe+4]==b'PE\0\0' and struct.unpack_from('<I',raw,pe+52)[0]==0x400000
    count=struct.unpack_from('<H',raw,pe+6)[0];optional=struct.unpack_from('<H',raw,pe+20)[0]
    sections=[]
    for index in range(count):
        at=pe+24+optional+40*index
        size,va,raw_size,offset=struct.unpack_from('<4I',raw,at+8)
        sections.append(dict(name=raw[at:at+8].split(b'\0')[0].decode(),rva=va,rawOffset=offset,rawSize=raw_size,virtualSize=size))
    def body(address,length):
        rva=address-0x400000
        section=next(row for row in sections if row['rva']<=rva and rva+length<=row['rva']+row['rawSize'])
        offset=section['rawOffset']+rva-section['rva']
        assert offset==rva
        return raw[offset:offset+length]
    decoder=Cs(CS_ARCH_X86,CS_MODE_32)
    windows=[]
    for address,length in [(0x430aa0,191),(0x430b70,288),(0x480350,44),(0x4802d0,70),(0x436960,47)]:
        code=body(address,length)
        windows.append(dict(va=hex(address),rawHex=code.hex(),sha256=hashlib.sha256(code).hexdigest(),
            instructions=[dict(va=hex(i.address),op=i.mnemonic,args=i.op_str,bytes=i.bytes.hex()) for i in decoder.disasm(code,address)]))
    templates=body(0x77b4b8,160)
    banks=[struct.unpack_from('<8I',templates,32*i) for i in range(5)]
    assert banks[0]==(2,2400,4800,1048576,512,512,64,80)
    assert banks[1]==(2,0,2400,1048576,512,512,240,240)
    assert any(i['va']=='0x430bb1' and i['op']=='lea' and i['args']=='ebx, [edi + ebx*2]' for i in windows[1]['instructions'])
    assert any(i['va']=='0x4802fe' and i['op']=='movsx' for i in windows[3]['instructions'])
    assert any(i['va']=='0x43696e' and i['args']=='dword ptr [esi + 0x24], 6' for i in windows[4]['instructions'])
    calls=[]
    offset=0x1000
    text_end=0x1000+next(s['rawSize'] for s in sections if s['name']=='.text')
    while True:
        offset=raw.find(b'\xe8',offset,text_end)
        if offset<0 or offset+5>text_end:break
        if (0x400000+offset+5+struct.unpack_from('<i',raw,offset+1)[0])&0xffffffff==0x430b70:
            code=raw[offset:offset+64]
            calls.append(dict(va=hex(0x400000+offset),rawForward64Hex=code.hex(),rawSha256=hashlib.sha256(code).hexdigest(),
                              executableReachabilityAccepted=False,originalUiRole=None))
        offset+=1
    manifest_path=ROOT/'app/src/main/assets/portraits/pc/media-manifest.json'
    manifest_raw=manifest_path.read_bytes();manifest=json.loads(manifest_raw)
    images={(row['faceId'],row['imageGroup']):row for row in manifest['images']}
    pairs=[face for face,group in images if group==1 and (face,2) in images]
    different=[face for face in pairs if images[(face,1)]['rgbaSha256']!=images[(face,2)]['rgbaSha256']]
    assert len(pairs)==964 and len(different)==680
    assert hashlib.sha256(a.executable.read_bytes()).hexdigest()==digest
    assert manifest_path.read_bytes()==manifest_raw
    report=dict(sourceExecutable=str(a.executable.resolve()),sourceExecutableSha256=digest,
        sourceReadOnly=True,wineUsed=False,capstoneVersion=__version__,peSections=sections,
        windows=windows,templateVa='0x77b4b8',templateRawHex=templates.hex(),templateWords=banks,
        staticBranchInterpretation=[
            '430b70 receives output-rect pointer, bank family, source index, variant. Family0 normalizes to1. Family1 maps index to2*sourceIndex+variant; variants2/5 call480350 and normalize its boolean complement.',
            'Family2 with variant3 calls4802d0: signed descriptor bytes0/1, guarded flag bit1 and face bounds, then adds160/180 before430940. Rect/crop semantic role and runtime reachability are not proved.',
            'Initialization templates bank1 base2400/count4800/cell64x80 and bank2 base0/count2400/cell240x240 match independently decoded FCE small-pair/large registry layout. Field0 value2 is preserved, not assigned an unproved semantic name.',
            '436960 generic widget constructor stores bank-family6. It therefore cannot establish that every ordinary UI portrait defaults to bank1. Per-entry callers remain required.'
        ],drawCallByteCandidates=calls,sourceImageManifestSha256=hashlib.sha256(manifest_raw).hexdigest(),
        sourceSmallPairs=964,smallPairRgbaDifferentCount=680,smallPairDifferentFaces=different,
        ordinarySmallUiCallerBindingAccepted=False,androidCropOrFullscreenTimingAccepted=False,
        wholeGoalComplete=False)
    a.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(bankTemplates=5,drawCallByteCandidates=len(calls),sourceSmallPairs=964,
                          differingSmallPairs=680,ordinaryUiRoleAccepted=False)))


if __name__=='__main__':main()
