# v0.16 验证与交付

- 运行代码：`d6ca5f282f23cbcbea27b9c06ac2cbaa8bdd7920`。
- 源码树：`7e34522fcd6f4d7ac1eb9aeeee97541ec4e299a0`；归档245个文件与本地运行代码逐字节一致。
- [PR #17](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/17)基于PR #16分支；未合入main。最后提交仅补文档与证据。
- [CI34841640072](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072)的1080×1920、1080×2340、1080×2400三组均成功。横屏截图分别为1920×1080、2340×1080、2400×1080。

## APK

[下载APK归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072/artifacts/10346389812)：`sanguo11-mobile-v016-d6ca5f2.apk`，496576字节，版本`0.16.0-campaign-ai-dev`，版本号16，包名`game.sanguo.mobile.dev`。

SHA256：`7caba187b235fb273c6ee5379bd8987fce076cbf2756f2a21d15c4f4817b42b8`。

签名证书SHA256：`8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。CI apksigner验证v2签名通过；下载后从APK签名块解析证书再次比对，并核对BUILD_COMMIT及SHA256SUMS。沿用v0.9以来的开发包名和固定开发签名。

## 已完成核验

| 范围 | 结果 |
| --- | --- |
| 核心 | 20组全部PASS，其中新增AI452断言 |
| UI模型 | 51749断言通过 |
| 资料管道 | 13份生成文件一致、15项测试通过 |
| Android | 三组API29 x86_64完整安装/触控/存读流程通过 |
| 旧版升级 | v0.9 APK原位覆盖，保留v8存档，写为v14后恢复状态一致 |
| 截图 | 每组75张，共225张尺寸/摘要校验；新增军情和AI结算共6张目检 |
| 稳定性 | 三组日志无应用FATAL EXCEPTION或ANR |
| Lint | 0错误、12条既有警告 |
| 源码 | CI归档245文件与构建源码完全一致 |

Android新增流程实际调用下一旬，断言冲车通过兵器战法伤城、优先消灭列表靠后的100兵残部且保留6000兵强敌；军情评估打开返回不改变随机数/存档，Activity重建后结果一致。其余历史流程保留。日志的SMOKE PASS总括文字沿用旧文案，新增AI流程的调用、断言和81/82号截图见该提交的GameSmokeRunner。

[机器可读摘要](verified-summary.json)和[全部截图SHA256](screenshots.sha256)记录了可复核证据。截图/日志归档：[1920](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072/artifacts/10346698735)、[2340](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072/artifacts/10346199032)、[2400](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072/artifacts/10346449214)；[源码归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072/artifacts/10346421809)、[Lint报告](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34841640072/artifacts/10346653971)。

## 限制

未验证ARM真机。验收证明本工程命令流程和存档正常，不证明与原版完全等价。全国原版地形0格、可核验官方完整开局0个；全特技组合、精确公式、全部PK/历史事件仍有缺口。范围和明确工程参数见[AI说明](../../AI_V0_16.md)及[功能表](../../FEATURES.md)。
