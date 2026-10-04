# 批十四：PC33 相位检查纠正与原 PCM 生命周期

基点182d1687。实现cf586593/fde6e48d/efd30edf，检查器76e45a01，音乐来源工具87bafac0；pc-pcm-incremental-guard.json列确切路径及前后SHA。不改core/API/runtime、人物metadata、公共入口或公共manifest；对已集成9e171f2的core/API/runtime无差异，架构边界通过。

纠正批十一至十三的PC33结论：不是已证明的播放失真。原检查器在三声叠加时取到HUD33周期波形相邻峰，偏移约±238采样（5.4ms），随后只搜索±8采样，产生约0.977的错误判定。新工具用声明原样本的独立尾段定位相位，并仍要求完整三声重建相关与PC33去干扰相关>.999、正可闻增益和原条件数限制。没有修改原录音或样本，也没有放宽验收阈值。12份历史录音复核全部通过，静音/错误采样率人工负例拒绝。pc33-phase-reassessment.json及逐轮pc33-reassessed文件保存原SHA和新输出；旧失败文件原样保留，播放回归判断撤回。原APK-FD加载或SoundPool解码问题均未被这些证据证明。

新增PcPcmEffectPlayer只负责已核验HUD33：后台验证并直接预加载原90406字节/45203帧PCM，六路MODE_STATIC，准备过程中关闭释放已建轨道；继承原音量/静音/ducking、fact.id去重和暂停/后台策略。六路PCM载荷共542436字节是代码边界，不是实测PSS。SoundEffects只读订阅真实session的WORLD_REPLACED/CLOSED停止短音效，不写规则/存档、不消耗RNG。其它九声仍明确为移动端原创合成，普通BGM/人物语音绑定仍0。

最新已安装APK：out/media/pc-pcm-build-03/frozen/app-debug.apk，283155299字节，SHA 00c18c9ce7763dbeac438ff60038d048df748eb04a3312923e04658426b44c1b；源efd30edf2790d8b08fcfd489beba02885c2faa9a。测试包SHA 1606d8f71bd1df0f15c9fce71b8e3e0db3c53a393d517b0d42ae95e00b13acfb。真实正常3D战法19项与完整Save/RNG、取消/重复提交、fact父ID通过；当次PC33完整混音相关0.9999997670、去干扰0.9999995580。

同包原PCM暂停播放头保持/恢复续播、真实world replacement停止/不重播、静音/音量0/真实focus/Home/退出释放通过。首次测试的非UI线程ready读取失败已保留（pc-pcm-effects-02），随后在测试中切到UI读取，未放宽播放器线程保护。所有新增PCM生命周期probe均标为测试探针，正常原PC33证明来自另一次实际规则战法事实，不冒充全部普通原SFX绑定。

同包共享原music2261与voice2616明确profile/track adapter：40项通过，真实联合PCM 0.9999994546、语音0.9999993906、ducking0.3499442；播放469/423ms，暂停前underrun0、排空后1。不是普通PATROL语音/BGM绑定完成。全部已结束轮次用户保存/库/偏好恢复逐字节一致，只操作5582，不清数据/不启动Wine，不操作5554录音。ARM/手机扬声器未验证。

附加原音乐49d670谓词86项原代码执行：验证读取42个原对象、比较virtual+40与491270返回的身份、匹配数>=10为真，无效势力不遍历。对象类/正常场景身份尚未证明，不擅自标作城市数，也不接入普通BGM；四个其它音乐谓词及场景/季节事实仍待闭合。

最新同包完整正常新局/656目录/656详情/保存读回/后台/退出重开通过646114项（644.77秒）：652批准来源加载原像素，4条未知明确保留；bitmap缓存16588800字节。完整Save/RNG及全部用户存档/库/偏好恢复字节一致，测试生成customOfficers/library.json已按备份恢复。首个头像loadWait3456ms，性能仍待改善；逐条等待时间及完整officer/nativeId/sourceVariant/asset连接保存在pc-pcm-normal-full-portraits.json.gz。此轮仅Scen000，不扩大为其它15来源或所有调用形态。

实际运行全应用PSS337672KiB、FD38，含3D与全人物目录/详情，不能当成音频独立PSS。AudioFlinger同PID5203看到六路45203帧原静态PCM完整预载、均inactive且各自underrun0；其它两条为原移动端SoundPool。退出释放另由安装生命周期测试证明；资源快照是运行中观察，不把事后无进程输出当成性能测量。

完整目标active；其它15来源正常流程、全人物形态/MOD优先级、普通原BGM/全部语音/其它原SFX事件绑定、多回合及ARM证据仍待完成。
