# R24 UI17顺序集成与v34交易、2D验证

完整AN已逐文件校验：4050文件301395977字节，manifest SHA `4dfb6cc8e1d964557c7e744afde5b013ba4968a5c48678e164c7ee74d1dccae2`。它含全部继承dirty成果、固定资源和4原生输入；没有从旧HEAD重建基线。

本轮仅合入UI完成提交`13ff5b9→c03174e36597f0d445bcbd97c4aed2f3f3904b5d`的19文件。4个app文件、Instrumentation、UI文档与图片逐一校验root原文件等于父提交，新增文件不得覆盖现有文件；progress仅附加新段。666份core/API/runtime/数据工具与原数据审计文件合入前后SHA相同。没有拷入UI旧AA核心、companion或WIP，没有发送消息或操作5580。`out/parity/ui-integration-c03174e/integration.json`及完整binary patch保留来源。

UI改动覆盖2D跨面板整次手势取消、失焦/后台清理惯性与拖放、3D小地图多指取消及识别器清理；暂停UI恢复hash复用成功原子保存的同一权威字节。规则/事件/StateToken边界保持R23。当前wall计时口径仍是UI17历史实现，下一UI18完成增量将明确纠正，不能把本批total当端到端墙钟。

组合双包26秒82任务构建成功，状态29361和typed交易168通过，架构检查通过，168固定资源/4原生输入/成长官职表全等。主87040486B SHA `bb6d8d1ceaa758d13a8ed087531950d6fbe340fdf7723dbf01b3315252db07d8`；test751443B SHA `0baf442df7a51a0d3692cacfe9e549a7e7e0ba767cd4d3e5fc86fbc5bd6cecc3`。封存在`out/parity/ui-integration-c03174e/apks`。

5554实际安装完整回读。使用正常ART v34的190曹操新局，236968B SHA636854cc…aaed：交易99项85.94秒通过，真实买粮、同旬拒绝、下一旬卖粮、取消/重复提交及完整存取恢复；2D mapEdges32项31.75秒通过，实际多指/缩放/跨面板取消/惯性→真实Home→前台/旋转及权威字节保持。合计131项，各套分别恢复起点。新版旋转helper额外确认真实布局与系统输入恢复，旧交易97的数量不能移用于本次99。R23横屏失败完整保留，本次成功不证明唯一原因已定位。

同一组合APK生产ART复验：功绩421/3.28秒、商人算术11595/.21秒、原能力4813行+日期3120检查/.21秒、官职2244/11.11秒、人物状态29361/12.64秒通过；同一独立测试probe6946fe32…bec7不含生产类/来源表，正常新局生成再次字节全等。用户7文件及整个APK前后全等。各UI套也恢复7原文件全等、无新增，auto82554269…99f0a9。无ARM真机/性能结论。

本中间组合包没有重新验证opening/mapNative/march/governanceArmy；不移植R23或UI独立5580结果。UI18完成提交`0ddedf6fd379b3a87df206bc1d09f0e1662155ab`已收到，先冻结完整AO再按c03174e→0ddedf6顺序合入其app/测试/docs，最后组合包继续新局、原生/2D行军、多回合/存取与规则验证。下一基点必须是0ddedf6，不能跳过c03174e。整体goal未完成，原交易价格/数量月结、非商人经验、官方/MOD启用、全数据与历史规则失败继续待处理。

完整结果见`round24-installed.json`；AO源位于`out/parity/checkpoint-20261003-ao/source`，以manifest/verified/completion实际核验为准。
