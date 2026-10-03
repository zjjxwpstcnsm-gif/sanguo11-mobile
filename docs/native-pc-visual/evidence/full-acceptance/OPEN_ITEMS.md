# 当前未满足条目

与matrix.json逐条一致；历史记录另存，不自动关闭。

## FAIL

| ID | 来源 | 具体缺口 |
|---|---|---|
| R00.I04 | prompts/R00_PROMPT.md:33 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.V01 | prompts/R00_PROMPT.md:41 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.G01 | prompts/R00_PROMPT.md:51 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R01.I03 | prompts/R01_PROMPT.md:32 | FilamentMapView构造器owner线程同步贴图decode；SceneWorkQueue生成与owner上传有效，但复合要求解码后台化不满足。 |
| R01.V01 | prompts/R01_PROMPT.md:40 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.G01 | prompts/R01_PROMPT.md:50 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R05.I03 | prompts/R05_PROMPT.md:32 | 本次API35原生生命周期正式平原场景surface.png显示明显逐格阶梯岸线；WaterVisualField明确保留矩形并集轮廓、仅平滑材质带，SceneMesh按整格water分组三角形。要求的岸线几何约束细化未落实。 |
| R05.G01 | prompts/R05_PROMPT.md:50 | 实际安装Surface原图仍有规则阶梯岸线；水体已渲染和港口存在，但连续可信岸线的完整视觉条件未满足。没有改变拓扑来制造通过。 |
| R06.I05 | prompts/R06_PROMPT.md:34 | FilamentMapView构造器同步loadGroundMaterials/loadAtlas/BitmapFactory.decodeStream；mesh异步队列不等于纹理全部异步。共享Proxy.shape.references存在，不宣称完全没有引用计数。 |
| R12.I01 | prompts/R12_PROMPT.md:30 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.V04 | prompts/R12_PROMPT.md:43 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.G01 | prompts/R12_PROMPT.md:50 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| X03.L009 | 03_RENDER_CONTRACTS.md:9 | 构造器同步loadAtlas/decodeStream；只有CPU mesh异步，不满足全部耗时解码放后台。 |
| X04.L040 | 04_VISUAL_ACCEPTANCE.md:40 | API29/35正常冷启动全国预览均原120秒ready超时，纯触控流程未完成V0。 |
| X06.L007 | 06_TEST_AND_PERF.md:7 | 安装成功，API29/35正式全国/原R12初始ready均失败；后续操作未到达。 |
| X06.L030 | 06_TEST_AND_PERF.md:30 | 旧最终20/18失败；当前API29为17次切换/0前后台后ready失败，API35为20/13后background frame loop stopped。两设备均未完成20+20，后续故障注入未到达。 |

## PARTIAL

