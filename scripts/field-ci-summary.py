#!/usr/bin/env python3
"""Publish only bounded game-test metrics/previews; never emit arbitrary logcat secrets."""
import re,sys
for line in open(sys.argv[1],errors='replace'):
 for tag in ['FIELD_PREVIEW ','FIELD_STRESS ','SceneAcceptance:','Sanguo3D:','MapRenderer:']:
  if tag in line:print(line[line.index(tag):].strip());break
