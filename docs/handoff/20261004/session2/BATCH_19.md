# 批十九：完整原普通音乐选择器及媒体只读消费

基点 a2bce320eefeec919976defff38fb7a0f9ee1537。新增 inspect_pc_music_composed_selector.py，用真实 Scenario.s11 registry、原单位/势力/城市 getter、地域 byte 表和有方向的关系位/计数，执行完整 5880e0 及 587f00/587d70/587fb0/49d670。只有 4d1900 音频后端捕获参数，条件/getter 无行为 shim；PC 只读、无 Wine、无规则命令或 RNG 调用。

4096 组合通过，覆盖己城数、地域归属、兵力 10000 两侧、关系位/计数、同异地域及四季；每组完整 3MiB 世界、1MiB 地域格表、16MiB 外部上下文和原 RNG 前后字节一致。原 dispatch 均为 [musicId,1,500,0xbf800000]。实际曲目分布：3/4/5/6 各 228、7 为 816、8 为 472、9 为 368、10 为 448、11 为 1080。无须把 sentinel -1 当用户音量。

music-composed-selector-native.json.gz SHA 7fb095cc89ed5a40805e9ce3d4c1d5fed1dfd6da887f2e6c4688b38c9903469b 保留全部输入和实际原 dispatch。组合 fixture 中城市0归属可能约束请求计数，报告保存实际原 getter 数量；不把请求0/42错误宣称为实际0/42。原共享建筑默认数据保留，受损建筑边界由批十六另行验证。本报告不等同于正常场景启动及 Android 事实投影。

新增 app-owned PcMusicPolicy/PcMapMusicDirective 与 PcMediaPlayback.sourceMapMusic：严格依原优先级选择；前置未知不跳过，原城市数超出0..42不接受，原 caller controlSlot 仅0..7；必须是同当前完整 StateToken 且其 parent 已从实际订阅到达，按 fact.id 去重。未知停止旧曲目，overflow 停止并等待 resync。音乐 repeat1/fade500，经既有共享焦点、原音乐流和用户音量线路；同 scene/track 不重复重开。没有从按钮、文案、项目 owner 或 turn 推导条件。

verify_pc_music_policy.py 将实际原 dispatch 作为 Java 期望，4096 对照加16项未知/优先级与13项 caller 边界共4125通过，music-policy-host-acceptance.json 保存源码 SHA。仅 host 逻辑证据，非实装播放证明。本批尚无正常场景事实 producer；普通 BGM、普通人物 voice 自动绑定仍0。Android构建与安装状态另记，不沿用旧包验证新代码。

本批无公共入口、全局构建配置、人物 metadata、core/game-api/game-runtime 修改；这些目录仍与已集成完成9e171f2字节一致。music-policy-incremental-guard.json 列每路径前后SHA。没有设备操作、存档写入、用户偏好修改或另一 serial 录音操作。完整目标 active。
