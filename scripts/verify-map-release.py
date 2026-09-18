#!/usr/bin/env python3
"""Verify recovered v40 artwork and authored maps in the source tree or an APK."""
from pathlib import Path
import hashlib,json,sys,zipfile
ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/'tools/content/map-v040-manifest.json'
def verify(apk=None):
    manifest=json.loads(MANIFEST.read_text())
    archive=zipfile.ZipFile(apk) if apk else None
    try:
        for entry in manifest['files']:
            data=archive.read(entry['apk_path']) if archive else (ROOT/entry['source_path']).read_bytes()
            if hashlib.sha256(data).hexdigest()!=entry['sha256']:
                raise SystemExit('Map/art release mismatch: '+entry['source_path'])
    finally:
        if archive:archive.close()
    print(f"PASS: {len(manifest['files'])} exact v40 map/art resources in {apk or 'source'}")
if __name__=='__main__':
    if len(sys.argv)>2:raise SystemExit('Usage: verify-map-release.py [APK]')
    verify(sys.argv[1] if len(sys.argv)==2 else None)
