# 项目进度

日期：2026-09-13。仓库：`zjjxwpstcnsm-gif/sanguo11-mobile`；可见性：private。

- 已确认用户最终要求：独立手机版、全玩法/武将/兵种/城池/地图、最终APK。
- 已建立PC本体＋PK暂定范围和官方资料索引；完整原版数据与隐藏规则尚未核对。
- M0：实现规则基础验证工程，不是完整复刻。
- 本地 `bash scripts/test-core.sh` 通过：193项断言。包含命令失败原子性、资源守恒、寻路阻挡、重复行动、战斗/攻城、断粮、版本/CRC校验、状态引用完整性、读档后确定性续局。
- Android构建通过：源码提交 `9c651982a9852019a6763b470b506f6b97db7ce0`，核心193项断言、`:app:assembleDebug`、`:app:lintDebug` 和 APK v2 签名校验全部通过。
- [构建记录](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752227881)
- [M0工程验证APK下载（ZIP内含app-debug.apk及SHA256SUMS）](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/34752227881/artifacts/10316257306)。需要登录有仓库权限的GitHub账号，产物有效期至2026-10-13，可通过CI重新生成。
- 该包是调试签名，尚未进行实际安装、真机触控或性能验证。完整复刻游戏尚未完成。

## 下一步

1. 保持现有核心测试及Android构建通过，再推进后续规则和内容。
2. 锁定原版具体版本，取得可核验的规则观察和合法可用的数据来源。
3. 完成M1地图/角色/剧本数据规格及来源记录，再推进完整全国战略层。
4. Android真机验证缩放、点选、暂停恢复、离线读档、安装升级和性能。

没有完成的项目继续保留在 `docs/FEATURES.md` 中，不可将M0的近似实现标成原版等价。
