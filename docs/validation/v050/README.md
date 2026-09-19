# v0.50 地图验证

基线：18b1c2d01d6ef5d0bc63fef8a3e483186a99e390，v0.49.0。

本地已完成：
- 110,495项静态断言：九份地图一致性、42城与45港关、非地理记录不变、每城容量和591个开发位、全部35港建满后的dock edge、主水域连接、18项地图/原美术摘要。
- 1,446项实际引擎断言：九剧本读取/存档、建满全部开发位的上下船、敌我港口、10关隘和14条主要陆路。
- 本轮地理改动明细：`geography.json`。该文件是确定性迁移产物，不是目测还原率。

构建与安装流程运行后，其真实结果写入对应Actions制品：`BUILD_COMMIT`、`SOURCE_CHECKSUMS.json`、`core-checks.txt`、`ui-model-checks.txt`、`installed-map50.txt`、`installed-systems49.txt`、`installed-flow48.txt`、`screenshots/`、APK和SHA256SUMS。只有实际产出且成功的检查才算通过；此源文件不预先声称远端构建成功。

近期回归入口为test-personnel49.sh；并非完整历史全量回归/全部升级矩阵。Android探针为API29 x86_64模拟器，未测试ARM真机。不提供虚构帧率、全国逐格完成率或官方精确公式结论。
