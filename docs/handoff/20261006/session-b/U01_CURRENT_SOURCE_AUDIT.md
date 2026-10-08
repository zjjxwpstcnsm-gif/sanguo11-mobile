# U01 当前来源语义审计

2026-10-08。未修改Unity、黄金文件、Bridge序列化、地图资源或规则。当前冻结61-r5 JAR重新运行原BridgeFixtureRecorder，仍是coalition-190/玩家0/固定20260923 seed，差异数据独立写u01-current61-readonly/difference.json。

黄金源提交faaa0e40，记录mapRevision63；现录mapRevision65，其它标量一致。terrain字符串都是59800字，恰19642格不同。11实体仅港口q/r变化，其余id/name/owner/troops/energy/officerId/gold/food/order都一致；非新增机构或兵值漂移。完整逐实体before/current见u01-current61-difference.json。

加载链证据：coalition-190.properties明确community-reference/重建（不能叫原官方剧本），ScenarioCatalog→ScenarioData→NationalMap.attach，依checksum固定national-map-v056.properties（SHA800f3471…c1d、revision65）覆盖全国地形和据点位置；MapCoordinates随后把200x200源坐标转到共享奇偶坐标。Gold的静态最新目录读取并未冻结旧map资源，因此fixture生成随着明确新局资源修订变化。NationalMap.validateIdentity接受旧63等已存修订，SaveCodec读取旧档本身terrain/mapRevision/坐标；NaturalStructures.seedOpening只对新局调用、65新增四原堤防，不对旧档复生。此查明的是来源差异，不能据此把黄金自动改成65或让旧档换图。

这轮又直接只读原PC整合安装Media/san11pkres.bin，完整SHAe61c97fe…9c31a；LINK资源4791/525470903/440008B/SHEX0008，SHAc726989d…6112全部实核，不从网页表格或旧记忆代替。照既有import_pc_map.py明确CODES词汇映射，column-major逐40000源cell对照当前地图全部一致、旧参考63恰19642分类不同，591开发格计数一致。全部87据点中心在当前源bytes分别为42 city type16/10 gate type18/35 port type17，11移动港坐标和源类型再核一致；JSON保存原region byte，不据此额外推原行政归属。证据u01-current61-pc-terrain-source.json。

原20类中土/荒地合并平原、岸/崖阻挡是现有映射边界；原20类型在PC数据保留。港名/稳定ID接线沿已声明对应，开发地父城沿现有行政表，不把40000地形核对夸成全部剧本/归属/事件/完整原玩法已还原。

待最终集成决定：U01静态fixture该绑定明确旧63源，还是另加明确新65 fixture/版本选择。必须各自有来源、旧存续行和Unity Editor实际读入证据；未经此闭合，本旧cmp仍失败，不改golden、Unity或Bridge编码掩盖。当前差异已被真实源解释，Unity适配/旧地图全流程及ARM仍未验收。
