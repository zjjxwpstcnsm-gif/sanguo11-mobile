# S06 host computation check

Starting runtime: 4e7d292; comparison uses unchanged core except the prior
TurnJournal/CombatEffects classes versus the S06 implementations. Fixture is
heroes-250, player 0, seed 12345, save decode before every turn, 28 factions.
JDK17 Linux x86_64 host; -Xmx1400m, one warm-up and three measured turns per case.
This is not Android or native render performance. Run:
`JSON_TEST_JAR=... bash scripts/benchmark-3d-combat.sh`.

| Hot samples (ms) | Before S06 | S06 |
| --- | --- | --- |
| 1 | 2328.05 | 2188.61 |
| 2 | 2189.48 | 2256.16 |
| 3 | 2169.72 | 2195.75 |
| median | 2189.48 | 2195.75 |

Median +0.29%. Every run emits 197 events and the same full-save SHA-256:
`2b6e2d25d2a1c5e9bfe9e1e8d95e743c44994c3b9f919a0b2555b2efea87b3e4`.
An earlier run concurrent with Gradle varied 2441–4437 ms; retained as contended
samples, not evidence of regression or improvement. The idle repeat resolves that
specific observation without changing workload, skipping rules or reducing events.

Visible native duration and total turn are measured separately by installed S06
instrumentation; no host sampler timing substitutes for them. Actual present FPS,
GPU/PSS/thermal/battery remain unavailable locally. The phone 10-second total-turn
goal is not established by this ~2.2-second host computation result.
