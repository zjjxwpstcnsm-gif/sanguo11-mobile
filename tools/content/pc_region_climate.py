"""Original geographic climate fields captured read-only from the supplied PC.

Used only by art conversion; no game World/state/RNG/save binding. The separate
Unicorn checker executes original41c3a0 to validate this decoded source chain.
"""
import json,struct
from pathlib import Path
from pc_resources import sha
ROOT=Path(__file__).resolve().parents[2]

class RegionClimate:
    def __init__(self,exe,shex,reference):
        self.reference=Path(reference);self.record=json.loads(self.reference.read_text())
        self.snapshot=(ROOT/self.record['snapshot']).read_bytes()
        if sha(self.snapshot)!=self.record['sha256'] or sha(exe)!=self.record['source_exe_sha256'] or len(self.snapshot)!=2240 or self.snapshot[:8]!=b'PCCLIM01':raise ValueError('Original live climate provenance mismatch')
        self.city_province=[struct.unpack_from('<I',self.snapshot,32+i*32+24)[0]for i in range(42)]
        self.province_climate=[struct.unpack_from('<I',self.snapshot,32+42*32+i*72+44)[0]for i in range(12)]
        if self.city_province!=self.record['city_province'] or self.province_climate!=self.record['province_climate'] or max(self.city_province)>=12 or max(self.province_climate)>5:raise ValueError('Original live climate fields mismatch')
        self.shex=shex;self.parents=exe[0x39c2b0:0x39c2b0+93]
        if shex[:8]!=b'SHEX0008' or len(shex)!=440008 or len(self.parents)!=93 or max(self.parents)>=42:raise ValueError('Original geographic climate tables')
    def resolve(self,row):
        q=int((row['x']*2-112)/4);r=int((row['z']*2-112-2*(q&1))/4)
        region=city=None
        if 0<=q<200 and 0<=r<200:
            region=self.shex[8+(q*200+r)*11+1]&127
            if region>=len(self.parents):raise ValueError('Unexamined region parent index')
            city=self.parents[region];climate=self.province_climate[self.city_province[city]]
        else:climate=0
        return dict(slot=row['slot'],kind=row['model'],x=row['x'],z=row['z'],source_cell=[q,r],region=region,city=city,climate=climate)
    def evidence(self):
        return dict(climate_reference=str(self.reference.resolve().relative_to(ROOT)),climate_snapshot_sha256=sha(self.snapshot),climate_source_map_id=4791,climate_source_map_sha256=sha(self.shex),region_parent_table_va='0x79c2b0',region_parent_table_sha256=sha(self.parents))
