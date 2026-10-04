/goal
目标：继承当前《三国志11威力加强版》安卓项目全部成果，以本地PC只读资源及原调用绑定为依据，完成全部武将头像和音频系统还原，包括日常背景音乐、战法/计略语音、战斗/动效/界面音效与音乐，并在正常3D游戏流程、存档退出和实际安装包验证；不要只交解码样本或播放按钮。

项目：/Users/paopao/workspace/sanguo11-mobile
PC只读参照：/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版
最新交接：docs/handoff/20261003/STATE.json、docs/validation/parity-20261003/ROUND_40.md及docs/handoff/20261004/CLOSEOUT.md。
先检查git status、README、适用AGENTS.md、progress.md、MODULE_RULES.md、PARITY_UI_CONTRACT.md、TECHNIQUE_POINTS_EVENT_CONTRACT.md和PC_PARITY_STATUS.md。原目录HEAD只是旧元数据，必须完整继承dirty源码、未跟踪资源、168固定输入及4份被忽略JNI；禁止reset、清理、从旧HEAD开工或复制旧AA核心。建立独立完整工作目录和codex/portrait-audio-restoration分支，独立构建缓存。另一会话负责剧本/人物权威metadata及文字数值页面，不改其文件。

当前实际进度：纯3D和原资源演示已接，六名典型人物的年龄头像/战法选择有来源核验，但全部人物头像覆盖、动态lookup和完整形态未闭合。读取docs/pc-visual/scenario-portraits-source-working.json、portrait-lookup-source-working.json和tools/content/inspect_pc_scenario_portraits.py等；不要把六人覆盖扩大成全武将还原。当前11种音效为可复现原创移动端合成，实际x86_64混音已检出，绝不是原PC音色还原；日常BGM、人物战法语音及完整原事件编号绑定仍未实现/核实。tools/audio、docs/uiux/pure3d-20261003/CONTRACT.md、TECHNIQUE_HUD.md为现有边界。原技巧HUD调用音效编号33仅静态核验，不代表样本身份已知。

职责与交付：
1. 全部武将头像：审计实际生效Face及MOD/备用覆盖资源，解码原像素，记录文件/资源编号/尺寸/透明度/SHA/解码路径与稳定人物ID映射；覆盖年轻/年老/变体及普通头像、对话/单挑/战法等实际使用形态。来源身份由会话一metadata契约确定，不修改人物姓名/数值/生卒/所属；不以AI生成或相似图片冒充原资源。保留未知与多版本差异。转换两次字节一致，原年龄/变体选择有依据，所有正常列表/详情/战斗入口真正加载正确像素，检查裁切比例、内存和生命周期。
2. 音频：盘点本地音效/音乐/语音包与真实原调用/编号/触发条件，区分原版、MOD、生效覆盖及备用。完成可复现解包/解码/格式转换，保留原样本及hash/时长/采样率/循环点/响度依据。接入菜单/正常地图日常BGM及场景切换、人物/兵种/战法/计略语音、攻击/暴击/行军/建设/回合等动效音效与演示音乐；源不存在或绑定未知明确标注，不用合成提示声代替还原声明，不猜所有人共享同一语音。
3. 播放系统：区分长音乐流与短音效/语音，处理循环、音量设置、优先级、叠加/ducking、暂停/后台/音频焦点/耳机断开、读档与新局切换、退出释放和重开；避免重复播放/音频泄漏/解码阻塞主线程，实际测资源和延迟。保留现有用户偏好与关闭音效行为。
4. 只消费已提交事实/演示事件，不根据按钮点击或文案猜战斗成功，不调用规则命令/存档写入/earn，不消耗规则RNG。GameEvent/TurnJournal已有去重ID及逐次techniquePointsFacts；R40 BridgeMessage Java DTO含完整StateToken与逐次事实，主动snapshot/失败/重复receipt/restore不重播，overflow明确resync。你负责app-owned AndroidGameBridge.poll的加法JSON序列化与对应Unity契约/客户端只读消费，保持schema1兼容与long精确整数，按fact.id去重，保留parentId/presentationParentId；声音线路不能同时播放NET与逐次奖励。需要更多规则事实时提出具体契约，不自行改core生成事件/RNG。

所有权：你拥有头像像素/音频资产、各自media manifest、资源转换工具、PortraitCatalog/OfficerPortrait及媒体加载、SoundEffects与新增BGM/voice播放器、音量控制、AndroidGameBridge JSON/Unity消费与媒体表现测试。会话一拥有core/game-api/game-runtime、剧本/人物metadata、剧本选择与武将文字数值页面。不要并改MainActivity、通用地图/3D规则、核心数值或公共数据manifest；必要公共入口变更先在自己的契约列确切补丁，由顺序集成处理。全局构建配置修改单独提交和说明，不混入人物数值。

协作与保护：PC目录只读、不启动Wine。两边通过officerId/nativeId/sourceVariant与只读媒体接口连接；数值metadata和media manifest分文件。不要并写progress.md、STATE.json、PC_PARITY_STATUS.md，使用docs/handoff/20261004/session2/记录范围、来源、接口、增量和验收。每批自分支提交，给出基点/提交/确切路径/SHA守卫；不要直接覆盖原目录、合并另一会话WIP或拷旧核心。设备优先独占emulator-5582，先核实可用且无人使用，否则串行协调；不清数据，安装前备份，结束后恢复并逐字节核对全部用户保存/库/偏好。不要操作另一serial的后台音频录制。

验收必须实际正常菜单/新局/全部人物目录/详情/年龄头像/3D出征战斗/战法计略/多回合/存档读回/退出重开。记录播放触发与权威事实对应、取消/失败不误播、暂停/跳过不重复、完整Save/RNG前后对照；真实PCM混音或录音证明声音出现，不能只以play返回值通过。新组合包重新构建、实际安装、记录SHA；旧包/旧原合成音通过不能移用。x86_64模拟器与ARM真机/手机扬声器证据分开，设备缺失明确记录。

持续自主推进，定期中文报告。最终交付可安装APK、完整源码和媒体工具、全人物头像有效覆盖、BGM/语音/音效来源及事件映射清单、实际播放/生命周期/规则不变证据、资源与性能记录和明确未完成项。不得将未知原资源静默标记为移动端简化。
