# 第六批：出征权威接口消费

2026-10-03：新版接口构建的57项实际交互检查通过，完整目标未完成。

DeployWizard消费GameApi.preview(DeploymentCommand)，合法性/稳定原因字段/真实数量范围/留守资源/AP/攻防/移动/后勤来自不可变DeploymentPreview。最终确认调用typed execute，以会话token重新验证，成功后从最新已提交World定位真实部队，不从旧镜像猜部队ID。删除UiModels.deployTroopCap及表单内重复校验；不会在UI计算战斗公式。

QuantityControl支持权威返回的空区间，不再把max强行抬到min；调整范围保留输入。搜索与切页不发预览，100ms合并连续输入，待核对时禁用提交，关闭移除待执行回调。统一弹窗禁用按钮颜色，并区分错误与可执行摘要。地形详情使用已有中文名称。

[57项安装结果](evidence-06/interaction.txt)：真实选择零库存木兽和斗舰，检查STOCK_INSUFFICIENT及weapon/ship字段、空数量范围；非法兵力检查LOAD_RANGE；攻防/AP与权威DTO比对；取消、旋转、修改输入均不改完整状态/RNG；双击只新增一队及一个authority revision；正式读档恢复所有字节。[自动/手动存档及模板库SHA](evidence-06/save-integrity.json)一致。没有直接调用deploy作为触控替代。

[初版禁用状态](evidence-06/before-disabled-state-run02.png)→[明确禁用与库存原因](evidence-06/02b-stock-error.png)，[输入错误](evidence-06/03-quantity-error-keyboard.png)、[权威数值](evidence-06/04-details.png)、[出征后自动定位部队](evidence-06/06-deployed-map.png)。完整录像out/uiux/iteration-06/run05/interaction.mp4，顺序解码与PTS在run05/frames。run02为52项通过，run04/05为57项；每版APK身份单独封存。

APK out/uiux/iteration-06/app-uiux-run05.apk，SHA256 `5b084cb272248e31fd4ffac11fc0eb8eb8eda583b682f8c7435966e44a63b4b7`，82239116字节；测试包同目录app-uiux-test-run05.apk。源码身份为d63ebb3加本批UI变更，规则依赖是玩法冻结checkpoint-20261003-c，不能把它称为原始规则基线构建。自己的core/data/tools目录504文件SHA仍与初始继承基线相同，冻结模块344文件逐项与玩法manifest相同。

**本分支此后构建需要新接口依赖。** 按[构建与集成说明](CONTRACT_BUILD.md)使用伴随build-inputs.tar.gz和manifest；其中也保存4个必要原生库。最终由玩法会话集成UI增量，不能用旧core直接构建新UI。未修改或覆盖另一会话文件。

本地重新运行冻结权威专项468+331=799项通过；UI模型49012、地形451、几何1136811、坐标1168801/898305、围城11371+overlay14通过。UI原6项重复上限断言随已删除的重复公式移除，对应规则由权威专项覆盖，实际表单消费由安装测试覆盖；没有压低任何历史失败断言。架构和168项固定资源检查通过。

性能仍未通过：本次13次实际查询耗时108–276ms，原始[采样](evidence-06/preview-timing.json)及run05/runtime.log保留；run02曾为87–239ms、run04为97–382ms。不同运行并非隔离性能基准，但证实同步World复制会卡主线程。已向玩法会话提交缓存/异步快照契约；暂不自行改core。合法预览后的特技/战法/射程补充说明仍用既有核心只读接口，尚待DTO补足。

继续推进内政、行军/攻击/战法/计略、外交和连续回合；原生全国加载仍慢，更多尺寸与ARM真机未验证。不能将本批出征通过等同完整目标完成。
