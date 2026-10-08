# A 地图射线测试准入250：独立冻结增量

248已证明B r8实际点击位置处于map.loading遮罩，B已在自己pose路径采用实际输出/curtain/focus/资源完成准入。本增量补足A既有UiUx.routePoint自身的验证缺口：默认false；必须当前attached/shown/enabled/focused MapHost，未retired native view、无加载遮罩/covered、真实outputVerified与>2frames、无pending mesh/assets、无fullscreen presentation且普通地图presentationReady，然后才运行原viewport/dock/minimap/CPUray检查。

仅独立stage UiUxInstrumentation.java，前SHA `fd7b1984…` 后 `fd2ac8e0…`；patch SHA `8f61af89d2586c725bae1a2e6ce53beb92b7a64d015ef5f7caa5cccbd00c9ad3`。实际单文件Java依赖编译exit0，另独立应用patch后完整SHA同。当前规范测试239、游戏165、B r9 APK均不改，不新增等待/重试/规则调用、不能用数学射线或snapshot取代玩家可见输入。

后继A测试源码或B冻结A测试依赖串行应用时须核确切前态；不覆盖其他测试变化。必须新测试APK构建/实际安装/签名与完整target保存/库/偏好备份恢复，再验证loading/failure/nullrenderer阶段射线不被记为pointer准入、真实地图正常移动/攻击/建设和全部SaveRNGStateToken纯性。未进行新APK运行；此只提高验收工具证据质量，不声称产品点击修复/目标完成。
