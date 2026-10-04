# 顺序集成请求与当前未闭合项

基点8400301，媒体分支codex/portrait-audio-restoration。会话一来源清单只读取已提交a367f33faf79f772c78089d549c5ad128e87014b；未合入其WIP。

## 头像输入

需要当前已提交人物只读DTO中提供officerId/nativeId/sourceVariant以及当前有效faceNativeId；缺失来源仍unknown。媒体可用已提交StateToken的year/birth执行已核验的年龄选择，但原已老face不回退。编辑或自定义头像优先保留现有ref/png。metadata与media manifest分文件；不得以姓名猜来源variant。

已提交源连接表保留13600请求、10656已核实身份、其余null；原序列化6152不同记录和3468年龄边界通过。FCE三个imageGroup分别保留240×240/64×80原尺寸，实际列表/对话/单挑调用group的证明仍需闭合。当前运行时有效全部人物覆盖仍为0，不得将源PNG数量当人物还原。

## MainActivity与MapHost待顺序补丁

MainActivity.sessionChanged(GameEvent)须在提前return前将每次已提交techniquePointsFacts传给只读HUD/音频队列。WORLD_REPLACED/CLOSED清队列并静态同步；预览/失败/重复receipt不补播。现有refresh() -> techniqueHud.update(state,player,points,defer)应只同步baseline，移除NET奖励声音线路；按fact.id消费独立事实，保留parentId/presentationParentId。

MapHost.replayFrame(TurnJournal.Event,float)在已提交演示阶段35%后，提供该journal事件稳定id给新releasePresentation(parentId)接口；当前无参数Runnable只能释放整次提交，不能精确对应逐次presentationParentId。commandEffects、暂停、跳过、退出和restore按同一去重策略丢弃已消费/取消瞬态，不执行或补算规则。

本批未修改MainActivity、MapHost、TurnPlayback或core/API/runtime。完成媒体队列后将登记确切diff与前像SHA，再按完成提交顺序集成，不能覆盖另一会话人物页面WIP。

## 声音覆盖

已核实并实装仅原HUD33：bank1/slot19，原2268头+2267PCM，原6f2c30读取格式核实。技巧点正负同样本，不计两个独立原音色。其余9种音效仍为移动端原创合成，BGM/1997语音候选事件/演员角色及所有场景还原继续待闭合。原voice类别表中的71 profile是来源索引，不得猜成71名演员；个人actor+100取自原序列化，事件/语言选择分别取证。

实际x86_64混音/生命周期证据与ARM真机/手机扬声器分开。最新fbc998c9包只接受已记录专项；全人物、正常菜单/新局/全战斗/计略/多回合完整矩阵未通过。
