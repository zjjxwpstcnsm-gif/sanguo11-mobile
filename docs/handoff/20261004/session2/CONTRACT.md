# 会话二：头像与音频还原契约

基点：840030195e39cec3c0d352e010a3c3e614893ca2。完整继承4299文件、336374894字节，逐SHA核验见inheritance.json。原目录及PC安装只读；不启动Wine。

## 所有权与接口

本分支拥有独立媒体manifest、像素/声音转换工具、PortraitCatalog、OfficerPortrait、媒体加载/播放、AndroidGameBridge JSON及Unity只读消费。不修改core/game-api/game-runtime、人物metadata、文字数值页面、MainActivity、公共地图/3D入口或共享台账。必要入口补丁先记录确切路径和前像SHA，再顺序集成。

人物连接键为officerId/nativeId/sourceVariant，不以姓名、sourceId相似或图像相似推断有效身份。等待会话一已完成metadata契约；只读检查其已提交契约，不复制WIP。源像素覆盖与人物有效覆盖分别统计，未知绑定不得填充为已还原。

schema1加法扩展：消息state{sessionId,generation,revision}和techniquePointsFacts[{id,parentId,presentationParentId,state,sequence,owner,before,after,delta,cause,phase,cityId,officerId}]。long始终保留整数。客户端只有成功应用的snapshot/delta才可消费事实；receipt/event/主动snapshot/restore不补播。每条事实以id去重，保留父ID及演示父ID；不播放同提交NET奖励。旧消息缺少扩展时保持显示兼容，不制造事实。overflow明确resync并丢弃待播瞬态，不重建遗漏声音。

## 当前事实边界

原列表仍32人旧图集/绘制替代；六人源年龄/战法取证不代表全人物还原。原FCE全部6600注册位置已按原机器码核验，964个非空Face各有三张原像素；人物运行时覆盖仍为0。旧小头像分组映射已明确拒收，使用portrait-pixels-current.json.gz。原HUD33现已确定为bank1/slot19，原资源2268头+2267 PCM；已实装正负两别名共享同一原样本并取得x86_64混音证据。其余9种短声音仍为移动端原创合成，BGM/语音的正常场景调用及全部形态仍待闭合。

逐事实队列/HUD拥有独立事实模式，与旧NET模式互斥；真实零净变化、跨sessionId恢复、同revision关闭、暂停、跳过和overflow/resync主机44项通过。公共MainActivity/MapHost/TurnPlayback未改；依据会话一已提交2910997生成fact-host.patch，前后SHA和独立编译证据分别见fact-host-guards.json、fact-host-compile-guards.json。装机专项的临时只读适配器不能算公共入口已闭合。

## 工作区与验证

独立目录/Users/paopao/.codex/worktrees/portrait-audio-restoration/sanguo11-mobile；分支codex/portrait-audio-restoration。构建输出、Gradle可写缓存均独立。只复用只读SDK/工具链输入。设备优先emulator-5582，须检查占用与保留用户文件；不触碰5554音频。每次安装先完整备份files/shared_prefs/库，结束恢复逐字节核对全部路径。x86_64与ARM真机证据分别记录。

本文件记录待完成目标与边界，不声明任何新APK或全部媒体已经验证。
