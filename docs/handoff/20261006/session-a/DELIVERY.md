# 20261006 Session A 当前安装交付与继续项

共同完整基点0e7b9bc2df90249a50851baeda58c7d183ea6059；main ef413be3653820dd6449ba7f02aa60bed5b26ef5仍未变。A生产完成6ea385ff2e1e021dc5d7d519f9f6cbb2ab57ffba：共享/压缩地图CPU数组、有界缓存/释放、全势力配色与独立可读文字、大堆额外余量及地图原加法材质准备。A分支codex/map-ui-media-repair，本目录完整继承；B精确完成224751a7/cc4e7abd及454d60db/1c3b54b5顺序复制于独立组合分支codex/map-ui-media-integration，没有B WIP或冲突。6442生产路径核验见INTEGRATION_AUDIT.json。

当前实际已安装组合包：`/Users/paopao/.codex/worktrees/map-ui-media-integration/sanguo11-mobile/out/session-a/apk-3fa96530-combined/app-debug.apk`，源码完整提交3fa965308134ad68b6752881dcb213dcb8b0a608，SHA256 **3e11df1874e77a54db8645f14f6164f4c7ea939020c28110a3d27a22cc3af200**。独立构建/5554安装读回同SHA，原168输入与4JNI同SHA，新增quad-add材质另以0adff6b5…77a2c运行时SHA保护（两次编译字节一致）。旧a4ad5221只构建未安装，已被本包取代，不计算其成绩。

实际5554 API29/x86_64完整继承私有扩容AVD，非ARM：组合包Runtime512MiB，16真实来源菜单→势力/取消重试→新局→全图近景平移→人物页→Home/方向/保存读取/Activity重开PASS2729；真正新PID读回完整Save/RNG PASS79。每轮安装前完整备份9内部/3797外部，最终所有原SHA读回一致、未清数据，5554锁释放。精确保存SHA/PID/内存峰值与前后文件SHA在DELTA_BATCH7.json与out/session-a/combined-13/session.json。纯显示的完整Save/RNG/StateToken按仪器已覆盖步骤检查；方向步骤当前仅完整Save/RNG，新局/读档/新进程是明确的权威会话变化，不声称跨会话Token相等。

大堆A单独包8e866064…同样实装16来源PASS2729+冷79；普通堆A单独包5a1625ed…实际384MiB，Source7/8正常PASS475+冷79，77次采样最高305.082MiB，9/3797恢复一致。这是独立包/独立备份安装结果，不拼旧Batch成绩。Android按设备配置给大堆，不按整机8/12/16GB推断；保留-PgameLargeHeap=false普通堆构建。largeHeap不是唯一修复，原用户ARM栈仍未知。

原生MP4原文件及时间戳未改：普通堆视频78df7c5…71c050，60.12秒/1365原生样本；组合视频303d4b8…3d7f5，59.99秒/1114原生样本，无音频，设备/本地SHA一致。VIDEO_EVIDENCE.json区分原生样本与解码器CFR输出重复；H264无PTS预览不得作为原版时序证明。完整过程截图/renderer/memory.csv/原生子进程smaps_rollup在各轮out目录，graphicsPss0不等于GPU VRAM0，各峰值不相加。

组合实际16局面解码逐源路径/variant/SHA与媒体manifest连接复验10720/未知0/2892PNG；正常图像695种，仍不算所有caller/年龄/全屏像素/音媒验收。当前4JNI没有动态13入口：真实FireState状态标记已接入，原13/138 factory/stop/纹理/混合只完成取证与加法材质准备，不称原粒子恢复。声音33技巧/1取消/49of78与9工程合成标识保留；原BGM/voice/58/普通事件、长曲0.995及-22待闭合。

下一完成B批次41313d2f7e7c41003c750a10dcf72c3b43c52120/969c518829351e84d4a1a0bba5275b8ee53fbd25尚未接入本包。须逐SHA审计20路径，A消费sceneFacts/原voice metadata/applied-event同StateToken接口，原speech caller/scene/region仍未知；完成A精确冻结增量后重新构建安装。军建、13000兵、原格子火/火计、多旬/单挑舌战/媒体全流程在更新组合包必须独立实测，不能以B旧包成绩或本包地图PASS宣布全目标完成。ARM目前ADB仍未连接，已询问原OOM设备型号/API及接入条件。完整目标继续，未标完成。

---

此前阶段记录（旧APK历史，不作为本包成绩）：

# A stage delivery: installed APK and bounded evidence

Goal remains incomplete. No B WIP, Unity/AndroidGameBridge serialization changes,
or frozen JNI changes were integrated. Public ledgers were not edited.

- Branch: `codex/map-ui-media-repair`.
- Main: `ef413be3653820dd6449ba7f02aa60bed5b26ef5`.
- Complete inherited audit successor: `0e7b9bc2df90249a50851baeda58c7d183ea6059`.
- Complete source: `/Users/paopao/.codex/worktrees/2191/sanguo11-mobile`.
- Installed game APK: `/Users/paopao/.codex/worktrees/2191/sanguo11-mobile/out/session-a/apk-fd9f455a/app-debug.apk`.
- APK SHA256: `daecedbb0436e5eebd5423b4b607d7f8926b544ce411409edb4448c8c6719d1e`.
- Normal non-debuggable/R8 package, actual runtime384MiB; no largeHeap added.
- Independent private Gradle/cache/build/output; full inheritance10714 paths
  verified before work, including168 fixed inputs and4 ignored JNI inputs.

