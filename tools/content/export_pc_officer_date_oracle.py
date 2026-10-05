#!/usr/bin/env python3
"""Original age source, ability-change selector and scenario-header evidence.

Read-only installation. Runs bounded original settings dispatch/startup/age and
current-ability paths; no Windows UI, Wine, guessed setting defaults or MOD state.
"""
import argparse,csv,gzip,io,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE,UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP
from inspect_pc_scenario_metadata import NativeMetadataDecoder
from inspect_pc_scenario_tail import NativeTailDecoder
from inspect_pc_scenario_officers import sha
from test_pc_city_action_costs import CityActionCostsTest

ROOT=Path(__file__).resolve().parents[2]

def export(installation,output,audit):
 installation=installation.resolve()
 for path in (output,audit):
  if path.resolve()==installation or installation in path.resolve().parents:raise ValueError('PC directory is read-only')
 exe=(installation/'san11pk.exe').read_bytes();shared=(installation/'Media/scenario/Scenario.s11').read_bytes()
 prior_raw=(ROOT/'docs/pc-data/scenario-metadata-native.json.gz').read_bytes();prior=json.loads(gzip.decompress(prior_raw))
 d=NativeMetadataDecoder(exe);u=d.u;sources=[]
 for source in prior['sources']:
  raw=(installation/source['path']).read_bytes()
  if sha(raw)!=source['sha256']:raise ValueError('Source changed')
  # Re-run only the actual global serializer, not the previously decoded buffer.
  u.mem_write(0x7200000,b'\xa5'*0x300000);d.call(0x4830b0,d.root)
  u.mem_write(d.stream,bytes(4096));u.mem_write(d.stream+8,struct.pack('<I',1));u.mem_write(d.stream+0x54,struct.pack('<III',22,1,2))
  d.raw,d.cursor,d.records=raw,16183,[];d.begin('global_header',d.root,0x1d8)
  d.call(0x483120,d.root,d.stream);d.finish_record()
  if d.cursor!=16194:raise ValueError('Source header extent changed')
  date=list(struct.unpack('<3i',u.mem_read(d.root+8,12)));flag=struct.unpack('<i',u.mem_read(d.root+0x18,4))[0]
  previous=bytes.fromhex(next(r for r in source['records'] if r['kind']=='global_header')['actor_hex'])
  if bytes(u.mem_read(d.root+4,24))!=previous[4:28] or flag!=struct.unpack_from('<i',raw,16190)[0]:raise ValueError('Header decoder differs')
  sources.append(dict(path=source['path'],sha256=source['sha256'],native_name=source['decoded']['name'],date=date,fixed_age_flag=flag,field_offset=16190,field_bytes=raw[16190:16194].hex(),header_sha256=sha(raw[16183:16194])))
 if len(sources)!=16:raise ValueError('Expected16 candidates')
 t=CityActionCostsTest();t.d=d=NativeTailDecoder(exe);d.decode_tail(shared,True);u=d.u
 ui=0x31000000;u.mem_map(ui,0x20000);allowed=set()
 def guard(machine,access,address,size,value,user):
  if d.stack-0x10000<=address and address+size<=d.stack+0x10000:return
  if not all(a in allowed for a in range(address,address+size)):raise ValueError('Unexpected native write '+hex(address))
 def allow(*ranges):
  allowed.clear()
  for a,n in ranges:allowed.update(range(a,a+n))
 def segment(start,end):
  u.emu_start(start,end,count=10000)
  if u.reg_read(UC_X86_REG_EIP)!=end:raise ValueError('Native segment boundary changed')
 def signed(v):return v if v<2**31 else v-2**32
 def text_at(p):return bytes(u.mem_read(p,100)).split(b'\0')[0].decode('big5')
 hook=u.hook_add(UC_HOOK_MEM_WRITE,guard)
 try:
  # Actual label binding for row2; stop before its presentation allocation call.
  allow();u.reg_write(UC_X86_REG_ESI,8);u.reg_write(UC_X86_REG_ESP,d.stack)
  segment(0x5460d3,0x5460e3);control,label=struct.unpack('<II',u.mem_read(d.stack-8,8))
  if control!=0x12b or text_at(label)!='能力變動':raise ValueError('Setting label binding changed')
  options=[]
  for index in (0,1):
   u.reg_write(UC_X86_REG_ESI,0x8b8010);u.reg_write(UC_X86_REG_EDI,index);u.reg_write(UC_X86_REG_ESP,d.stack)
   segment(0x546160,0x546172);option_control,option_label,zero=struct.unpack('<III',u.mem_read(d.stack-12,12))
   if zero!=0 or text_at(option_label)!=('有效','無效')[index]:raise ValueError('Setting choices changed')
   u.mem_write(ui,bytes(0x20000));u.mem_write(ui+0x11c00,struct.pack('<I',1));u.mem_write(ui+0x21c,struct.pack('<I',99))
   expected=bytearray(u.mem_read(ui,0x20000));struct.pack_into('<I',expected,0x21c,index)
   u.reg_write(UC_X86_REG_ECX,ui);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,option_control))
   allow((ui+0x21c,4));segment(0x545350,0x4f2830)
   if bytes(expected)!=bytes(u.mem_read(ui,0x20000)):raise ValueError('Setting event mutation differs')
   options.append(dict(value=index,label=text_at(option_label),control_id=hex(option_control),label_address=hex(option_label)))
  # Original new-game copy + fixed-age override. Only named world fields may change.
  startup=[]
  for fixed in (0,1,-1):
   for selected in (0,1):
    for off,val in [(0x18,fixed),(0x28,77),(0x2c,0),(0x38,0)]:u.mem_write(d.root+off,struct.pack('<i',val))
    u.mem_write(ui+8,struct.pack('<I',selected));u.reg_write(UC_X86_REG_ESI,ui);u.reg_write(UC_X86_REG_ESP,d.stack)
    before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
    allow((d.root+0x28,4),(d.root+0x2c,4),(d.root+0x38,4));segment(0x4a4373,0x4a4381);segment(0x4a443b,0x4a446b)
    expected=1 if fixed else selected
    struct.pack_into('<i',before,d.root-0x7200000+0x28,expected)
    if fixed:
     struct.pack_into('<i',before,d.root-0x7200000+0x2c,1);struct.pack_into('<i',before,d.root-0x7200000+0x38,3)
    if bytes(before)!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Startup override has unexpected effect')
    startup.append(dict(fixed_age_flag=fixed,selected=selected,effective_growth_disabled=expected))
  allow();officer=d.root+0xc0bc;spouse=officer+0x190
  # Host prepares two valid original actors; all subsequent original queries are read-only.
  u.hook_del(hook);hook=None
  for p in (officer,spouse):
   u.reg_write(UC_X86_REG_ECX,p);t.call(0x489f10);u.reg_write(UC_X86_REG_ECX,p);t.call(0x488470,0);u.mem_write(p+0xa0,struct.pack('<I',0))
  u.mem_write(officer+0x60,struct.pack('<i',1));u.mem_write(officer+0xa4,struct.pack('<i',0));u.mem_write(officer+0xc8,bytes([80]*5));u.mem_write(officer+0xd0,struct.pack('<5i',*([8]*5)));u.mem_write(officer+0x12a,struct.pack('<5H',*([105]*5)));u.mem_write(spouse+0xe8,struct.pack('<i',99))
  if t.call(0x47a630,spouse)!=1:raise ValueError('Invalid spouse fixture')
  hook=u.hook_add(UC_HOOK_MEM_WRITE,guard);rows=[]
  for year,month,day in [(184,1,1),(190,1,1),(200,12,21),(250,12,30)]:
   for turn in [0,1,2,3,32,33,34,35,36,37,360,720,3600]:
    for birth in [1,156,183,200,250]:
     for fixed in (0,1):
      for requested in (0,1):
       for off,val in [(8,year),(12,month),(16,day),(0x18,fixed),(0x28,1 if fixed else requested),(0x5c,turn)]:u.mem_write(d.root+off,struct.pack('<i',val))
       u.mem_write(officer+0x48,struct.pack('<i',birth));before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
       u.reg_write(UC_X86_REG_ECX,officer);age=signed(t.call(0x488a20));u.reg_write(UC_X86_REG_ECX,officer);politics=t.call(0x48a110,3,1)&255
       if before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Date/current calculation mutated state')
       rows.append([year,month,day,turn,birth,fixed,requested,1 if fixed else requested,age,politics])
 finally:
  if hook is not None:u.hook_del(hook)
 buf=io.StringIO();writer=csv.writer(buf,delimiter='\t',lineterminator='\n');writer.writerow(['start_year','start_month','start_day','elapsed_turns','birth','fixed_age_flag','requested_growth_disabled','effective_growth_disabled','native_age','native_politics']);writer.writerows(rows);output.write_text(buf.getvalue())
 functions=[('age',0x488a20,0x488a50),('date_year',0x4824b0,0x4824e4),('setting_dispatch',0x545350,0x545470),('setting_selected_display',0x54632c,0x546345),('startup_setting',0x4a4373,0x4a4381),('startup_override',0x4a443b,0x4a446b)]
 report=dict(schema=1,exe_sha256=sha(exe),shared_sha256=sha(shared),prior_metadata_sha256=sha(prior_raw),setting=dict(label='能力變動',control_id=hex(control),label_address=hex(label),options=options,config_byte_offset=8,global_byte_offset=0x28),startup_cases=startup,sources=sources,oracle=dict(rows=len(rows),sha256=sha(output.read_bytes()),actor=dict(base=80,curve=8,experience=105,stat=3,injury=1,native_rank=0,rank_stat=3,rank_bonus=5,spouse_skill=99)),functions=[dict(name=n,start=hex(a),end=hex(z),sha256=sha(exe[a-0x400000:z-0x400000])) for n,a,z in functions],limits=['Native selector portions stop before OS-facing presentation/settings persistence; no Windows UI automation','Setting defaults and saved user preference are not inferred','Fixed-age flag is proven by its actual arithmetic and16 source fields; no official/MOD activation claim','All oracle expected numbers are original x86 outputs; no gameplay or old save rewritten'])
 audit.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(dict(rows=len(rows),sources=len(sources),startup_cases=len(startup),oracle_sha256=report['oracle']['sha256'])))

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('installation','output','audit'):p.add_argument('--'+name,required=True,type=Path)
 a=p.parse_args();export(a.installation,a.output,a.audit)
