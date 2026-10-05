# 批28：正常三种枪兵战法原sound49/78接入候选

基点95f40f8065f14a7fc515729384d6c9a798220052。仅头像/音频所有权：新增两个原PCM资源的独立manifest/转换工具、PcTacticPcmPlayer/PcTacticSoundPolicy、既有PcPresentationPlan不可变事实选择、SoundEffects与媒体验收测试。MapHost唯一共用入口先以d84db56c提交确切SHA/patch契约后在自有integration目录顺序应用，见TACTIC_SOUND_HOST_PATCH_28.md/json。规则/API/runtime、metadata、MainActivity、公共manifest和全局台账不改；原目录/会话一不动。

来源闭合：原Shared serializer逐native skill39/41/42/48姓名/描述和战法0..31执行原490c90 getter；同一constructor第三参数指向table84858真实战法对象。原描述分别勇將/鬥神/槍神/霸王明确“戰法成功…會心一擊”，原5ae610检查相应raw技能并返回已产生会心值，由5aff75放record0；已失败的原record54路径将record0清0。只执行readonly getter和已有结果的字段写入，未进入原命中/会心计算或RNG。160案例两次字节一致，结合批27完整原record→renderer240→callback564cb0，证实sound49无已施展会心、sound78已施展会心的动作分支。

早期把通用文字池指针距当技能编号41槍神的推断已撤回，没有进入代码/manifest。实际serializer目录明确41鬥神、42槍神、48霸王；原严格Big5说明、读取offset/recordSHA和原指令均保留。第一源工具的push地址偏差失败日志保留，修正为原5ae696，没有替换原分支或改变保护。

仅明确THRUST/SPIRAL/DOUBLE_THRUST（原突刺/螺旋突刺/二段突刺，native0/1/2）、已建立source3D、已提交immutable journal事实可接音。声源不依赖人物名字/动态画像，不问技能或掷随机数；CriticalHit必须与实际actorId和既有战法匹配。其它不明战法/普通攻击/计略/舰船/设施不借用这三路。已提交会心事实只决定49/78，不改变任何源规则公式，也不声称原会心概率已完整移植。

样本原bank4 slot15/53，header2284/wave2283，22050Hz mono PCM16，原分别22675/22822帧；原描述/PCM/WAV SHA完整保留。可复现libswresample22.05→44.1kHz s16 mono仅格式转换，完整flush、帧数精确加倍，不调响度/裁切/补帧；两次5文件字节一致。原输入WAV与转换后的播放WAV都入独立媒体资产目录。六个短PCM AudioTrack（三个每样本）worker准备，SHA/尺寸/PCM格式检查，已有gain/暂停/焦点/后台/读档/退出统一释放。

原单次回调不应同时叠旧合成tactic/critical。MapHost前奏及后续动作统一tactic-source去重ID；原全屏实际GPU提交后/既有动作0.35进度为当前Android时点，reduced motion同样消费一次。原callback完整state-entry和PC壁钟仍待核，不把这些移动端阶段宣称PC时序完成。取消/失败提交无已提交事件；已提交但战法未命中的原动作仍可发49，49不是成功提示声。声音不消费NET和逐次奖励双线路，PC33事实路径保持。

主机原callback向量已对实际production数字政策22检查/6绑定向量（含明确unbound其他action17等）。新包实际正常3D两种原音/PC33 PCM、取消/双提交/生命周期、全Save/RNG和数据恢复另行验收；未安装不能称完成。地图BGM/人物正常voice/全SFX/全部caller/MOD覆盖/原完整时序及ARM仍未完成，目标active。
