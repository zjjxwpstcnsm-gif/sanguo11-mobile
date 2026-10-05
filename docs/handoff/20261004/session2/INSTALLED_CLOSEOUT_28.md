# 批28安装验收截点：原枪兵战法音效推进，目标active

应用55572ae8847f18337c1f3402af1c3a7484284e35，308015016字节，SHA `fadbfeb645d3f8f76278bdb677ff3010411074fdaaa3f919cef9ab2eac836e48`。原测试包SHA40ed39036c88c4e1267a633ea7fa63bc144f5f425c322aaa0fcc372f6d6fdf06用于正常战法、动态头像与全目录；仅媒体测试追加69a4f7b4后测试包2079579字节，SHA `01af7fa59634ec21158f8a426c9153c6505ef299d82eaf9dd29b4d0de844a872`，用于生命周期/六回合/菜单/共享适配器/桥接。两个组合都实际安装并读回SHA，应用二进制相同。不是使用构建目录后来重生成的应用。

核心增量是已明确的突刺/螺旋突刺/二段突刺三个原枪兵战法sound49/78：原Shared名字/描述、同一原战法getter、已产生会心字段、原renderer及callback链闭合。原PCM保留，仅确定性22.05→44.1kHz格式转换；两次输出字节相同。正常已提交战法的两路真实PCM分别joint0.999999590/0.999999726，独立贡献均超过原0.999门槛，原HUD33逐次事实同声检出。原声替代该路径旧合成action/critical，不叠两条。取消不播、双确认一次、全Save/RNG参考一致。准确范围、样本SHA、时序限制见TACTIC_SOUND_INSTALLED_28.md及各INSTALLED_28.json。其它未核音效仍明确是移动端合成。

新增六轨短PCM池实测545964字节缓冲；90项47.12秒验证暂停头固定/续播、重复/暂停/mute/zero事件不补播、退出六条AudioTrack全释放。正常UI系统Back原sound1录音相关性0.999999676；普通完成dismiss不触发。全Save/RNG及用户文件恢复。显式生命周期调用不是新增正常战法绑定。

同应用新局/来源选择、656目录/656详情646114检查713.55秒通过，652批准原像素/4未知、652解码、LRU16588800字节；存读档/Home/退出重开通过。稳定list-400及原动态190实际3D帧经查看；非六人簡雍路径28.01秒通过，原共享190及效果层可见、4张GPU缓存和renderer替换释放。准备战斗夹具及控制暂停帧不等于完整PC时序/色彩。全来源/caller形态未扩大为已完成。

同应用六实际回合128.37秒通过；每次自动保存等于当前完整状态，Home恢复不改Save/RNG。七份完整原始Save保留session2/six-turn-authority-28，SHA和精确十进制signed long双RNG在SIX_TURN_INSTALLED_28.json/authority.csv，没有转double。PSS198225..210105KiB。额外主机JDK17往返字节检查发现每份payload仅战报gzip头第10字节0→255，容器CRC相应变化；原BattleReports.write使用GZIPOutputStream。两份gzip解压内容、其它payload字节、strategic/event RNG均一致。完整主机字节检查仍false，原捕获Save不改、不规范化；初始失败及定位输出保留out/media/save-evidence-host-28及session2/save-host-audit-28，不能当Android保存损坏或宣称主机整文件字节通过。

同应用正常菜单原2238循环/音量/耳机断开/focus/Home/退出释放117.92秒通过；firstWrite384ms、buffer16384帧、第一循环underrun0。四个预先固定10/20/30/40秒2秒录音窗口最小0.999994605，offset61597/61597/61597/60936，证明原曲实际发声。整首连续性0.435328170低于未变0.995门槛，失败WAV/log保留，不丢帧/重排/修补来通过；长音乐连续性仍未闭合。

同应用来源适配器47项22.89秒通过；未知/无效演员拒绝、实际PATROL父receipt加明确status/profile/music夹具、去重/优先级/暂停/跳过/mute/focus/noisy/Home/退出、全Save/RNG一致。真实music2261与voice2616混音music0.999998559、joint0.999999432、voice0.999999393，duck0.349965388。这是夹具适配器，不是正常PATROL原语音或地图BGM绑定。Android JSON只读桥接18项3.12秒通过；完整schema1/StateToken/逐次fact.id保留，仍无Unity Player运行证据。

每轮独占5582，所有用户保存/库/偏好逐字节恢复，原auto/manual3 SHA02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69。未清数据、未启动Wine、未写PC或原工程/会话一、未操作5554录音。core/API/runtime保持完成9e171f2字节；会话一后续完成提交没有擅自集成。只有MapHost先提交确切SHA契约后的媒体入口顺序应用，MainActivity/公共manifest/全局台账本批未改。新增源码、媒体工具、证据与SHA守卫分别提交。

正常菜单BGM绑定1，正常地图BGM/人物voice仍0；200个原soundId的盘点不等于200事件已绑定，三种枪兵战法不等于全兵种。其它武将调用形态、MOD生效优先级、全部原事件音色、长播放连续性、原完整时序/色彩、当前规则投影MusicSourceContext及voice选择、Unity Player/ARM真机/手机扬声器继续未完成。完整目标active，不能称全部头像/音频系统已还原。
