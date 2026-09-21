# v0.61 两角、边界与连续外景 — 最终交付验收

## DONE — 精确构建、实际安装、独立APK发布

[独立APK直接下载](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/v0.61.0-9ad16d82c5a3/sanguo11-mobile-v0.61.0-9ad16d82c5a3.apk)

- 基线main：049652d45641ff247c964ec30f70f5cff5c629f3，包含指定PR48。本轮没有合并无关PR。
- 最初正式功能源提交：9b69f6fd09cd15a75e09666ec12ef0f39575dd7e。
- 实际BUILD_COMMIT：9ad16d82c5a33211d28fa84e677b53e155f535b5。两者之间仅androidTest与CI触发变更，地图/正式渲染/游戏规则未改。
- [成功运行35559224769](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35559224769)：assembleDebug、定向模型、实际Android安装、Release发布和回下载比对全部SUCCESS。原始VALIDATION_STATUS与ANDROID_STATUS均通过，而不是只依据continue-on-error步骤的conclusion。
- v0.61.0 / versionCode61 / mapRevision61 / CityAtlas56 / SaveCodec31；包名game.sanguo.mobile.dev；APK22339175 bytes。
- APK SHA256：279290bc30c1c5f7e5f8b9d4b12d2cde951f8382cf975f71a585e2641d2d6905。
- 正式地图SHA256：d5d5659e40a23619dd8515713d21997715733d20cc937a964662591e22f076c4。
- 外景资源SHA256：451fed332f742aaef8a88e5781c15eda8e1ad0fd29ae7d20c3e0dbe82214a60f。
- APK v2签名验证通过；证书SHA256：8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24，与v060相同。
- 安装APK、Release回下载APK及本会话独立回下载产物的哈希一致。运行截图的实际BuildConfig为0.61.0/61 source=9ad16d82c5a3。
- [PR49](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/49)指向main，保留待审未合并。最终文档是构建后记录；不是要求文档提交SHA与BUILD_COMMIT相同。

## A — 东南角

原来存在有效水面缺失与外海VOID没有连续外景显示两类问题；没有独立复现缓存孔洞/LOD丢块，不冒称修复了这种显示故障。

东南过渡区41格V→Q，范围x139–174,y163–199；东南外海869格仅外景SEA，范围x175–199,y129–199。范围是外接框，不代表整个矩形被改；逐格以正式修订/外景白名单为准。Q可点击但禁行，外景不可点击/寻路/开发，未增加可玩疆域。

在coalition-190和heroes-250正常MapView中，全区域旧新同坐标远景及海岸中景已实际查看。每个全国剧本全区域视野覆盖869/869外景格，原整片黑洞/内湾缺口不再出现，海面连续；全国200×200矩形画框仍保留。未靠裁角、缩小相机或统一VOID填水隐藏问题。

## B — 西北角：主要大片已处理，14格局部未完成

56格确认水面V→Q，范围x31–49,y8–24。182格只增加外景：ARID126、ROCK56，范围x0–38,y0–24。原图标题遮挡、山体/荒漠与外侧边缘分开记录；标题下具体材质存在估计，未把对应位置当作新增真实可玩沙地。

界外记录依据校准后海岸/山崖/墙线、城市港口相对侧别、连续外景通向图边的多线索判读，不仅是无网格或上轮分类名。原版引擎enabled-hex表未取得；这属于图像边界判断，不等于原版逐格可玩边界认证。

两全国剧本配对完整视野覆盖182/182外景格。整片碎裂黑格已改为荒原/山体与水沟衔接，但中景仍能看到14格未解决黑孔，该局部区域尚未完成，不能改名为合法界外：

(18,24),(29,24),(30,24),(33,15),(34,15),(34,18),(35,14),(37,15),(37,20),(37,21),(37,22),(37,23),(37,24),(38,15)。

源坐标包络x18–38,y14–24；混合岸线、墙体遮挡和局部配准不确定，缺无遮挡参考或原版地形/有效格表。保持VOID，不绘制任何兜底外景。

## C — 地图可玩性与实际测试

206个唯一地理修改全部V→Q：东南41、西北56、北侧107、永安内部2。V→陆地0，其他地形变化0，原有Q/W规则变化0，自然设施变化0。七份全国剧本共享源格只计一次；以前版本成果没有重复计数。源VOID1271→1065，其中1051外景呈现、14待证；19800 axial padding单列。

