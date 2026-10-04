#!/usr/bin/env python3
"""Read named person properties with original constructors, serializers and getters.

This is the native load boundary, not a completed scenario start. Only fields
with consumed source bytes are classified as serialized source values. Current
ability caches, XP, age and startup-only flags must not be inferred from zeros.
"""
import argparse
import collections
import gzip
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_READ, UC_HOOK_MEM_WRITE, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from inspect_pc_scenario_units import NativeScenarioUnitDecoder
from inspect_pc_scenario_officers import NativeOfficerDecoder,ROOT,BASE,STRIDE,sha
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes

# Numeric UI properties0..140 also contain runtime-derived selectors. Keep the
# source-backed subset explicit; remaining properties are audited separately.
FIELDS=list(range(3,53))+[58,59,60,61,62,88,89]+list(range(106,122))+list(range(128,136))
EXCLUDED_SOURCE={5,30,31,32,33,34,52,58,59,60,61,62,88,89,128,129,130,131,132,133,134,135}
TEXT_FIELDS={'surname':(0x48e630,4,5),'given_name':(0x48e680,9,5),'courtesy_name':(0x48e6d0,14,5),'full_name':(0x4905b0,None,None)}


def source_value_offsets(field,offsets):
    return offsets if field==20 else [i for i in offsets if i!=100]


def source_byte_map(exe,raw):
    decoder=NativeOfficerDecoder(exe);_,before=decoder.decode(raw)
    result=collections.defaultdict(set)
    for index in range(152):
        changed=bytearray(raw);changed[index]^=1;_,after=decoder.decode(bytes(changed))
        for offset,(a,b) in enumerate(zip(before,after)):
            if a!=b:result[offset].add(index)
    return {offset:sorted(indices) for offset,indices in result.items()}


class NativeFields(NativeScenarioUnitDecoder):
    def __init__(self,exe):
        super().__init__(exe)
        self.exe=exe;self.actor=0;self.reads=set();self.writes=[];self.collect=False;self.selector=False
        self.buffer=self.stream+0x2000;self.text=self.stream+0x1000
        self.u.mem_map(self.text,0x4000)
        self.u.hook_add(UC_HOOK_MEM_READ,self.read_actor)
        self.u.hook_add(UC_HOOK_MEM_WRITE,self.write_world,begin=0x7200000,end=0x74fffff)
        self.u.hook_add(UC_HOOK_CODE,self.bio_boundary,begin=0x48e8fc,end=0x48e8fc)
        self.call(0x73ca80)
        self.descriptors=[]
        for index in range(141):
            ptr,kind,sub,flag=struct.unpack('<4I',self.u.mem_read(0x8ab758+index*16,16))
            name=bytes(self.u.mem_read(ptr,128)).split(b'\0',1)[0].decode('big5')
            self.descriptors.append(dict(id=index,name=name,kind=kind,subtype=sub,flags=flag,labelPointer=hex(ptr)))

    def call(self,function,*args,receiver=None):
        u=self.u;u.reg_write(UC_X86_REG_ESP,self.stack)
        u.mem_write(self.stack,struct.pack('<%dI'%(len(args)+1),self.stop,*[a&0xffffffff for a in args]))
        if receiver is not None:u.reg_write(UC_X86_REG_ECX,receiver)
        u.emu_start(function,self.stop,count=300000)
        if u.reg_read(UC_X86_REG_EIP)!=self.stop:raise ValueError('Original getter did not return: '+hex(function))
        value=u.reg_read(UC_X86_REG_EAX);return value if value<0x80000000 else value-0x100000000

    def read_actor(self,u,access,address,size,value,user):
        if self.collect and self.actor<=address<self.actor+0x190:
            self.reads.update(range(address-self.actor,min(address-self.actor+size,0x190)))

    def write_world(self,u,access,address,size,value,user):
        if self.collect:self.writes.append(dict(address=hex(address),bytes=size,value=value))

    def bio_boundary(self,u,address,size,user):
        if self.selector:u.emu_stop()

    def text_field(self,function):
        self.call(0x4719b0,self.buffer,4095,receiver=self.text)
        self.call(function,self.text,self.actor)
        ptr,capacity,length=struct.unpack('<3I',self.u.mem_read(self.text,12))
        if ptr!=self.buffer or capacity!=4095 or length>4095:raise ValueError('Original text getter escaped output')
        raw=bytes(self.u.mem_read(ptr,length))
        if bytes(self.u.mem_read(ptr+length,1))!=b'\0':raise ValueError('Original text getter omitted terminator')
        try:decoded=raw.decode('big5');error=None
        except UnicodeDecodeError:decoded=None;error='game gaiji mapping required; no replacement characters'
        return dict(rawHex=raw.hex(),text=decoded,unknown=error,getter=hex(function))

    def biography_selector(self):
        self.call(0x4719b0,self.buffer,4095,receiver=self.text)
        u=self.u;u.reg_write(UC_X86_REG_ESP,self.stack);u.mem_write(self.stack,struct.pack('<III',self.stop,self.text,self.actor))
        self.selector=True
        try:u.emu_start(0x48e850,self.stop,count=300000)
        finally:self.selector=False
        if u.reg_read(UC_X86_REG_EIP)==self.stop:
            return dict(unknown='original validity check rejected this load-boundary actor',originalFunction='48e850')
        if u.reg_read(UC_X86_REG_EIP)!=0x48e8fc:raise ValueError('Expected original installed-slot biography dispatch')
        message_id=struct.unpack('<I',u.mem_read(u.reg_read(UC_X86_REG_ESP),4))[0]
        return dict(originalFunction='48e850',messageId=message_id,resourceNumber=message_id//5000,resourceIndex=message_id%5000,
                    boundary='48e8fc ->49cb60 original text interpreter',rendering='not executed; control codes and gaiji retained')

    def initialize_special_slot_flag(self,actor):
        # Execute the actual prefix, including the registry-index calculation.
        # The remainder refreshes runtime ability caches using world/date state
        # which is not initialized by this source-record audit.
        u=self.u;before=bytes(u.mem_read(actor,0x190))
        u.reg_write(UC_X86_REG_ECX,actor);u.reg_write(UC_X86_REG_ESP,self.stack)
        u.mem_write(self.stack,struct.pack('<I',self.stop))
        u.emu_start(0x48aa50,0x48aa7b,count=300000)
        if u.reg_read(UC_X86_REG_EIP)!=0x48aa7b:raise ValueError('Special-slot initialization boundary changed')
        after=bytes(u.mem_read(actor,0x190))
        if before[:0x17c]!=after[:0x17c] or before[0x180:]!=after[0x180:]:raise ValueError('Special-slot prefix changed other actor fields')
        return struct.unpack('<I',after[0x17c:0x180])[0]