## Completed production changes

`d7221f8e`: arbitrary faction slots always obtain stable opaque fills. Text uses
a separate contrast policy for plaques, normal/disabled/selected/spanned text
and dialogs. Host audit found13 of16 sources with active zero colors before
the change; all47 slots now remain usable. Actual16-source picker/map screenshots
and47-slot button checks supplement the host proof.

`08a57f80`: lossless shared PC quarter-cell vertices, compact original-ground
surface attributes and16-pattern index cache; prune obsolete window coverage
before asynchronous construction, release retired MapHost references. Desktop
384MiB old-code OOM reproduced in SurfaceBuilder array copies; fixed repeated
full/near transitions passed, exact indexed geometry/UV/water/grid parity checked.
CPU unique arrays177286368B ->60182568B. This is not a recovered stack from the
user's ARM screenshot.

`c6327130`: real immutable FireState dots/remaining-life labels. This is a state
marker; original cell-fire particles are not integrated. Latest16-source media
identity joins10720/unknown0,2892 PNG SHA and695 normal assets at actual dates;
dynamic-age/caller host checks are not framebuffer/lifecycle/ARM proof.

`0c007271`: supplied EXE factory/controller/resource138/template13, original
positions, stop/fade, texture and blend evidence; native extension contract.

`47326188`: ordinary military MOVE uses exact clicked hex; explicit approach,
garrison, delivery reroute and saved route intent retained. B's completed rule
increment is required for actual new ordinary-city MOVE behavior. Frozen theme
and movement dependencies have exact paths/SHA in THEME_FROZEN.json and
MOVEMENT_FROZEN.json. No later rendering-throttle production patch was supplied.

## Installed evidence and limits

- Run05, same APK above: all16 real source-menu/faction/new-game/map/normal
  officer-directory paths completed;51 actual full-to-near transitions, Home
  and orientation. Complete Save/RNG/StateToken unchanged under pure presentation.
  Final early save assertion failed; overall Run05 remains FAIL.
- Run07: actual Source15 flow plus normal save3/load/Activity exit-reopen PASS314;
  saved/captured SHA equal. This is supplementary same-APK evidence, not a
  retrospective Run05 overall PASS or a new-process proof.
- Heap08: actual Source7/8 flow PASS475 in diagnostic mode. Explicit post-GC
  HPROF199444332B, host/device SHA equal,1 FilamentMapView and released preview
  MapHost field guards. Large holders are SceneMesh.vertices62877724B, direct
  buffer arrays6475188B. This does not establish the sole OOM cause.
- Cold09: first normal flow PASS314, second-stage harness failed because
  instrumentation had already terminated the process before pidof. Original
  data restored. Cold10 corrected observer PASS:314 normal checks +79 fresh-process checks,
  PID17291 ->24237, actual complete saved/runtime SHA
  `92dd0ae7ee541c6348a07e0f3b53c4ba283d8fb6c5cd4d10721bbc135878373e`.
  Original9/3797 files restored exactSHA and lock released.
- Every finished session above restored all original9 internal/3797 external
  regular files by final full-tree SHA. No clear-data or user-resource cleanup.
- 5554 is a complete private expanded clone of the original AVD, API29/x86_64,
  hostGPU; original AVD retained/stopped.5582 untouched. No ARM connected.
- Run05 sampled Java maximum402559744/402653184B (383.91/384MiB), only93440B
  remaining before GC; blocking Alloc GC observed. No OOM in that sequence does
  not prove safe ARM margin. Native/PSS/GPU estimates are explicitly separate.
- Video retains actual frames, with assigned25fps because elementary H264
  packet timestamps were lost. It is not full16-source or original timing proof.

## Reproducibility and guards

See INSTALLED_16_SOURCE_MATRIX.json, HEAP_PROFILE.json, VIDEO_EVIDENCE.json,
DELTA_BATCH5.json/DELTA_BATCH6.json for exact artifact paths, SHA, actual checks,
before/after production SHA, JNI4 and168-resource guards, original4301-path
guard and unchanged original dirty status. Conflicts: none. B WIP merged: none.

`device_session.py` owns5554 through an exclusive lock, complete archives and
fresh every-file SHA; it restores only changed/missing original members from
guarded full archives, removes only generated new files, then hashes the entire
restored tree. `inspect_heap.py` reads SDK-converted HPROF with mmap; it reports
unique array ownership within groups and explicitly excludes allocation stacks.

## Still required

User ARM allocation-stack and long normal workflow evidence; adequate Java,
native and GPU margin; B completed frozen increment and final serial combination
new APK; real movement/fire/build/multiple turns/contests plus save/fresh process
on that package. Original cell-fire native factory and additive map material,
normal map BGM/real speaker/profile event production, remaining58 and ordinary
event sounds, full/ordinary media pixel/crop/color/time/lifecycle checks and the
whole-song0.995 threshold/-22 capture diagnosis remain open. Known33 techniques,
1 cancel,49/78 hit mappings and9 explicitly synthetic engineering cues retained.
Windows Documents/Expansion remains unknown; no other copy is requested.
