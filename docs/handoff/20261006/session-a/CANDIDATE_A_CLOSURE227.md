# A 完成依赖闭包 227：替代 226 的最终集成输入

226核对的是候选实际编译的主题子集，对从原始source archive重建的路径覆盖不充分：旧原文件即使已由外部主题编译输入替代，仍需要精确后态。227同时比较原始源码与编译子集，补入5份已完成A主题文件（FactionColors、UiTheme、两份可读文本色、styles）。**最终集成使用227，226只保留历史取证，不能应用226后关闭外部主题输入。**

固定增量：`out/session-a/candidate-a-closure227/a-production-closure.tar.gz`，SHA `fa81f88a31224cd71d7b448d30a7937bbbca290f88e5b68c7931c31bc1852dcb`，37 A 路径；34份候选前态文件直接由固定source tar读回逐SHA验证，新文件以absent前态。所有最终后态在tar读回核验。具体路径/前后SHA、共享根与Bridge守卫、合成源码清单与实际stage224全部app文件同SHA数量，见 CANDIDATE_A_CLOSURE227.json。

只包含已提交A生产依赖与224 staged Main/picker，未包含任何B生产、共享根Gradle、Bridge、Unity或原四JNI修改；额外两JNI独立守卫。全部规范A生产仍保持当前媒体队列源码，未将新选项装入当前设备。旧主题子集不能在应用227后再覆盖Main；应只编译正式新37路径，B选项/API/core/runtime/两页使用candidate60冻结闭包。

共同基点、完整A源225（11330文件、SHA `05a6c153…`）、三份冻结B JAR、224实际控件步骤和四参数入口说明仍见224/226；完整源225是已提交当前A生产加224补丁/工具，不是已安装新选项。复现脚本 `freeze_candidate_a_closure226.py --revision 227` 制作227，拒绝覆盖既有输出。旧226脚本首次遗漏原始主题前态，本次修正且保留旧回执，不隐去历史差异。

227源码合成清单与stage224已编译全部app Java/资源/资产逐SHA一致，只证明精确输入一致和已记录编译；须独立构建组合APK、核168与原4/新增2JNI、每次安装前完整备份并实际正常新局/取消/单挑/多旬/保存/冷重开后恢复读回。已有包分项成绩不转移；5554媒体/音频/双堆队列继续，ARM和整个目标仍未完成。
