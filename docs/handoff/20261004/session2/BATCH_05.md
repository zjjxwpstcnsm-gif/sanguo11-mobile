# 批五：首PCM起播的原音乐流

目标active，当前正常场景音乐绑定仍0。本批基点3df0154，播放器/解码sink提交730cbcc，单独runner注册53e023b，驱动23fa59d；确切增量和前后SHA见music-stream-incremental-guard.json。529受保护源码/API/runtime/metadata/JNI仍与8400301完整交接一致。MainActivity、MapHost、TurnPlayback、公共人物manifest、共享台账未改。人物会话新完成9e171f2已公开保存来源连接；仅读取已提交契约/DTO，本批没有取用或合入其WIP，下一步以该只读三元组闭合头像。

PcVorbisDecoder新增受控PCM sink，原压缩字节验证后，每块原PCM写入自己的临时缓存，同时送入AudioTrack。PcMusicStreamPlayer使用单一后台worker、64KiB decoder工作数组、至少16KiB输出buffer，UI调用只调整控制或取消，不等待整曲解码。EOS后从已完成缓存的原loopStart或明确repeat=true的0位置继续，同一AudioTrack不中断；one-shot等待真实播放头消费所有原帧后释放。原音频数据不改增益/不归一化/不重采样；系统volume、500ms开始fade、focus duck与voice duck经AudioTrack增益控制。场景identity/资源/repeat/fade来自外部已核实只读宿主，播放器不选菜单/地图曲目，不读规则对象，不调用命令/存档/RNG。

foreground/focus/pause保留音频位置；静音保留时间轴，恢复不回放；相同scene identity和原musicId/repeat重复调用不重启。新identity取消旧job；stop立即暂停/flush，后台释放codec/track和自己临时文件，close关worker。共享音频焦点、耳机断开广播及真实scene callbacks尚未集成，不能因这些控制接口存在就算系统验收通过。已有SoundEffects仍保留原焦点逻辑，下一步必须统一长音乐/voice/SFX宿主，避免同应用互抢焦点。

新组合包完整重建后冻结、安装、读回SHA，app-debug.apk为130400702字节，SHA0c03d8240155347097f36881228b13d27450cd79e6b3747a581d1ddfb1673f28，源23fa59d；测试包2035407字节，SHA37600cd6f027b7843c234a7ecbc36c2c611fc0df4e647bc77b3cfa508af3e79e。emulator-5582正常3D Activity中，以明确原资源2261（musicId24）的临时只读适配器实际播放，19项通过，见music-stream-installed.log。此资源的正常原调用角色仍未核实，本测试不是“地图应播24”的声明。

实际AudioTrack播放头前进，首块提交138ms，尚未完整解码整首6.42s音乐；重复directive不重启。两个原完整循环边界之后，暂停头保持固定；resume、voice duck、静音/取消静音、foreground/focus控制均保持同一stream位置；stop释放全部own cache/codec/track。新scene one-shot首块55ms，submitted/played均283136帧、loops=0，消耗完才释放。媒体操作前后完整authority Save/RNG一致。安装后全部原auto/manual3、库、偏好路径逐字节恢复，无新增文件、无stream cache残留；未操作另一serial录音。

实际模拟器PCM中检出完整原2261波形，源参考WAV/Ogg SHA由原转换manifest守卫，完整波形相关性0.9999978803。checker先降采样定位，再在原采样率精确对齐，原先仅降采样会因起始亚帧位置得到0.9699；未降低0.995门槛或修改样本/录音。见music-stream-waveform.json和music-stream-pcm-provenance.json。不是play返回值或调用次数证明；仍不是ARM扬声器、PC原生codec或实际原正常scene选曲验收。

未完成：共享focus/混音优先级/实际voice duck、耳机断开、正常菜单/地图/单挑/舌战/演示场景原绑定、自动生命周期回调及综合性能。头像像素已解码但运行时仍0；9个其它短cue仍合成，人物语音/原事件绑定未完成。公共HUD事实patch未顺序集成。持续以完整原目标推进，不能把本流专项替代全人物/正常3D战斗计略/多回合/存档退出重开组合包验收。
