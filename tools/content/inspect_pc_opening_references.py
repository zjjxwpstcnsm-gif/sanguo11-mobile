#!/usr/bin/env python3
"""Read source actor joins and site resources through actual native dispatch.

Runs a layered Shared/scenario read and the examined493400 postload only.
It does not emulate menu settings, event conditions/effects or external files.
The generic building getter selects city or gate/port layout itself;47b350
must NEVER be applied to a gate/port (the native inventory base differs).
"""
import argparse,gc,gzip,json,struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_READ,UC_HOOK_MEM_WRITE
from inspect_pc_layered_scenario import NativeLayeredWorld
from inspect_pc_scenario_fields import NativeScenarioFields
from pc_startup_platform import StartupPlatform
from pc_resources import Archive
from audit_pc_restoration_sources import ROOT,EXE_SHA,json_bytes,sha,output_guard

PERSON_FIELDS=[57,75,76,90,92]
SITE_FIELDS=[3,4,5,7,9,10,11,12,13,14,17,18,19,20,31,32,33,34,35]+list(range(45,57))
LIMITS=['Installed source candidate, not official/active-priority certification',
        'Direct493400 postload, not complete normal PC start',
        'Shared Chinese NLS sort/menu settings/opening events remain unknown',
        'Only supplied installation exists; external Documents/Expansion coverage unknown',
        'No PC installation writes or Wine; no Android import performed by this reader']

class OpeningReader(NativeLayeredWorld):
    def __init__(self,exe):
        super().__init__(exe);self.observing=False;self.read_set=set();self.write_set=[]
        self.observation_hooks=[]
    def observe(self):
        self.observation_hooks=[self.u.hook_add(UC_HOOK_MEM_READ,self.observe_read,begin=0x7200000,end=0x74fffff),self.u.hook_add(UC_HOOK_MEM_WRITE,self.observe_write,begin=0x7200000,end=0x74fffff)]
        # Previous native postload translated some getter blocks before tracing.
        # Recompile those blocks with the new read/write observers installed.
        self.u.ctl_flush_tb()
    def observe_read(self,u,access,address,size,value,user):
        if self.observing:self.read_set.update(range(address,address+size))
    def observe_write(self,u,access,address,size,value,user):
        if self.observing:self.write_set.append((address,size))
    def get(self,function,actor,field,addresses):
        self.read_set=set();self.write_set=[];self.observing=True
        try:
            value=self.call(function,actor,field,count=2000000)
            value=value if value<0x80000000 else value-0x100000000
        finally:self.observing=False
        if self.write_set:raise ValueError('Native query mutated world '+str(self.write_set))
        dependencies=[dict(address=hex(a),**addresses[a])for a in sorted(self.read_set)if a in addresses]
        return dict(value=value,getter=hex(function),serializedDependencies=dependencies,
                    scope='original_dispatch_after_direct_postload',completeOpening=False)

