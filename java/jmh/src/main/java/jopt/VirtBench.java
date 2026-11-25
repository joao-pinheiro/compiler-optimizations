package jopt;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Mono/bi/megamorphic call site: sum area() over 1024 shapes. One fork per @Param value, so each fork profiles only its own mix. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class VirtBench {
    interface Shape { int area(); }
    static final class Sq implements Shape { final int s; Sq(int s){this.s=s;} public int area(){ return s*s; } }
    static final class Rect implements Shape { final int w,h; Rect(int w,int h){this.w=w;this.h=h;} public int area(){ return w*h; } }
    static final class Tri implements Shape { final int b,h; Tri(int b,int h){this.b=b;this.h=h;} public int area(){ return b*h/2; } }

    @Param({"1", "2", "3"}) int kinds;
    Shape[] arr;
    Sq[] sq;

    @Setup public void setup() {
        arr = new Shape[1024];
        sq = new Sq[1024];
        for (int i = 0; i < 1024; i++) {
            arr[i] = switch (i % kinds) { case 0 -> new Sq(i); case 1 -> new Rect(i, 2); default -> new Tri(i, 4); };
            sq[i] = new Sq(i);
        }
    }

    @Benchmark public long interfaceCall() { long t = 0; for (Shape s : arr) t += s.area(); return t; }
    /** Same work, static type is the final class: no virtual dispatch at all (only meaningful for kinds=1 numbers). */
    @Benchmark public long finalClassCall() { long t = 0; for (Sq s : sq) t += s.area(); return t; }
}
