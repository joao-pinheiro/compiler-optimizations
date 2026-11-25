package jopt;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Unrolling: run with default flags and with -XX:LoopUnrollLimit=0. SuperWord is disabled in both so vectorization does not hide the unroll effect. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LoopBench {
    int[] a = new int[4096];
    @Setup public void setup() { for (int i = 0; i < a.length; i++) a[i] = i * 31; }

    @Benchmark public int sum() { int s = 0; for (int i = 0; i < a.length; i++) s += a[i]; return s; }
    /** loop-carried dependency: cannot vectorize either way */
    @Benchmark public int prefix() { int[] x = a; int p = 0; for (int i = 0; i < x.length; i++) { p = p * 3 + x[i]; } return p; }
}
