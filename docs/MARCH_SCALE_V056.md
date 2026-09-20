# Native-grid march scale (engineering calibration)

Measured using the same `MarchScale56Benchmark`, 10,000 spear troops, all route sites made friendly, no water shortcuts, no terrain edits. A shortest route terminates at any of seven goal-city cells. Baseline main bb8e58e was compiled separately, not simulated by halving new results. One turn is ten days; food is 1,000 units/turn for this army. Results are parameter checks, not historical-original formulas.

| Route | old cells/cost | new cells/cost | old/new move budget | old/new turns | old/new food |
|---|---:|---:|---:|---:|---:|
| Luoyang-Changan | 13/13 | 25/25 | 4/8 | 4/4 | 4000/4000 |
| Xiangyang-Jiangling | 9/9 | 21/21 | 4/8 | 3/3 | 3000/3000 |
| Hanzhong-Chengdu | 29/32 | 51/51 | 4/8 | 8/7 | 8000/7000 |

`MarchScale.base` recalibrates only retired mobile base movement budgets for the new national map and its crops. Six-neighbor topology, terrain costs, ranges, seven-cell footprints, ration formulas and skill/technology additive bonuses are not doubled. Local engineering fixtures retain old budgets. Ship/transport base budgets use the same geographic scale; their individual campaign route benchmarks remain pending. These results do not certify nationwide road fidelity.
