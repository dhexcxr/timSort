# TimSort (Java)

A clean-room implementation and empirical performance benchmark of Tim Peters' adaptive hybrid sorting algorithm (TimSort) in Java, constructed from core design specifications.

---

## Overview

TimSort is an adaptive, stable, natural merge sort variant designed to exploit pre-existing order in real-world data. It achieves an optimal time complexity of O(N) on partially ordered data while maintaining a worst-case guarantee of O(N log N).

This implementation translates foundational CPython mechanics into idiomatic Java generics (<T extends Comparable<T>>), featuring dynamic run construction, stack-based merge invariants, an adaptive galloping heuristic, and an empirical benchmarking harness.

---

## Architectural Mechanics

### 1. Natural Run Identification & Inversion
The algorithm scans input sequences to detect contiguous ordered segments ("runs"):
* Strictly Descending Runs: Identified and reversed in-place using two-pointer swaps to guarantee stability.
* Ascending Runs: Preserved as-is.
* Sub-Run Padding: Runs shorter than minRun are extended to minimum length using binary insertion sort, minimizing merge overhead on small arrays.

### 2. Bitwise Minimum Run Calculation (minRun)
Replicates the CPython bit-shift algorithm to select a minimum run length in the range [32, 64]. This ensures that N / minRun is at or slightly below a power of 2, balancing subsequent merge trees:

    // Deriving optimal minRun length via bitwise shifting
    int rValue = 0;
    while (arraySize >= 64) {
        rValue |= arraySize & 1;
        arraySize >>= 1;
    }
    int minRun = rValue + arraySize;

### 3. Stack-Based Merge Invariants
To maintain balanced merges with O(N log N) bounds, runs are tracked on a call stack enforcing strict size invariants across the top three runs (X, Y, Z):
1. Z > Y + X
2. Y > X

If either invariant is violated, intermediate runs are merged deterministically until stability is restored.

### 4. Adaptive Galloping Heuristic
During sub-array merges, the engine monitors consecutive "wins" from a single run. Once consecutive selections exceed minGallop (default 7), the algorithm enters gallop mode:
* Uses exponential stepping (1, 3, 7, 15...) to bracket the target element range.
* Resolves the exact index offset and performs bulk sub-array copies.
* Dynamically scales minGallop (lowering the threshold if galloping continues to yield runs, and incrementing to penalize when returns diminish).

---

## Empirical Benchmarks

The repository includes an automated testing harness evaluating correctness assertions and runtime performance across 1,000,000 integers against traditional algorithms and native JVM implementations:

| Algorithm | Average Runtime (1M Elements) | In-Place | Stable |
| :--- | :--- | :--- | :--- |
| BubbleSort | O(N^2) — Infeasible at scale | Yes | Yes |
| Standard MergeSort | Baseline O(N log N) | No | Yes |
| This TimSort Implementation | Significantly faster on partially ordered data | No | Yes |
| Java Collections.sort() | Production-optimized JVM baseline | No | Yes |

*Run assertions verify element count parity and ascending order against Java standard library baselines.*

---

## Primary References

* **Tim Peters' Original CPython Design Specification:**
  [CPython `listsort.txt`](https://github.com/python/cpython/blob/main/Objects/listsort.txt) — Foundational design document detailing natural run mechanics, adaptive galloping mode, and stack merge invariants.
* **Autumn Skerritt's Algorithmic Breakdown:**
  [Skerritt.blog: TimSort Analysis](https://skerritt.blog/timsort/) — Architectural breakdown of real-world run identification and galloping threshold dynamics.
* **General Reference:**
  [Wikipedia: TimSort](https://en.wikipedia.org/wiki/Timsort)