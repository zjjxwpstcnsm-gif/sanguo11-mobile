#!/usr/bin/env python3
"""Stage every original voice candidate and separate approved identity/type joins.

Two independent conversions must agree. No action names, speaker role, language
or MOD activation guesses; actor identity remains the completed metadata join.
"""
import argparse
import hashlib
import json
from pathlib import Path

ARCHIVE_SHA='e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a'
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def sha(data):return hashlib.sha256(data).hexdigest()


def stage(first,second,portrait,assets):
    a=json.loads((first/'manifest.json').read_text());b=json.loads((second/'manifest.json').read_text())
    if a['sourceSha256']!=ARCHIVE_SHA or b['sourceSha256']!=ARCHIVE_SHA:
        raise ValueError('Original archive changed')
    aa={r['resourceId']:r for r in a['entries'] if 2287<=r['resourceId']<=4283}
    bb={r['resourceId']:r for r in b['entries'] if 2287<=r['resourceId']<=4283}
    if set(aa)!=set(range(2287,4284)) or set(aa)!=set(bb):
        raise ValueError('All1997 original voice candidates required')
    rows=[]
    for resource in sorted(aa):
        row,repeat=aa[resource],bb[resource]
        fields=['oggSha256','sourceSha256','sourceOffset','sourceBytes','sampleRate','channels','decodedSamples','endGranule','pcmS16leSha256','loopStartSample','loopEndSample','peakS16','rmsDbfs']
        if any(row[k]!=repeat[k] for k in fields):raise ValueError('Independent conversion metadata differs')
        original=(first/row['asset']).read_bytes()
        if original!=(second/repeat['asset']).read_bytes() or sha(original)!=row['oggSha256']:
            raise ValueError('Independent original voice bytes differ')
        if row['sampleRate']!=44100 or row['channels'] not in (1,2) or row['endGranule']<1:
            raise ValueError('Unexamined voice timeline/format')
        asset='audio/pc/voices/'+str(resource)+'.ogg';target=assets/asset
        target.parent.mkdir(parents=True,exist_ok=True)
        if target.exists() and target.read_bytes()!=original:
            raise ValueError('Existing voice differs; explicit replacement required')
        target.write_bytes(original)
        rows.append(dict(resourceId=resource,voiceCandidateIndex=resource-2287,asset=asset,bytes=len(original),
                         oggSha256=row['oggSha256'],originalKovsSha256=row['sourceSha256'],
                         sourceOffset=row['sourceOffset'],sourceBytes=row['sourceBytes'],sampleRate=row['sampleRate'],channels=row['channels'],
                         frames=row['endGranule'],referenceDecodedFrames=row['decodedSamples'],referencePcmSha256=row['pcmS16leSha256'],
                         referenceFrameDifference=row['decodedSamples']-row['endGranule'],
                         timelineStatus='REFERENCE_EQUALS_SOURCE_EOS' if row['decodedSamples']==row['endGranule'] else 'REFERENCE_DIFFERS_SOURCE_EOS_ORIGINAL_DECODER_PENDING',
                         loopStartFrame=row['loopStartSample'],loopEndFrame=row['loopEndSample'],loopBasis=row['loopBasis'],
                         peakS16=row['peakS16'],rmsDbfs=row['rmsDbfs'],normalization='NONE_ORIGINAL_OGG_BYTES',
                         actionBinding=None,speakerBinding=None,status='ORIGINAL_RESOURCE_NATIVE_INDEX_VERIFIED_ACTION_SPEAKER_UNBOUND'))
    pixels=portrait.read_bytes();approved=json.loads(pixels);identities=[];seen=set()
    if approved['schema']!=1 or approved['sourceFaceSha256']!='5e6a69ac3555910196465a40a7d02092cc7b8e7dc7de8e106eac1173b6e29519':
        raise ValueError('Unverified original actor join')
    for row in approved['identities']:
        key=(row['officerId'],row['nativeId'],row['sourceVariant'])
        if key in seen or row['voiceTypeRaw'] not in range(8):raise ValueError('Invalid/duplicate original voice type identity')
        seen.add(key)
        identities.append({k:row[k] for k in ['officerId','nativeId','sourceVariant','sourcePath','sourceSha256','recordSha256','identityStatus','voiceTypeRaw']})
    if len(identities)!=10656:raise ValueError('Approved source join count changed; re-audit required')
    root=dict(schema=1,sourceArchive='Media/san11pkres.bin',sourceArchiveSha256=ARCHIVE_SHA,sourceExecutableSha256=EXE_SHA,
              nativeVoiceIo='4d2380 ->resource2287+voiceId+(996 if alternateRaw and voiceId>=5 else0)',
              validNativeVoiceIds=[0,1000],alternateOffset=996,alternateThreshold=5,
              repeatConversionsByteEqual=True,totalBytes=sum(r['bytes'] for r in rows),voices=rows,
              limits=['1997 source candidates, not1997 known actions or speakers.',
                      'alternateRaw is the original manager+0x30 input, not a proven language/character-sex choice.',
                      '172 PyAV decoded frame counts differ from original Ogg EOS; both are retained, not padded/trimmed.',
                      'PyAV integer PCM reference; Android codec timeline/hash and real playback require separate evidence.'])
    identity_root=dict(schema=1,sourceExecutableSha256=EXE_SHA,approvedMediaManifestSha256=sha(pixels),
                       metadataCommit=approved['metadataCommit'],metadataRequestSha256=approved['metadataRequestSha256'],
                       voiceTypeFieldOffsetHex='0x100',voiceTypeBasis='Original152-byte actor deserializer48b7b7..48bb28; consumed by original4d0010',
                       identities=identities,limits=['Exact saved officerId/nativeId/sourceVariant/path/sourceSha/recordSha required.',
                                                     'No name matching, default variant, action or speaker role inference.'])
    (assets/'audio/pc/voice-manifest.json').write_text(json.dumps(root,ensure_ascii=False,indent=2)+'\n')
    (assets/'audio/pc/voice-identities.json').write_text(json.dumps(identity_root,ensure_ascii=False,separators=(',',':'))+'\n')
    print(json.dumps(dict(voices=len(rows),approvedJoins=len(identities),voiceOggBytes=root['totalBytes'],repeatByteEqual=True)))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('first',type=Path);p.add_argument('second',type=Path)
    p.add_argument('--portrait-manifest',type=Path,required=True);p.add_argument('--assets',type=Path,required=True)
    a=p.parse_args();stage(a.first,a.second,a.portrait_manifest,a.assets)
