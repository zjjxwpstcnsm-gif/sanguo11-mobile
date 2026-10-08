#!/usr/bin/env python3
"""Pinned native message decompression; quarantine new-game option labels.
Record order does not prove UI index, enum value or active resource priority.
"""
import argparse,json,struct
from pathlib import Path
from inspect_pc_message_resources import decode
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve text evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA;rows=[];resources=[]
 needles=['壽命','死亡','戰死','難度','初級','上級','超級','史實','長壽','假想','不死']
 for resource in ['S11MSG00.s11','S11MSG01.s11','S11MSG03.s11']:
  path=installation/'Media/msg'/resource;source=path.read_bytes();print('Original decompression',resource,flush=True);data,proof=decode(exe,source);count=struct.unpack_from('<H',data)[0];offsets=list(struct.unpack_from('<%dI'%count,data,2));assert all(2+4*count<=a<len(data)for a in offsets)and sorted(offsets)==offsets
  resources.append(dict(path='Media/msg/'+resource,sourceSha=sha(source),decodedSha=sha(data),count=count,proof=proof));offsets.append(len(data))
  for index in range(count):
   raw=data[offsets[index]:offsets[index+1]];found=[x for x in needles if x.encode('big5')in raw]
   if found:rows.append(dict(resource=resource,recordIndex=index,offset=offsets[index],bytes=len(raw),matched=found,rawHex=raw.hex(),big5Diagnostic=raw.decode('big5',errors='backslashreplace')))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,resources=resources,rows=rows,limits=['Original4711c0 decoder/resource dictionary/source bytes fixed and output guarded','Diagnostic decoding retains controls/unknown bytes, not final UI strings','Need original message getter/newgame chooser binding for global messageId/enum value/UI labels/defaults','No PC writes, Wine or external text tables']),ensure_ascii=False,indent=2)+'\n');print('PASS original option text candidates',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
