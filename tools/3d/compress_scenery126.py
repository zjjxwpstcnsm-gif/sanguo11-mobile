#!/usr/bin/env python3
"""Encode only v126 scenery with the unchanged project ETC2/mipmap pipeline."""
from pathlib import Path
source=Path(__file__).with_name('compress_atlases.py').read_text()
source=source.replace("[('sites','atlas.png'),('field','atlas.png'),('field','unit-atlas.png')]", "[('field/v126','scenery-atlas.png')]")
source=source.replace("docs/3d/texture-compression.json", "docs/native-pc-visual/feedback-v126-texture-compression.json")
exec(compile(source,'existing ETC2 encoder','exec'))