def inspect(installation,output,only_source=None):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('EXE changed')
    manifest_bytes=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    manifest=json.loads(manifest_bytes);shared=(installation/'Media/scenario/Scenario.s11').read_bytes()
    archive=Archive(installation/'Media/san11pkres.bin');shex=archive.read(4791);archive.close()
    if len(shex)!=440008 or shex[:8]!=b'SHEX0008' or sha(shex)!='c726989df91d44c99ea3c3f613d41e7e725c43002a9775dfac2e43516f5c6112':raise ValueError('Original SHEX geography changed')
    sources=[]
    for source in manifest['scenarios']:
        path=source['sourcePath']
        if only_source and path!=only_source:continue
        raw=(installation/path).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed '+path)
        world=OpeningReader(exe);platform=StartupPlatform(world,installation)
        shared_read=world.load(shared,True);scenario_read=world.load(raw)
        addresses=NativeScenarioFields.source_addresses(shared_read,'Media/scenario/Scenario.s11')
        addresses.update(NativeScenarioFields.source_addresses(scenario_read,path))
        world.call(0x73c840);world.call(0x73ca80)
        postload=world.call(0x493400,receiver=world.root,count=50000000)
        labels={}
        for kind,base,selected in [('person',0x8ab758,PERSON_FIELDS),('site',0x8ab048,SITE_FIELDS)]:
            labels[kind]=[]
            for field in selected:
                pointer=struct.unpack('<I',world.u.mem_read(base+field*16,4))[0]
                name=bytes(world.u.mem_read(pointer,128)).split(b'\0')[0].decode('big5')if pointer else None
                labels[kind].append(dict(id=field,name=name,originalLabelPointer=hex(pointer)))
        world.observe()
        before=bytes(world.u.mem_read(0x7200000,0x300000));rng=bytes(world.u.mem_read(0x8a5d44,4));people=[];sites=[]
        for native in range(850):
            actor=world.root+0xc0bc+native*0x190
            valid=bool(world.call(0x47a600,actor))
            people.append(dict(nativeId=native,valid=valid,properties={str(i):world.get(0x4c8720,actor,i,addresses)for i in PERSON_FIELDS}if valid else {},unknown=[]if valid else ['native-validity-gate']))
        for native in range(87):
            actor=world.root+0x89730+native*0x38
            if not world.call(0x47a630,actor):raise ValueError('Original site proxy is inactive '+str(native))
            props={str(i):world.get(0x4c69a0,actor,i,addresses)for i in SITE_FIELDS}
            text_object=world.meta+0x4100;buffer=world.meta+0x4300
            world.call(0x4719b0,buffer,511,receiver=text_object);world.call(0x4905b0,text_object,actor)
            pointer,capacity,length=struct.unpack('<3I',world.u.mem_read(text_object,12))
            if pointer!=buffer or capacity!=511 or length>511:raise ValueError('Original name escaped buffer')
            name_raw=bytes(world.u.mem_read(pointer,length));name=name_raw.decode('big5')
            x=props['9']['value'];y=props['10']['value']
            if not 0<=x<200 or not 0<=y<200:raise ValueError('Source coordinate outside SHEX')
            offset=8+(x*200+y)*11;region=shex[offset+1]
            if region>86:raise ValueError('Site has unexamined SHEX region')
            parent=world.call(0x4839f0,region)
            if not 0<=parent<42:raise ValueError('Native parent getter has no city')
            sites.append(dict(nativeBuildingId=native,name=name,nameRawHex=name_raw.hex(),nameGetter='4905b0',properties=props,
                parentNativeCityId=parent,parentGetter='4839f0',shexRegion=region,shexRegionSourceOffset=offset+1))
        if before!=bytes(world.u.mem_read(0x7200000,0x300000))or rng!=bytes(world.u.mem_read(0x8a5d44,4)):raise ValueError('Getters changed complete world/RNG')
        sources.append(dict(path=path,sha256=sha(raw),sharedSha256=sha(shared),sourceVariant=source['sourceVariant'],scenarioId=source['scenarioId'],people=people,sites=sites,postloadReturn=postload,postloadWorldSha256=sha(before),rngHex=rng.hex(),queriesPure=True,completeOpening=False))
        print(json.dumps(dict(source=path,people=len(people),sites=len(sites),postload=postload)),flush=True)
        del world,platform;gc.collect()
    if not sources:raise ValueError('No source matched')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_bytes),descriptors=labels,sources=sources,shexSha256=sha(shex),shexResourceId=4791,limits=LIMITS)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    output.with_suffix('.summary.json').write_bytes(json_bytes(dict(packedSha256=sha(output.read_bytes()),sourceCount=len(sources),sites=sum(len(s['sites'])for s in sources),personSlots=sum(len(s['people'])for s in sources),completeOpening=False,limits=LIMITS)))
    return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--source');p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.source)
