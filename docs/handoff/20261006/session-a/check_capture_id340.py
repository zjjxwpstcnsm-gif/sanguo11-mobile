#!/usr/bin/env python3
import re,json
from pathlib import Path
from unittest.mock import patch
import device_session as ds
DOC=Path(__file__).resolve().parent
class Nonce:
 def __init__(self,x):self.hex=x
checks=0;examples=[]
for label in ['large-menu-pcm','中文长名称'*50,'x'*1000,'source14','a/b-c d!']:
 for _ in range(100):
  identity=ds.allocate_run_id(label,{})
  for suffix in ['','_init44100','_init48000','_menu','_cold']:
   assert re.fullmatch('[A-Za-z0-9_]{1,64}',identity+suffix);checks+=1
  assert len(identity.rsplit('_',1)[1])==32
 examples.append({'label':label[:30],'baseLength':len(identity),'longestSuffixLength':len(identity+'_init48000')})
for root in ['files/session-a-map/','files/uiux/','files/session-a-game-mix/']:
 first=ds.allocate_run_id('large-menu-pcm',{});nonce=first.rsplit('_',1)[1]
 with patch.object(ds.uuid,'uuid4',side_effect=[Nonce(nonce),Nonce('a'*32 if nonce!='a'*32 else 'b'*32)]):
  second=ds.allocate_run_id('large-menu-pcm',{root+first+'/original':{'sha256':'original'}})
 assert second!=first;checks+=1
report={'checksPassed':checks,'examples':examples,'fullUuidPreserved':True,'originalRootCollisionsRejected':True,'scope':'Actual helper identity generator and suffix grammar/collision checks only; no game/PCM/Android/ARM acceptance.','wholeGoalComplete':False}
out=DOC/'CAPTURE_ID340.json';assert not out.exists();out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report))
