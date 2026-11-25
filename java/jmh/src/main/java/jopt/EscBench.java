package jopt;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Run with -prof gc, with and without -XX:-DoEscapeAnalysis */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class EscBench {
    record Point(int x, int y) {
        Point add(Point o) { return new Point(x + o.x, y + o.y); }
        int dist2() { return x * x + y * y; }
    }
    int x = 3, y = 4;
    static Point keep;

    /** Point does not escape: scalar-replaced. */
    @Benchmark public int local() {
        Point p = new Point(x, y);
        Point q = p.add(new Point(1, 2));
        return q.dist2();
    }
    /** Point escapes through a static field: must be allocated. */
    @Benchmark public int escapes() {
        Point p = new Point(x, y);
        keep = p;
        return p.add(new Point(1, 2)).dist2();
    }
}
