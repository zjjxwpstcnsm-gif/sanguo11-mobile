#!/usr/bin/env python3
"""Prove current actual warm/cold raw save differences without ignoring any game field."""
from pathlib import Path
import hashlib,json,struct,zlib
R=Path(__file__).resolve().parents[2];E=R/'out/session-b/search-debate-current-apk-acceptance-v2/evidence';D=R/'out/session-b/search-debate-current-raw-replay';O=R/'out/session-b/search-debate-current-raw-boundary.json'
def sha(b):return hashlib.sha256(b).hexdigest()
rows=[]
for label,actual,replayed in [(f'trial-{i}',E/f'search/trial-{i}-finished.sg11',D/f'trial-{i}-replayed.sg11')for i in range(3)]+[('cold',R/'out/session-b/search-debate-current-cold-actual.sg11',D/'cold-replayed.sg11')]:
 a,b=actual.read_bytes(),replayed.read_bytes();assert len(a)==len(b)
 for raw in [a,b]:assert struct.unpack_from('>I',raw,0)[0]==0x53473131 and struct.unpack_from('>I',raw,4)[0]==39 and struct.unpack_from('>I',raw,8)[0]==len(raw)-20 and struct.unpack_from('>Q',raw,12)[0]==zlib.crc32(raw[20:])
 diff=[i for i,(x,y)in enumerate(zip(a,b))if x!=y];payload=[i for i in diff if i>=20];assert len(payload)==1;osByte=payload[0];start=osByte-9;assert a[start:start+3]==b[start:start+3]==b'\x1f\x8b\x08' and a[osByte]==0 and b[osByte]==255
 assert set(diff)<=set(range(16,20))|{osByte};assert a[20:osByte]==b[20:osByte]and a[osByte+1:]==b[osByte+1:]
 decoded=[];compressed=[]
 for raw in [a,b]:
  d=zlib.decompressobj(31);out=d.decompress(raw[start:]);assert d.eof;decoded.append(out);compressed.append(len(raw)-start-len(d.unused_data))
 assert decoded[0]==decoded[1]and compressed[0]==compressed[1]
 rows.append(dict(label=label,actualPath=str(actual),actualSha256=sha(a),replayedPath=str(replayed),replayedSha256=sha(b),bytes=len(a),differentOffsets=diff,gzipStart=start,gzipOsByte=osByte,actualOs=0,hostOs=255,gzipCompressedBytes=compressed[0],decodedBytes=len(decoded[0]),decodedSha256=sha(decoded[0]),allOtherPayloadBytesExactlyEqual=True,bothRootCrcValid=True))
assert not O.exists();O.write_text(json.dumps(dict(wholeGoalComplete=False,normalSaveAndColdByteEqualityTestedWithinAndroid=True,crossRuntimeBoundary='Only embedded report GZIP OS byte and consequent outer CRC; no game/RNG/extension/field removed or ignored',cases=rows),indent=2)+'\n');print('PASS current actual warm3/cold complete raw save boundary; only GZIP OS/outer CRC',sha(O.read_bytes()))
