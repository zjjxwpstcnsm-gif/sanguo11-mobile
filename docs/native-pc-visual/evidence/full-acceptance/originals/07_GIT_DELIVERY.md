# 串行 Git、PR 与 APK 交付

## 1. 先读，再写

R00 确认当前目录/工作树、remote URL、最新 main、已知基线的祖先关系、现有 `agent/native-pc-visual` 和对应 PR。不要把缓存 README 的版本当最新状态。

存在本地未提交修改先识别来源，不能 `reset --hard`、清理掉用户文件或跨分支覆盖。多个 Agent 用隔离 clone；本路线只允许一位串行 Agent 写目标分支。

本包默认不合并任何旧 PR、不改 main、不删分支。历史提示词曾要求合并不代表当前阶段仍授权；本包是新串行路线。

## 2. 首个 PR 与持续交付

R00 有真实源码/资源修复后立即 commit/push 并创建一个指向 main 的 Draft PR。不要用只加 progress.md 的空壳 PR 冒充代码交付。后续 R01–R18 更新相同 PR 并按阶段补交付记录。

提交信息建议 `native-pc/Rxx: <实际变更>`。一个阶段可有多个聚焦提交，但最终回复前必须将本阶段全部必要源码、资源和验证工具推到远端。不要把 APK、SDK、私钥、缓存、巨量重复截图直接提交仓库。

PR 已意外关闭/合并时先读实际状态。用户已授权的 main 新进展可以在保留串行成果的前提下整合并回归；不自行把串行分支重建成旧 main。需要新 PR 时保留祖先与交接原因，不能偷偷回退。

## 3. 失败重试有幂等性

任何 connector 写入超时/报错，先读取远端验证实际结果。已出现目标 SHA/PR/运行，不重复创建。网络瞬时失败最多做合理的有限重试；权限问题不要无限刷请求。

`git ls-remote` 或连接器分支读取应确认最终 HEAD；本地 clean 并不能证明远端已更新。不能只贴本地 commit hash 就说 push 成功。

## 4. 源码与 APK 身份

推荐顺序：

1. 修改源码/资源/测试，完成本地受影响验证，提交到分支。
2. 以明确的完整 source SHA 构建/运行；CI 必须 checkout 这个 SHA，并验证 HEAD，而非拉取易变的 latest。
3. 检查 APK 内 BuildConfig SOURCE_REVISION 与该源码一致；无 .git 的构建不得只写 `source-archive` 而不另附可靠完整 SHA。
4. 记录 APK SHA-256、应用 ID、versionCode/versionName、ABI、开发签名证书指纹、实际后端、构建命令、CI job 和测试证据。
5. 若最后仅追加报告，记录 evidence-only HEAD；证明它相对 source SHA 不改变运行源码/资产。任何生产改动都必须重新构建。
6. 复读远端及 PR，确认最新提交与报告相符。

保持现有 applicationId 和开发签名，以支持覆盖安装；versionCode 随交付递增，不硬编码本包日期或旧版本号。开发密钥不是生产签名；不能泄露/生成伪称生产签名的证书。

提供适合手机安装的 ARM64 或 universal **独立 .apk**；x86_64 模拟器 APK 应另外标注。原生包里存在 Filament 的 .so 是正常的，不要为了声称“纯 Java”删除 JNI。不得包含 libunity.so 却称已去掉 Unity，反之也不能拿无 Player 的包冒称 Unity。

## 5. 下载与留存

交付独立 APK + evidence.zip；优先使用可访问的 CI artifact/用户可取的附件。仅存在本地路径不算可下载。给出真实文件路径/链接并验证文件存在；标注链接是否需要 GitHub 登录以及制品保留策略/有效期（能确认时）。

不使用 `latest` 的旧 Release 代替当前构建。证据包中的 manifest 要能关联截图、日志、设备、源码与 APK。没有可用云发布权限就提供本会话真实可下载文件，不能编造 release URL。

## 6. 最终回应模板

阶段：Rxx，状态：PASS/PARTIAL/BLOCKED。
源码：完整 source SHA；远端：最终 HEAD；PR：实际链接，保持未合并。
结果：实际完成的功能与画面改善、运行入口。
验证：主机、构建、模拟器、ARM64、视觉、性能逐项，列新增/继承失败。
制品：独立 APK、hash、应用/版本/ABI；证据包。
未完成：具体缺陷、需要补的验收与下一阶段必须先修的阻塞。

此模板不允许将“已写出脚本”改写成“已运行脚本通过”。