def inspect(installation,output,only_source=None):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Executable provenance changed')
    prior_packed=(ROOT/'docs/pc-data/scenario-officers-native.json.gz').read_bytes();prior=json.loads(gzip.decompress(prior_packed))
    if prior['source_executable_sha256']!=EXE_SHA:raise ValueError('Prior executable mismatch')
    mappings={(r['source'],r['native_index']):r for r in prior['mappings']}
    records={(r['source'],r['native_index']):r for r in prior['records']}
    map_bytes=source_byte_map(exe,bytes.fromhex(prior['records'][0]['raw_hex']))
    decoder=NativeFields(exe);sources=[];counts=collections.Counter();property_maps={}
    for source in prior['sources']:
        if only_source and source['path']!=only_source:continue
        raw=(installation/source['path']).read_bytes()
        if sha(raw)!=source['sha256']:raise ValueError('Source changed: '+source['path'])
        decoder.decode_units(raw);u=decoder.u
        slot_flags=[decoder.initialize_special_slot_flag(decoder.root+0xc0bc+i*0x190) for i in range(1100)]
        before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
        actors=[]
        for native in range(850):
            key=(source['path'],native);record=records[key];actor=decoder.root+0xc0bc+native*0x190
            if sha(raw[record['offset']:record['offset']+152])!=record['sha256']:raise ValueError('Record changed')
            decoder.actor=actor;decoder.collect=True;values={}
            try:
                valid=bool(decoder.call(0x47a600,actor))
                active=bool(decoder.call(0x47a630,actor))
                names={name:decoder.text_field(spec[0]) for name,spec in TEXT_FIELDS.items()}
                serialized_name=''.join(record['decoded']['name_bytes'])
                if valid and names['full_name']['rawHex']!=serialized_name:raise ValueError('Original full-name getter differs from identity audit at '+str(key))
                biography=decoder.biography_selector()
                for field in FIELDS:
                    decoder.reads=set();decoder.writes=[]
                    if field==5:
                        values[str(field)]=dict(unknown='effective portrait registry not loaded; serialized base face remains in prior audit');continue
                    try:value=decoder.call(0x4c8720,actor,field)
                    except Exception as error:
                        values[str(field)]=dict(unknown=str(error),stop=hex(u.reg_read(UC_X86_REG_EIP)));counts['getter_unknown']+=1;continue
                    if decoder.writes:raise ValueError('Read getter mutated native world: '+str(decoder.writes))
                    offsets=sorted({i for off in decoder.reads for i in map_bytes.get(off,[])})
                    # Every property first reads status byte100 for validity.
                    # That gate alone does not make its returned value source-
                    # backed (e.g. property40 health has no serialized value).
                    value_offsets=source_value_offsets(field,offsets)
                    backed=valid and bool(value_offsets) and field not in EXCLUDED_SOURCE
                    values[str(field)]=dict(value=value,actorReadOffsets=sorted(decoder.reads),recordSourceOffsets=offsets,
                        recordValueSourceOffsets=value_offsets,recordValiditySourceOffsets=[100] if 100 in offsets else [],
                        status='serialized_source_getter' if backed else 'load_boundary_only_not_startup_truth')
                    counts['source_backed' if backed else 'load_boundary_only']+=1
                    property_maps.setdefault(str(field),set()).update(offsets)
                if decoder.writes:raise ValueError('Text/selector getter mutated native world')
            finally:decoder.collect=False
            if rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Getter consumed RNG')
            actors.append(dict(nativeId=native,officerId=mappings[key]['project_id'],identity=mappings[key]['identity'],
                recordOffset=record['offset'],recordSha256=record['sha256'],names=names,serializedNameRawHex=serialized_name,
                loadBoundaryValid=valid,loadBoundaryActive=active,specialSlotFlag=slot_flags[native],biographySelector=biography,properties=values,
                coverage=dict(complete=False,sourcePropertyIds=[int(k) for k,v in values.items() if v.get('status')=='serialized_source_getter'],
                    loadBoundaryOnlyPropertyIds=[int(k) for k,v in values.items() if v.get('status')=='load_boundary_only_not_startup_truth'],
                    getterUnknownPropertyIds=[int(k) for k,v in values.items() if 'unknown' in v],
                    notYetInvokedPropertyIds=[i for i in range(141) if i not in FIELDS and i!=1],
                    sourceTextFields=[k for k,v in names.items() if valid and v['unknown'] is None],
                    unknown=['complete_startup_context','active_resource_priority','decoded_gaiji','native_reference_identity_joins','skill_effect_binding'])))
        if before!=bytes(u.mem_read(0x7200000,0x300000)):raise ValueError('Named getter block changed native world')
        sources.append(dict(path=source['path'],sha256=source['sha256'],officers=actors,nativeWorldReadOnly=True,rngUnchanged=True))
        print(json.dumps(dict(source=source['path'],officers=len(actors),doneSources=len(sources))),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,identityAuditSha256=sha(prior_packed),descriptors=decoder.descriptors,
        sourceByteMap={str(k):v for k,v in map_bytes.items()},propertySourceOffsets={k:sorted(v) for k,v in property_maps.items()},
        counts=dict(counts),sources=sources,specialSlotInitialization=dict(function='48aa50',stopBefore='48aa7b',completePostload=False),limits=['Native load boundary before actual scenario-start events and MOD overrides',
            'Runtime-only zero fields are not imported as startup XP/current state',
            'Biographies have proven original message selectors; rendered text/gaiji and active resource priority still pending',
            'No PC writes, no Wine, no runtime resource or old save changed'])
    output.parent.mkdir(parents=True,exist_ok=True);payload=json_bytes(report);output.write_bytes(gzip.compress(payload,mtime=0))
    summary={k:v for k,v in report.items() if k not in ('sources','sourceByteMap')};summary.update(sources=[dict(path=s['path'],sha256=s['sha256'],officers=len(s['officers'])) for s in sources],packedSha256=sha(output.read_bytes()),decodedSha256=sha(payload))
    output.with_suffix('.summary.json').write_bytes(json_bytes(summary));return report


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    parser.add_argument('--output',type=Path,required=True);parser.add_argument('--source')
    args=parser.parse_args();inspect(args.installation,args.output,args.source)
