package jopt;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** SuperWord: run with default flags and with -XX:-UseSuperWord */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class VecBench {
    @Param({"4096"}) int n;
    int[] a, b, c;
    float[] fa;
    long[] la;
    @Setup public void setup() {
        a = new int[n]; b = new int[n]; c = new int[n]; fa = new float[n]; la = new long[n];
        for (int i = 0; i < n; i++) { a[i] = i; b[i] = 2 * i; fa[i] = i; la[i] = i; }
    }
    @Benchmark public int[] add() { for (int i = 0; i < n; i++) c[i] = a[i] + b[i]; return c; }
    @Benchmark public int intSum() { int s = 0; for (int i = 0; i < n; i++) s += a[i]; return s; }
    @Benchmark public int intDot() { int s = 0; for (int i = 0; i < n; i++) s += a[i] * b[i]; return s; }
    @Benchmark public long longSum() { long s = 0; for (int i = 0; i < n; i++) s += la[i]; return s; }
    @Benchmark public float floatSum() { float s = 0; for (int i = 0; i < n; i++) s += fa[i]; return s; }
}