200×200原生odd-q、World-aware MapCoordinates/MapRaster、局部crop offset与奇偶相位、42城/10关/35港/591开发位、七格城市、CityAtlas56和既有出征/战报/行军等保留。没有新增陆路、开发地或航道，未移动据点/部队来拼接地图。

NationalExterior仅在revision61、同地图ID/layout、World-aware全国源坐标、真实VOID的明确白名单上启用；真实有效地形优先。原World.inside/sourceInside语义未改。MapView/MapOverview/小地图/正式开局预览共享外观查询，外景不染势力色，不计领土，不回退选中附近城市。局部裁区边缘不套全国界外。Q/W视觉水面连接与行军权限分离。

最终源定向模型包含全源格字符/坐标往返、odd-q拓扑、VOID/padding区分、修改格及邻域、保护对象、港口/上下河/船/运输/科技/AI/自动行军、非法Q起终点、七格城、中立设施、旧新存档和行军预算。REFERENCE61 CORE PASS记录5834099条及继承通路检查53105；PROJECTION PASS191807。不同套件存在重复/继承，不求和冒称独立测试总数。新批量工具48项负向/幂等检查，继承v06024项及更早守卫通过；51份正式地图/美术资源摘要通过。

Android实际环境：API29、x86_64、Pixel2配置、1080×2340截图。最终Reference61日志9866条PASS，另保留native200实际操作92项；没有失败断言或FATAL。覆盖coalition-190、heroes-250、central-mobile-sandbox、jingxiang-mobile-sandbox：两角完整远景、关键边界中景/近景、三LOD/平移缩放、外景/未知格/padding点击排除、Q和已有W详情、小地图跳转、势力着色、正式开局预览、出征/行军/下一旬、保存退出重读与旧档。两全国剧本实际屏内padding触摸均执行成功。Q旁合法行军使用标明的androidTest部队放置夹具，不改真实地形或据点；墙/堤坝夹具不冒充自然设施发现。

真实原APK的v058/v059/v060共12份Android存档与三个原编码器交叉验证通过，v060→v061同签名覆盖安装通过，旧手动存档字节未变。旧档保留旧地图/revision/预算/单位与设施；旧revision不套61外景，未删除、静默迁移或仅改revision。新地图需新开局查看。

## D — 考证与未验证范围

206新Q水面外观CONFIRMED_VISIBLE，原版航行权限206 UNRESOLVED，保持禁行并可查看详情。1051外景材质中951 CONFIRMED_VISIBLE、100 ESTIMATED；外景边界账目1051为多线索图像判读CONFIRMED_VISIBLE，但未取得原版enabled-hex数据。外景面积不是地理复原格数，更不是原版航行结论。

西北14格、标题下精确材质、原版逐格有效边界与航行权限、继承地形细分和部分栈道/港口证据仍待完善。物理ARM/FPS与全部历史CI未认证。不宣称地图100%还原或西北全部收尾。

## 原图、配对目视与失败保留

本会话实际解压读取7200×6752原图，SHA256 a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0；原图与参考裁片不提交、不打包、不发布。原图校准与逐格记录在data/map/reference-v061/；继承工具/25×25分块而非重建全国源图。

原发布v060基线run35554158663；最终run35559224769。两全国剧本西北source(28,22)相机(1634.9999,2816.6665)、东南(169,167)相机(6324.9995,5163.333)，均scale0.3，MapView1080×1789，截图1080×2340。相同中心/相机/LOD配对，未裁切原图。最终目视查看两剧本完整两角、关键中景、荒原/山体/海面近景、局部预览与读档部队界面。详细原文件名/指纹见visual-review.json；原始截图254张及基线37张随独立日志包发布，不代表254张都逐一人工认证。

被拒绝候选保留：run35557950714的测试误采非外景(28,6)，修成真实ARID(28,10)和ROCK(28,21)且增加种类断言；run35558626282的读档测试忽略重复点击已选部队会取消选择，修成真实取消/重选/身份断言。未改地形讨好测试、未删断言、未吞失败；前两包没有发布。最终第三次完整通过。

[原始前后截图及日志](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/v0.61.0-9ad16d82c5a3/corners61-screenshots-and-logs.zip)（约508MB） · [构建源归档](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/v0.61.0-9ad16d82c5a3/sanguo11-mobile-v061-source.zip) · [APK身份](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/v0.61.0-9ad16d82c5a3/APK_IDENTITY.json)
