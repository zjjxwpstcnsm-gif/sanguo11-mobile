# 本批 UI 边界与继承

工作目录 /Users/paopao/.codex/worktrees/3005/sanguo11-mobile，分支 codex/pure3d-20261003。
完整 AP 4078 文件和四份 native 输入逐 SHA 核验后提交 c19de29。
原目录、旧 UI 副本和 core/game-api/game-runtime/data/unity 保持只读。
旧 UI 会话 01a0fd3c-4290-74f1-a722-a895696b7424 于接管检查时 idle，最后完成全量合回。
本批不修改共享 progress 或 PARITY_UI_CONTRACT 的同一段，不向其他会话发消息。

地块查询消费既有 TerrainPresentation.detail(detached LegacyView.draft, Hex)，这是已有只读接口。
输入真实轴坐标；输出权威 terrain、local/national/axial 与 infantryCost。界外抛 IllegalArgumentException。
接口不提交、无随机数、无事件、不改变 StateToken。UI 保留当前部队 ID、命令草稿与镜头；全存档字节验收。
若最终集成方增加平台无关 DTO，应保留以上语义与 state(sessionId,generation,revision)，由其实现 runtime。

音效仅消费 GameEvent 已提交 receipt 的 StateToken 和 TurnJournal 不可变事件 id/阶段/状态差分。
preview、失败、取消不触发命令声音；UI 点击与命令音效独立。音频使用固定素材，不使用规则 RNG。
权威 TURN_COMMITTED 提示和 journal 播放进度分开；详情字符串不参与事件类型判断。
音源为 tools/audio/generate_ui_sounds.py 的原创移动端合成，不声称 PC 音色/编号匹配。
只读检查确认原 LINK 包含 2027 KOVS，参照 vgmstream 原生解码器并验证 Ogg 页边界，
但 PC 事件绑定尚未核实，因此没有把未知语音编号冒充攻击/UI 声音。

纯3D：原 MapView 移到 androidTest 历史夹具，不进入生产 APK。必要坐标计算已在 TileGeometry、
GridWorldTransform、ScenePicking。地图失败只显示存档保留/重试，不创建可玩替代地图。
旧 cameraX/Y 世界中心沿用，sceneEnabled=false 升为 true；nativeFailure 保留显式重试，
普通进程终止的 nativeSession 标记不阻止下次加载3D。map-display 的导航/姓名/兵条偏好保留。

设备 emulator-5582，独立 ANDROID_USER_HOME/AVD、Gradle 可写缓存和测试输出。
5554/5580 未操作。独立设备使用正常 190 测试局作为种子，不复制/更改用户设备数据。
每次安装备份本设备 files/shared_prefs，结束逐字节恢复并删除本次新增测试文件，不清数据。
ARM 真机未验证；模拟器首帧/后台恢复和长期性能未通过前不宣称流畅。


## 尚需规则会话提供的性能接口

现存 captureSave/legacyView 必须在 serial logic owner 调用，UI 不将 World/RNG 交给后台读写。
如需进一步减少保存、暂停与新局首次安装的主线程停顿，请由规则会话实现并冻结：
输入 expected StateToken + 请求用途(auto/manual/UI read)；输出提交状态的不可变 save bytes /
不可变显示 DTO 与实际 token；错误 STALE_SESSION/STALE_REVISION/HOST_BUSY/CLOSED/HOST_ERROR。
读取不消费 RNG、不发命令、不推进回合。字节保持当前 SaveCodec，不改格式、不自动修复旧档。
回调与销毁按 sessionId/generation 撤销，后台完成不覆盖更新的局面或用户存档。
本批未越界实现此接口，也不把主线程数百毫秒/数秒停顿记作流畅通过。

## 已观察的实装边界

同一 AP 规则 + UI08 APK，城格/己方占格24触控检查；UI13 APK继续覆盖真实运输命令、
AI产生的敌方单位与 CITY/GATE/PORT 邻接格，61.10秒流程通过，所有查询完整save/RNG字节不变。
敌方部分是由真实 core nextTurn 生成、经正式 session install 的验收夹具，独立于普通触控行军。
海岸UI08 12个地点/朝向/跨度组合、104检查通过；源Surface截图证明下邳大块补底/断面消失。
音频UI08焦点等流程34.14秒通过；实际设备混音9音色最低相关0.9976，不用play返回值代替可听证据。
这些是各自APK的证据，不能移用为后续APK的完整通过。

