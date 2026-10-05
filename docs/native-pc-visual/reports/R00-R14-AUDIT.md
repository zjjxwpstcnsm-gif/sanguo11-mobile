# R00–R14 总审计与实际修复 — PARTIAL

审计日期：2026-09-25。结论：没有证据支持R00–R14已全部按要求完成。15个阶段均有正式实现或承接成果，但整体均应保持PARTIAL；明确的运行FAIL、美术FAIL和未执行的设备验收分别记录，不能笼统写成只剩真机。

## 1. 输入与远端基线

读取指定ZIP（SHA256 74e794192d71311b84fed6dfbfd8f25c54a1aa334fbd8234d9e36d145640d03c），包含00_GLOBAL_RULES及R00–R14各阶段要求。逐项表覆盖108条必须实施、75条必须验证、15条门槛、15条交付，共213条；全局边界另行核对。完整表带原文、提示词行号、正式源码路径、当前/历史证据和限定结论，不按条目比例推算完成率。

实时main为ac29b458325b52d6e302ca44270d16552de4ed7f，架构PR66确已合并。输入串行分支为eeb09b4b0037a482b440816ec2fe718c9701ab75；同一agent/native-pc-visual、同一PR67继续，没有从旧main重建或合并main。原生路径仍MainActivity -> MapHost -> FilamentMapView；GameSession保留唯一权威。Filament1.56.0/OpenGL，未启动Unity或新增第二引擎。

本轮先用GitHub CI取回精确源码archive，再核对远端tree与本地文件；容器无法直连GitHub且无Android设备，不能把CI构建说成本地Android运行。源码修复7625ad31492fed97486937cbb1131a5ee35e6688；最终构建源码badd7e56308b4f6a47d60353d49023be8fc492a2。APK之后仅追加审计文档/证据，最终证据HEAD由PR和交付包REMOTE_DELIVERY.json给出，绝不把它冒充APK内SOURCE_REVISION。

## 2. 本轮正式源码修复

新增SceneRenderGate，并接入既有MapHost窗口焦点、可见性、Activity暂停/恢复与释放。旧版全屏势力预览会让前后两个MapHost同时驱动原生渲染；主窗口被遮挡并不必然触发Activity.onPause。修复后的有效条件为resumed && focused && visible && !closed。暂停帧循环而不销毁场景，回到当前窗口再恢复，晚到回调不能复活已释放宿主，状态只能由owner线程修改。

这是正式游戏宿主修改，不是测试专用Activity、空接口或仅文档。它消除遮挡窗口继续提交帧的缺口，不改变地图画面资源，因此不宣称新增美术改善。暂停小模态窗口背后的地图、保留最后一帧是明确权衡。全图预览容量/上传延迟与日期合成画面问题仍存在，没有声称由本修复一并解决。

CI验证从eeb09到badd的core、game-api、game-runtime、data、app/src/main/assets、unity差分严格为空；没有改玩法、地图拓扑、七格、港口、AI、存档、随机数。全部298个正式资产与APK内字节相等。此限定是本轮相对输入的差分，不扩大为整个历史R00–R14相对main从未改过任何元数据。

## 3. 逐阶段结论

