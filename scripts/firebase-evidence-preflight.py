#!/usr/bin/env python3
"""Read-only Firebase evidence recovery and current capacity/quota inventory.
No API enablement, billing mutations, quota changes, or test submissions.
"""
import datetime, json, os, pathlib, subprocess, sys, urllib.error, urllib.parse, urllib.request
out = pathlib.Path('out/firebase-preflight'); out.mkdir(parents=True, exist_ok=True)
project = os.environ['FIREBASE_PROJECT_ID']
started = datetime.datetime.now(datetime.timezone.utc)
(out/'queried-at.txt').write_text(started.isoformat()+'\n')

def command(name, args):
    p = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    (out/name).write_text(p.stdout)
    (out/(name+'.stderr')).write_text(p.stderr)
    (out/(name+'.exit')).write_text(str(p.returncode)+'\n')
    return p

token = subprocess.check_output(['gcloud','auth','print-access-token'], text=True).strip()
def get(name, url):
    try:
        req = urllib.request.Request(url, headers={'Authorization':'Bearer '+token})
        with urllib.request.urlopen(req, timeout=60) as r:
            data = r.read(); code = r.status
    except urllib.error.HTTPError as e:
        code=e.code; data=e.read()
    except Exception as e:
        code=0; data=json.dumps({'error':str(e)}).encode()
    (out/(name+'.json')).write_bytes(data)
    (out/(name+'.http')).write_text(str(code)+'\n')
    print(name, 'HTTP',code)
    try: return json.loads(data)
    except ValueError: return {}

meta=get('project','https://cloudresourcemanager.googleapis.com/v1/projects/'+project)
number=meta.get('projectNumber', project)
get('billing','https://cloudbilling.googleapis.com/v1/projects/'+project+'/billingInfo')
get('quota','https://serviceusage.googleapis.com/v1beta1/projects/'+str(number)+'/services/testing.googleapis.com/consumerQuotaMetrics?view=FULL&pageSize=200')
get('first-matrix','https://testing.googleapis.com/v1/projects/'+project+'/testMatrices/matrix-2qh8xls7v829b')
command('models.json',['gcloud','firebase','test','android','models','list','--project='+project,'--format=json'])
command('capacities.json',['gcloud','firebase','test','android','list-device-capacities','--project='+project,'--format=json'])
for kind in ['allocation/usage','rate/net_usage']:
    query={'filter':'metric.type="serviceruntime.googleapis.com/quota/'+kind+'" AND resource.labels.service="testing.googleapis.com"',
           'interval.startTime':(started-datetime.timedelta(hours=36)).isoformat(), 'interval.endTime':started.isoformat(),'pageSize':'10000'}
    get('usage-'+kind.replace('/','-'),'https://monitoring.googleapis.com/v3/projects/'+project+'/timeSeries?'+urllib.parse.urlencode(query))
# Save every history/execution page; project-wide test usage is not inferred from this repo alone.
base='https://toolresults.googleapis.com/toolresults/v1beta3/projects/'+project
histories=[]; executions=[]; page=''; i=0; usage_complete=True
while True:
    d=get('histories-'+str(i),base+'/histories?'+urllib.parse.urlencode({'pageSize':100,'pageToken':page}))
    usage_complete &= 'error' not in d
    histories.extend(d.get('histories',[]));page=d.get('nextPageToken','');i+=1
    if not page:break
for h in histories:
    hid=h['historyId'];page='';i=0
    while True:
        d=get('executions-'+hid+'-'+str(i),base+'/histories/'+urllib.parse.quote(hid,safe='')+'/executions?'+urllib.parse.urlencode({'pageSize':100,'pageToken':page}))
        usage_complete &= 'error' not in d
        executions.extend(d.get('executions',[]));page=d.get('nextPageToken','');i+=1
        if not page:break
# Conservative rolling-24h count; do not presume a quota reset timezone.
recent=[]
for e in executions:
    if int(e.get('creationTime',{}).get('seconds',0)) >= started.timestamp()-86400:
        mid=e.get('testExecutionMatrixId')
        if mid:
            m=get('usage-matrix-'+mid,'https://testing.googleapis.com/v1/projects/'+project+'/testMatrices/'+mid)
            usage_complete &= 'error' not in m and bool(m.get('testExecutions'))
            recent.extend(m.get('testExecutions',[]))
        else: usage_complete=False
(out/'execution-usage.json').write_text(json.dumps({'queriedAt':started.isoformat(),'rolling24hDeviceExecutions':len(recent),'allHistories':len(histories),'allExecutions':len(executions),'complete':usage_complete and all((out/(n+'.http')).read_text().strip()=='200' for n in ['histories-0','quota']) and all((out/(f'executions-{h["historyId"]}-0.http')).read_text().strip()=='200' for h in histories)},indent=2))
if '--inventory-only' in sys.argv: raise SystemExit(0)
# Recover exactly the authorized historical run. Never rerun it for missing evidence.
prefix='gs://test-lab-aa4a2056a57tx-i9q99na06ix40/native-cold/36362991199-1/api35-1/'
command('first-gcs-list.txt',['gcloud','storage','ls','--recursive',prefix])
(out/'raw-first').mkdir(parents=True, exist_ok=True)
p=command('first-gcs-copy.txt',['gcloud','storage','cp','--recursive',prefix,str(out/'raw-first')])
(out/'SCOPE.txt').write_text('Read-only inventory and historical evidence recovery. No test was submitted. HTTP/command failures remain evidence gaps.\n')
if p.returncode: raise SystemExit(p.returncode)
