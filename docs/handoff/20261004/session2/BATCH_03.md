# 批三：逐次事实、阶段队列与原HUD33实装验证

目标仍active，未完成全部头像与音频还原。本批基点19392c8，媒体实现经f59a758、affafbc、373965c、d3769d0；实际新组合包源9ff6d93，最新PCM工具f11735d。所有增量父提交、确切路径、前后SHA见fact-incremental-guard.json。c02c9d6独立提交测试manifest注册，去除重复旧runner；没有修改全局Gradle配置、MainActivity、MapHost、TurnPlayback、core/API/runtime、人物metadata或公共数据manifest。529个保护文件仍与完整8400301交接一致。

TechniqueFactQueue仅接收实际GameEvent，按完整StateToken和fact.id对应的提交/序列去重，验证整批后入队。普通refresh只更新baseline，事实模式与NET模式互斥。两个正负变化即使总值相同也保留；按精确presentationParentId释放，后阶段不能越过前阶段。后台丢弃瞬态，暂停保留顺序，跳过取消尚未开始的frame及同阶段队列。已结束而未提交的阶段记为skip，迟到事实静默。事实队列或阶段历史超容量会清瞬态并显式要求resync。跨sessionId恢复与同revision关闭按真实GameSession契约处理，不重播旧事实。没有World、命令、earn、存档写入或规则RNG依赖。

待集成的三个公共文件补丁是fact-host.patch，源2910997516cec8aad4d0564688f6090e944eddc8，patch SHA f0aa1998712d3b8ad64f472d33bbf962751aa8c493be2675433de24234ad3a10。各前后像见fact-host-guards.json。在独立out目录从该提交读取人物页面和API/runtime源码，与自有媒体类编译通过，见fact-host-compile-guards.json；未取用另一会话WIP。补丁尚未应用，目的地有更新时必须重新守卫/生成，不能覆盖新人物页面。

新包out/media/fact-frozen-06/app-debug.apk为87636917字节，SHA c90f8f51c326248979dccbd3bc27f8af833a21e2504f39d0623184543766092b。测试包2027499字节，SHA d8310f25684b7f2c4748adfb28895a87592504a7d453886bda182d4f4a475b08。完整构建成功后冻结两包，再实际安装并读回SHA；前面受后续源修改影响的构建没有用于验收。

| 检查 | 结果与边界 |
| --- | --- |
| Java队列 | 44项，包括真实零净变化、恢复更换sessionId、同revision关闭及完整Save/RNG |
| 新包正常3D HUD专项 | 42项：真实Activity命令两次编辑产生两个不同journal阶段；四次真实城池修复覆盖准确阶段、后台、暂停、跳过；完整控制Save/RNG一致 |
| 新包PCM | 一个原HUD33样本检出四次独立播放，44100Hz；波形联合相关性0.9999996025，解释能量0.9999992051 |
| 新包音频生命周期 | 51项：音量/静音、竞争焦点、Home后台、前台恢复、退出释放及完整Save/RNG；11个cue对应10个唯一样本，其中1个原PC、9个仍为移动端合成 |
| 新包Android JSON | 18项，完整token/逐事实/long超过2^53精确整数、主动snapshot与失败不补播 |
| 实际安装输出驱动C# | 52项，真实Contracts/Client源码；不是Unity Player/JNI运行证据 |
| 用户数据恢复 | 每次安装测试原auto/manual3、所有原有库/偏好路径逐字节一致，无新增文件；动画设置恢复原值，未操作5554录音 |

装机HUD专项临时安装了只读GameEvent订阅，并向HUD提供实际记录的journal父ID。它使用正常Activity真实规则命令和3D渲染，验证自有媒体路径，但不代表待集成公共回调已经上线。事实证据见fact-installed-events.json、fact-installed.log；实际事件为1次含两条事实的零净变化提交、4次修复、1次无事实恢复。实际播放是两次编辑、第一次修复及暂停后第三次修复，后台/跳过各修复保持静默。PCM窗口只读来自emulator-5582进程82554的既有WAV文件，未改动录音器；指纹与原窗口偏移见fact-pcm-provenance.json。四次计数由同一原样本同时拟合，不能把正负两个别名当成两种音色。工具锁定原样本SHA并拒绝单次波形冒充两次及静默输入。

前面的失败完整保留在fact-failures-retained.json：系统动画未开启时守卫停止；测试错误假定两次编辑共享阶段、随后又误判为空阶段；生命周期调用方拼错既有通过标记。后两次源审查确认World.success每次关闭独立journal阶段，按两个实际父ID改测；所有失败也恢复用户文件。PCM最初固定gain门槛不适合真实低音量，改为16-bit量化噪声以上并保持严格原样本完整波形拟合，三项正负控制通过，未调整实际录音或原样本。

本批没有提高头像运行时覆盖（仍0），没有新增已绑定BGM/人物语音或声明其它合成短声为原PC。后续继续原头像调用形态/有界加载器与已提交sourceVariant接口连接、原菜单/地图BGM场景调用、人物/兵种/战法/计略语音及其它原音效触发。公共补丁顺序集成后必须再次安装验证无临时适配器的正常完整流程，包括全人物、战斗/计略、多回合、保存退出重开及性能。只有x86_64模拟器证据；ARM真机、手机扬声器、真实Unity Player尚未验证。
