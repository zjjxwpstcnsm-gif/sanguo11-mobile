#!/usr/bin/env python3
"""All corruption, partial-region and scenery-leak failures must be fatal. Non-writing."""
import copy,json
import audit_native61 as a
plan=a.ledger();after=a.MAP.read_bytes();before=a.old60(after)
assert a.apply_bytes(before,plan)==after and a.apply_bytes(after,plan)==after
cases=[(before+b'\n',plan),(after+b'\n',plan)]
for key,value in [('before','P'),('after','W'),('source',[200,0]),('source',[-1,0]),('source',[True,0]),('source',[9,41]),('region','missing'),('evidence','ESTIMATED'),('validity_evidence','UNRESOLVED'),('original_navigation_evidence','CONFIRMED_VISIBLE'),('pixel_crop',[0,0,7200,6752]),('calibration_id','unreviewed')]:
 p=copy.deepcopy(plan);p['cells'][0][key]=value;cases.extend([(before,p),(after,p)])
for edit in ['duplicate','missing','hash','reference']:
 p=copy.deepcopy(plan)
 if edit=='duplicate':p['cells'].append(p['cells'][0])
 elif edit=='missing':p['cells'].pop()
 elif edit=='hash':p['after_sha256']='0'*64
 else:p['reference']['sha256']='0'*64
 cases.append((before,p))
for raw,p in cases:
 try:a.apply_bytes(raw,p)
 except(AssertionError,IndexError,KeyError,TypeError):pass
 else:raise AssertionError('Corrupt/nonreviewed/incomplete input accepted')
s=json.loads((a.DIR/'scenery-cells.json').read_text());resource=a.EXTERIOR.read_bytes();u=json.loads((a.DIR/'remaining-void.json').read_text())
a.validate_exterior(after,s,resource,u)
ex=[]
for key,val in [('source',[9,41]),('source',[35,14]),('source',[200,0]),('kind','WATER'),('boundary_basis','UNRESOLVED'),('boundary_evidence','UNRESOLVED'),('current','Q'),('region','missing'),('evidence','UNRESOLVED')]:
 x=copy.deepcopy(s);x['cells'][0][key]=val;ex.append((after,x,resource,u))
x=copy.deepcopy(s);x['cells'].append(x['cells'][0]);ex.append((after,x,resource,u))
x=copy.deepcopy(s);x['cells'].pop();ex.append((after,x,resource,u))
x=copy.deepcopy(s);x['map_sha256']='0'*64;ex.append((after,x,resource,u))
x=copy.deepcopy(u);x['unresolved_cells'].pop();ex.append((after,s,resource,x))
x=copy.deepcopy(u);x['original_enabled_hex_table_obtained']=True;ex.append((after,s,resource,x))
ex.extend([(before,s,resource,u),(after,s,resource+b'outside.35.14=SEA\n',u),(after,s,resource+b'outside.0.0=ARID\n',u),(after,s,resource.replace(b'revision=61',b'revision=60'),u)])
for args in ex:
 try:a.validate_exterior(*args)
 except(AssertionError,IndexError,KeyError,ValueError,TypeError):pass
 else:raise AssertionError('Scenery leaked into effective/unknown/old/corrupted map')
a.audit(after)
print('REFERENCE61 GUARDS PASS:',len(cases)+len(ex),'negative cases; preimage/postimage/idempotency, terrain/evidence/region, exterior identity, unknown/padding exclusion')
