#!/usr/bin/env python3
"""Read-only EXE effect/texture bindings; unknown KSEF semantics stay unknown.

Keeps raw record hashes, potential loose-file overrides and exact decoded pixels.
Diagnostic contact sheets are not Android game assets or restoration acceptance.
"""
import argparse, json, struct
from pathlib import Path
from pc_resources import Archive, sha, effects, wftx
from pc_effect_formats import graph

ROOT=Path(__file__).resolve().parents[2]
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation, output, report):
    from PIL import Image, ImageDraw
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA: raise ValueError('Reinspect effect bindings for changed EXE')
    loose={}
    for p in installation.rglob('*'):
        if p.is_file(): loose.setdefault(p.relative_to(installation).as_posix().lower(),[]).append(p)
    def overrides(name):
        return [dict(path=p.relative_to(installation).as_posix(),bytes=p.stat().st_size,
                     sha256=sha(p.read_bytes()),activation='unproven; archive/loose branch selected at runtime')
                for p in loose.get(name.lower(),[])]
    archive=Archive(installation/'Media/san11pkres.bin'); rows=[]; textures=[]
    output=output.resolve(); report=report.resolve();output.mkdir(parents=True,exist_ok=True)
    try:
        # 413230 chooses archive resource0x7c or media/effect/effect.wft.
        # These 33 common sheets supply particles/lightning/fire/rocks. The
        # 347 dynamic texture slots below are an additional, separate library.
        common_raw=archive.read(124);common_frames=[];common_images=wftx(common_raw)
        for index,im in enumerate(common_images):
            rgba=im.convert('RGBA');path=output/f'common-texture-{index:02d}.png';rgba.save(path,optimize=False)
            common_frames.append(dict(image=index,size=list(im.size),rgba_sha256=sha(rgba.tobytes()),
                                      diagnostic_png=path.relative_to(ROOT).as_posix(),png_sha256=sha(path.read_bytes()),
                                      android_output=None,runtime_binding=None))
        if len(common_frames)!=33:raise ValueError('Common effect sheet count changed')
        common=dict(resource_id=124,source_file='media/effect/effect.wft',bytes=len(common_raw),sha256=sha(common_raw),
                    format='WFTX0010',frames=common_frames,potential_overrides=overrides('media/effect/effect.wft'),
                    source_resolver='0x413230 -> 0x46e470 archive resource0x7c; alternate loose 0x46db70',
                    frame_binding='KSEF texture selector and runtime dynamic substitution still unresolved',
                    validation='source loader and exact pixel decode only',android_output=None,runtime_binding=None)
        for index in range(244):
            offset=0x37692c+index*12; pointer,r,flag=struct.unpack_from('<III',exe,offset)
            p=pointer-0x400000
            if not 0<=p<len(exe):raise ValueError('Effect filename pointer')
            name=exe[p:exe.index(b'\0',p)].decode('ascii')
            raw=archive.read(r)
            if not name.startswith('media/effect/effect') or not name.endswith('.efdx') or raw[:8]!=b'KSEF0131':raise ValueError('Effect table changed')
            size,root_count,root_ptr=struct.unpack_from('<III',raw,8)
            if size!=len(raw) or root_count!=1 or root_ptr!=20:raise ValueError('KSEF root header')
            rows.append(dict(effect_index=index,source_file=name,resource_id=r,bytes=len(raw),sha256=sha(raw),
                             format='KSEF0131',source_table_offset=hex(offset),table_flag=flag,
                             flag_semantics='unresolved',potential_overrides=overrides(name),
                             graph=graph(raw),conversion='graph boundaries only; emitter, blend, time and texture bindings unresolved',
                             android_output=None,runtime_binding=None,validation='source binding only'))
        if sorted(row['resource_id']for row in rows)!=list(range(125,369)):raise ValueError('Effect resource coverage changed')
        # 0x4144a7 reads this table with textureIndex*8. The following call
        # 0x4144b2 passes its first word to the effect instance factory 0x413d20.
        # 4142b0 uses the second word as destination top, adding source height
        # for bottom; all four rectangle coordinates shift by texture quality.
        # 414177 chooses the last common library image (33-1 = sheet32).
        presentations=[]
        for index in range(347):
            template,raw_flags=struct.unpack_from('<II',exe,0x3774e0+index*8)
            if not 0<=template<len(rows):raise ValueError('Presentation template outside effect table')
            presentations.append(dict(texture_index=index,effect_index=template,resource_id=rows[template]['resource_id'],
                                      source_file=rows[template]['source_file'],raw_flags=raw_flags,
                                      flags_semantics='destination_top_pixels_at_full_source_resolution',source_table_offset=hex(0x3774e0+index*8),
                                      texture_composition=dict(common_resource_id=124,common_image=32,
                                          destination_left=0,destination_top=raw_flags,
                                          evidence='414177 last common sheet;4142b0 source height+table second word;454ee0 copy',
                                          limits='Active texture quality shift, pixel conversion and runtime composition not yet ported'),
                                      binding_evidence='0x4144a7 -> 0x4144b2 -> 0x413d20',
                                      gameplay_cause='unresolved',runtime_binding=None))
        previews=[]
        for index in range(347):
            r=369+index;raw=archive.read(r);images=wftx(raw)
            name=f'media/effect/effect_tex{index:03d}.wft';frames=[]
            for j,im in enumerate(images):
                rgba=im.convert('RGBA');path=output/f'texture-{index:03d}-{j}.png';rgba.save(path,optimize=False)
                alpha=rgba.getchannel('A');hist=alpha.histogram()
                frames.append(dict(image=j,size=list(im.size),source_mode=im.mode,
                                   rgba_sha256=sha(rgba.tobytes()),alpha_zero=hist[0],alpha_full=hist[255],
                                   alpha_fractional=sum(hist[1:255]),diagnostic_png=path.relative_to(ROOT).as_posix(),png_sha256=sha(path.read_bytes())))
            previews.append(images[0].convert('RGBA'))
            width,height=images[0].size;destination=presentations[index]['texture_composition']
            destination.update(destination_right=width,destination_bottom=destination['destination_top']+height)
            if destination['destination_right']>common_images[32].width or destination['destination_bottom']>common_images[32].height:
                raise ValueError('Source presentation destination outside common sheet32')
            textures.append(dict(texture_index=index,resource_id=r,source_file=name,bytes=len(raw),sha256=sha(raw),format='WFTX0010',
                                 potential_overrides=overrides(name),frames=frames,
                                 source_resolver='0x413c63 archive resource=369+textureIndex; 0x413c6b loose filename formatter',
                                 android_output=None,runtime_binding=None,validation='exact source pixel decode only'))
        if archive.read(716)[:8]==b'WFTX0010':raise ValueError('Texture range needs reinvestigation')
        extra=[]
        for r in (4861,4862):
            raw=archive.read(r);needle=struct.pack('<I',r);hits=[];start=0
            while True:
                start=exe.find(needle,start)
                if start<0:break
                hits.append(hex(start));start+=1
            extra.append(dict(resource_id=r,bytes=len(raw),sha256=sha(raw),format=raw[:8].decode('ascii'),
                              graph=graph(raw),exe_u32_occurrences=hits,source_file=None,conversion='graph boundaries only; semantics unresolved',runtime_binding=None))
        placements=effects(archive.read(4792))
        for row in placements:
            row['table_reference_candidate']=rows[row['effect']]['source_file'] if row['effect']<244 else None
            row['binding_status']='source-confirmed effect-table index; normal Android runtime binding pending'
            row['binding_evidence']='5a2407 resource4792 -> 5a2497/413af0 -> runtime row+10 -> 413f00/413d20 factory'
            row['transform_evidence']='413a80 copies xyz to row matrix+30/+34/+38 and constructs yaw Y rotation'
            row['effect_resource_id']=rows[row['effect']]['resource_id'] if row['effect']<244 else None
        for start in range(0,len(previews),80):
            sheet=Image.new('RGB',(8*144,10*144),(32,32,32));draw=ImageDraw.Draw(sheet)
            for k,im in enumerate(previews[start:start+80]):
                thumb=im.copy();thumb.thumbnail((140,120));x=(k%8)*144;y=(k//8)*144
                sheet.paste(thumb,(x+(140-thumb.width)//2,y),thumb)
                draw.text((x+2,y+122),f'{start+k:03d} / res {369+start+k}',fill=(255,255,255))
            sheet.save(output/f'contact-{start//80:02d}.png')
        result=dict(schema=1,goal_complete=False,executable_sha256=sha(exe),source_archive='Media/san11pkres.bin',
                    source_policy='installation read-only; active archive supplied bytes; loose/MOD precedence unproven without PC runtime',
                    table=dict(offset='0x37692c',stride=12,count=244,sha256=sha(exe[0x37692c:0x37749c])),
                    function_evidence=[dict(va=hex(v),bytes=n,sha256=sha(exe[v-0x400000:v-0x400000+n]))for v,n in[(0x413be0,0xf9),(0x45a320,0x1e0),(0x457dd0,0xf0),(0x459e40,0x26),(0x4589f0,0x6a),(0x46d5b0,0x9c),(0x46d030,0x190),(0x45c6f0,0x3b0),(0x414177,0x34),(0x4142b0,0xb0)]],
                    presentation_table=dict(offset='0x3774e0',stride=8,count=347,sha256=sha(exe[0x3774e0:0x3774e0+347*8])),
                    presentation_bindings=presentations,
                    effects=rows,common_textures=common,textures=textures,extra_pk_effects=extra,map_seff=placements,
                    limits=['KSEF graph boundaries confirmed; material/texture usage and time evaluation unresolved','Names/index order do not identify gameplay effects',
                            'SEFF source table/matrix binding confirmed; no original Android ambient effect emitter','No original fullscreen text playback or PC pixel/timing acceptance',
                            'Diagnostic PNGs/contact sheets do not enter the APK'])
        report.parent.mkdir(parents=True,exist_ok=True);report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps(dict(effects=len(rows),textures=len(textures),common_textures=len(common_frames),extra_pk=len(extra),placements=len(placements),runtime_effects_added=0)))
    finally:archive.close()

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v139-effect-source')
    p.add_argument('--report',type=Path,default=ROOT/'docs/pc-visual/effect-bindings-source.json')
    a=p.parse_args();inspect(a.installation,a.output,a.report)
