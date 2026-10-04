# 批四：原音乐来源、选择器与实际Android解码

目标active，完整BGM系统尚未还原。本批从a215806继续；资源/媒体类a9ebbf0，单独测试manifest注册e9579a1，独占安装驱动7aa459b，保留单资源实际PCM的probe ef0cc1c，比较/帧测试/未知场景修正890bb77。每路径前后SHA及父提交见music-incremental-guard.json。没有修改MainActivity、MapHost、TurnPlayback、core/API/runtime、人物metadata、公共数据manifest或共享台账；529保护文件仍完全等于8400301完整交接。

原调用链4d04f0 -> 4cfc20 -> 4cf9b0 -> resource2237+musicId已追踪。inspect_pc_music_policy.py执行原5880e0选择器与4cfc20切换逻辑共318组合，原指令保持不变；只shim明确记录的只读谓词结果与音频边界。无效对象不派发，源属性96a61f0的0..3分支对应3..6，其余值在该分支默认7；情势谓词优先产生7..11。重复曲目更新gain而不重开，常见切换参数500ms，-1停止；有效0..29，其它编号拒绝。谓词587f00/587d70/587fb0/49d670的完整只读游戏条件尚需闭合，不能凭人物或强弱文字猜。96a61f0通过4825a0计算再5a2ec0设置，但实际日期参数转换仍待核对。场景调用6907a4的9c57074=1选择16、其它选择12；该flag并非已证明的PK版本标志，场景也不能先标为菜单。全部unknown保留，music-native-policy.json.gz记录边界，不属于正常场景播放验收。

30首原音乐无损unwrap Ogg共42745612字节，进入audio/pc/music独立命名空间，来源archive SHA e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a。source KOVS/ogg/PCM SHA、原偏移、44100Hz/stereo帧数与三处partial loop位置完整保留在独立music-manifest.json；没有归一化、重新编码或替换音色。30首PyAV帧数均与原EOS granule相同。APK内30 Ogg逐个读回SHA相同且ZIP_STORED，Android AssetFileDescriptor可直接使用。

PcVorbisDecoder强制在非UI线程执行，先验证原压缩字节SHA，用真实MediaExtractor/MediaCodec解码到自己的PCM临时文件；最多65536字节Java工作缓冲。格式或帧数不符明确失败，不猜padding/trim。取消、stall与异常释放codec/extractor并移除自有未完成文件。PcMusicFrameStream以原样本帧位置读取，intro仅一次、partial loop按原start、无loop元数据仅在明确repeat=true指令下从0重播；one-shot EOF不重播。没有GameApi/World/规则/存档/RNG依赖。此类还不是完整AudioTrack播放器，不能在UI等待整曲缓存后声称完成长音乐流。

首次真实安装494f0794...包通过184项；最新组合包完整重建/冻结/读回后，又全部30首通过185项，并另做2261保留PCM专项11项。最新app-debug.apk为130397278字节，SHA 00945eed35a061fb6f8f155b97e6d4acaf58f192ecfe693b8f4640f037464f6d，源ef0cc1c；测试APK2031287字节，SHA9439160b978d11846ceb0aead2559370274aa8cc851af65c9098cf68c7699201。两次全部Android解码30个PCM SHA逐字节一致，所有原时间轴及循环边界准确；每首临时文件立即删，最大单首PCM47162540字节，全部477662124字节仅顺序经过临时缓存。完整Save/RNG一致，所有用户auto/manual3、库/偏好路径安装前后逐字节一致；未操作另一serial的录音，结束无probe缓存泄漏。12项独立帧测试另覆盖完整intro、partial loop、one-shot EOF及非法边界。

30首Android OMX.google.vorbis.decoder PCM与PyAV参考SHA全部不同，不能把SHA不同比较抹去或标成PC原生解码器字节相同。实际保留的2261为283136帧，最大逐采样差1，差值分布-1:283070、0:282982、+1:220，平均-0.49949494，RMS0.70729906个PCM整数，相关性0.99999997875；只证明这一首的量化差异，不能推广到其它29首，更不能推广到PC原生codec或手机扬声器。见music-pcm-difference-2261.json。原PCM不作增益修正，不修剪或重写。初次比较临时脚本提出过任意过严相关阈值而失败，保留观察数据，正式compare_android_music_pcm.py只测量、不制造通过结论。

首次全30解码累计185.703秒，最长22.958秒；第二次188.456秒，最长18.638秒。正式BGM必须首PCM块即流式起播，并在后台建立有界缓存/循环数据；等待整曲完成会造成不可接受的起播延迟。接下来完成共享音频焦点、volume/mute、voice ducking、后台/耳机断开、暂停/读档/退出重开以及正常场景接入与真实混音。实际AudioTrack播放、场景选曲、音乐生命周期和延迟尚未通过，当前有效正常BGM场景绑定仍0，不能把本批解码/循环字节专项叫作BGM还原完成。

头像运行时覆盖仍0，原短样本仅HUD33，其它9种短声仍移动端合成；人物语音、完整原事件绑定、公共入口无适配器整合、全人物/3D战斗/计略/多回合/存档完整矩阵与ARM真机仍未完成。上一包的专项不能替新音乐组合包通过整体矩阵。下一步从真实场景身份和原条件完成音乐只读选择与流式播放器，再继续人物/形态与原语音/SFX绑定。
