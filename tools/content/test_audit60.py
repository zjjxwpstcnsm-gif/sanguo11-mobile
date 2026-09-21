#!/usr/bin/env python3
"""All guard tests are non-writing; failures may not be demoted to warnings."""
import copy
import audit_native60 as a
import audit_native61 as current
plan=a.ledger();after=current.old60(a.MAP.read_bytes());before=a.old59(after)
identity=current.DIR/'inherited-release60'
assert a.digest((identity/'map-release-manifest.json').read_bytes())=="d49bc32c4be9128a22b182828d88aa1103957b20bff9ec135b03606c6092e4f5"
assert a.digest((identity/'version.properties').read_bytes())=="d531dffd14615f360bc1ae1be52ea5ed6468f4cc5a5f071efcc0ba942666210b"
assert a.apply_bytes(before,plan)==after and a.apply_bytes(after,plan)==after
cases=[(before+b'\n',plan),(after+b'# corruption\n',plan)]
for key,value in [('before','P'),('source',[200,0]),('source',[-1,0]),('region','missing'),('evidence','ESTIMATED'),('original_navigation_evidence','CONFIRMED_VISIBLE'),('pixel_crop',[0,0,7200,6752]),('calibration_id','unreviewed'),('source',[174,16])]:
 p=copy.deepcopy(plan);p['cells'][0][key]=value;cases.append((before,p));cases.append((after,p))
p=copy.deepcopy(plan);p['cells'].append(p['cells'][0]);cases.append((before,p))
p=copy.deepcopy(plan);p['cells'].pop();cases.append((before,p))
p=copy.deepcopy(plan);p['after_sha256']='0'*64;cases.append((before,p))
p=copy.deepcopy(plan);p['reference']['sha256']='0'*64;cases.append((before,p))
for raw,p in cases:
 try:a.apply_bytes(raw,p)
 except (AssertionError,IndexError,KeyError):pass
 else:raise AssertionError('Unreviewed/corrupt/occupied/wrong-region/duplicate input accepted')
a.audit(after, identity/'map-release-manifest.json', identity/'version.properties')
print('REFERENCE60 GUARDS PASS:',len(cases),'negative cases; strict regional allowlist, full hashes, preimages, reference/evidence, protected objects, idempotency and inherited ledgers')
