# Custom portrait media boundary24

Architecture check identified an unintended new direct core consumer in PortraitMediaSources after custom override rejection was added. Keep that DTO-only class free of core references. Move the existing immutable custom snapshot read into already-reviewed OfficerPortrait, then change only MapHost criticalSource helper to OfficerPortrait.presentationSource. No allowlist or global configuration changes.

Exact MapHost before SHA256 `349e236a8d1023a3533fa2c3f8dab8fb09f375a7e2512c0930fe136ce72fe1f9`, after `145c2eb5d3ed77e49d72c63d8a23ebf4323f52c1994fc4f1a451aa67805d79dc`, patch `4a73694d0b6d43627fc9498094c22624e3d0434e29934a6826276b3d9937709d`. Candidate/guards `out/media/dynamic-portrait-custom-boundary-24/`. Apply sequentially in the independent integration workspace after this contract commit. Rebuild before installation; build24-final is superseded and will not be used.
