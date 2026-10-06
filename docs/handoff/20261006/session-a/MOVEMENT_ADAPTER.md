# B 请求的 A 行军入口适配

B 只读交接：f55b/session-b/CONTRACT.md 的 A exact movement adapter request；B 负责 MarchOrders.arrivalDestination 的规则修复，不由 A 改 core/API/runtime 或保存策略。

MainActivity.previewMarch 对普通军事行军使用已有纯 previewMove(unit.id, exactAxialTarget)。新增 previewTargetMarch 保留原目标推断，仅用于明确自动攻击/接近路径、已显示非MOVE任务的改选，以及已有运输改道。显式进驻仍调用 previewCity；UI routeMove 保存/恢复标记保留。正常军事行军提示不再承诺点城格自动入城。

不能据此单独宣称普通进城格保留部队已修复：A 当前完整基点仍是0e7b9bc2，其旧 core 尚有普通移动自动入库逻辑。需等待 B 的完成提交冻结，最终串行组合后从 Source14 刘备永安携金3000的正常部署→己方城格移动→补金/取消/确认→下一旬施工→保存读取路径验收。不得用人为单位/snapshot替代此正常路径。

UiTheme/FactionColors 的生产修复已在A分支提交（d7221f8e起及c6327130主题后继），B只读接口与实际依赖以本分支最终冻结增量SHA清单为准，不借用WIP或并写MainActivity。
