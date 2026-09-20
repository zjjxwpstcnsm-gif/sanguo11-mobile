#!/usr/bin/env python3
"""No filesystem writes: guarded, invertible and idempotent map update contract."""
from audit_native58 import *
p=ledger();post=MAP.read_bytes();before=baseline57_bytes(post)
assert transform(before,p)==post
assert transform(post,p,True)==before
# Corrupt a baseline outside the reviewed corrections: must not accept or overwrite it.
corrupt=before.replace(b'terrain.199=',b'terrain.199=Z',1)
try:transform(corrupt,p)
except AssertionError:pass
else:raise AssertionError('Corrupted baseline accepted')
# A correct whole-file digest with an incorrect per-cell before is also rejected.
import copy
bad=copy.deepcopy(p);bad['cells'][0]['before']='V'if bad['cells'][0]['before']!='V'else'M'
try:transform(before,bad)
except AssertionError:pass
else:raise AssertionError('Per-cell before mismatch accepted')
print('REFERENCE58 APPLY GUARDS PASS: exact parent/post-image, per-cell before, corruption rejection, deterministic inverse')
