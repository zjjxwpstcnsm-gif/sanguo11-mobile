# v0.14 实际验收

- 可执行代码：`b2eec25d20cadf4cd5196aeb0b6e99d9e556191b`；代码树：`accbdf61e15126ccbee9fb67fcbe1cbf9eef7a12`。
- [成功CI34834950707](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34834950707)，job `103946585840`。
- [APK产物](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34834950707/artifacts/10345390694)：`sanguo11-mobile-v014-b2eec25.apk`，447335字节。
- APK SHA256：`eede36311a81b3a51627133be463914c3eb259227651466c464699218c6478f6`。
- APK证书SHA256：`8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。CI的apksigner v2校验成功，下载后读取实际APK v2签名块核对证书。版本`0.14.0-march-dev`，versionCode14，包名`game.sanguo.mobile.dev`。

## 已通过

核心历史回归及新增75条行军断言、51749条UI投影/相机断言；Android构建与Lint（0错误/12警告）。API29 x86_64模拟器分别以1080×1920、1080×2340、1080×2400配置横屏运行完整操作，每组66张截图与SMOKE PASS，未发现测试日志中的应用崩溃/ANR。

新增真实触控覆盖：出征后选中部队→点目标→预览/取消→Activity重建后保留未执行预览→开始行军→跨旬前进→任务定位→改道/停止→抵达城旁→手动攻城。三屏均通过；另抽查三个尺寸的路线预览、持续行军和抵达截图，按钮和提示可见。

实际安装v0.9旧包后保存v8局面，再使用本轮APK覆盖升级；原数据保留，转写v13后恢复状态相等。额外1张升级截图；共199张PNG的摘要见[screenshots.sha256](screenshots.sha256)，操作截图198张与升级截图分开计数。

下载APK、操作证据、源码及Lint四份归档后，全部核对GitHub提供的归档SHA256。归档内BUILD_COMMIT与APK对应提交一致；232份跟踪源码逐字节对比通过。机器可读记录见[verified-summary.json](verified-summary.json)。收尾文档提交不改变已验证运行时代码。

## 仍未验证或未完成

ARM实体手机、温控与长时间运行尚未实测。原版全国逐格地图、官方完整开局、全特技交互、完整PK和事件及精确数值仍有缺口，不能将本轮自动行军和界面改善称为全玩法100%还原。完整状态见[FEATURES.md](../../FEATURES.md)；单挑/舌战界面和人物精细模型按本轮要求暂缓。
