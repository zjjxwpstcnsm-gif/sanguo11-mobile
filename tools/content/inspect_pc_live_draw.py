#!/usr/bin/env python3
"""Match actual original D3D9 stream prefixes to read-only WKMD resources.

Preserves ambiguous near/far matches. No image synthesis or PC state mutation.
The v1 observer stores 512 bytes from stream0; version2 samples the first
declared vertex and appends render/stage/sampler state plus vertex bytecode.
Whole WKMD-prefix matches still require zero base/minimum in both versions.
"""
import argparse,json,struct,collections
from pathlib import Path
from pc_resources import Archive,sha

RENDER_NAMES={7:'ZENABLE',8:'FILLMODE',9:'SHADEMODE',14:'ZWRITEENABLE',15:'ALPHATESTENABLE',
    19:'SRCBLEND',20:'DESTBLEND',22:'CULLMODE',23:'ZFUNC',24:'ALPHAREF',25:'ALPHAFUNC',
    27:'ALPHABLENDENABLE',28:'FOGENABLE',34:'FOGCOLOR',35:'FOGTABLEMODE',36:'FOGSTART',
    48:'RANGEFOGENABLE',60:'TEXTUREFACTOR',137:'LIGHTING',168:'COLORWRITEENABLE',
    171:'BLENDOP',174:'SCISSORTESTENABLE',175:'SLOPESCALEDEPTHBIAS',178:'DEPTHBIAS',
    194:'SRGBWRITEENABLE'}
STAGE_NAMES={1:'COLOROP',2:'COLORARG1',3:'COLORARG2',4:'ALPHAOP',5:'ALPHAARG1',
    6:'ALPHAARG2',11:'TEXCOORDINDEX',24:'TEXTURETRANSFORMFLAGS',26:'RESULTARG',32:'CONSTANT'}
SAMPLER_NAMES={1:'ADDRESSU',2:'ADDRESSV',5:'MAGFILTER',6:'MINFILTER',7:'MIPFILTER'}

def inspect_draw_state(data,offset):
    words=struct.unpack_from('<256I',data,offset+4888)
    if words[0]!=0x32545344 or 4+words[1]*3+(words[2]+words[3])*4>256:
        raise ValueError('Source draw-state bounds')
    at=4;render={};stages=[];samplers=[]
    for _ in range(words[1]):
        key,hr,value=words[at:at+3];at+=3
        render[RENDER_NAMES.get(key,'STATE_'+str(key))]=dict(key=key,hresult=hr,value=value)
    for count,names,target in ((words[2],STAGE_NAMES,stages),(words[3],SAMPLER_NAMES,samplers)):
        for _ in range(count):
            stage,key,hr,value=words[at:at+4];at+=4
            target.append(dict(stage=stage,key=key,name=names.get(key,'STATE_'+str(key)),hresult=hr,value=value))
    textures=[]
    if words[at]==0x32585444:
        count=words[at+1];at+=2
        if at+count*12>256:raise ValueError('Source texture-description bounds')
        for _ in range(count):
            stage,hr,kind,desc_hr,*description=words[at:at+12];at+=12
            textures.append(dict(stage=stage,get_hresult=hr,resource_type=kind,description_hresult=desc_hr,
                description=dict(zip(('format','type','usage','pool','multisample','quality','width','height'),description))))
    magic,get_hr,function_hr,byte_count=struct.unpack_from('<4I',data,offset+5912)
    if magic!=0x32565344:raise ValueError('Source vertex-shader record')
    bytecode=data[offset+5928:offset+5928+byte_count] if get_hr==0 and function_hr==0 and 0<byte_count<=2048 and byte_count%4==0 else None
    return dict(render=render,texture_stages=stages,samplers=samplers,textures=textures,
        vertex_shader=dict(get_hresult=get_hr,function_hresult=function_hr,bytes=byte_count,
            sha256=sha(bytecode) if bytecode else None,bytecode_words=list(struct.unpack('<%dI'%(len(bytecode)//4),bytecode)) if bytecode else None))

def inspect(source,capture,output):
    archive=Archive(source/'Media/san11pkres.bin');models=[]
    try:
        for rid in range(len(archive.entries)):
            raw=archive.read(rid)
            if raw[:8]!=b'WKMD0010':continue
            if len(raw)<160 or struct.unpack_from('<I',raw,8)[0]!=len(raw):raise ValueError(('WKMD length',rid))
            count=struct.unpack_from('<I',raw,68)[0]
            for stream in range(count):
                code,stride,nv,vp=struct.unpack_from('<4I',raw,80+stream*80)
                if code!=0x112 or stride!=32:continue
                size=min(512,nv*stride)
                if vp+nv*stride>len(raw):raise ValueError(('Source vertex bounds',rid))
                models.append((nv,raw[vp:vp+size],dict(resource_id=rid,stream=stream,sha256=sha(raw))))
    finally:archive.close()
    data=capture.read_bytes();magic,version,start,size=struct.unpack_from('<4I',data)
    assert magic==0x31445350 and (version,size) in ((1,4888),(2,7976),(3,8540))
    footer,count,end,reserved=struct.unpack_from('<4I',data,len(data)-16)
    assert footer==0x444e4544 and reserved==0 and len(data)==32+count*size
    records=[]
    for i in range(count):
        offset=16+i*size;v=struct.unpack_from('<16I',data,offset)
        assert v[:2]==(0x57415244,i)
        constants=struct.unpack_from('<1024f',data,offset+64)
        def register(index):return list(constants[index*4:index*4+4])
        candidates=[]
        if v[12]==32 and v[3]==0 and v[4]==0 and v[14]==0 and v[15]==0:
            prefix=data[offset+4376:offset+4888]
            for nv,p,description in models:
                if nv==v[5] and prefix[:len(p)]==p:candidates.append(description)
        records.append(dict(draw=i,primitive=v[2],base_vertex=v[3],minimum_vertex=v[4],vertices=v[5],primitives=v[7],stride=v[12],stream_offset=v[13],stream_hresult=v[14],lock_hresult=v[15],models=candidates,world_c30=[register(30+k)for k in range(4)],light_c18=register(18),tint_c4=register(4),fog_c26=register(26),fade_c27=register(27)))
        if version>=2:records[-1]['draw_state']=inspect_draw_state(data,offset)
        if version>=3:
            stream_magic,index,stream_offset,stride,hr,lock_hr,byte_count=struct.unpack_from('<7I',data,offset+7976)
            if stream_magic!=0x33564244 or index!=1 or byte_count>512:raise ValueError('Stream1 prefix bounds')
            records[-1]['stream1']=dict(index=index,offset=stream_offset,stride=stride,get_hresult=hr,lock_hresult=lock_hr,bytes=byte_count,
                prefix_hex=data[offset+8028:offset+8028+byte_count].hex())
    report=dict(source_capture=str(capture),source_capture_sha256=sha(data),capture_version=version,start_present=start,end_present=end,draw_calls=count,matched_draws=sum(bool(r['models'])for r in records),stride_counts=dict(collections.Counter(r['stride']for r in records)),records=records,limits=['Prefix matches retain all resource ambiguities','No texture/index/final raster equivalence inferred','Version1 has no draw-state record; version2 preserves HRESULTs','Original readback overhead is not native frame-rate evidence'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k]for k in ('draw_calls','matched_draws','stride_counts')}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('capture',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.capture,a.output)
