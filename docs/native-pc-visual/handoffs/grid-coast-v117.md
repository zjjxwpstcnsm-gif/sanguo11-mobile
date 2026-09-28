# 网格与岸线专项交接 — v117 / PARTIAL

本轮用户只要求 R18 后三项反馈，不开始新阶段。继续 `agent/native-pc-visual` / Draft PR67，main 保持未合并。

正式 APK source：`7a6b2a354ad93db30602d59c2632777243d55801`；APK SHA-256：`48ec6ad14cf9f2eefaf020566471dac5292bf3850c33b411262ce89b47f6f185`。v117，Filament1.56 OPENGL，完整身份见[报告](../reports/grid-coast-v117.md)和[manifest](../evidence/grid-coast-v117/manifest.json)。最终远端 HEAD 在独立证据的 remote-confirmation.json；后续提交仅文档/证据。

已接入普通网格双色线、永久禁行格及遮挡过滤、共享岸线几何平滑，并同步格线、领土、拾取、贴地高度。没有改变 authoritative World、格中心、水陆连通、地图文件或 300 份既有资源；编辑网格和潜在可航水格保留。

主机与构建/lint通过；API29 实际 v116/v117 正常游戏入口局部对比通过。API35 首轮旧版 PASS/候选 ready FAIL；同APK复跑旧版ready FAIL/候选PASS874，取得12组画面；双方稳定性未关闭。全国原 ready 仍 FAIL，完整触控未到达/本专项未覆盖。核心 logistics:75/既有船运规则回放失败继续有效。真机 WIF 被 attribute condition 拒绝，零物理提交；没有 Adreno/Mali/30分钟热稳验收。

视觉只确认局部改善。PC 参考缺失；VOID 地图外缘固定，仍可能阶梯；有限折线不是完全像素抗锯齿。V1–V4、全国美术、全部地图编辑与旧档升级没有借本轮 PASS 清零。

如用户另行要求继续，先从实时远端核查此分支、PR和 manifest，再定位 API35/全国 beginFrame 准入停滞并按原门槛复验。不要用放宽时限、降低样本、改哈希/断言或强制成功码来关闭问题。本轮到此，不自动开始后续开发或合并。
