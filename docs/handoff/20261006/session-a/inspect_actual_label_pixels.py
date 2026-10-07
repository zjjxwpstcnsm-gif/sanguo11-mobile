#!/usr/bin/env python3
"""Read actual screenshot pixels in declared inspected label regions; no image edits."""
import argparse
import collections
import hashlib
import json
import pathlib
from PIL import Image


def luminance(rgb):
    values = [v/255 for v in rgb]
    values = [v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4 for v in values]
    return .2126*values[0]+.7152*values[1]+.0722*values[2]


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--image', type=pathlib.Path, required=True)
    p.add_argument('--regions', type=pathlib.Path, required=True)
    p.add_argument('--output', type=pathlib.Path, required=True)
    args = p.parse_args()
    if args.output.exists():
        raise ValueError('Fresh pixel evidence output required')
    im = Image.open(args.image).convert('RGB')
    rows = []
    for region in json.loads(args.regions.read_text()):
        box = region['box']
        if len(box) != 4 or not (0 <= box[0] < box[2] <= im.width and 0 <= box[1] < box[3] <= im.height):
            raise ValueError('Region outside actual screenshot')
        counts = collections.Counter(im.getpixel((x, y)) for y in range(box[1], box[3]) for x in range(box[0], box[2]))
        ink, background = tuple(region['solidInkRgb']), tuple(region['solidPlaqueRgb'])
        if counts[ink] < 30 or counts[background] < 30:
            raise ValueError('Declared solid glyph/plaque pixels not actually present')
        a, b = luminance(ink), luminance(background)
        contrast = (max(a, b)+.05)/(min(a, b)+.05)
        rows.append({**region, 'actualSolidInkPixels': counts[ink],
                     'actualSolidPlaquePixels': counts[background], 'actualSolidContrast': contrast,
                     'solidContrastAtLeast4_5': contrast >= 4.5,
                     'antialiasShadowAndOtherPixels': sum(counts.values())-counts[ink]-counts[background]})
    report = {'screenshot': str(args.image.resolve()),
              'screenshotSha256': hashlib.sha256(args.image.read_bytes()).hexdigest(),
              'actualSize': list(im.size), 'regions': rows,
              'scope': 'Actual declared screenshot regions and solid glyph/plaque colors only; anti-alias/shadow pixels excluded, not all map labels/Android/PC/ARM readability acceptance',
              'completeGlobalReadability': False, 'wholeGoalComplete': False}
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'regions': len(rows), 'actualContrasts': [r['actualSolidContrast'] for r in rows]}))


if __name__ == '__main__':
    main()
