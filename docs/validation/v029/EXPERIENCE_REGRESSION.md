# 既有手机体验回归

以下原始配对测量对应运行提交 `37ac1cc`（与最终构建b08c518的应用代码一致），[CI35128269168](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35128269168)通过。[归档10460543739](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35128269168/artifacts/10460543739) ZIP SHA-256 `c67c01fb9eabb033f69450f2e7a5288a980e5607c006bb50d2598016d4712808`。前后均为EXPERIENCE PASS，无FATAL EXCEPTION或ANR。

沿用v0.28工作流的**实际v0.27 APK基线**，同一API29模拟器先运行候选、再运行旧包；这不是v0.28/v0.29性能配对。v0.28交付记录仍保留在相邻v028目录。实际v0.28/v0.29位移与升级对比见 PAIRED_DISPLACEMENT.md。

普通选中镜头不跳、稳定跳过已行动队、全国缩放、数量草稿恢复、双击/过期确认、全国返回、运输/补给/编队等既有流程均通过。所测竖屏地图792px及横屏1112px宽保持v0.28改进，不列为v0.29新增。

| 同模拟器指标 | v0.27 | v0.29 |
|---|---:|---:|
| 42城670将真实整旬中位数（3次） | 580.25ms | 579.49ms |
| 200×200、42城40队3运输真实整旬中位数（3次） | 3186.14ms | 3242.24ms |
| 大图软件绘制中位数 | 4.43ms | 4.95ms |
| 大图所测可见格访问数 | 103 | 113 |

大图整旬约+1.8%，绘制约+11.7%，候选可视面积也更宽；不能由本次模拟器短样本推导全面提速或ARM帧率。原始测量见 experience-before.txt、experience-after.txt，均来自37ac1cc这一轮，不混用不同运行轮次。

此前3201500的[CI35122887411](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35122887411)及bb4e3ea的[CI35125136388](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35125136388)也已通过。3201500那轮大图整旬4935.61→5046.35ms（+2.2%），绝对耗时随运行环境波动；本次回归目的为保持已交付操作并记录实际性能，不包装旧功能或放宽门禁。

最终构建b08c518的同流程通过[CI35132774416](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774416)，[归档10462702099](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35132774416/artifacts/10462702099)，GitHub记录ZIP SHA-256 `a79efe48f623155c8d74d2300848e5765b867cb9912e76d50c848be661f4ae4f`；工作流日志确认候选与旧APK两次EXPERIENCE PASS。上表及本目录原始数值继续明确对应37ac1cc那一轮；最终构建只增加两处完整回归的窗口等待，应用源码逐字节相同。中间f0df381的CI35130621913也已通过。