| ID | 来源 | 具体缺口 |
|---|---|---|
| R00.I01 | prompts/R00_PROMPT.md:30 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.I02 | prompts/R00_PROMPT.md:31 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.I03 | prompts/R00_PROMPT.md:32 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.I07 | prompts/R00_PROMPT.md:36 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.I08 | prompts/R00_PROMPT.md:37 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.V02 | prompts/R00_PROMPT.md:42 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.V04 | prompts/R00_PROMPT.md:44 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.V05 | prompts/R00_PROMPT.md:45 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R00.D01 | prompts/R00_PROMPT.md:57 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| R01.I01 | prompts/R01_PROMPT.md:30 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.I02 | prompts/R01_PROMPT.md:31 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.I04 | prompts/R01_PROMPT.md:33 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.I05 | prompts/R01_PROMPT.md:34 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.I06 | prompts/R01_PROMPT.md:35 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.V03 | prompts/R01_PROMPT.md:42 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.V05 | prompts/R01_PROMPT.md:44 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.D01 | prompts/R01_PROMPT.md:56 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R02.I01 | prompts/R02_PROMPT.md:30 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.I02 | prompts/R02_PROMPT.md:31 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.I03 | prompts/R02_PROMPT.md:32 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.I04 | prompts/R02_PROMPT.md:33 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.I05 | prompts/R02_PROMPT.md:34 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.I06 | prompts/R02_PROMPT.md:35 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.V01 | prompts/R02_PROMPT.md:40 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.V02 | prompts/R02_PROMPT.md:41 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.G01 | prompts/R02_PROMPT.md:50 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.D01 | prompts/R02_PROMPT.md:56 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R03.I01 | prompts/R03_PROMPT.md:30 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I02 | prompts/R03_PROMPT.md:31 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I03 | prompts/R03_PROMPT.md:32 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I04 | prompts/R03_PROMPT.md:33 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I05 | prompts/R03_PROMPT.md:34 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I06 | prompts/R03_PROMPT.md:35 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I07 | prompts/R03_PROMPT.md:36 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.I08 | prompts/R03_PROMPT.md:37 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.V01 | prompts/R03_PROMPT.md:41 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.V02 | prompts/R03_PROMPT.md:42 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.V03 | prompts/R03_PROMPT.md:43 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.G01 | prompts/R03_PROMPT.md:51 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.D01 | prompts/R03_PROMPT.md:57 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R04.I01 | prompts/R04_PROMPT.md:30 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.I02 | prompts/R04_PROMPT.md:31 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.I03 | prompts/R04_PROMPT.md:32 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.I04 | prompts/R04_PROMPT.md:33 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.I05 | prompts/R04_PROMPT.md:34 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.I06 | prompts/R04_PROMPT.md:35 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.I07 | prompts/R04_PROMPT.md:36 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.V01 | prompts/R04_PROMPT.md:40 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.V03 | prompts/R04_PROMPT.md:42 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.V04 | prompts/R04_PROMPT.md:43 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.V05 | prompts/R04_PROMPT.md:44 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.G01 | prompts/R04_PROMPT.md:50 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R04.D01 | prompts/R04_PROMPT.md:56 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R05.I01 | prompts/R05_PROMPT.md:30 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.I02 | prompts/R05_PROMPT.md:31 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.I04 | prompts/R05_PROMPT.md:33 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.I05 | prompts/R05_PROMPT.md:34 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.I06 | prompts/R05_PROMPT.md:35 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.I07 | prompts/R05_PROMPT.md:36 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.V01 | prompts/R05_PROMPT.md:40 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.V02 | prompts/R05_PROMPT.md:41 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.V03 | prompts/R05_PROMPT.md:42 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.D01 | prompts/R05_PROMPT.md:56 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R06.I01 | prompts/R06_PROMPT.md:30 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.I02 | prompts/R06_PROMPT.md:31 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.I03 | prompts/R06_PROMPT.md:32 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.I04 | prompts/R06_PROMPT.md:33 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.I06 | prompts/R06_PROMPT.md:35 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.I07 | prompts/R06_PROMPT.md:36 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.V01 | prompts/R06_PROMPT.md:40 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.V02 | prompts/R06_PROMPT.md:41 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.V03 | prompts/R06_PROMPT.md:42 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.V04 | prompts/R06_PROMPT.md:43 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.V05 | prompts/R06_PROMPT.md:44 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.G01 | prompts/R06_PROMPT.md:50 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R06.D01 | prompts/R06_PROMPT.md:56 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| R07.I01 | prompts/R07_PROMPT.md:30 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I02 | prompts/R07_PROMPT.md:31 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I03 | prompts/R07_PROMPT.md:32 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I04 | prompts/R07_PROMPT.md:33 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I05 | prompts/R07_PROMPT.md:34 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I06 | prompts/R07_PROMPT.md:35 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I07 | prompts/R07_PROMPT.md:36 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.I08 | prompts/R07_PROMPT.md:37 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.V01 | prompts/R07_PROMPT.md:41 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.V02 | prompts/R07_PROMPT.md:42 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.V03 | prompts/R07_PROMPT.md:43 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.G01 | prompts/R07_PROMPT.md:51 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.D01 | prompts/R07_PROMPT.md:57 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R08.I01 | prompts/R08_PROMPT.md:30 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.I02 | prompts/R08_PROMPT.md:31 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.I03 | prompts/R08_PROMPT.md:32 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.I04 | prompts/R08_PROMPT.md:33 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.I05 | prompts/R08_PROMPT.md:34 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.I06 | prompts/R08_PROMPT.md:35 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.I07 | prompts/R08_PROMPT.md:36 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.V01 | prompts/R08_PROMPT.md:40 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.V02 | prompts/R08_PROMPT.md:41 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.V04 | prompts/R08_PROMPT.md:43 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.G01 | prompts/R08_PROMPT.md:50 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.D01 | prompts/R08_PROMPT.md:56 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R09.I01 | prompts/R09_PROMPT.md:30 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.I02 | prompts/R09_PROMPT.md:31 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.I03 | prompts/R09_PROMPT.md:32 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.I04 | prompts/R09_PROMPT.md:33 | LandscapeProfile为全局确定参数并被正常SceneMesh/Vegetation使用；未发现按样板坐标/截图相机选择专用分支。profile存在不表示已通过V2，故仍PARTIAL。 |
| R09.I05 | prompts/R09_PROMPT.md:34 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.I06 | prompts/R09_PROMPT.md:35 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.I07 | prompts/R09_PROMPT.md:36 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.V01 | prompts/R09_PROMPT.md:40 | 正常地图源码调用LandscapeProfile；HOST R09检查样板在真实地图。正式入口无独立美化Activity；可达不等于本次3D ready。 |
| R09.V05 | prompts/R09_PROMPT.md:44 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.G01 | prompts/R09_PROMPT.md:50 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.D01 | prompts/R09_PROMPT.md:56 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R10.I01 | prompts/R10_PROMPT.md:30 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.I02 | prompts/R10_PROMPT.md:31 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.I03 | prompts/R10_PROMPT.md:32 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.I04 | prompts/R10_PROMPT.md:33 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.I05 | prompts/R10_PROMPT.md:34 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.I06 | prompts/R10_PROMPT.md:35 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.I07 | prompts/R10_PROMPT.md:36 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.V01 | prompts/R10_PROMPT.md:40 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.V02 | prompts/R10_PROMPT.md:41 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.G01 | prompts/R10_PROMPT.md:50 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.D01 | prompts/R10_PROMPT.md:56 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R11.I01 | prompts/R11_PROMPT.md:30 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.I02 | prompts/R11_PROMPT.md:31 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.I03 | prompts/R11_PROMPT.md:32 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.I04 | prompts/R11_PROMPT.md:33 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.I05 | prompts/R11_PROMPT.md:34 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.I06 | prompts/R11_PROMPT.md:35 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.I07 | prompts/R11_PROMPT.md:36 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.V01 | prompts/R11_PROMPT.md:40 | 主机fixture验证部分typed events；没有逐一真实产生全部War.Plot及截图，CALM/EXTINGUISH未有完整安装证据。 |
| R11.V02 | prompts/R11_PROMPT.md:41 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.V03 | prompts/R11_PROMPT.md:42 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.G01 | prompts/R11_PROMPT.md:50 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.D01 | prompts/R11_PROMPT.md:56 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R12.I02 | prompts/R12_PROMPT.md:31 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.I03 | prompts/R12_PROMPT.md:32 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.I04 | prompts/R12_PROMPT.md:33 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.I05 | prompts/R12_PROMPT.md:34 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.I06 | prompts/R12_PROMPT.md:35 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.I07 | prompts/R12_PROMPT.md:36 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.D01 | prompts/R12_PROMPT.md:56 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R13.I01 | prompts/R13_PROMPT.md:30 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.I02 | prompts/R13_PROMPT.md:31 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.I03 | prompts/R13_PROMPT.md:32 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.I04 | prompts/R13_PROMPT.md:33 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.I05 | prompts/R13_PROMPT.md:34 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.I06 | prompts/R13_PROMPT.md:35 | MapLibrary内部使用AtomicFile，但MapEditorActivity SAF export直接openOutputStream("wt")写入；外部provider断写/回滚未验证，不能凭内部原子存储宣布全PASS。 |
| R13.I07 | prompts/R13_PROMPT.md:36 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.V04 | prompts/R13_PROMPT.md:43 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.V05 | prompts/R13_PROMPT.md:44 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.G01 | prompts/R13_PROMPT.md:50 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.D01 | prompts/R13_PROMPT.md:56 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R14.I01 | prompts/R14_PROMPT.md:30 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.I02 | prompts/R14_PROMPT.md:31 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.I03 | prompts/R14_PROMPT.md:32 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.I04 | prompts/R14_PROMPT.md:33 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.I05 | prompts/R14_PROMPT.md:34 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.I06 | prompts/R14_PROMPT.md:35 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.I07 | prompts/R14_PROMPT.md:36 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.V01 | prompts/R14_PROMPT.md:40 | 此次focused-window API29/35均初始ready失败，月份循环未执行；另行当前fixture双API的跨月/跨年/重建原整屏正确，各50对完整存档一致，但四季/正式剧本日期未全覆盖，旧日期缺陷未关闭。 |
| R14.G01 | prompts/R14_PROMPT.md:50 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.D01 | prompts/R14_PROMPT.md:56 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| X03.L017 | 03_RENDER_CONTRACTS.md:17 | 现有GridWorldTransform/Camera保留，HOST坐标测试执行；设备视口与缩放矩阵未完整执行。 |
| X03.L033 | 03_RENDER_CONTRACTS.md:33 | TerrainSurface.renderedHeight采用与mesh同源三角面；山后/移动单位/模型优先级安装检验不足。 |
| X04.L042 | 04_VISUAL_ACCEPTANCE.md:42 | V1未关闭；缺本次完整近中远两方向关网格正式地图对照，保留历史视觉FAIL。 |
| X04.L044 | 04_VISUAL_ACCEPTANCE.md:44 | V2未关闭；当前全国预览/正式初始ready失败，fixture不能作为局部完整样板。 |
| X06.L005 | 06_TEST_AND_PERF.md:5 | 20主机回归exit0；完整core输入/候选42调用均12通过30失败，不能称全量core通过。 |
| X06.L014 | 06_TEST_AND_PERF.md:14 | API29/35当前fixture各12组合/各50对完整sg11逐字节一致；不能外推所有必含场景。 |
| X06.L043 | 06_TEST_AND_PERF.md:43 | 有界CPU队列与每帧最多8块/4ms墙钟上传；原2ms是起始目标非GPU实测，贴图仍同步，ARM64未测。 |
| X06.L045 | 06_TEST_AND_PERF.md:45 | GPU bytes仅模型/纹理估算；没有驱动实分配内存测量。 |
| X06.L046 | 06_TEST_AND_PERF.md:46 | universal APK 37387038 bytes已独立核验；没有单独ARM64 APK大小，不能混比。 |

