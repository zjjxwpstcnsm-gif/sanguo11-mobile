#!/usr/bin/env python3
"""Non-writing regression of new and inherited strict preimage contracts."""
import copy
import audit_native59 as a
import audit_native58 as old
plan=a.ledger();after=a.MAP.read_bytes();before=a.old58(after)
assert a.apply_bytes(before,plan)==after and a.apply_bytes(after,plan)==after
assert a.transform(after,plan,True)==before
cases=[(before+b'\n',plan),(after+b'# altered baseline\n',plan)]
for key,value in [('before','P'),('source',[200,0])]:
    p=copy.deepcopy(plan);p['cells'][0][key]=value;cases.append((before,p))
p=copy.deepcopy(plan);p['cells'].append(p['cells'][0]);cases.append((before,p))
p=copy.deepcopy(plan);p['after_sha256']='0'*64;cases.append((before,p))
for raw,p in cases:
    try:a.apply_bytes(raw,p)
    except (AssertionError,IndexError):pass
    else:raise AssertionError('Corrupt/different/duplicate/per-cell mismatch accepted')
p58=old.ledger();before57=old.transform(before,p58,True)
assert old.transform(before57,p58)==before
for raw in [before57+b'\n',before57.replace(b'terrain.199=',b'terrain.199=Z',1)]:
    try:old.transform(raw,p58)
    except AssertionError:pass
    else:raise AssertionError('Inherited v058 corruption check lost')
assert len(plan['cells'])==109 and len(p58['cells'])==89
print('REFERENCE59 APPLY GUARDS PASS: exact pre/post, duplicate/outside/before/hash/corruption failures; full idempotency; inherited 89-cell ledger unchanged')