| 阶段 | 已核实的正式成果 | 主要未闭合项 | 整体 |
|---|---|---|---|
| R00 | 原生正式启动/配置；历史fresh/reload；本轮补后端、基线、环境、场景索引 | 原交付链缺失；完整故障注入/冷启动/真机未认证，补文档不等于补造原PASS | PARTIAL |
| R01 | 不可变投影、队列与资源生命周期；本轮窗口门控修复 | texture/rig仍部分owner线程解码；异常初始化/native崩溃/真实内存矩阵未闭合 | PARTIAL |
| R02 | 连续坐标场/相机/拾取、内部buffer与UI坐标转换 | 全方位、横竖屏、安全区、动态分辨率与实际触控矩阵未完整验收 | PARTIAL |
| R03 | 全局约束/共享边缘、地形分块和LOD；历史后续PASS423 | 8/24/72细分共享canonical平面，不等于精细山脊；连续镜头接缝和轮廓需验收 | PARTIAL |
| R04 | 连续材质、地表纹理与三档采样 | V1未闭合；最终R04安装曾被取消，早期不同SHA成功不能顶替 | PARTIAL |
| R05 | 深浅水场、岸边材质、稳定水面 | 原图仍见阶梯水岸；完整行船/编辑通行与PC岸线对照未通过 | PARTIAL |
| R06 | 受控GLB/PNG子集、几何加载队列、回退和资源校验 | 非全部解码异步；跨世界/异常释放和完整资产美术验收缺项 | PARTIAL |
| R07 | 城港关/设施资源、679据点实例与120设施映射测试 | 过渡低模/共享小图集；地基、任意坡岸、朝向及PC多角度美术未过 | PARTIAL |
| R08 | 有界植被窗口、稳定视觉随机、合批与道路/单位排除 | 稀疏林缘/孤立木板；全程LOD、过绘和手机性能未过 | PARTIAL |
| R09 | 正式32×32洛阳样板及玩法状态回归，不是孤立演示 | V2明确FAIL；山形/水岸/建筑/景观仍冲突，不能当全国复制模板验收 | PARTIAL |
| R10 | 13类、26GLB、8刚体clip、GPU实例draw和逐成员接地 | CPU缓存刚体非GPU骨骼/全脚IK；全动作实拍与50–100部队物理性能未过；原R10安装失败不能由R11改探针追写PASS | PARTIAL |
| R11 | 真实战斗事件、权威先提交、播放/跳过/账本及黄金存档回归 | 全计谋类别、全程视觉与手工播放矩阵缺项；烟尘是有限不透明几何 | PARTIAL |
| R12 | 双开关、范围/拾取、小地图和编辑器覆盖接入 | 本轮完整势力预览仍FAIL pending614；后续确认开局/命令未到达；UI日期画面陈旧 | PARTIAL |
| R13 | 编辑器、视觉patch、内部原子保存、武将/旧档回归 | 全城港关触控CRUD、外部SAF失败/冷重启、头像导入完整闭环与真机未验收 | PARTIAL |
| R14 | 权威月份只读投影、四季光照/植被/地表/水uniform | 当前7月和4月UI仍画1月；PC同季节对照、阴影时序/过绘/真机未验收 | PARTIAL |

R03/R06/R07后续运行记录已有成功但早期manifest仍PENDING；R10/R11缺独立manifest。原文件保留，原PROGRESS/DEFECTS按blob原样移存history/，当前审计不将这些不一致直接改写为历史通过。R00缺失文件是明确标注日期的追溯新增。详细213条表见evidence/R00-R14-checklist-index.json所列交付文件与哈希。

## 4. 实际验证，不能互相替代

最终CI：[36091281604](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36091281604)。设备是API29、Pixel2配置、x86_64、SwANGLE软件后端、4GiB AVD RAM、512MiB heap。

