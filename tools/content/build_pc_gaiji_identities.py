#!/usr/bin/env python3
"""Build fresh-game font-verified identity/text facts without changing source IDs."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,json_bytes

def build(proof,expected_sha,output):
    raw=proof.read_bytes()
    if sha(raw)!=expected_sha:raise ValueError('Unpinned glyph evidence')
    report=json.loads(gzip.decompress(raw));folder=ROOT/'docs/handoff/20261004/session1'
    glyphs={int(g['codeHex'],16):g['unicode']for g in report['glyphs']}
    def decode_name(raw):
        result='';at=0
        while at<len(raw):
            size=2 if raw[at]>=128 else 1;part=raw[at:at+size];code=int.from_bytes(part,'big');result+=glyphs[code]if code in glyphs else part.decode('big5');at+=size
        return result
    if report['sourceExecutableSha256']!=EXE_SHA or len(report['rows'])!=64:raise ValueError('Original provenance/coverage differs')
    manifest_bytes=(folder/'source-manifest.json').read_bytes();manifest=json.loads(manifest_bytes)
    fields_bytes=(folder/'source-fields-native.json.gz').read_bytes();fields=json.loads(gzip.decompress(fields_bytes));by_fields={s['path']:{p['nativeId']:p for p in s['officers']}for s in fields['sources']}
    messages_bytes=(folder/'biography-messages-native.json.gz').read_bytes();messages=json.loads(gzip.decompress(messages_bytes));by_message={r['messageId']:r for r in messages['messages']}
    values=bytearray();text_records=[]
    def integer(n):values.extend(struct.pack('>i',n))
    def text(s):b=s.encode('utf8');integer(len(b));values.extend(b)
    integer(0x50474931);text(EXE_SHA);text(expected_sha);integer(16)
    for source in manifest['scenarios']:
        rows=[r for r in report['rows']if r['sourcePath']==source['sourcePath']]
        if len(rows)!=4 or any(r['sourceSha256']!=source['sourceSha256']or r['sourceVariant']!=source['sourceVariant']for r in rows):raise ValueError('Mixed original source')
        for k in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(source[k])
        integer(4)
        for row in rows:
            f=by_fields[row['sourcePath']][row['nativeId']]
            if f['recordSha256']!=row['recordSha256']or f['serializedNameRawHex']!=row['nameRawHex']:raise ValueError('Text/identity source differs')
            courtesy=f['names']['courtesy_name'];unknown=[]
            try:courtesy_text=decode_name(bytes.fromhex(courtesy['rawHex']))
            except UnicodeDecodeError:courtesy_text='';unknown.append('courtesyNameGaiji:'+courtesy['rawHex'])
            selector=f['biographySelector'];biography='';rendered=''
            if selector.get('resourceNumber')!=2 or selector.get('messageId')not in by_message:unknown.append('biographyResourceIndex')
            else:
                msg=by_message[selector['messageId']];rendered=msg['renderedSha256']
                for span in msg['spans']:
                    if span['kind']=='text':biography+=span['text']
                    elif span['kind']=='unknown_glyph':
                        try:biography+=decode_name(bytes.fromhex(span['rawHex']))
                        except UnicodeDecodeError:biography+='[undecoded original glyph '+span['rawHex']+']';unknown.append('biographyGaiji:'+span['rawHex'])
                    elif span['kind']!='format':raise ValueError('Unexamined original biography control')
                if msg['unknownControls']:unknown.append('biographyUnknownControls')
            unknown.append('activeResourcePriority')
            for key in ['officerId','nativeId','canonicalOfficerId','birth','sex']:integer(row[key])
            for v in [row['sourceName'],row['nameRawHex'],row['recordSha256'],courtesy_text,courtesy['rawHex'],biography,messages['resourceSha256'],rendered]:text(v)
            integer(len(unknown))
            for s in unknown:text(s)
            text_records.append(dict(**row,courtesy=courtesy_text,biography=biography,biographyRenderedSha256=rendered,unknown=unknown))
    output.mkdir(parents=True,exist_ok=False);(output/'identities.bin.gz').write_bytes(gzip.compress(values,mtime=0));(output/'index.txt').write_text(sha(values)+'\n')
    (output/'build-report.json').write_bytes(json_bytes(dict(schema=1,sourceExecutableSha256=EXE_SHA,glyphProofSha256=expected_sha,sourceManifestSha256=sha(manifest_bytes),fieldsReportSha256=sha(fields_bytes),messagesReportSha256=sha(messages_bytes),binarySha256=sha(values),rows=text_records,stableRuntimeIds=True,completeGoal=False)))
    print(json.dumps(dict(rows=64,binarySha256=sha(values),bytes=len(values))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('proof',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.proof,a.expected_sha,a.output)
