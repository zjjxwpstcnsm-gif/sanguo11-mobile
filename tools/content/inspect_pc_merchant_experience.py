#!/usr/bin/env python3
"""Pin experience/current-politics/quote ordering before merchant price integration."""
import argparse,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

def inspect(exe):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified EXE')
 pointer=struct.unpack_from('<I',raw,0x8ae58c-0x400000)[0]
 name=raw[pointer-0x400000:pointer-0x400000+32].split(b'\0')[0].decode('big5')
 if name!='政治':raise ValueError('Political property label mismatch')
 functions=[]
 for name,start,end in [('ordered_trade_segment',0x5cacb7,0x5cad20),('experience_award',0x4a70d0,0x4a7277),
  ('experience_get',0x489180,0x48919e),('experience_set',0x48a810,0x48a853),
  ('current_abilities_refresh',0x48a2d0,0x48a38d),('current_ability',0x48a110,0x48a2c3),('growth_curve',0x48a030,0x48a10e),
  ('injury_set',0x48a8e0,0x48a900),('injury_percentage',0x488fb0,0x489016)]:
  functions.append(dict(name=name,address=hex(start),end_exclusive=hex(end),sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest()))
 return dict(schema=1,source_path='san11pk.exe',executable_sha256=EXE_SHA,functions=functions,
  political_property=dict(id=28,label='政治',descriptor='8ab758 +28*16 =8ab918',native_stat_index=3,base_field='officer+cb',experience_field='officer+130 uint16',current_field='officer+173 uint8',alternate_cached_field='officer+178 uint8, refresh passes condition selector-1'),
  order=['5cacb7: award5 experience to stat3 and refresh abilities','5cacc8: award50 merit','5cacd5: mark city merchant used','5cace6: call5ca620 using refreshed current politics','5cacf9/5cad0a: commit gold/food','5cad1b: debit20 AP'],
  proven=dict(experience_cap=3000,normal_city_award=5,flat_growth_unmodified_ability='clamp(base + experience/100,1,100)',cases=160,quotes_changed_from_before_award=32,
   method='Original ordered segment, original experience/ability/quote/resource functions; complete3MiB and RNG comparison'),
  named_properties=[dict(id=i,name=n,set_branch=a) for i,n,a in [(15,'配偶','4a3c13 ->48a4b0 ->officer+60'),(21,'官職','4a3c69 ->48a750 ->officer+a4'),(33,'政治經驗','4a3d45 ->48a810(stat3) ->officer+130'),(57,'傷病','4a3d6b ->48a8e0 ->officer+15c')]],
  injury=dict(valid_selectors=[-1,0,1,2,3],percentages=[100,100,80,50,30],affected_stats=[0,1,2,3],charm_unchanged=True,
   operation='After base/experience cap, multiply by percentage and truncate, minimum1; alternate cache175..179 excludes injury',
   cases=100,invalid_selector_cases=4,method='Original named property setter and ability refresh; exact3MiB/RNG checks',scope='Other rank/spouse/age modifiers absent in this fixture'),
  boundaries=['Segment begins after action marking and ends before presentation; full command admission not rerun',
   'Isolated valid city officer: curve/rank-reference/spouse-reference fields set-1, condition selector0, no unit mentoring; full current-ability model not yet reproduced',
   'Injury selector named via original property57; age-curve and rank/spouse skill modifiers still require full joint verification',
   'Project currently has no equivalent saved merchant political experience; static quotes alone cannot reproduce this ordering',
   'No gameplay implementation change in this evidence increment'])

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--exe',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
 if a.output.resolve()==a.exe.parent.resolve() or a.exe.parent.resolve() in a.output.resolve().parents:raise ValueError('Read-only PC directory')
 a.output.write_text(json.dumps(inspect(a.exe),ensure_ascii=False,indent=2)+'\n')
