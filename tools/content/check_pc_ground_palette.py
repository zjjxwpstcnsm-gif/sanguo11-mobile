#!/usr/bin/env python3
"""Compare packaged ground texels/gutters and numeric dimensions to source WFTX.

Does not invoke the atlas packer: a shared packing error must fail this check.
"""
import argparse, json
from pathlib import Path
import numpy as np
from PIL import Image
from pc_resources import Archive, sha
from import_pc_map import textures

ROOT = Path(__file__).resolve().parents[2]

def check(source, output):
    assets = ROOT / 'app/src/main/assets/3d/pc-map'
    dimension_image = Image.open(assets / 'palette-sizes.png')
    assert dimension_image.mode == 'RGB' and dimension_image.size == (36, 4)
    dimensions = np.asarray(dimension_image)
    rows = []
    archive = Archive(source / 'Media/san11pkres.bin')
    try:
        for quarter in range(4):
            raw = archive.read(4800 + quarter)
            images = textures(raw)
            assert len(images) == 36
            palette = Image.open(assets / f'palette-{quarter}.png')
            assert palette.mode == 'RGB' and palette.size == (1560, 1560)
            atlas = np.asarray(palette)
            for index, im in enumerate(images):
                pixels = np.asarray(im.convert('RGB'))
                height, width = pixels.shape[:2]
                assert width in (64, 128, 256) and height in (64, 128, 256)
                x, y = (index % 6) * 260, (index // 6) * 260
                actual = atlas[y+2:y+2+height, x+2:x+2+width]
                assert np.array_equal(actual, pixels), (quarter, index, 'changed source texels')
                # Verify all four edges and corners through source modulo address.
                yy = np.arange(-2, height+2) % height
                xx = np.arange(-2, width+2) % width
                assert np.array_equal(atlas[y:y+height+4, x:x+width+4], pixels[yy[:,None], xx]), (quarter, index, 'wrap gutter')
                assert dimensions[quarter,index].tolist() == [width//32, height//32, 0], (quarter, index, 'numeric lookup')
                rows.append(dict(quarter=quarter, resource=4800+quarter, index=index,
                                 width=width, height=height, rgb_sha256=sha(actual.tobytes())))
    finally:
        archive.close()
    report = dict(status='PASS_SOURCE_144_PALETTES', goal_complete=False,
                  checked_images=len(rows), images=rows,
                  dimensions_sha256=sha((assets/'palette-sizes.png').read_bytes()),
                  limits=['CPU texel/address verification; GPU mip behavior and PC raster parity require separate evidence'])
    output.write_text(json.dumps(report, indent=2)+'\n')
    print(report['status'], 'images=',len(rows))

if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', required=True, type=Path)
    a=p.parse_args();check(a.installation,a.output)