## NOT_RUN

| ID | 来源 | 具体缺口 |
|---|---|---|
| R02.V03 | prompts/R02_PROMPT.md:42 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R02.V04 | prompts/R02_PROMPT.md:43 | NativeR02Test仅计算三档bw/bh后重复pick，没有调整安装renderer内部缓冲并发送同一view触点；不能用该HOST测试替代设备要求。 |
| R02.V05 | prompts/R02_PROMPT.md:44 | 数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| R03.V04 | prompts/R03_PROMPT.md:44 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R03.V05 | prompts/R03_PROMPT.md:45 | 同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| R04.V02 | prompts/R04_PROMPT.md:41 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| R05.V04 | prompts/R05_PROMPT.md:43 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R05.V05 | prompts/R05_PROMPT.md:44 | 水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| R07.V04 | prompts/R07_PROMPT.md:44 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R07.V05 | prompts/R07_PROMPT.md:45 | 正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| R08.V03 | prompts/R08_PROMPT.md:42 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R08.V05 | prompts/R08_PROMPT.md:44 | 确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| R09.V02 | prompts/R09_PROMPT.md:41 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.V03 | prompts/R09_PROMPT.md:42 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R09.V04 | prompts/R09_PROMPT.md:43 | V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| R10.V03 | prompts/R10_PROMPT.md:42 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.V04 | prompts/R10_PROMPT.md:43 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R10.V05 | prompts/R10_PROMPT.md:44 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| R11.V04 | prompts/R11_PROMPT.md:43 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R11.V05 | prompts/R11_PROMPT.md:44 | HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| R12.V03 | prompts/R12_PROMPT.md:42 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.V05 | prompts/R12_PROMPT.md:44 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R13.V01 | prompts/R13_PROMPT.md:40 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.V02 | prompts/R13_PROMPT.md:41 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R13.V03 | prompts/R13_PROMPT.md:42 | 内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| R14.V02 | prompts/R14_PROMPT.md:41 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.V03 | prompts/R14_PROMPT.md:42 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| R14.V04 | prompts/R14_PROMPT.md:43 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| X00.L003 | 00_GLOBAL_RULES.md:3 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L008 | 00_GLOBAL_RULES.md:8 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L010 | 00_GLOBAL_RULES.md:10 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L014 | 00_GLOBAL_RULES.md:14 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L016 | 00_GLOBAL_RULES.md:16 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L018 | 00_GLOBAL_RULES.md:18 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L020 | 00_GLOBAL_RULES.md:20 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L022 | 00_GLOBAL_RULES.md:22 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L028 | 00_GLOBAL_RULES.md:28 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L032 | 00_GLOBAL_RULES.md:32 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L038 | 00_GLOBAL_RULES.md:38 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L040 | 00_GLOBAL_RULES.md:40 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L042 | 00_GLOBAL_RULES.md:42 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L044 | 00_GLOBAL_RULES.md:44 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L048 | 00_GLOBAL_RULES.md:48 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L050 | 00_GLOBAL_RULES.md:50 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L052 | 00_GLOBAL_RULES.md:52 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L056 | 00_GLOBAL_RULES.md:56 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L057 | 00_GLOBAL_RULES.md:57 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L058 | 00_GLOBAL_RULES.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L060 | 00_GLOBAL_RULES.md:60 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L062 | 00_GLOBAL_RULES.md:62 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L066 | 00_GLOBAL_RULES.md:66 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L068 | 00_GLOBAL_RULES.md:68 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L070 | 00_GLOBAL_RULES.md:70 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L072 | 00_GLOBAL_RULES.md:72 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L076 | 00_GLOBAL_RULES.md:76 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X00.L078 | 00_GLOBAL_RULES.md:78 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X00.L080 | 00_GLOBAL_RULES.md:80 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X03.L003 | 03_RENDER_CONTRACTS.md:3 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L007 | 03_RENDER_CONTRACTS.md:7 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L011 | 03_RENDER_CONTRACTS.md:11 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L013 | 03_RENDER_CONTRACTS.md:13 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L019 | 03_RENDER_CONTRACTS.md:19 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L021 | 03_RENDER_CONTRACTS.md:21 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L027 | 03_RENDER_CONTRACTS.md:27 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L029 | 03_RENDER_CONTRACTS.md:29 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L031 | 03_RENDER_CONTRACTS.md:31 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L037 | 03_RENDER_CONTRACTS.md:37 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L039 | 03_RENDER_CONTRACTS.md:39 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L045 | 03_RENDER_CONTRACTS.md:45 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L047 | 03_RENDER_CONTRACTS.md:47 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L049 | 03_RENDER_CONTRACTS.md:49 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L053 | 03_RENDER_CONTRACTS.md:53 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L055 | 03_RENDER_CONTRACTS.md:55 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L059 | 03_RENDER_CONTRACTS.md:59 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X03.L061 | 03_RENDER_CONTRACTS.md:61 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X04.L005 | 04_VISUAL_ACCEPTANCE.md:5 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L007 | 04_VISUAL_ACCEPTANCE.md:7 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L011 | 04_VISUAL_ACCEPTANCE.md:11 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L013 | 04_VISUAL_ACCEPTANCE.md:13 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L015 | 04_VISUAL_ACCEPTANCE.md:15 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L017 | 04_VISUAL_ACCEPTANCE.md:17 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L019 | 04_VISUAL_ACCEPTANCE.md:19 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X04.L025 | 04_VISUAL_ACCEPTANCE.md:25 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L026 | 04_VISUAL_ACCEPTANCE.md:26 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L027 | 04_VISUAL_ACCEPTANCE.md:27 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L028 | 04_VISUAL_ACCEPTANCE.md:28 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L029 | 04_VISUAL_ACCEPTANCE.md:29 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L030 | 04_VISUAL_ACCEPTANCE.md:30 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L031 | 04_VISUAL_ACCEPTANCE.md:31 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L032 | 04_VISUAL_ACCEPTANCE.md:32 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L033 | 04_VISUAL_ACCEPTANCE.md:33 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L034 | 04_VISUAL_ACCEPTANCE.md:34 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L036 | 04_VISUAL_ACCEPTANCE.md:36 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L050 | 04_VISUAL_ACCEPTANCE.md:50 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L054 | 04_VISUAL_ACCEPTANCE.md:54 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X04.L056 | 04_VISUAL_ACCEPTANCE.md:56 | 已定位正式调用链，但本条全部条件未完成当前候选证明；同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| X05.L005 | 05_ASSET_PIPELINE.md:5 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L007 | 05_ASSET_PIPELINE.md:7 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L009 | 05_ASSET_PIPELINE.md:9 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L013 | 05_ASSET_PIPELINE.md:13 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L015 | 05_ASSET_PIPELINE.md:15 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L017 | 05_ASSET_PIPELINE.md:17 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L021 | 05_ASSET_PIPELINE.md:21 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L023 | 05_ASSET_PIPELINE.md:23 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L025 | 05_ASSET_PIPELINE.md:25 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L029 | 05_ASSET_PIPELINE.md:29 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L031 | 05_ASSET_PIPELINE.md:31 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L033 | 05_ASSET_PIPELINE.md:33 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L035 | 05_ASSET_PIPELINE.md:35 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L039 | 05_ASSET_PIPELINE.md:39 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L041 | 05_ASSET_PIPELINE.md:41 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L043 | 05_ASSET_PIPELINE.md:43 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L047 | 05_ASSET_PIPELINE.md:47 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L049 | 05_ASSET_PIPELINE.md:49 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L051 | 05_ASSET_PIPELINE.md:51 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L055 | 05_ASSET_PIPELINE.md:55 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X05.L057 | 05_ASSET_PIPELINE.md:57 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L058 | 05_ASSET_PIPELINE.md:58 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L059 | 05_ASSET_PIPELINE.md:59 | 已定位正式调用链，但本条全部条件未完成当前候选证明；四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L060 | 05_ASSET_PIPELINE.md:60 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X05.L062 | 05_ASSET_PIPELINE.md:62 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| X06.L006 | 06_TEST_AND_PERF.md:6 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L008 | 06_TEST_AND_PERF.md:8 | 无ARM64真机；Adreno/Mali、30分钟长稳和真机性能未执行。 |
| X06.L010 | 06_TEST_AND_PERF.md:10 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L016 | 06_TEST_AND_PERF.md:16 | 12组合fixture不包含全部城市六向、港口、编辑、自定义/旧档矩阵；不得以少量命令覆盖原全量清单。 |
| X06.L018 | 06_TEST_AND_PERF.md:18 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L022 | 06_TEST_AND_PERF.md:22 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L024 | 06_TEST_AND_PERF.md:24 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L026 | 06_TEST_AND_PERF.md:26 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L032 | 06_TEST_AND_PERF.md:32 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L036 | 06_TEST_AND_PERF.md:36 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L040 | 06_TEST_AND_PERF.md:40 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L041 | 06_TEST_AND_PERF.md:41 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L042 | 06_TEST_AND_PERF.md:42 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L044 | 06_TEST_AND_PERF.md:44 | 探针结束meminfo出现No process found，记录NOT_AVAILABLE而非0内存；没有真机峰值PSS。 |
| X06.L047 | 06_TEST_AND_PERF.md:47 | 目标代表ARM64设备未测；模拟器全国ready超时是运行FAIL，不当作手机10秒性能结论。 |
| X06.L048 | 06_TEST_AND_PERF.md:48 | 没有30分钟真实游戏混合操作长稳证据；生命周期循环不替代该条。 |
| X06.L049 | 06_TEST_AND_PERF.md:49 | 没有物理设备thermal降级/恢复验证。 |
| X06.L051 | 06_TEST_AND_PERF.md:51 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L055 | 06_TEST_AND_PERF.md:55 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L057 | 06_TEST_AND_PERF.md:57 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X06.L061 | 06_TEST_AND_PERF.md:61 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X06.L063 | 06_TEST_AND_PERF.md:63 | 已定位正式调用链，但本条全部条件未完成当前候选证明；旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| X07.L005 | 07_GIT_DELIVERY.md:5 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L007 | 07_GIT_DELIVERY.md:7 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L009 | 07_GIT_DELIVERY.md:9 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L013 | 07_GIT_DELIVERY.md:13 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L015 | 07_GIT_DELIVERY.md:15 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L017 | 07_GIT_DELIVERY.md:17 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L021 | 07_GIT_DELIVERY.md:21 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L023 | 07_GIT_DELIVERY.md:23 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L029 | 07_GIT_DELIVERY.md:29 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L030 | 07_GIT_DELIVERY.md:30 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L031 | 07_GIT_DELIVERY.md:31 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L032 | 07_GIT_DELIVERY.md:32 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L033 | 07_GIT_DELIVERY.md:33 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L034 | 07_GIT_DELIVERY.md:34 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L036 | 07_GIT_DELIVERY.md:36 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L038 | 07_GIT_DELIVERY.md:38 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L042 | 07_GIT_DELIVERY.md:42 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L044 | 07_GIT_DELIVERY.md:44 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L048 | 07_GIT_DELIVERY.md:48 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L049 | 07_GIT_DELIVERY.md:49 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X07.L050 | 07_GIT_DELIVERY.md:50 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L051 | 07_GIT_DELIVERY.md:51 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L052 | 07_GIT_DELIVERY.md:52 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L053 | 07_GIT_DELIVERY.md:53 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X07.L055 | 07_GIT_DELIVERY.md:55 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L005 | 09_SCOPE_AND_DECISIONS.md:5 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L009 | 09_SCOPE_AND_DECISIONS.md:9 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L011 | 09_SCOPE_AND_DECISIONS.md:11 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L015 | 09_SCOPE_AND_DECISIONS.md:15 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L016 | 09_SCOPE_AND_DECISIONS.md:16 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| X09.L017 | 09_SCOPE_AND_DECISIONS.md:17 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L018 | 09_SCOPE_AND_DECISIONS.md:18 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L019 | 09_SCOPE_AND_DECISIONS.md:19 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L020 | 09_SCOPE_AND_DECISIONS.md:20 | 已定位正式调用链，但本条全部条件未完成当前候选证明；正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| X09.L024 | 09_SCOPE_AND_DECISIONS.md:24 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| XR00.L004 | prompts/R00_PROMPT.md:4 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR00.L008 | prompts/R00_PROMPT.md:8 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR00.L010 | prompts/R00_PROMPT.md:10 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR00.L012 | prompts/R00_PROMPT.md:12 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR00.L014 | prompts/R00_PROMPT.md:14 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR00.L016 | prompts/R00_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| XR00.L024 | prompts/R00_PROMPT.md:24 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR00.L047 | prompts/R00_PROMPT.md:47 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR00.L053 | prompts/R00_PROMPT.md:53 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 |
| XR00.L059 | prompts/R00_PROMPT.md:59 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR00.L061 | prompts/R00_PROMPT.md:61 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR00.L063 | prompts/R00_PROMPT.md:63 | 正常新剧本全国3D readiness在API29/35原120秒超时；存档/镜头完整流程未闭合。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR00.L067 | prompts/R00_PROMPT.md:67 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR01.L016 | prompts/R01_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| XR01.L058 | prompts/R01_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR01.L066 | prompts/R01_PROMPT.md:66 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR02.L016 | prompts/R02_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| XR02.L058 | prompts/R02_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR02.L066 | prompts/R02_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。数学矩阵不等于设备拾取；内部尺寸三档、UI边缘、运动单位/山后命中的完整安装矩阵未做。 |
| XR03.L016 | prompts/R03_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| XR03.L059 | prompts/R03_PROMPT.md:59 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR03.L067 | prompts/R03_PROMPT.md:67 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。同源采样与局部失效有主机证据；跨块巡航/LOD物体贴地与地貌轮廓缺完整当前设备图组。 |
| XR04.L016 | prompts/R04_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 |
| XR04.L058 | prompts/R04_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR04.L066 | prompts/R04_PROMPT.md:66 | 四层材质真实装包；V1近中远两方向实拍与纹理质量对照未闭合，不能把混合算法当美术达标。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR05.L016 | prompts/R05_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| XR05.L058 | prompts/R05_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR05.L066 | prompts/R05_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。水陆硬约束有源码/主机结果；弯道上下船、增删水后实际航行与PC水岸对照不足。 |
| XR06.L016 | prompts/R06_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 |
| XR06.L058 | prompts/R06_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR06.L066 | prompts/R06_PROMPT.md:66 | 异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR07.L016 | prompts/R07_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| XR07.L059 | prompts/R07_PROMPT.md:59 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR07.L067 | prompts/R07_PROMPT.md:67 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。正常据点映射存在；城港关地基/轮廓/比例/全LOD与六向玩法设备矩阵未完整闭合。 |
| XR08.L016 | prompts/R08_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| XR08.L058 | prompts/R08_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR08.L066 | prompts/R08_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。确定性散布/硬排除源码可追；树林道路/山口/农田三组安装多视角、叶片过绘与闪烁缺验证。 |
| XR09.L016 | prompts/R09_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| XR09.L058 | prompts/R09_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR09.L066 | prompts/R09_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。V2十项不能凭profile命名通过；当前完整地图尚未ready，原PC对照缺同区域相机组与完整操作。 |
| XR10.L016 | prompts/R10_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 |
| XR10.L058 | prompts/R10_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR10.L066 | prompts/R10_PROMPT.md:66 | 13资产族是映射覆盖而非美术通过；实际行军/攻击全类别、脚滑/水位/遮挡与大量单位设备压力未闭合。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |
| XR11.L016 | prompts/R11_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| XR11.L058 | prompts/R11_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR11.L066 | prompts/R11_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。HOST fixture并未证明所有实际事件；CALM/EXTINGUISH等实际触发及全类别实拍缺失，播放取消/重建设备覆盖未闭合。 |
| XR12.L016 | prompts/R12_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| XR12.L058 | prompts/R12_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR12.L066 | prompts/R12_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| XR13.L016 | prompts/R13_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| XR13.L058 | prompts/R13_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR13.L066 | prompts/R13_PROMPT.md:66 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。内部AtomicFile不能替代外部SAF写入中断；完整CRUD→存储→重开→新剧本、头像/旧档设备矩阵不足。 |
| XR14.L016 | prompts/R14_PROMPT.md:16 | 必要的正式地图同区域/相机/设备图组未完整取得；本次P0录屏为加载中的全国/正式地图，fixture图不作PC美术通过。月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
| XR14.L058 | prompts/R14_PROMPT.md:58 | 本次身份与独立制品已核验；本条历史逐阶段生产/交付全部条件尚未独立闭合。详见原213与原CI完整日志。 |
| XR14.L066 | prompts/R14_PROMPT.md:66 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 本条特别禁止/共通条件未取得全部正式路径运行证据。 |

## NOT_REACHED

| ID | 来源 | 具体缺口 |
|---|---|---|
| R01.V02 | prompts/R01_PROMPT.md:41 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R01.V04 | prompts/R01_PROMPT.md:43 | 旧最终20次切换/18次前后台后失败；本次API29完成17次切换、0次前后台，下一次ready失败；API35完成20次切换、13次前后台后background frame loop stopped。创建时贴图解码仍在owner；晚到worker及故障恢复设备步骤未全部到达。 |
| R12.V01 | prompts/R12_PROMPT.md:40 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R12.V02 | prompts/R12_PROMPT.md:41 | 两API冷启动全国预览失败；原R12初始ready失败，四组合/重建/日期/下一旬后续未到达，纯触控链未通过。 |
| R14.V05 | prompts/R14_PROMPT.md:44 | 月份到季节已接入；本次正式月份探针被初始ready阻塞；API35 fixture跨月跨年/重建画面已核对，但四季近中远/三画质/资源长稳仍缺。 |
