#!/usr/bin/env python3
"""Verify original font glyph indices/pixels and the64 quarantined name records.
No portrait assets, PC files or saved games are modified.
"""
import argparse,csv,gzip,json,struct,zlib,binascii
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP
from inspect_pc_scenario_officers import NativeOfficerDecoder
from pc_resources import Archive
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,json_bytes,output_guard
GLYPHS={0xfa40:chr(0x4f37),0xfa41:chr(0x749d),0xfa45:chr(0x5101)}
ORTHOGRAPHY={chr(0x6731)+chr(0x5101):chr(0x6731)+chr(0x96cb)}
def decode_name(raw):
    out='';i=0
    while i<len(raw):
        n=2 if raw[i]>=128 else 1;part=raw[i:i+n];code=int.from_bytes(part,'big')
        out+=GLYPHS[code] if code in GLYPHS else part.decode('big5');i+=n
    return out

def font_frames(raw):
    if raw[:8]!=b'WFTX0010' or struct.unpack_from('<II',raw,8)!=(len(raw),2):raise ValueError('Original two-page font differs')
    off=16;frames=[]
    for _ in range(2):
        w,h,bits,palette,mips,res=struct.unpack_from('<HH4B',raw,off);off+=8
        if (bits,palette,mips,res)!=(8,1,0,0):raise ValueError('Unexamined font format')
        pitch=(w+3)&~3;data=raw[off:off+pitch*h];off+=pitch*h;pal=raw[off:off+1024];off+=1024
        if len(data)!=pitch*h or len(pal)!=1024:raise ValueError('Truncated original font')
        frames.append((w,h,pitch,data,pal))
    if off!=len(raw):raise ValueError('Unknown original font tail')
    return frames

