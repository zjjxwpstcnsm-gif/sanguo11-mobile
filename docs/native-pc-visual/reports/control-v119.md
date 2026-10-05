# v119：扰乱与伪报的部队智力修正 — PARTIAL

## 承接与授权

本轮是用户对v118数值的明确纠正：“扰乱和伪报应该根据双方部队中武将智力，水平相当约20%”。继续agent/native-pc-visual / Draft PR67；main实时ac29b458，架构PR66已合；输入HEAD5cc511faa6d2fea837f0fecc29f5e2c28c933f8f。复读00_GLOBAL_RULES，用户此条是限定规则修正授权，不开展新阶段、不合并、不启Unity。

正式数值修复b6222f81d1922011eec019701021de2966523710；最终APK源码53a31e751de7dc403f88a292a9da01d016c39565 / tree40e816a0b88460c7731f6a5024f12043cde3b2a2。后者只修正测试输入驱动，生产规则相同。

## 正式路径

Army.intelligence原本就取主将与所有副将最高智力，此处没有重写；问题是普通基础值过高，且v118只修了扰乱。War.plotChance现在同时处理CONFUSE/MISLEAD：`clamp(20 + 智力差 / 2, 5, 85)`%，整数除法沿用旧行为。此是用户指定基准校准，不宣称核验了PC原版完整公式。

| 双方部队最高智力差 | 普通成功率 |
|---|---:|
| 低20点 | 10% |
| 相等 | 20% |
| 高20点 | 30% |
| 高40点 | 40% |

之后依然调用原Skills.plotChance，免疫优先，特技持有者自身智力达到条件才必定成功；机略/言毒、虚实/神算、倾国及防御特技行为均保留。因此“20%”不覆盖特技干预。确认界面WarUi与实际War.resolvePlot读同一方法，2D/3D无分开的概率。旧档未来命令使用新数值；SaveCodec、RNG算法、消费、范围、状态效果没有改变。

所有app/src/main、game-api/runtime、地图data、Unity、300份资源及v118拖放/森林/螺旋改动保留。本次唯一正式规则修改是上述两项普通基准。

## 主机验证

独立解出输入5cc511f的正式源码，使用相同合法fixture和一万组不同种子对照。两边主将40、副将80，证实双方都使用最高80而非主将40。

| 实际命令 | v118 | v119 |
|---|---:|---:|
| 扰乱 | 4527/10000，45.27% | 2042/10000，20.42% |
| 伪报 | 6563/10000，65.63% | 2042/10000，20.42% |

ControlIntelligence40106检查通过，覆盖高智副将进攻/防守、主副将互换、同20/80/100智力、边界、无关武力/统率、存档恢复、预览不耗RNG、特技持有者条件、免疫优先与实际消费；ControlProbability40037也通过，螺旋命中和混乱分布与v118相同。Balance54、架构/GameSession1673、地图投影119600及共享坐标32通过。

完整core仍CoreTest.logistics:75失败；额外独立执行的RulesParity.actions:54、MapSkills.geography:29、Campaign.merge:91、CampaignAi.supply:85、WorldSystems.diplomacy:87也失败，输入/候选完整失败日志逐字相同，归类inherited。原断言没有删除/放宽，不把工作流continue-on-error显示成功冒充测试PASS。首个新fixture把太守当副将造成保存校验失败，已改为新增专用测试副将；未修改正式保存验证。

## APK与设备

最终源码 `53a31e751de7dc403f88a292a9da01d016c39565`，CI36518573174 / build109246251182 / artifact11011628805通过。构建命令见manifest；下载后复核DEX源码、300资源一致性、四ABI及签名。APK为sanguo11-mobile-v119.apk，37,403,418字节，game.sanguo.mobile.dev / versionCode119 / 0.119.0-control-intelligence。

最终交付SHA256 `a907ed6577f702664b452298a41ca1418582aebb3ae634d239125003584884d0`；既有开发签名SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。首轮b6222f8源码对应CI36517853259 / artifact11011721894、APK SHA d74112953458cf74241e3380901dfa6326161221924b0b4411cc7a2e64571bd0仅作为失败复现历史，不是最终交付包。

最终API29/job109246862165与API35/job109246862081均PASS37，设备拉回APK字节/哈希等于最终交付。每台覆盖扰乱/伪报各成功和失败四例：真实确认界面20%，真实执行后的完整SaveCodec/RNG/自动存档与独立命令参考相同，气力100→85、行动消费一次。共8组实际/参考存档逐字相等，16张实际确认/结果屏幕图；已直接查看两API的扰乱与伪报确认原图，均显示20%。下载的运行artifact11012058041/API29、11012332060/API35也通过存储SHA与ZIP CRC校验。

首轮API29/35均停在计略列表测试点击：截图已显示“扰乱·气力15”，但原驱动要求列表TextView或父级暴露ACTION_CLICK，未到达确认框。53a31e7只把驱动改为定位可见节点后发送实际触点，并提前连接无障碍；保留原10秒等待、20%与全量存档断言，首轮失败原始文件继续归档。首个太守fixture校验失败也保留，未修改正式保存校验。

安装专项使用真实MainActivity/WarUi/GameSession，但合法fixture由程序选择，目标通过正常mapPick回调选择，再以实际触点点击计略列表和执行按钮。这是2D规则/界面专项，不是全触控或3D视觉验收，不能据此关闭3D失败。

v118的两轮API29输入PASS/候选ready FAIL、API35双方FAIL继续开放；拖放实操/森林视觉/密度性能未因本轮规则测试关闭。物理设备本轮NOT_RUN，先前WIF attribute condition拒绝依然是已知访问阻塞；全国、PC美术、V1–V4、SAF和ARM64长稳未验收。整体仍PARTIAL，不合main、不启动新阶段。

## 交付证据

源码b6222f8与测试驱动53a31e7已实际提交推送；最后证据提交只包含docs/native-pc-visual。最终远端HEAD与分支/PR/主分支复读记录在交付ZIP的remote-confirmation.json，避免证据提交自引用。ZIP同时包含完整初始失败/最终成功日志、原始截图、8组存档、构建元数据、源码补丁及逐文件SHA清单；APK独立交付。
