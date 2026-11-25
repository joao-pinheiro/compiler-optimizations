# compiler-optimizations

The code and data behind the series *Common compiler optimizations*:

1. [C (gcc and clang)](https://jpinheiro.com/blog/2026/compiler-optimizations-1-c)
2. [Go](https://jpinheiro.com/blog/2026/compiler-optimizations-2-go)
3. [Java and the JIT](https://jpinheiro.com/blog/2026/compiler-optimizations-3-java)
4. [When it bites](https://jpinheiro.com/blog/2026/compiler-optimizations-4-when-it-bites)

Toolchains: gcc 15.2, clang 21.1, Go 1.27.4, OpenJDK 25.0.5.1 (HotSpot C2), JMH 1.37, ESP-IDF Xtensa gcc esp-15.2.0.
C and Go benchmarks ran on an Intel Xeon W-2145 (Skylake, 8 cores / 16 threads), pinned to one logical CPU with its SMT sibling idle,
the performance governor on and other workloads paused, 10 runs each. Java benchmarks ran on an i7-14700KF, pinned to logical CPU 3
(physical P-core 1): the JIT compiles for the CPU it runs on, so the Java listings and timings stay on one machine.

## Layout

```
c/       C examples, one file per optimization (sum.c is the primer's example, slp.c the straight-line
         vectorization, bench.c the benchmark that measured nothing, debug.c the gdb example)
c/tu/    the cross-file case (apply in its own translation unit, with and without -flto)
c/esp32/ the ESP32 shared-flag example (Xtensa)
go/goopt tour examples (folding, inlining, bounds checks, escape analysis, tail calls), part 4's examples (bites.go:
         spin loops, Login/clear) and the benchmark-elision tests; asm127.txt is the -S output, trimmed
java/    HotSpot C2 examples, the jasm.py disassembler, a JMH project and raw results (see java/README.md)
```

## Reproduce

C (`-fcf-protection=none` drops Ubuntu's `endbr64`, `-U_FORTIFY_SOURCE` keeps `printf` as `printf`):

```bash
gcc   -O2 -S -masm=intel -fno-asynchronous-unwind-tables -fcf-protection=none -U_FORTIFY_SOURCE -o - c/print.c
clang -O2 -S -masm=intel -fno-asynchronous-unwind-tables -o - c/print.c
```

Benchmark that measured nothing (compare the two loops in the assembly, or time them):

```bash
gcc -O2 c/bench.c -o bench && taskset -c 3 ./bench
```

Published C and Go timings: `c/results/bench-xeon-w2145.txt` (10 runs of each of `bench` built with gcc and clang at `-O2` and `-O0`)
and `go/goopt/results/bench-xeon-w2145.txt` (`-count 10`; summarize with `benchstat`). `c/results/run-bench.sh` made them: it sets
the performance governor (needs root; it restores the previous governor on exit), picks the least-loaded physical core, and runs prebuilt
binaries placed next to it, pinned with `taskset`: `bench-{gcc,clang}-O{2,0}` (built with `-static`), `goopt.test` (`go test -c`)
and the `dry-benchmark` repo's `godry.test` and `goifs.test`. It writes `env.txt` and one `*_bench.txt` per benchmark, which
become the `results/` files.

"Value optimized out" (repeat with -O0 and -Og):

```bash
gcc -O2 -g -o debug c/debug.c
gdb -batch -ex 'break debug.c:8' -ex run -ex 'print doubled' -ex 'print x' ./debug
```

ESP32 (ESP-IDF toolchain):

```bash
xtensa-esp32-elf-gcc -O2 -mlongcalls -S -o - c/esp32/flag.c
```

Go:

```bash
cd go/goopt
go build -gcflags='-S' .        # assembly
go build -gcflags='-m' .        # inlining and escape analysis
go build -gcflags='-d=ssa/check_bce/debug=1' .   # remaining bounds checks
taskset -c 3 go test -bench . -count 10
```

Java: see `java/README.md` (listings with `jl.sh`/`jasm.py`, no hsdis needed; JMH with `jmh/run.sh`).
Measured numbers and their interpretation are in `java/RESULTS.md`.

## License

MIT, see `LICENSE`.
