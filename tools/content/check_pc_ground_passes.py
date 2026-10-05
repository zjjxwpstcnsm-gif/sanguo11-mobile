#!/usr/bin/env python3
"""Cross-check captured original terrain texture/state passes against resources.

Requires a source normal-map draw capture and its same-frame managed textures.
This validates source evidence only, not an Android ground pipeline.
"""
import argparse,json
from pathlib import Path

BASE='e549cd2d0623d4332efbdf1883345871b7bd16d616ad3ff8c6ed7f1f4181eddc'
PAINT='d51cee117cfbcaadaa2a1acf4c2031521750ef5985e0b789e07a6c67b6c081d9'
OUTLINE='7cec5ac0ddeb335ec0be19570aa788f966b17ea89e35db30e36826782cb38319'

def check(draw_path,textures_path,output,quarter=None):
    draws=json.loads(draw_path.read_text());textures=json.loads(textures_path.read_text())
    assert draws['capture_version']==3 and draws['start_present']==textures['start_present'] and draws['end_present']==textures['end_present']
    rows=[]
    for tex in textures['textures']:
        if tex['first_stage']!=0:continue
        draw=draws['records'][tex['first_draw']];state=draw['draw_state'];shader=state['vertex_shader']['sha256']
        if shader not in (BASE,PAINT,OUTLINE):continue
        assert draw['primitive']==4 and draw['stride']==24
        assert tex['lock_hresult']==0 and tex['bytes']==tex['width']*tex['height']*4
        matches=[m for m in tex['known_source_rgb_matches'] if m['rgba_sha256']==tex['rgba_sha256']]
        assert matches, (tex['index'],'Source RGBA mismatch')
        render=state['render']
        for name,value in {'ZWRITEENABLE':1,'SRCBLEND':5,'DESTBLEND':6,'ALPHABLENDENABLE':1,'FOGENABLE':1,'BLENDOP':1,'SRGBWRITEENABLE':0}.items():
            assert render[name]['hresult']==0 and render[name]['value']==value,(draw['draw'],name)
        stage={s['name']:s for s in state['texture_stages'] if s['stage']==0}
        sampler={s['name']:s for s in state['samplers'] if s['stage']==0}
        for key in ('MAGFILTER','MINFILTER','MIPFILTER'):assert sampler[key]['hresult']==0 and sampler[key]['value']==2
        if shader==PAINT:
            assert {(m['resource'],m['image'])for m in matches}=={(4800,35),(4801,35),(4802,35),(4803,35)}
            assert stage['COLOROP']['value']==2 and stage['ALPHAOP']['value']==4
            assert sampler['ADDRESSU']['value']==sampler['ADDRESSV']['value']==3
            kind='original-ground-paint'
        elif shader==OUTLINE:
            assert any(m['resource']==4807 and m['image']==0 for m in matches)
            assert render['CULLMODE']['value']==2 and stage['COLOROP']['value']==3
            kind='original-ground-outline'
        elif any(m['resource']==4804 for m in matches):
            assert any(m['resource']==4804 and m['image']==1 for m in matches)
            assert stage['COLOROP']['value']==stage['ALPHAOP']['value']==5
            kind='original-ground-grid'
        else:
            assert all(m['resource'] in (4800,4801,4802,4803) and m['image']<35 for m in matches)
            if quarter is not None:assert any(m['resource']==4800+quarter for m in matches), (draw['draw'],'active season palette differs')
            assert render['CULLMODE']['value']==3 and stage['COLOROP']['value']==5 and stage['ALPHAOP']['value']==4
            kind='original-ground-base'
        rows.append(dict(draw=draw['draw'],pass_type=kind,texture=tex['path'],rgba_sha256=tex['rgba_sha256'],
                         source_matches=matches,shader_sha256=shader,alpha_min=tex['alpha_min'],alpha_max=tex['alpha_max']))
    assert sum(r['pass_type']=='original-ground-base'for r in rows)==21
    for kind in ('original-ground-paint','original-ground-outline','original-ground-grid'):assert sum(r['pass_type']==kind for r in rows)==1
    report=dict(status='PASS_SOURCE_GROUND24_TEXTURE_STATE_PASSES',goal_complete=False,records=rows,source_archive_quarter=quarter,
                source_draw_capture_sha256=draws['source_capture_sha256'],source_texture_capture_sha256=textures['capture_sha256'],
                limits=['One actual supplied source map frame; source-only evidence',
                        'Android generated normal/PBR/fog/outline still require replacement and installed PC comparison',
                        'Archive candidate resource4804 requires strict WFTX palette/mip decoder; old RGB-only parser rejected it'])
    output.write_text(json.dumps(report,indent=2)+'\n');print(report['status'],len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('draw_report',type=Path);p.add_argument('texture_report',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--quarter',type=int,choices=range(4),help='Observed source archive row: autumn0,spring1,summer2,winter3')
    a=p.parse_args();check(a.draw_report,a.texture_report,a.output,a.quarter)
