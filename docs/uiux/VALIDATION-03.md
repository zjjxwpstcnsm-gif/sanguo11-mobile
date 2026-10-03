# 第三批：3D加载过渡与被遮挡场景的绘制开销

状态：加载交互16项通过；全国远景性能仍不够好，未宣称流畅或ARM真机通过。

`MapHost` 在首次切入3D时保留现有2D地图，显示准备提示与48dp取消按钮；遮罩接收地图触摸，避免按占位图误选3D目标。切回2D、取消、关闭预览或Activity释放时移除遮罩与原生资源。`FilamentMapView` 的既有真实Surface像素验证成功后才移除遮罩，仍要求20次实际画面提交、装载完成及非均匀像素；没有合成ready或削弱健康保护。

实测发现：遮罩完全盖住Surface时，原实现仍反复绘制装载不完整的全国场景，造成GPU拒绝提交且拖慢剩余资源上传。现在仅在遮罩存在且资源未装齐时跳过被遮挡场景的绘制，继续受原预算和GPU准入约束的资源上传。完整资源就绪后恢复原有全质量绘制、计帧及像素验证。没有改资源、LOD、分辨率、MSAA、规则或数据。

独立 emulator-5580 / API29 / x86_64 实际触屏[16项结果](evidence-03/loading-interaction.txt)：进入加载、加载中误触拦截、取消释放、重新加载、横屏、Home暂停恢复、检测到真实画面才揭开遮罩、返回并释放、完整权威状态/RNG不变。源视频 `out/uiux/iteration-03/run02/interaction.mp4` 已完成封装并顺序解码，样本与PTS在同目录frames。截图中的地图内容仍为继承美术，不是美术验收。

性能观测保留：

- run01（只有遮罩，仍重复绘制未完成场景）：第二次加载从首次progress记录00:33:06.139到00:34:47.715仍有100项pending，测试的恢复后90秒条件失败。beginAttempts2786 / beginSkipped2689 / 实际提交97。
- run02（遮挡时只推进装载）：第二次加载从首次progress记录00:36:47.775到00:37:44.697达到pending0、20次实际提交并PixelCopy验证成功，观测区间约56.9秒，包含旋转与一次Home。beginAttempts1551 / beginSkipped1206。
- 这两个单次模拟器观测没有控制所有缓存和GPU负载变量，不能当作稳定基准或真机帧率。仍有主线程停顿和较慢的全国远景；本批没有把它们标为解决。

失败输出和首次采集脚本错误都保留：run01的screens最初误拉了旧opening套件，已移到wrong-suite-pull，正确loading截图重新拉至screens；最终证据使用按套件和run分开的目录。

验证APK：`out/uiux/iteration-03/app-uiux-run02.apk`，82233056字节，SHA256 `72327515ec818fae9c12b080c0db1f19e65ca9f6a6c59a33f1c3ca3dbae405ca`。源码身份为1ba9823加本批变更，测试包同目录app-uiux-test-run02.apk。架构检查及168项固定地图/美术资源检查通过。

[权威预览契约](RULE_UI_CONTRACT.md)已按用户要求发送到“对齐三国志11玩法与数据”会话：优先出征全校验预览，其次建设工期与运输摘要。接口未落地前，UI不增加规则公式。后续继续编队/数量输入与全部命令流程；完整目标仍在进行。

同一APK的[开局/存档/设置62项回归](evidence-03/opening-regression.txt)通过，视频在 `out/uiux/iteration-03/opening-regression/interaction.mp4`。正式新局后实际加载测试槽3恢复完整旧状态，未把旧测试通过结论直接沿用到新渲染路径。
