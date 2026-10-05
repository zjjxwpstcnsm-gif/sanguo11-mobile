#!/usr/bin/env python3
"""Use the unchanged ETC2/mipmap pipeline for the dedicated v127 atlas."""
from pathlib import Path
source=Path(__file__).with_name('compress_atlases.py').read_text()
source=source.replace("[('sites','atlas.png'),('field','atlas.png'),('field','unit-atlas.png')]", "[('field/v127','scenery-atlas.png')]")
source=source.replace('docs/3d/texture-compression.json','docs/native-pc-visual/feedback-v127-texture-compression.json')
exec(compile(source,'existing ETC2 encoder','exec'))
