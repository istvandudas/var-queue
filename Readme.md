# VarQueue
A modern, sequence‑driven ring‑buffer queue library that provides lock‑free concurrency patterns from SPSC to MPMC, built entirely on VarHandles with minimal memory fencing.

## Background
Before Java 9, low‑level concurrency primitives relied on:
- sun.misc.Unsafe (non‑portable, unofficial, and inherently risky)
- Atomic* classes (limited, object‑oriented, and not ideal for arrays or custom data structures)

Java 9 introduced VarHandles to provide:
- the power of Unsafe, but in a safe, supported, and future‑proof API
- fine‑grained memory ordering modes (plain, opaque, acquire, release, volatile)
- efficient access to arrays, fields, and off‑heap structures
- a foundation for high‑performance, lock‑free algorithms (including ring buffers and queues)

VarHandles are now the official mechanism for implementing:
- ring buffers
- lock‑free queues
- concurrent counters
- atomic state machines
- memory‑ordered algorithms and data structures

## Purpose
- Today (early 2026), there is no widely adopted, production‑grade Java concurrency library that mirrors JCTools while 
being implemented entirely with VarHandles instead of Unsafe.
- sun.misc.Unsafe is scheduled for removal after Java 25, making VarHandle‑based designs the long‑term path forward.

## How to use
For now, the library is not published to any public Maven repository.
After cloning the project, the intended workflow is to publish it to your local Maven repository and use it like any
other dependency.

## Next steps
This library currently focuses on queue implementations tailored to the needs of my own projects.
The natural evolution is to extend the collection set — for example, maps or other lock‑free structures — 
but there is no fixed roadmap yet. These will likely be the next areas of exploration.
Plus adding public maven publishing.

# Performance test results
Performance is always compared against ConcurrentLinkedQueue (CLQ).
Each test measures:
- 1M offer operations
- 1M mixed operations
- 1M poll operations

Each scenario is executed 10 times and averaged (see the PerfTests for details).

The results may vary between runs, but they consistently reflect the fundamental performance characteristics of these
queues.

## Multi Producer Multi Consumer - MPMC (4P : 4C)

| Test | MPMC (M ops/s) | CLQ (M ops/s) | MPMC% vs CLQ |
|------|----------------|---------------|--------------|
| Offer | 29.18 | 12.08 | **+141.5%** | 
| Mixed | 3.20 | 6.96 | **−54.0%** | 
| Poll | 2128.38 | 3035.41 | **−29.9%** |

---

## Multi Producer Single Consumer - MPSC (4P : 1C)

| Test | MPSC (M ops/s) | CLQ (M ops/s) | MPSC% vs CLQ |
|------|----------------|---------------|--------------|
| Offer | 28.49 | 13.03 | **+118.7%** |
| Mixed | 5.53 | 8.50 | **−34.9%** |
| Poll | 5250.72 | 2576.38 | **+103.8%** |

---

## Single Producer Multiple Consumer - SPMC (1P : 4C)

| Test | SPMC (M ops/s) | CLQ (M ops/s) | SPMC% vs CLQ |
|------|----------------|---------------|--------------|
| Offer | 210.97 | 45.16 | **+367.1%** |
| Mixed | 6.23 | 8.20 | **−24.0%** |
| Poll | 1873.06 | 2814.86 | **−33.5%** |

---

## Single Producer Single Consumer - SPSC (1P : 1C)

| Test | SPSC (M ops/s) | CLQ (M ops/s) | SPSC% vs CLQ |
|------|----------------|---------------|--------------|
| Offer | 199.81 | 48.31 | **+313.4%** |
| Mixed | 72.45 | 41.13 | **+76.2%** |
| Poll | 4558.59 | 2716.87 | **+67.8%** |
