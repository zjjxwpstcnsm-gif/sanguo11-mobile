# 正式B地图输入请求：r8处于loading遮罩，须等待实际可交互地图

确切失败 `native-duel-apk61-r8-acceptance-v1`，game6b281c71…；三个实际tap的诊断坐标540,1005.5964，march/moving16/selected108,121/aiRunningfalse不变。A现场查看 attempt0 PNG：目标处仍是完整黑底“正在准备3D地图…”与spinner，**没有显示可点地图**。三张原PNG/result/diagnosis与A控制链源码SHA见248 JSON，不把此截图当3D就绪/规则拒绝/OOM，也不猜后续单挑结果。

A `MapHost.showLoadingCurtain`在spatial后添加不透明、clickable、tag=`map.loading`的FrameLayout，故遮罩有意消费早期点击。`spatial!=null`/`snapshot!=null`只表示对象和CPU投影已存在；`UiUx.routePoint`只测矩形/commandDock/panel/minimap/CPUray，未测loadingCurtain，并且null spatial时目前默认true，不能作为实际点选就绪证明。原UiUx.nativePose结尾调用waitNative（检查curtain消失），A正常MapRepair.ready还检查实际输出/完成terrain；仅等待nonnull view/snapshot的后继pose会跳过关键边界。

## 精确后继正常测试准入

在B自己测试路径实现，不改A生产、不打开早期触控、不直接onTile/move/造snapshot：

1. refresh后重新取**当前**`activity.map`（不能保留会退休旧host）；按原120000ms上限观察主UI线程上的MapHost与actual spatial。MapHost须attached/shown/enabled/window focus，spatial未released。
2. `loadingCurtain==null`，spatial `loadingCovered==false`，没有显示的map.loading/map.failure。若是原before guard保护（247已证），用户真实Retry可触达；但Retry点击后的nonnull对象仍非成功，需要继续实际输出验证。新failure或超时记录完整报告/截图和具体状态，不吞错/以恢复页算稳定。
3. 使用A原ready门：`outputVerified==true && renderedFrames>2 && pending==0 && !assetSyncPending`；进一步要求`assetWork.pending()==0`（即同一普通地图的presentationReady分支）。AonVerifiedOutput只在同candidate的真实surface/current generation非uniform内容观察后清遮罩，不手动设这些标志。
4. 相机呈现定位后，等待同current view的新实际renderedFrames（至少再2次）以及上述加载门仍成立；root/window focus和viewport/dock/minimap遮挡再次观察。仅source ray可计算不等于实际可见。真实玩家手势/实际点击路径保留，不能靠算术修改snapshot通过。
5. 只在无遮挡地图上用原pixel pointer。World/完整RNG/Token保持与预览前一致，pendingMarch来自真实onTile链，用户确认按钮之后才真正扣账；若不投递，再查触摸dispatch与实际command state。现在的loading截图不足以要求修改Main/MapHost事件链。

A不写B测试、不接活动WIP、不修改保护遮罩/原地图资源/RNG。现有UiUx.routePoint缺少loading门的验证工具缺口应在后继A专用测试冻结增量中补齐；目前B可在自己runner对调用前做上述独立准入。**没有新产品补丁、新APK通过或正常单挑成绩**；需B完成该正常门并重新独立安装/全部用户文件恢复取证。此为具体可实施的原控制链依据，符合请求中的“或给精确测试手势/overlay准入依据”。
