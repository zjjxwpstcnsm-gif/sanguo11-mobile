# 同模拟器新旧APK配对耗时

| 场景 / 测量 | v0.27 中位 / P95 ms | v0.28 中位 / P95 ms |
|---|---:|---:|
| 42cities-670officers / setWorld-range-index | 0.092 / 0.287 | 0.075 / 0.398 |
| 42cities-670officers / softwareDraw | 6.256 / 9.615 | 6.461 / 8.216 |
| 42cities-670officers / refresh-detail | 3.496 / 11.284 | 4.489 / 12.279 |
| 42cities-670officers / city-list | 0.096 / 0.202 | 0.104 / 0.144 |
| 42cities-670officers / injected-drag-to-idle | 301.643 / 387.372 | 309.682 / 365.976 |
| 42cities-670officers / injected-pinch-to-idle | 220.628 / 268.161 | 235.295 / 444.404 |
| 200x200-42cities-40units-3transports / setWorld-range-index | 1.532 / 2.780 | 1.111 / 2.152 |
| 200x200-42cities-40units-3transports / softwareDraw | 6.820 / 9.224 | 6.827 / 8.514 |
| 200x200-42cities-40units-3transports / refresh-detail | 5.974 / 12.014 | 5.899 / 8.635 |
| 200x200-42cities-40units-3transports / city-list | 0.073 / 0.111 | 0.072 / 0.115 |
| 200x200-42cities-40units-3transports / injected-drag-to-idle | 344.037 / 379.657 | 274.400 / 574.447 |
| 200x200-42cities-40units-3transports / injected-pinch-to-idle | 257.213 / 379.148 | 241.439 / 300.340 |
| 42cities-670officers / 完整旬模拟，中位（范围） | 877.6 (872.6–952.5) | 926.4 (919.7–999.5) |
| 200x200-42cities-40units-3transports / 完整旬模拟，中位（范围） | 4936.0 (4920.1–5117.5) | 5121.5 (5096.4–5554.2) |
