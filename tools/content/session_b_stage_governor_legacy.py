#!/usr/bin/env python3
"""Extract only own pinned completed Source20 save, never user backup data."""
import hashlib,tarfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ARCHIVE_SHA='16a41f0d17db20b4504998ab970939d7912e18ffcecc7cdca33b22ac57772916'
SAVE_SHA='97444678b0f456cfc555657cba30b4af5e23cc012a09a6311576988524fb3cd8'
def main():
 archive=ROOT/'docs/handoff/20261006/session-b/capacity-flow-20.tar.gz';assert hashlib.sha256(archive.read_bytes()).hexdigest()==ARCHIVE_SHA
 with tarfile.open(archive)as t:raw=t.extractfile('capacity/actual-source.sg11').read()
 assert len(raw)==2216604 and hashlib.sha256(raw).hexdigest()==SAVE_SHA
 output=ROOT/'out/session-b/governor-legacy-installed-source20.sg11';output.parent.mkdir(parents=True,exist_ok=True)
 if output.exists():assert output.read_bytes()==raw
 else:output.write_bytes(raw)
 print('PASS own actual Source20 legacy save staged at exact SHA '+SAVE_SHA)
if __name__=='__main__':main()
