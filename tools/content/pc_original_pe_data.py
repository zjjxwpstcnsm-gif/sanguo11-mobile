"""Map only original PE section holes, retaining constructed native domains.
No replacement rule or callback, no fabricated data: every byte comes from the
exact checked source EXE (section zero-fill only where PE virtual size requires).
"""
import struct,hashlib
from pathlib import Path
def load_original_data(machine):
 raw=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/san11pk.exe').read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 pe=struct.unpack_from('<I',raw,0x3c)[0];count=struct.unpack_from('<H',raw,pe+6)[0];opt=struct.unpack_from('<H',raw,pe+20)[0];base=struct.unpack_from('<I',raw,pe+52)[0];section_start=pe+24+opt;records=[]
 for index in range(count):
  at=section_start+40*index;name=raw[at:at+8].rstrip(b'\0').decode('ascii');virtual_size,va,file_size,file_offset=struct.unpack_from('<IIII',raw,at+8)
  if name!='.data':continue
  low=base+va;high=low+virtual_size;cursor=(low+4095)//4096*4096;end=(high+4095)//4096*4096
  holes=[]
  for start,stop,permissions in sorted(machine.mem_regions()):
   if stop<cursor or start>=end:continue
   if start>cursor:holes.append((cursor,min(start,end)))
   cursor=max(cursor,stop+1)
  if cursor<end:holes.append((cursor,end))
  for start,stop in holes:
   assert start%4096==0 and stop%4096==0;machine.mem_map(start,stop-start)
   copied_start=max(start,low);copied_end=min(stop,low+file_size);data=raw[file_offset+copied_start-low:file_offset+copied_end-low];machine.mem_write(copied_start,data)
   assert bytes(machine.mem_read(copied_start,len(data)))==data
   records.append(dict(section=name,start=hex(start),end=hex(stop),source_offset=file_offset+copied_start-low,bytes=len(data),sha256=hashlib.sha256(data).hexdigest()))
 return records
