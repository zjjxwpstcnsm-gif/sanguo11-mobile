#!/usr/bin/env python3
"""Generate the media-only raw selector tables from the executed original report.

The generated class accepts explicit native inputs. It has no event names,
officer identity inference, RNG, world access or automatic playback.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path

EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def stage(report, destination):
    raw = report.read_bytes()
    source = json.loads(gzip.decompress(raw))
    if source['sourceExecutableSha256'] != EXE_SHA or source['nativeChecks'] != 8248 or len(source['rows']) != 7952:
        raise ValueError('Incomplete native acceptance report')
    arrays = [('TYPES', [x for row in source['voiceTypes'] for x in row]),
              ('BASES', source['profileBaseVoiceIds']), ('ABILITY', source['abilityVariantTable']),
              ('FEEDBACK_A', source['feedbackVariantTables'][0]), ('FEEDBACK_B', source['feedbackVariantTables'][1])]
    if [len(a) for _,a in arrays] != [24,71,16,16,16]:
        raise ValueError('Native table extent changed')
    lines = [
        'package game.sanguo.mobile;', '',
        '/** Generated from unchanged PC selector execution. Raw inputs only; no event binding or playback. */',
        'final class PcVoicePolicy {',
        '    static final String SOURCE_EXECUTABLE_SHA256 = "'+EXE_SHA+'";',
        '    static final String NATIVE_REPORT_SHA256 = "'+hashlib.sha256(raw).hexdigest()+'";',
    ]
    for name, values in arrays:
        lines.append('    private static final int[] '+name+' = {'+','.join(map(str,values))+'};')
    lines += '''
    private PcVoicePolicy() {}
    private static boolean valid(int profile, int voiceTypeRaw, boolean actorValid) {
        return actorValid && profile >= 0 && profile < BASES.length && voiceTypeRaw >= 0 && voiceTypeRaw < 8;
    }
    /** Original4d1290: current unsigned-byte values already calculated by the committed state. */
    static int abilityVoice(int nativeProfile, int voiceTypeRaw, boolean actorValid,
                            int ability0, int ability1, int ability2, int ability3) {
        if (!valid(nativeProfile, voiceTypeRaw, actorValid) || ability0 < 0 || ability0 > 255
            || ability1 < 0 || ability1 > 255 || ability2 < 0 || ability2 > 255 || ability3 < 0 || ability3 > 255) return -1;
        int kind = voiceTypeRaw * 3;
        int superior = ability1 > ability0 && ability1 > ability2 && ability1 > ability3 ? 1 : 0;
        return BASES[nativeProfile] + ABILITY[2 * (TYPES[kind] + 4 * TYPES[kind + 1]) + superior];
    }
    /** Original4d13b0. feedbackRaw remains raw until each caller proves its semantics. */
    static int feedbackAVoice(int nativeProfile, int voiceTypeRaw, boolean actorValid, int feedbackRaw) {
        return feedback(nativeProfile, voiceTypeRaw, actorValid, feedbackRaw, FEEDBACK_A);
    }
    /** Original4d1490. Kept distinct even though this executable has equal feedback tables. */
    static int feedbackBVoice(int nativeProfile, int voiceTypeRaw, boolean actorValid, int feedbackRaw) {
        return feedback(nativeProfile, voiceTypeRaw, actorValid, feedbackRaw, FEEDBACK_B);
    }
    private static int feedback(int profile, int voiceTypeRaw, boolean actorValid, int feedbackRaw, int[] table) {
        if (!valid(profile, voiceTypeRaw, actorValid) || feedbackRaw < 0 || feedbackRaw > 1) return -1;
        int kind = voiceTypeRaw * 3;
        int override = TYPES[kind + 2];
        int variant = override >= 0 && override < 14 ? override : table[2 * (TYPES[kind] + 4 * TYPES[kind + 1]) + feedbackRaw];
        return BASES[profile] + variant;
    }
}
'''.strip('\n').splitlines()
    destination.parent.mkdir(parents=True, exist_ok=True)
    data = ('\n'.join(lines)+'\n').encode()
    destination.write_bytes(data)
    print(json.dumps(dict(javaSha256=hashlib.sha256(data).hexdigest(), nativeReportSha256=hashlib.sha256(raw).hexdigest())))


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('report', type=Path)
    p.add_argument('--destination', type=Path, required=True)
    a=p.parse_args()
    stage(a.report, a.destination)
