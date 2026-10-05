# Media volume host patch25

Second sequential MainActivity additive entry: add app-owned music/voice sliders and explicit resume-current-music control to established sound settings before existing audition. Existing effect slider/master mute are preserved; new preference keys already exist in SoundEffects. No core/data/metadata/rule/save/RNG change.

Exact path app/src/main/java/game/sanguo/mobile/MainActivity.java, before SHA 2d454f8b3aa742f31d2e2fb3604a2da955a626a03dc9e43d326d3394cea3210e, after 89a3c972c2709f75fef9751907162ccacace16356b8325f549d5f001d90eb7dc, patch 65ec44c9b0c964d256b0a41829b947942a2fd8409158365e8c14ad43322d5bac. Candidate/guards out/media/menu-music-volume-host-25. Apply only after this contract commit, into inherited independent integration workspace. Fresh installed actual sliders/headset resume evidence required.
