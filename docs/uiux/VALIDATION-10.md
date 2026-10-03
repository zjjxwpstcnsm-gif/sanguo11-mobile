# 第十批：战斗触控、目标空间与连击误触

2026-10-03，独立 san11-uiux / emulator-5580 / API29 x86_64。最终安装包战斗66项、水军18项、行军/回合64项通过；整体目标仍在进行。

## 改动与实际发现

第一轮战法双击只提交了一个权威命令，但第二下落到替换布局后出现的战果条，意外打开战果详情。保留[失败记录](evidence-10/combat-before-failure.txt)、[原图](evidence-10/before-repeat-tap.png)和[正式读档恢复](evidence-10/failure-recovery.txt)。成功提交后增加300ms新触摸手势保护，消费落在此窗口内的完整新手势，不延迟规则、动画或绘制；已开始的原手势不截断。现有world/revision校验和一次提交锁仍保留。覆盖兼容命令及现有typed出征、城市和对局提交入口。

[修复后](evidence-10/after-repeat-tap.png)双击不再打开战果；随后主动点击战果仍正常显示、返回。战法选择改为至少56dp的整行按钮、14sp文字和随窗口高度滚动的列表，核心不可用原因[完整显示](evidence-10/authority-reason.png)，没有新写任何兵种/伤害/成功率公式。

攻击、行军或其他地图目标选择期间临时收起城池/部队快捷行，返回后恢复，释放96dp地图高度。[横屏改前](evidence-10/before-target-landscape.png) / [横屏改后](evidence-10/after-target-landscape.png)。地图、原版资源和画质设置未改。属于移动端布局适配，不称原版界面复刻。

## 实装验证范围

- [战斗66项](evidence-10/combat.txt)：真实攻击空地错误、攻击确认取消/返回、双击只提交一次；战法非法地块、山地阻挡权威预览、只读RNG、横竖屏、取消回到选目标、系统返回、退出选取；真实突刺扣费/目标位置/版本号；主动展开战果与返回；沙地不可用原因完整可见；火计取消、双击唯一提交与权威气力成本；正式读档恢复全部局面。
- [水军18项](evidence-10/naval.txt)：斗舰投石列表、预览、真实扣费一致为权威上下文15气力；紧凑横屏最后一项/取消可见；真实地图选择、双击唯一提交、正式读档恢复。[预览图](evidence-10/naval-preview.png)。数字来自Army.tacticCost，不在UI复制陆/水分支。
- [行军/回合64项](evidence-10/march.txt)：同最终包真实出征、非法/合法目标、取消、双击单次移动、推进一旬、暂停/Home恢复、2×速度、跳过、搜索战报、横屏详情返回、读档及设置恢复，覆盖共用目标栏与连击保护回归。
- 使用已有core testFixtures的DisplacementFixture，通过SessionProbe.install → activateWorld → 真实host安装。沙地和斗舰为测试前布置变体；命令全部来自屏幕触控。未使用旧GameSmokeRunner反射替换world，也没有让测试调用规则代替按键。小场景用于UI流程，不验证全国剧本平衡、官方地形呈现或所有战法组合。
- 自动档、手动槽3和武将库最终SHA在evidence-10/save-after.sha256。失败也通过游戏读档恢复，没有清除模拟器数据或覆盖用户模拟器5554。

## 构建与证据

源码身份67057ff加本批UI/测试，仍使用冻结G的346份依赖和contract-v2.init.gradle。root保护504文件与G输入SHA一致；架构边界、两包各168固定美术资源通过，证据见本目录evidence-10。

- 已安装验证x86_64：out/uiux/iteration-10/app-uiux-run03.apk，82244112字节，SHA256 `bc436c9a7ad73ecd76471a0f7b05fbc3f32b7ee18f684b8982f9655944718c98`。
- ARM64：out/uiux/iteration-10/app-uiux-arm64.apk，82186953字节，SHA256 `98faac17d4c00c56b7c9dc52fa231853ef5dbcbbfa88a927eaae878baec861db`。仅构建、ABI和继承native库核验，没有ARM安装/性能结论。
- 本批源码快照out/uiux/iteration-10/source、source-manifest.json，配合第九批未变化的352项out/uiux/iteration-09/build-inputs.tar.gz。构建方式详见CONTRACT_BUILD.md。
- 最终战斗run03/interaction.mp4，水军run04/interaction.mp4，行军run05/interaction.mp4；frames记录实际解码PTS。查看了战法前后横屏、完整原因、水军10秒实际帧。早期run02视频首次申请50/75秒超出实际长度，解码错误保留；改用5/15/25/35秒后成功，原视频未修改，不当成游戏崩溃。
- run01是实际失败；run02是尚未收起快捷导航时的65项中间通过，均保留。最终版本以run03及以后证据为准。

下一步继续运输、其余内政与高级外交、地图多指手势、长时操作、生命周期、首次查询及原生全国加载。更多尺寸、完整复合战斗和ARM实测仍未完成；本批通过不替代最终合并包全流程验收。