历史 GameSmokeRunner/ReferenceXX 中直接创建/投影 MapView 的版本专属夹具保留在androidTest，
不进入生产APK；当前 SceneInstrumentation、UiUxInstrumentation mapEdges/march 已迁3D。
全部历史2D专属版本套件尚未逐项迁移，不声明所有Android测试全绿。


声音提交/暂停补充：本地命令仅 result.ok 后消费 journal；TURN 播放仅 work.done 且 error=null 后
允许音频（分势力候选画面在完整权威提交前保持静音），不为追赶声音重演命令或 RNG。
演示暂停保留活跃 battle SoundPool stream 的位置，UI 点击仍可听；焦点丢失/后台停止全部stream。
军事建设完成从已提交回合前后公开只读 Structure.complete 比较取得，键含完整 StateToken 身份，
不是根据人读 message、HP 猜测，也不再运行建设规则。

首批加载补充：普通游戏在没有保存镜头时直接朝选中地块/本城建立首批几何；
恢复镜头在首个 setWorld 之前设置，不先为坐标0生成无用地图。
编辑器初始局部3D，可从检查页全图浏览；不再先加载全国、随后立即丢弃全国任务。
首帧提交只记一次，不能被 Surface 重建覆盖。resume_verified 仅是渲染器从门控开启到
像素检查的时间，完整用户等待另看 UIUX foreground 样本，不据此宣称流畅。
关闭系统动画时已提交命令直接进入最终演示阶段，仍播放动作声音；确认弹窗关闭期间
不依赖GPU提交，不把已提交声音当作预览声音。实装 reducedMotion02 75检查全部通过，
外层脚本该次误传了 PASS UIUX（实际标志 UIUX PASS），原始结果保留；最终包会按正确标志复跑。

编辑器实装 installed-editor04：APK29，61检查，84.32秒；包含独立草稿、真实触摸画笔、
第二指取消、七格城池选择、城市移动/撤销、AtomicFile backup恢复、库删除与固定版本旧档、
90据点自定义新局。后续首批加载优化需要在最终包复验，不移用旧包完成状态。

音效修正：PC地图中的真实暴击若没有专属全屏演出线索，不能被当成无声音事件。
未知武将暴击/成功暴击计略在动作演示35%后播放 CRITICAL；有专属源画面者仍在实际提交画面后播放。
实装 criticalAudio02：15检查，16.53秒，真实PC地图/原始能力/既有枪神特技，
实际支付的螺旋突刺、取消纯读、双击一次、伤害和RNG与直接规则参考完整存档字节一致。
只改Android测试夹具，不修改业务规则、原始能力档或数据文件。GameAudio确认非零CRITICAL流一次。

长时诊断修正：e009581 24个真实回合及24次Home的逐轮authority/auto等值检查均完成，
但最后一轮从测试线程遍历UI资源集合触发ConcurrentModificationException，整套保留FAIL，705.31秒。
新的diagnostic/frames/音频去重集合采集使用runOnMainSync取不可变字符串/计数；内存CSV每轮持久化。
没有删断言或把旧失败改成PASS，下一份同包会重新跑24回合。

恢复A/B：原有后台应用startActivity路径仍保留约4-5秒真实墙钟。
Android10源码的APP_SWITCH_DELAY_TIME=5*1000与此现象相符，推测其中包含系统的应用切换限制：
https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android10-release/services/core/java/com/android/server/wm/ActivityTaskManagerService.java
独立userResume01使用系统任务启动(am start -W)并把shell等待、主线程等待、新PixelCopy全部计入墙钟；
六轮为1174/1203/1127/1131/1135/1181ms，完整authority/RNG不变。1000ms目标全部false，不能宣称流畅。
该测试为root模拟器系统启动对照，不等同ARM真机用户点击或GPU性能。
