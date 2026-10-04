#!/usr/bin/env python3
"""Actual native page acceptance using existing complete backup/restore wrapper.

Run only after the source sweep handle has authoritatively finished. Frozen
prototype input hashes and candidate APK readbacks stay mandatory.
"""
import argparse,json,hashlib
from pathlib import Path
from types import SimpleNamespace
from verify_pc_source_opening_ui import run as source_flow
ROOT=Path(__file__).resolve().parents[2]
def run(a):
    guard=json.loads(a.source_guard.read_bytes())
    for path,digest in guard.items():
        p=Path(path)
        if not p.is_absolute()or not p.is_relative_to(ROOT/'out/session1')or hashlib.sha256(p.read_bytes()).hexdigest()!=digest:raise ValueError('Prototype input changed '+path)
    source_flow(SimpleNamespace(serial=a.serial,apk=a.apk.resolve(),test_apk=a.test_apk.resolve(),source_guard=a.source_guard.resolve(),campaign_save=a.campaign_save.resolve(),output=a.output.resolve(),external_backup=a.external_backup.resolve(),info_only=True,contest_flow=True,contest_resume=a.resume,source_opening=False,source_resume=False,source_index=0))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--apk',required=True,type=Path);p.add_argument('--test-apk',required=True,type=Path);p.add_argument('--source-guard',required=True,type=Path);p.add_argument('--campaign-save',required=True,type=Path);p.add_argument('--external-backup',required=True,type=Path);p.add_argument('--output',required=True,type=Path);p.add_argument('--resume',action='store_true');a=p.parse_args();run(a)