| 检查 | 实际结果 | 结论边界 |
|---|---|---|
| 新SceneRenderGate host | PASS76断言：20次模态策略循环、8种状态、线程/终止约束 | 非76次真机触控 |
| 审计门控 + R01–R14 host + architecture | 16组脚本全部exit0 | 不等于15阶段美术/设备通过 |
| 完整core原输入eeb09 | exit1，AI uses deployment commands，CoreTest.logistics:75 | inherited FAIL |
| 完整core最终badd | 同样exit1及相同断言 | 分类任务成功不是完整core PASS；没删断言/改成功码 |
| APK、androidTest、lintDebug | 实际构建通过；签名/ABI/SOURCE_REVISION独立核对 | 非安装/美术结论 |
| 未修改生产树eeb09 + 同一新增Android探针 | 原始FAIL：覆盖窗口仍queued=true，frames4 | 成功复现缺陷，不叫旧版运行PASS |
| badd同一探针 | PASS101 checks，包括readiness轮询；3轮真实Dialog、资源身份及完整SaveCodec相等 | 仅门控/数据/组件内容范围 |
| 原完整NativeR12Instrumentation | FAIL；预览WAITING_FRAME，pending614 | 后续势力确认/startScenario/出征移动攻击回合存读档未到达 |
| 本轮原始PNG目视 | 24张，11对Surface/UI加2张重复末帧；日期像素FAIL | 没有用像素数量算法替代目视 |
| 当前内存采样 | meminfo为No process found | 不具备PSS/GPU性能结论 |
| ARM64真机、Adreno/Mali、30分钟热稳定 | NOT_RUN | APK含ARM64不等于设备通过 |
| 本轮全程录屏/完整运动审查 | NOT_RUN | 没有把静图或历史抽帧称作完整视频验收 |

因此最终candidate Android job及整个audit workflow是失败状态，失败来自保留的R12门槛；构建和prerelease上传步骤成功。不宣传CI全绿。首轮7625的R12也失败pending606，随后badd只是修正CI对两份core退出码的采集并重建复测，没有改R12成功条件。

## 5. 实像与参考

最终badd的calendar fixture为1/7/4月。TextView和snapshot正确，但audit-calendar-m7-ui.png与m4-ui.png顶部仍显示190年1月上旬；不能因模型月份断言通过宣布日历修复。logcat有HWUI dequeueBuffer -110/fallback，根因仍未证实。本轮窗口门控改善没有关闭这个问题。

水岸台阶、粗硬城墙、稀疏植被和孤立木板仍可见。保留已有官方手册与用户洛阳02参考；用户02是640×372图，精确相机/季节UNKNOWN，用户01为1×1占位响应不能拿来验收。当前没有足够配准的四季PC参考，不能宣称与电脑版一致。V1未闭合、V2 FAIL，禁止借阶段编号越过局部美术门槛。

## 6. 独立APK与身份

[下载本轮APK](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/native-audit-badd7e56308b/sanguo11-native-r00-r14-audit.apk)

[原始Android运行证据](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/native-audit-badd7e56308b/audit-evidence.zip)；这是candidate Android原始证据，不含另行交付完整213条表/host/baseline总包。

- source：badd7e56308b4f6a47d60353d49023be8fc492a2
- APK：37354097 bytes；SHA256 b4deb41359eb2cedcac949444f87c9454fe020ce5d52b94ab4565e36a79b1687
- applicationId：game.sanguo.mobile.dev；versionCode98；versionName0.98.0-native-r00-r14-audit；minSdk26/target35
- ABI：arm64-v8a / armeabi-v7a / x86 / x86_64
- 证书SHA256：8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24，保持既有开发签名
- 后端：Filament1.56.0/OpenGL；APK无libunity.so；298正式资产逐文件字节比对一致
- GitHub prerelease：native-audit-badd7e56308b，目标commit同source，APK及raw evidence实际uploaded

构建命令：

```sh
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true
```

安装/启动与完整探针在scripts/verify-native-r00-r14-audit.sh。启动组件game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity，加载真实coalition-190 auto.sg11后正常MapHost切换原生3D。脚本的pm clear只用于隔离CI模拟器，不得直接对有用户存档的手机执行。

## 7. 交付与下一阶段阻塞

本轮已实际修改生产源码、提交并非强推远端、构建和安装验证、保留失败、发布精确APK并补齐审计索引。源码之后的证据提交必须只有docs/native-pc-visual，最终远端HEAD及PR67复读见交付元数据。PR保持Draft，不合并main。

仍需关闭R12全图预览、UI日期/覆盖层刷新、残留owner线程解码、局部V1/V2美术和完整物理设备验收；全core规则失败单列，不擅改玩法。此次没有自动启动R15或任何下一阶段。
