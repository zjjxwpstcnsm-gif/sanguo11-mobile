#!/usr/bin/env python3
"""Import original candidate facts/results with source identity; no activation inference."""
import argparse,json,hashlib
from pathlib import Path
def export(source,output):
    if output.exists():raise ValueError('Preserve import')
    raw=source.read_bytes();r=json.loads(raw)
    assert r['exeSha']=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'and r['wholeWorldAndRngRestored']and not r['completeGoal']
    people={p['native']:p for p in r['people']}
    assert len(people)==len(r['people']) and len(r['scores'])==14*len(people)
    lines=['# Original589ac0/589ca0 receipt '+hashlib.sha256(raw).hexdigest()+' source '+r['source']['sourceSha256']]
    for person in people.values():lines.append('\t'.join(map(str,['P',person['native'],person['health'],person['war'],person['personality'],person['bonus'],int(person['ruler'])])))
    for score in r['scores']:lines.append('\t'.join(map(str,['S',score['native'],score['health'],score['allowLowHealth'],score['score']])))
    for row in r['cohorts']:lines.append('\t'.join(map(str,['C',','.join(map(str,row['nativeCrew'])),row['health'],row['allowLowHealth'],row['pickedNative'],int(row['validOriginalUnit'])])))
    output.write_text('\n'.join(lines)+'\n');print('PASS original cohort import',hashlib.sha256(output.read_bytes()).hexdigest())
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args();export(a.source,a.output)
