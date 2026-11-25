package jopt;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Run once normally and once with -XX:CompileCommand=dontinline,jopt.InlineBench::square */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class InlineBench {
    int[] a = new int[1024];
    @Setup public void setup() { for (int i = 0; i < a.length; i++) a[i] = i; }
    static int square(int x) { return x * x; }
    @Benchmark public int sumSquares() { int t = 0; for (int i = 0; i < a.length; i++) t += square(a[i]); return t; }
}
