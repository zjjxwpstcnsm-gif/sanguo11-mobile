# v0.20 验证记录

验证代码 `0af87c26fed7e70fdb737461c2b693e945a12f32`；最终文档提交不改变程序代码或 APK。

## 结果

- [完整 CI](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34933681936)三组全部通过：Android API29，1080×1920、1080×2340、1080×2400，包含横竖屏及原有全部操作回归。
- 24组核心PASS；新增209条战果/寻路断言、45条押送断言；UI投影/相机51770条断言通过。真实v15城内俘虏夹具升级v16仍保留原城，随军押送存读一致。
- v0.9 APK同签名覆盖安装成功，原有v8存档保留、载入、写为v16并重启还原一致。
- Android构建及Lint通过：0错误、17警告，警告包括目标SDK、备份配置、View构造方法及国际化文本；未把警告计为零。
- 三屏共292张截图（98/97/97）；逐组核对SMOKE PASS和归档SHA256。检查了编队确认、副将五维/适性/特技资料、随军俘虏资料、入城后的地图、战果条和44模型图集。
- 281个构建源码文件逐字节匹配验证提交，APK的构建提交、归档摘要、文件SHA256和固定开发证书一致。

## 关键操作

默认「军事→出征」成功组成孙权主将、周瑜和甘宁副将三人部队；取消确认保持存档和资源不变，超选第三名副将不会加入。逐个资料入口可打开真实武将数据。

实际地图攻击显示俘获概率、金粮缴获和“随军押送”；击破格可预定下旬进入。俘虏随实际行军移动并可重启还原；实际进入本营后，押送部队引用清除，俘虏改为本营关押。核心另验入城失败原子性、再次出征留俘虏在城内、原势力救回、第三方转押、主将继承/全队死亡及在途赎回。

首次追加安装回归发现：快速出征会沿用军事菜单的滚动位置。已修正为出征后定位新部队、将详情滚回顶部；最终三屏完整回归通过该操作。

## 安装包

- [APK及签名记录](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34933681936/artifacts/10382388186)
- 文件：`sanguo11-mobile-v020-0af87c2.apk`，549842字节，v0.20.0-battle-map-dev / versionCode20。
- SHA256：`190af4c505eeeca24ce496b5440db223a5d2358c776ee49fc33db8fcc4ec62e9`
- 签名证书SHA256：`8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`
- [完整构建源码](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34933681936/artifacts/10382366708)
- 截图和日志：[1920](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34933681936/artifacts/10382343136)、[2340](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34933681936/artifacts/10382627216)、[2400](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34933681936/artifacts/10383186529)

## 边界

实机马达震动手感未验证，触感受硬件与系统设置影响。缴获与概率沿用本项目明确标注的工程规则，尚非原版精确公式核验；官方逐格地图、完整开局及全事件/特技交互仍有缺口，不能据此宣称100%复刻。
