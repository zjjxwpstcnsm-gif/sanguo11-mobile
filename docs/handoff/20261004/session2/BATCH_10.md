# 批十：全量原语音资产、人物类型连接和安卓解码边界

完整目标active，尚未完成。继承9df65dc7；资源/目录/共享解码器5405915c，严格EOS与原语音索引拒绝战法ordinal推断7bec08f8。core/game-api/game-runtime和人物metadata与已完成9e171f2继续字节一致，不合其它人物WIP，不修改MainActivity/地图规则/人物文字数值/共享manifest。

stage_pc_voices.py逐字节比较两次独立原转换，包内新增全部资源2287..4283的1997个原Ogg候选，合计24291347字节。保留原KOVS SHA、归档偏移/字节、Ogg SHA、原EOS granule、PyAV解码计数/PCM SHA、采样率/声道、循环点、原增益下参考PCM peak/RMS依据。voice-manifest.json和voice-identities.json为独立音频文件，未写人物权威metadata；10656个已批准officerId/nativeId/sourceVariant/path/sourceSha/recordSha连接原actor+0x100 voiceType，拒绝身份变化/未知/错SHA。全部1999个新增音频文件再次staging与生产assets字节一致。不是1997个已知动作，也不按姓名或男女猜声音包。

首次staging采用音乐的PyAV帧数=原EOS前提，被172个语音反例正确拒绝；没有跳过这些声音。150个参考计数少128帧、22个多897..996帧；保存两套计数和明确未解决状态。最初观察包5405915c对原始解码输出只测量、不裁/补；其全部1997个实际Android输出恰好等于原EOS。随后生产PcVoiceCatalog/共享PcVorbisDecoder恢复严格EOS检查，任何帧数不同均失败，不做padding/trimming。

PcAudioSource只抽出不可变原音频描述，原音乐保留原30资源/循环/严格时轴；PcVoiceCatalog在媒体worker读清单、按原4d2380 voiceId0..1000与alternateRaw条件选择2287+id+996（前5共用），alternateRaw的语言/用途未知，不命名为中文或性别。原人物连接是保存身份精确核验，只返回原voiceType，不生成动作或发言者、不写规则/RNG/保存。当前仍没有正常语音播放入口。

实际5582两次完整安装包解码：第一观察包0ceeb585...，27316项/1997源，156.17秒；严格生产包源7bec08f87d728b15987708606100067cbfe0dc61、283144775字节、SHAe0fa3eda0898ca0966bc8f9ac19ead453f731d8b42c5d3ccf3371b86470344b3；test2056623字节，SHA4ff1f88f5e85243201a42310a8e2e022d0ef970ae162e332aa1af49737f2cde8。冻结后实际安装和读回SHA、全部原音频assets字节核验。第二轮27317项/1997源，157.63秒；两轮所有1997个PCM SHA/帧数相同，全部等于原EOS，完整authority Save/RNG不变。全部候选包含1992 mono及5 stereo，44100Hz。

第二轮在进程退出之前每100样本测资源：FD始终47，原生线程16..17；初次目录/JSON分配PSS172120KB，稳定期48918..54401KB，结束52660KB；无逐次codec句柄/线程累积，当前PCM与.part/目录逐份释放。解码耗时median64ms、p95 130ms、max441ms；这是串行解码探针延迟，不能当正常事件到扬声器延迟，也不是正常3D长期内存。

全部Android PCM整数hash与PyAV不同，不能宣称原PC native PCM字节相同。保留2296完整实际PCM，与报告SHA守卫核对；实际20507帧、参考20379帧。两个声明的重叠诊断表明起点对齐时前20379帧相关0.9999999816160069、最大差1 PCM步；平移128帧相关仅0.2617。安卓额外尾部128帧均非零，peak30，原样保留，没有以首部补零弥合。该诊断不修改任何样本、不接受PC原解码器/扬声器或其它1996参考对照；工具compare_android_voice_pcm.py保留可复核路径。

同两个包音乐30资源/原循环读取/全部Save/RNG复验分别103.89与133.62秒通过，共享解码描述未改变音乐帧/循环字节。每轮独占5582，原两保存/全部库/偏好恢复字节一致，只移除当前测试生成的voice-source.json及自己PCM；不清数据、不操作5554录音。ARM/手机扬声器无设备证据。

原语音记录上游继续执行：5038e0..503940进入点位于4721d0之后（该调用/规则/RNG未执行），明确raw输入0/1/2使构造器第10参数为12/11/11；原4fe455..4fe4f7将参数1/2/10写到record+0/+4/+0x98。六项原捕获/字段验证两次字节一致。**批九旧tacticIndexRaw标签是未命名13路speech selector，未证明等于War/Army战法ordinal**。MEDIA_INPUT_CONTRACT.md已经纠正，不以SPIRAL.ordinal()接声音；同sideRaw选择发言侧与反馈的证据继续有效。只读契约需要真实已提交动作/结果/发言角色及state/id，不要求core保存音频编号，不自行生成规则事件。

未完成：1997原样本虽已完整打包/解码，正常voice播放仍0；13/58原语音动作语义、上游侧/槽与真正已提交事实未闭合；原正常BGM场景与完整谓词、其余9原SFX、统一focus/noisy/优先级/ducking/音量及完整播放生命周期、全部人物形态/MOD/其它15来源正常流程、组合多回合保存退出/ARM。当前仅PC33有正常事实和实际混音还原证据，不将解码/目录/性能探针当正常系统还原。

同严格新包e0fa3eda...正常656目录/3详情、实际保存读回、后台、退出重开324153项PASS97.05秒；公开正常3D战法取消/双确认/实际提交事实/完整Save和RNG对照19项PASS17.31秒。当次PCM原PC33去两个已声明移动端合成干扰相关0.9999995446479859，联合0.999999758584035。所有本批run均恢复原两用户保存及全部库/偏好字节，原SHA02ddb3d...，没有借旧APK或旧混音扩大验收。上批全部原像素运行证据按其旧包范围保留；本批所有新音频包内字节与源一致。
