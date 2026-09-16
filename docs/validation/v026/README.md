# v0.26 验收记录

实际构建代码：`b75e2495f891801bca9a86c122cb22b094bee2ec`。基线PR #33按授权合并为main `7f37ecf29506532bdee2ad571c109c631632d7b9`；本轮[PR #34](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/34)未合并。最后提交仅补README、progress及docs证据，不改动已验收的运行时代码、测试和构建配置。

## CI、APK与安装验证

[CI35085267945](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35085267945)三组全部成功。全部核心/UI/内容、Gradle测试、Android主APK/测试APK、Lint和安装操作门槛通过。CI Lint为0错误/21条非阻塞警告；本地20条。

- [APK产物10442343895](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35085267945/artifacts/10442343895)：`sanguo11-mobile-v026-b75e249.apk`，6,233,346字节，versionCode 26，versionName `0.26.0-logistics-campaign-dev`。
- APK SHA-256：`47edc0678bbe0117ffe9388a95ed0fdbb8dab98e5896074c399caafd7ab121c0`。
- 包名 `game.sanguo.mobile.dev`；固定证书SHA-256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。下载后再次执行apksigner验证。
- 381份源码归档文件逐字节匹配构建提交；六份ZIP摘要全部匹配Actions元数据。435张PNG通过完整性检查，摘要在 `screenshots.sha256`，其他核对在 `artifacts.json` / `verification.json`。

API29 x86_64、420dpi：

| 分辨率 | 作业 | 结果 | 截图数 |
|---|---|---|---:|
| 1080×1920 | 104758515643 | 横竖屏SMOKE PASS、v0.9与v0.25 UPGRADE PASS | 147 |
| 1080×2340 | 104758515388 | 横竖屏SMOKE PASS | 144 |
| 1080×2400 | 104758515737 | 横竖屏SMOKE PASS | 144 |

每组新增10张v026截图，目检三屏横竖屏的运输确认、异常/在途列表、支援预览，确认按钮可见且列表仍有滚动空间。实际操作包含三将上限、途中粮耗、预览取消字节不变、确认一次派送、任务当前位置与异常城定位、筛选及Activity重建恢复、跨团支援扣来源预算且立即更新报告。原有全部操作回归保留。五份主流程/升级日志无应用FATAL EXCEPTION或ANR。

真实v0.25 APK使用上轮交付文件，SHA-256 `d065889f86ee60988927105dac928eab18bf2d7773d8ccec0dc7fcc960bcd123`。旧APK实际打开真实v19档后，原位安装v026；升级前后旧档字节保留，军团设置、战略意图和在途运输继续下一旬与期待局面逐字节相同，重建不重复执行。v0.9真实APK/v8档覆盖升级门槛同时保留。真实v19夹具为4389字节，SHA-256 `4f56eef5b6ede99a0dbcf204374c8898a0db96ddd07d5ca4eba277a052006ad0`。

完整CI关键输出见 `ci-results.txt`。CI脚本整旬观测为42城670将232ms、200×200/40城40队504ms；Gradle复跑310ms、531ms。没有把这些桌面运行器数字或Activity重建称为ARM真机、系统杀进程实测。

## 本地验证

全部既有核心回归通过；新增1095条物流/经营/战役断言，旧105条战略经营断言保留。51,795条UI模型/相机断言、19项内容测试、两项资源生成核对、Android主包/测试包构建和Lint通过。本地Lint为0错误/20警告。完整输出见 `core-local.txt`、`ui-local.txt`、`android-local.txt`。

36旬固定种子多城/两委任军团经营，逐旬独立核对金、粮、兵、全兵装库存及在途总量。真实建设、生产、调将、派送、收入、粮耗均发生，保存恢复后继续逐字节相同；后方从保留线附近恢复。禁止进攻贯穿全部四季，没有凭空增加人员或物资。

两组完整行军均从城内、零预置部队开始：保留2000初始城防的原弱城回归，另增8000初始城防的坚城。都验证两个出发城的军队真实会合、足够兵力、不同兵种和冲车过单格狭道；坚城另要求冲车实际攻城记录，25旬内攻下。弱城可以在冲车抵达攻击位置前由远程队攻下，不把这个结果冒称冲车已攻击。

## 缺陷前后对照

- `baseline-probes.txt`：v0.25在同一运输费用、在途耗粮、有效设施攻击停滞探针失败，新版全部通过。
- `campaign-journey-before.txt` / `campaign-journey-after.txt`：相同完整行军回归，原AI `ba482e0` 无法实际会合，修复后通过。没有只把部队摆在敌城边。
- `fix-evidence.txt`：记录新Android夹具君主忠诚、测试场景报告未关闭、筛选跨场景保留的问题，以及实际修复的支援后报告未刷新的界面缺陷。没有删除失败断言。
- 真实v19夹具由v0.25未修改编码器生成，源码在 `GenerateV19.java`；旧100金费用保留历史事实，默认单将/留驻，不退款、重复派遣或扣款。

## 性能与范围

`performance.txt`记录JFR热点：寻路重复计算同一编队控制区。现在每次同步搜索只计算一次，命令结束即丢弃；全国异常筛选每个运输任务仅查一次路，同时标记起终城。

同一本地运行环境的整旬观测：v0.25为42城670将479ms、200×200/40城40队2138ms；本轮最终完整行军修复后367ms、817ms。均为桌面JVM单次观测，非ARM真机帧率、温控或统计性能承诺。

精确运输粮耗/速度/断粮比例、完整战术运输队及舰船货运、任意狭道全局协同仍有差异。原版全国逐格地形、完整官方开局、全事件和精确公式未完成。Activity重建不等于系统低内存杀进程；未做ARM真机测试。[实际实现和官方依据](../../LOGISTICS_CAMPAIGN_V0_26.md)。
