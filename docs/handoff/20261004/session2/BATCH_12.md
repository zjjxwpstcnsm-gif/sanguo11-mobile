# 批十二：PC33 加载与真实混音回归隔离（未闭合）

基点19bda07c，候选bb64e55767ef294937aa109ad2c5fcd80b89fc40。只改SoundEffects与新增PcEffectSourceFile，不改core/API/runtime、人物metadata或公共入口。保留失败波形，不降低PCM阈值，不变更原样本。五轮测试独占5582，安装前备份、结束后恢复全部用户保存/库/偏好字节；未操作5554录音。

同设备旧包e0fa3eda正常战法PCM通过（联合0.9999997563、PC33 0.9999995561），新共享包efcee0ad重复失败。四个相关WAV在两APK内同字节，PC33均未压缩、4字节对齐。增益诊断为0.75，voiceDucking=false、sourceMedia=false。恢复旧focus路径仍失败；不能将焦点说成已证明原因。

8b57a6e9的临时同步复制包cdd00847一次通过（联合0.9999997586、PC33 0.9999995446）。随后bb64e557把复制/SHA校验移至后台，固定缓存仅90450字节，退休SoundPool不接受晚到加载。正式APK SHA 24a60cf91d89d8b85fc4da92b0b58dbc83ddee395e18794311092dc3b61fa96b实际安装后，正常战法19项及完整Save/RNG通过，但PCM再次失败（联合0.9880843680、PC33 0.9773588717）。设备缓存SHA仍为原样本1cf7d3edbf967ac7f7123fdb2f91a54714827520abce2ced13c4a70610909f78。因此路径诊断一次通过不证明稳定修复；bb64e557标题中的fix仅为候选，不是验收结论。

同包共享播放器41项通过，实际receipt/静音/后台/优先级/暂停/系统NOISY/释放及完整Save/RNG检查通过。但此次真实PCM复核失败（music 0.99999856、joint 0.70295371、voice 0.63595946）。保留观察时间、父ID、波形和失败输出，尚未判断为播放差异或全窗口模板匹配选中别次/暂停片段。不得移用上一包PCM结果；此处仍为显式profile/track adapter，正常原BGM/voice绑定仍0。

pc33-source-loader-ab.json包含五轮波形绝对路径/SHA、实际包SHA、恢复结果和严格校验输出；逐轮JSON及四份APK manifest已提交。未编辑的原波形保留out/media对应目录。APK冻结于out/media/pc33-source-build-04/frozen，当前未通过；DELIVERY_CURRENT仍保留批十。架构检查通过，core/API/runtime对9e171f2无差异。

后续检查同一PC样本重复SoundPool加载的时序、实际decoded PCM及按观察时间定位的共享voice段。全人物形态/MOD、正常音乐/语音/其它原音效事件绑定、多回合组合及ARM/手机扬声器仍未完成，目标active。