def png(width,height,raw):
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',binascii.crc32(kind+data)&0xffffffff)
    rows=b''.join(b'\0'+raw[i*width:(i+1)*width]for i in range(height))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',width,height,8,0,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b'')

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    if output.exists():raise ValueError('Preserve earlier evidence')
    output.mkdir(parents=True);exe=(installation/'san11pk.exe').read_bytes();d=NativeOfficerDecoder(exe);u=d.u
    archive=Archive(installation/'Media/san11pkres.bin');fonts=[];glyphs=[]
    for resource,cell,file in [(4851,16,'font2.bmp'),(4852,24,'font1.bmp')]:
        packed=archive.read(resource);frame=font_frames(packed)[1];w,h,pitch,data,palette=frame
        bmp=(installation/'全修改器/字体修改器/原版字体'/file).read_bytes();at=struct.unpack_from('<I',bmp,10)[0]
        if struct.unpack_from('<ii',bmp,18)!=(w,h):raise ValueError('Original BMP geometry differs')
        bmp_data=b''.join(bmp[at+j*pitch:at+(j+1)*pitch]for j in range(h-1,-1,-1))
        if data!=bmp_data:raise ValueError('Installed font pixels differ from supplied original BMP')
        fonts.append(dict(resourceId=resource,resourceSha256=sha(packed),bmpSha256=sha(bmp),pixelSha256=sha(data),width=w,height=h,cell=cell,pixelsByteEqual=True))
        for code,ch in GLYPHS.items():
            u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,code));u.emu_start(0x432190,d.stop,count=1000)
            if u.reg_read(UC_X86_REG_EIP)!=d.stop:raise ValueError('Original glyph index boundary differs')
            index=u.reg_read(UC_X86_REG_EAX);x=index%16*cell;y=index//16*cell
            pixels=b''.join(data[(y+j)*pitch+x:(y+j)*pitch+x+cell]for j in range(cell))
            image=png(cell,cell,pixels);name='font-'+str(resource)+'-'+hex(code)[2:]+'.png';(output/name).write_bytes(image)
            glyphs.append(dict(codeHex=hex(code)[2:],unicode=ch,codepoint=hex(ord(ch)),glyphIndex=index,resourceId=resource,x=x,y=y,width=cell,height=cell,pixelSha256=sha(pixels),evidenceImage=name,imageSha256=sha(image),recognition='visual inspection of actual original glyph; no slot-based naming'))
    archive.close()
    prior_bytes=(ROOT/'docs/pc-data/scenario-officers-native.json.gz').read_bytes();prior=json.loads(gzip.decompress(prior_bytes))
    coverage_bytes=(ROOT/'docs/handoff/20261004/session1/scenario-person-runtime-coverage.json.gz').read_bytes();coverage=json.loads(gzip.decompress(coverage_bytes));ids={(r['sourcePath'],r['nativeId']):r for r in coverage['people']}
    catalog_raw=(ROOT/'core/src/main/resources/content/officers.tsv').read_bytes();catalog=list(csv.DictReader(catalog_raw.decode().splitlines(),delimiter='\t'));rows=[];source_cache={}
    for r in prior['records']:
        if r['native_index']>=670 or r['decoded']['name'] is not None:continue
        source=r['source'];native=r['native_index'];pc=source_cache.setdefault(source,(installation/source).read_bytes());record=pc[r['offset']:r['offset']+152]
        if sha(record)!=r['sha256']:raise ValueError('Original source record changed')
        parsed,actor=d.decode(record,native);name=decode_name(bytes.fromhex(''.join(parsed['name_bytes'])));candidate_name=ORTHOGRAPHY.get(name,name)
        matches=[c for c in catalog if c['name']==candidate_name and int(c['birth'])==parsed['birth'] and c['gender']==('男'if parsed['sex']==0 else '女')]
        if len(matches)!=1:raise ValueError('Name/birth/sex identity join is not unique')
        canonical=matches[0];gameplay=ids[(source,native)]
        if gameplay['recordSha256']!=sha(record)or not gameplay['sourceOnlyIdentity']:raise ValueError('Original runtime identity guard differs')
        stats_equal=list(map(int,canonical['stats'].split(',')))==parsed['stats'];apt_equal=canonical['aptitudes']==''.join('CBAS'[v]for v in parsed['aptitude_codes']) if 'aptitudes'in canonical else canonical['aptitude']==''.join('CBAS'[v]for v in parsed['aptitude_codes'])
        rows.append(dict(sourcePath=source,sourceSha256=sha(pc),sourceVariant=gameplay['sourceVariant'],nativeId=native,officerId=gameplay['officerId'],canonicalOfficerId=int(canonical['id']),sourceName=name,catalogName=canonical['name'],orthographicVariant=name!=canonical['name'],birth=parsed['birth'],sex=parsed['sex'],recordSha256=sha(record),nameRawHex=''.join(parsed['name_bytes']),sourceOffset=r['offset'],baseStats=parsed['stats'],aptitudeCodes=parsed['aptitude_codes'],statsEqualCatalog=stats_equal,aptitudesEqualCatalog=apt_equal,identityMethod='original-font name + original-serializer birth/sex + unique catalog candidate; runtime ID preserved'))
    if len(rows)!=64 or len({r['sourcePath']for r in rows})!=16:raise ValueError('All64 original source name rows required')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,originalGlyphIndexFunction=dict(start='0x432190',endExclusive='0x4321df',sha256=sha(exe[0x432190-0x400000:0x4321df-0x400000])),fonts=fonts,glyphs=glyphs,rows=rows,priorOfficerReportSha256=sha(prior_bytes),runtimeCoverageSha256=sha(coverage_bytes),catalogSha256=sha(catalog_raw),limits=['Actual installed archive pixels match local original BMPs','Glyph character reading is explicit human visual verification, not CP950 replacement','Font runtime option/override activation stays unknown','Only name identity closure; complete per-person field and scenario-start coverage remain separate'],completeGoal=False)
    (output/'report.json.gz').write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(rows=64,stableRuntimeIds=sorted({r['officerId']for r in rows}),canonicalIds=sorted({r['canonicalOfficerId']for r in rows}),sha256=sha((output/'report.json.gz').read_bytes()))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
