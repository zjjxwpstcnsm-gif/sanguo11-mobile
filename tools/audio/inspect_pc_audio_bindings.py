#!/usr/bin/env python3
"""Execute original sound-ID/profile lookups and capture HUD33 dispatch offline.

Only platform audio availability and critical-section imports are shimmed.
No original game command, rule domain or RNG is entered. No Wine or install writes.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from pc_resources import Archive
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP

EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def inspect(installation, output):
    raw = (installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest() != EXE_SHA:
        raise ValueError('Reinspect changed source executable')
    if output.exists() or installation.resolve() in output.resolve().parents:
        raise ValueError('Fresh output outside read-only source required')
    pe = struct.unpack_from('<I', raw, 0x3c)[0]
    section_count, optional = struct.unpack_from('<H', raw, pe+6)[0], struct.unpack_from('<H', raw, pe+20)[0]
    base = struct.unpack_from('<I', raw, pe+52)[0]
    sections = []
    for index in range(section_count):
        at = pe+24+optional+40*index
        size, va, length, offset = struct.unpack_from('<IIII', raw, at+8)
        sections.append((base+va, length, offset))
    def read(va, size):
        for address, length, offset in sections:
            if address <= va and va+size <= address+length:
                data = raw[offset+va-address:offset+va-address+size]
                if len(data) == size:return data
        raise ValueError('Unexamined original PE address')
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, 0x4a0000)
    u.mem_write(0x400000, raw[:0x4a0000])
    u.mem_map(0x8b1000, 0x3000);u.mem_write(0x8b1000, read(0x8b1000, 0x3000))
    manager, stack, stop, shims = 0x10000000, 0x20000000, 0x30000000, 0x30001000
    u.mem_map(manager, 0x4000);u.mem_map(stack, 0x2000);u.mem_map(stop, 0x2000)
    def call(address, args=(), this=manager):
        sp = stack+4096
        u.mem_write(sp, struct.pack('<'+'I'*(len(args)+1), stop, *[v & 0xffffffff for v in args]))
        u.reg_write(UC_X86_REG_ESP, sp);u.reg_write(UC_X86_REG_ECX, this)
        u.emu_start(address, stop, count=20000)
        if u.reg_read(UC_X86_REG_EIP) != stop:raise ValueError('Unexamined native call path')
        return u.reg_read(UC_X86_REG_EAX)
    effect_rows = []
    table = read(0x8b1468, 200*12)
    for event in range(200):
        bank, slot, debounce = struct.unpack_from('<3I', table, event*12)
        handle = call(0x4cff90, (event,))
        expected = struct.unpack('<I', read(0x8b1dc8+bank*8, 4))[0] if bank < 10 else 0xffffffff
        if handle != expected:raise ValueError('Original effect bank lookup differs')
        effect_rows.append(dict(soundId=event, bank=bank, slot=slot, debounceRaw=debounce, nativeBankHandle=handle,
                                status='ORIGINAL_TABLE_AND_BANK_LOOKUP_VERIFIED_EVENT_ROLE_PENDING'))
    for bad in [-1, 200, 2147483647]:
        if call(0x4cff90, (bad,)) != 0xffffffff:raise ValueError('Invalid native effect boundary')
    profiles = []
    for profile in range(71):
        base_voice = struct.unpack('<i', read(0x7f1420+profile*4, 4))[0]
        for variant in range(14):
            voice = call(0x4cffd0, (profile, variant))
            if voice != base_voice+variant:raise ValueError('Original voice profile lookup differs')
            profiles.append(dict(profile=profile, variant=variant, voiceId=voice, primaryResource=2287+voice,
                                 alternateResource=2287+voice+(996 if voice >= 5 else 0), officerId=None,
                                 status='PROFILE_INDEX_VERIFIED_ACTOR_ROLE_UNBOUND'))
    for args in [(-1,0),(71,0),(0,-1),(0,14)]:
        if call(0x4cffd0, args) != 0xffffffff:raise ValueError('Invalid native voice boundary')
    captures = []
    def platform(machine, address, size, user):
        sp = machine.reg_read(UC_X86_REG_ESP);ret = struct.unpack('<I', machine.mem_read(sp,4))[0]
        if address == 0x6e98b0:
            descriptor = struct.unpack('<I', machine.mem_read(sp+4,4))[0]
            captures.append(bytes(machine.mem_read(descriptor,16)).hex())
            machine.reg_write(UC_X86_REG_EAX, 0)
            consumed = 8
        elif address == 0x6e96a0:
            machine.reg_write(UC_X86_REG_EAX, 1);consumed = 0
        else:consumed = 4
        machine.reg_write(UC_X86_REG_ESP, sp+4+consumed);machine.reg_write(UC_X86_REG_EIP, ret)
    for address in [0x6e98b0,0x6e96a0,shims,shims+16]:
        u.hook_add(UC_HOOK_CODE, platform, begin=address, end=address)
    u.mem_write(0x74e360, struct.pack('<I', shims));u.mem_write(0x74e35c, struct.pack('<I', shims+16))
    u.mem_write(manager+0x34, struct.pack('<I', manager+0x1000))
    u.mem_write(manager+0x70, struct.pack('<I', manager+0x2000))
    if call(0x4d0570, (33,0,0xbf800000,0,0)) != 1 or len(captures) != 1:
        raise ValueError('HUD33 dispatch not observed')
    packet = bytes.fromhex(captures[0])
    if packet[0] != 1 or packet[2] != 19:raise ValueError('HUD33 original bank/slot differs')
    header_resources = list(struct.unpack('<10I', read(0x7f130c,40)))
    wave_resources = list(struct.unpack('<10I', read(0x7f1334,40)))
    requested_resources=[]
    def resource_io(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
        if address==0x46dae0:
            requested_resources.append(struct.unpack('<I',machine.mem_read(sp+8,4))[0])
            machine.mem_write(manager+0x3400,struct.pack('<I',manager+0x3500))
            machine.reg_write(UC_X86_REG_EAX,manager+0x3400);consumed=8
        else:machine.reg_write(UC_X86_REG_EAX,0);consumed=12
        machine.reg_write(UC_X86_REG_ESP,sp+4+consumed);machine.reg_write(UC_X86_REG_EIP,ret)
    for address in [0x46dae0,0x6e9240]:u.hook_add(UC_HOOK_CODE,resource_io,begin=address,end=address)
    music_rows=[]
    u.mem_write(manager+0x20,struct.pack('<I',manager+0x1000))
    for music_id in range(30):
        requested_resources.clear()
        if call(0x4cf9b0,(music_id,1,0x3f800000,0))!=1 or requested_resources!=[2237+music_id]:raise ValueError('Original BGM resource IO index')
        music_rows.append(dict(musicId=music_id,resourceId=requested_resources[0],status='ORIGINAL_MUSIC_RESOURCE_INDEX_VERIFIED_SCENE_UNBOUND'))
    voice_io_checks=0
    u.mem_write(manager+4,struct.pack('<I',manager+0x1000))
    for alternate in (False,True):
        u.mem_write(manager+0x30,struct.pack('<I',int(alternate)))
        for voice_id in range(1001):
            requested_resources.clear()
            expected=2287+voice_id+(996 if alternate and voice_id>=5 else 0)
            if call(0x4d2380,(0,voice_id,0x3f800000,0))!=1 or requested_resources!=[expected]:raise ValueError('Original voice resource IO index')
            voice_io_checks+=1
    actor_voice_types=[]
    for kind in range(8):
        destination=manager+0x3200;u.mem_write(destination,bytes(12))
        call(0x4d0010,(destination,kind))
        fields=struct.unpack('<3i',u.mem_read(destination,12))
        actor_voice_types.append(dict(actorVoiceTypeRaw=kind,selectorFields=list(fields),status='ORIGINAL_TYPE_SELECTOR_EXECUTED_ROLE_LABEL_UNKNOWN'))
    pcm_formats=[]
    archive=Archive(installation/'Media/san11pkres.bin')
    try:
        for bank,resource in enumerate(header_resources):
            header=archive.read(resource);wave=archive.read(wave_resources[bank]);count=struct.unpack_from('<H',header,2)[0]
            for slot in range(count):
                offset=struct.unpack_from('<I',header,8+slot*4)[0]
                if not offset:continue
                descriptor=header[offset:offset+40]
                source_descriptor,format_out=manager+0x3000,manager+0x3100
                u.mem_write(source_descriptor,descriptor);u.mem_write(format_out,bytes(32))
                if call(0x6f2c30,(format_out,),source_descriptor+4)&255!=1:raise ValueError('Original wave format reader')
                tag,channels,rate,average,alignment,bits,extra=struct.unpack('<HHIIHHH',u.mem_read(format_out,18))
                packed=struct.unpack_from('<I',descriptor,4)[0]
                if (tag,channels,rate,alignment,bits,extra)!=(1,(packed>>27)&7,packed&0x7ffffff,struct.unpack_from('<H',descriptor,8)[0],16,0):
                    raise ValueError('Native PCM format mismatch')
                samples,start,length=struct.unpack_from('<III',descriptor,16)
                if average!=rate*alignment or length!=samples*alignment or start+length>len(wave):raise ValueError('Native PCM extent')
                pcm_formats.append(dict(bank=bank,slot=slot,descriptorSha256=hashlib.sha256(descriptor).hexdigest(),
                    sampleRate=rate,channels=channels,blockAlign=alignment,bitsPerSample=bits,averageBytesPerSecond=average,
                    rawPcmSha256=hashlib.sha256(wave[start:start+length]).hexdigest(),nativeFormatHex=bytes(u.mem_read(format_out,18)).hex(),
                    loopStartRaw=struct.unpack_from('<I',descriptor,28)[0],loopFlagRaw=descriptor[32]&1,
                    status='ORIGINAL_PCM_WAVEFORMAT_READER_VERIFIED'))
    finally:archive.close()
    report = dict(schema=1, sourceExecutableSha256=EXE_SHA, sourcePolicy='READ_ONLY_NO_WINE',
                  effectTable=dict(address='8b1468', stride=12, sha256=hashlib.sha256(table).hexdigest()),
                  soundBankHeaderResources=header_resources, soundBankWaveResources=wave_resources,
                  effectLookups=effect_rows, voiceProfiles=profiles,
                  nativeChecks=200+3+71*14+4+1+len(pcm_formats)+30+voice_io_checks+8,pcmFormats=pcm_formats,
                  musicResources=music_rows,voiceResourceIoChecks=voice_io_checks,actorVoiceTypes=actor_voice_types,
                  tacticVoiceProfileTable=list(struct.unpack('<13i',read(0x838c94,52))),
                  hud33=dict(call='6318b2 -> 4d0570 -> 6e98b0', bank=1, slot=19,
                             headerResource=header_resources[1], waveResource=wave_resources[1], dispatchHex=captures[0],
                             status='ORIGINAL_SAMPLE_SELECTOR_AND_NATIVE_PCM_FORMAT_VERIFIED_PLAYBACK_PENDING'),
                  codeSha256={hex(a):hashlib.sha256(read(a,b-a)).hexdigest() for a,b in [(0x4cff90,0x4cffc1),(0x4cffd0,0x4d000c),(0x4d0570,0x4d0697),(0x4d2380,0x4d2441),(0x6f2c30,0x6f2ca2)]},
                  limits=['Platform availability=1 and critical section imports shimmed, backend captured rather than played.',
                          'No officer identity/profile inference, no BGM event identity or all-event trigger acceptance.',
                          'Music/voice resource IO index executed unchanged with an explicit archive-return/codec boundary shim; no decode/playback in that native harness.',
                          'No original rule command, RNG, save or Wine access.'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(nativeChecks=report['nativeChecks'],hud33=report['hud33'])))


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
