# 第七批：实际行军、回合与战报适配

2026-10-03，独立 emulator-5580 / san11-uiux / API29 x86_64 实际触控 **64项通过**。完整目标仍在进行。

## 代码与操作结果

BattleReportUi按可见窗口高度适配键盘及横竖屏，紧凑时收起附加说明、压缩结果统计；增加48dp清空搜索按钮，将分页操作移出数据列表，统一详情弹窗的样式、尺寸及返回跟踪。保留地图与现有原版资源，没有更改任何战斗或回合规则。

[实际测试记录](evidence-07/interaction.txt)涵盖：真实编队出征；非法行军目标不能确认、系统返回取消；合法预览及取消保持完整权威状态/RNG不变；连续双击只移动一次、提交一个revision；取消下一旬不改变状态；实际推进一旬、暂停、Home/恢复、2×速度与跳过；暂停不阻止权威计算，速度保留暂停，跳过不重做规则。战报显示真实提交记录，搜索空态、清空、横屏完整行、详情返回可用。最后通过正式读档恢复全部局面与RNG，通过设置恢复本次开始时的速度2×。

键盘缺陷的[修复前截图](evidence-07/before-empty-keyboard.png)与[修复后截图](evidence-07/after-empty-keyboard.png)；[横屏战报](evidence-07/reports-landscape.png)、[非法路线](evidence-07/invalid-route.png)。并未通过直接调用行军/回合命令来替代触控。测试只读查询用于选择可见合法/非法格和核对权威状态。

## 可重现交付

源码为3330ffb加本批UI/测试变更，使用已冻结checkpoint-20261003-c依赖。构建仍需[独立依赖说明](CONTRACT_BUILD.md)中的init脚本；344份冻结输入SHA不变。原core、API、runtime、data与tools/content共504文件与最初继承基线完全一致。最终由玩法会话集成UI提交。

- 已安装验证包：out/uiux/iteration-07/app-uiux-run05-universal.apk，86991845字节，SHA256 `ce91af46c05e6bf352a949125fd25e7f6adbe75957500e1f763312591946eea1`。
- ARM64单独构建：out/uiux/iteration-07/app-uiux-arm64.apk，82182469字节，SHA256 `d15cf38cf1402d1d3db07f52dbff274e1e09b74e86e89b70984bf30e0afee37b`。只有arm64-v8a；worker/unicorn与已继承库逐字节相同，168固定资源校验通过。**没有ARM设备，未安装/运行此包，不宣称性能通过。**
- universal是本次x86_64验证身份，沿用项目的原ABI打包策略，附带32位Filament但32位没有PC worker；不作为32位原生演出已验证的交付。
- 全程录像：out/uiux/iteration-07/run05/interaction.mp4（27031116字节）；5/15/25/35秒实际顺序解码样本在run05/frames，已查看35秒暂停演示帧。录像采样不作为游戏帧率数据。
- 测试：app/src/androidTest/java/game/sanguo/mobile/UiUxInstrumentation.java，suite=march。配套app/src/androidTest/tools/run-uiux.py只允许独立AVD，测试前校验自动档/恢复槽SHA，失败后启动restore suite通过真实游戏UI读档，保存失败证据与恢复日志。未清除任何模拟器数据。
- [测试后文件SHA](evidence-07/save-after.sha256)：自动档、手动槽3、自建武将库与本批前基线一致。架构边界及168固定美术资源检查通过。

完整源码快照在out/uiux/iteration-07/source及source-manifest.json；冻结规则模块和四个原生库复用iteration-06/build-inputs.tar.gz，不依赖共享工作区的可写构建目录。

## 失败与边界

run01落点含地图覆盖区域，未打开行军预览；排除覆盖区后实际格点击通过。run02末尾漏导航回菜单、run04清空后多发了一次Back，属于测试路径错误，分别保留完整失败证据。run03实际复现键盘下空结果说明被裁剪，修复后同一断言通过。run02/03/04恢复日志及字节校验都保留，未以文件覆盖伪装读档验证。

同步出征预览仍会造成主线程停顿；未宣称其性能问题已解决。本批没有复测全国3D加载耗时。更小尺寸、横屏键盘、32位、ARM真机、连续多旬和长时交互尚未验证。攻击、战法、计略、外交和其他内政仍需继续完成实际路径。
