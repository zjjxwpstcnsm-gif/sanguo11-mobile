# Batch 01：完整继承、逐源清单与原文件槽位边界

实现基点 8400301；继承封存提交 0a6fb12。完整 4299 文件/336374894 字节全等、168 固定资源通过、4 JNI 原输入保留，独立 Gradle/VM 缓存；checkpoint 丢失的 48 可执行权限按基点 Git mode 恢复，不改变字节。

工具 `audit_pc_restoration_sources.py` 完整只读扫描安装 1806 文件/2780250717 字节（包含根 .DS_Store 6148 字节；旧审计数量不直接移用）。发现 205 份类型 22 剧本，16 份主目录已解码；备份/附件只记候选和路径范围，实际激活、官方/MOD 身份与优先级未知。全部安装文件 SHA 在 source-manifest.json。

逐源 key 保留 path+完整 SHA，剧本 ID 为 pc-scenNNN-完整 SHA，符合现有 80 字符边界。重复内嵌编号 6 的两份源不合并。逐人物 metadata 与只读头像请求各 13600 行，独立 sourceVariant/nativeId；Scen014 native279→10333、333→10279 保留。10656 身份记录、666 独立人物、64 字形隔离、2880 额外槽均不扩大严格有效口径，完整字段人物与完整剧本仍为 0。

输出 metadata SHA c34aec664e825c91c8fb84c6eea30dab45ec44a8821b5bc05ad4aa37167b4046；头像需求 SHA e63fae48211df94dc66109a14fff2c8b522fe429e0051dd8e9128a5fec6420b0。媒体只读连接此清单，不改 metadata；未知身份不能由图像或 native 编号猜填。

原 EXE 43aa50 的槽位/格式准入实际执行 259 例，允许 0–254、拒绝负数/255/999，捕获原格式指针 7924f8 和参数，原 5MiB 代码/静态内存逐字节不变。路径为 media\scenario\Scen%03d.s11，Shared 为独立表指针。停在 sprintf/安全 cookie 前；不宣称实际菜单枚举、OS 文件优先级、外部补丁或完整开局事件已执行。报告 SHA a23f77ded5027b92e9976bc8db451b462a9e01bfaf7517a67220946643bb5c1c，第二次输出全等。

来源工具六项测试通过：逐源换位/字形保留、SHA 改动拒绝、Scen014 错接拒绝、身份冲突拒绝、catalog/前审计更换拒绝、PC 输出拒绝和相同备份仍独立。首次错接测试使用了跨源相同字节，最终唯一键守卫已拒绝，但错误断言预期不同；保留失败日志，改用真实换位记录证明前置字节拒绝。转换最终两次 JSON/gzip 字节一致，见 source-repeat-verification.json。

仍未完成：字形原映射、全部原传记 LS11 解压/索引、205 候选的实际生效链、原开局后处理/事件、全部设施/资源/外交/人物字段语义及正式来源新局转换。当前源清单未改规则或旧档。DTO 与页面增量另批验证，不把此表格通过称 APK/正常流程完成。

可复现：

```sh
python3 tools/content/audit_pc_restoration_sources.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/session1/source-repeat
PC_INSTALLATION='/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' PYTHONPATH=tools/content python3 tools/content/test_pc_restoration_sources.py
PYTHONPATH=out/session1/toolchain/pc-emulate:tools/content python3 tools/content/inspect_pc_restoration_loader.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版' --output out/session1/loader-format-repeat.json
```
