# Java (HotSpot C2) optimization examples

Measured results and interpretation: `RESULTS.md`.

## Layout

- `jopt/*.java` - standalone examples (default package), one per optimization. Compile with `cd jopt && javac *.java`.
- `jasm.py` - disassembles C2 output without hsdis (needs `objdump`). `jl.sh Cls method [jvm flags]` wraps it (run from `jopt/`; `JL_ARGS` passes program args, `JL_TIMEOUT` the timeout in seconds).
- `jmh/` - JMH 1.37 project (Maven). `jmh/src/main/java/jopt/*Bench.java`, `jmh/run.sh`, raw output in `jmh/results/`.
- `results/` - raw output of the standalone timing runs (warm-up, deopt cliff).

## Run

```bash
# C2 listing of one method (offsets relative to the method start)
cd jopt && javac *.java
../jl.sh Unroll sum -XX:-UseSuperWord
java -XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=print,Cls::method Cls 2>&1 | python3 ../jasm.py --last

# JIT events
java -XX:+PrintCompilation Virt shift
java -XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=PrintInlining,Virt::total Virt bi

# JMH, pinned to one core; keep the machine idle
cd ../jmh && mvn -q -B package
./run.sh                 # everything (about 40 minutes); or ./run.sh virt esc_on esc_off ...
taskset -c 3 java -jar target/benchmarks.jar VecBench -f 5 -wi 5 -i 5 -jvmArgsAppend -XX:-UseSuperWord

# standalone timings
taskset -c 3 java Warm; taskset -c 3 java Cliff; taskset -c 3 java Naive
timeout 8 java Loops          # hangs on purpose (plain field hoisted); `java Loops volatile` finishes
```

Notes: `CompileCommand=print` can change what C2 emits for the same method (see item 11a in `RESULTS.md`), so confirm a listing with a timing.
