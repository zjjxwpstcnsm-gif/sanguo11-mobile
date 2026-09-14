# v0.17 本地构建与未完成的运行验收

代码提交 `afedab15b327b6e22605ffbc4648533e1f710056`，源码树 `24bb2509172b4f5ff413428521f44ed5524e89ba`。PR #18 基于已合并 #15/#16/#17 的 main。本文件之后的提交只记录验收，不改变APK运行代码。

## 已通过

- 全套纯Java核心回归，含实际v1—v14旧存档夹具、行军/天下/AI、生命周期与全国格点转换。新增生命周期/格点检查159354条，其中绝大多数为40000格往返和邻接验证，不能换算为玩法完成率。
- UI投影/相机51749条断言；内容管线13输出校验、15项内容破坏测试；新增第9个内置包后的175项剧本检查。原创世代传承实际执行3旬AI、正月登场/寿终/继承及存读续局通过。
- 官方Android35 API下编译全部应用/操作测试Java代码。最终使用Temurin17.0.16+8、Gradle8.11.1、AGP8.9.2、Android35平台与Build Tools35.0.0执行 `:app:assembleDebug :app:assembleDebugAndroidTest lint`，最终构建成功。
- Lint：0错误、0致命错误、14警告。主要为中文文本拼接，另有既有Manifest/View警告；未将这些警告计为运行验证通过。
- APK包名 `game.sanguo.mobile.dev`，versionCode17，versionName `0.17.0-lifecycle-dev`，最低API26/目标API35。桌面名称修正为“三国·研制版”。
- APK V2签名验证成功，证书SHA-256仍为 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`，与既有固定开发签名一致。包内全部core资源与仓库原始字节逐一相同，ZIP完整性检查通过。

## APK

文件：`sanguo11-mobile-v017-afedab1.apk`

大小：513098 字节

SHA-256：`a2d8b7fd45446d5b8b4a5865712a295a14f9009960520517b7a93905bd63d522`

这是本地构建的开发APK，不是本轮CI生成的产物。通过ChatGPT附件交付；APK和测试APK没有提交到git。

## 明确未通过/未执行

[首次CI](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34847207661)的3个矩阵任务及重试，均在运行器分配前失败：runner_id=0、steps为空、无日志与产物。取日志返回BlobNotFound；现有连接无法读取具体注释，因此没有把故障归因为代码或账户额度。

[最终代码CI](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34848230258)同样失败。当前本地环境没有 `/dev/kvm` 或可用Android设备，未运行API29三种屏幕原生操作、v0.9覆盖升级、Activity重建实际界面或ARM真机性能。本轮没有截图证据，也不能沿用旧版本截图作本轮验证。

新增原生操作自动化已编译进测试APK：生卒编辑/取消/应用、继承/取消/重建、履历、剧本回调导入/取消/坏文件以及200×200源图绘制压力夹具；**已编译不等于已执行**。PR保留打开，待运行环境恢复后执行现有CI门禁。

## 还原程度

原版地形0格、官方完整可玩开局0个；全特技交互、精确公式、全历史事件、决战称霸及完整原版AI仍有缺口。详见 [本轮范围](../../LIFECYCLE_V0_17.md) 和 [完整功能表](../../FEATURES.md)。
