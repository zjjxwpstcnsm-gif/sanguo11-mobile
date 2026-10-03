#!/usr/bin/env python3
"""Pin original merchant usage/turn call sites without modifying the PC installation."""
import argparse,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

def inspect(exe):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
 rows=[]
 for address,target,opcode,label in [
  (0x58051e,0x4a1180,0xe8,'ordinary turn date advance'),
  (0x5805d0,0x59c330,0xe8,'ordinary turn global settlement'),
  (0x59c423,0x598630,0xe8,'settlement action reset'),
  (0x598650,0x487860,0xe8,'87-site reset dispatcher'),
  (0x487911,0x47b730,0xe9,'city reset tail jump'),
  (0x4878af,0x48da10,0xe9,'port reset tail jump'),
  (0x4878e3,0x48da10,0xe9,'gate reset tail jump')]:
  code=raw[address-0x400000:address-0x400000+5]
  if code[0]!=opcode or address+5+struct.unpack('<i',code[1:])[0]!=target:raise ValueError(label)
  rows.append(dict(label=label,address=hex(address),target=hex(target),instruction_hex=code.hex()))
 for address,code,label in [(0x47b730,'c781a400000000000000c3','city flags clear dword+a4'),(0x48da10,'c7416800000000c3','gate/port flags clear dword+68')]:
  if raw[address-0x400000:address-0x400000+len(code)//2].hex()!=code:raise ValueError(label)
  rows.append(dict(label=label,address=hex(address),instruction_hex=code))
 return dict(schema=1,source_path='san11pk.exe',executable_sha256=EXE_SHA,records=rows,
  merchant=dict(flag_offset='city+a4',bit=1,admission='5caaf0 -> 481310',commit='5cacd5 -> 47b6f0',reset='598630 loop 87 sites, cities through 487860 -> 47b730'),
  verification=['test_pc_merchant_quote:256 flag admissions and75 commit resource cases','test_pc_merchant_turn:180 date advances/540 year-month-day results,4 flag patterns across87 sites,3MiB mutations and original RNG unchanged'],
  compatibility=['Existing positive saved traded volume means used; retain all balances and volume','Normal project global turn clears map; no save schema change'],
  limits=['Bounded original functions and statically connected normal turn calls, not full PC gameplay execution','Does not verify quantity bounds, current politics modifiers, price initialization/update, merchant actor merit or enabled MODs','Project building-kind eligibility remains a separate parity question'])

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--exe',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 if a.output.resolve()==a.exe.parent.resolve() or a.exe.parent.resolve() in a.output.resolve().parents:raise ValueError('Read-only PC installation')
 a.output.write_text(json.dumps(inspect(a.exe),indent=2)+'\n')
