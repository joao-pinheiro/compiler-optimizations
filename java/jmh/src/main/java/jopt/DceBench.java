package jopt;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/** The Java version of "the benchmark that measured nothing": FNV-1a over 1024 bytes (the C and Go versions use h*31+c instead). */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class DceBench {
    static final byte[] data = new byte[1024];
    static { for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 7); }
    byte[] d = data;   // non-constant reference, so the array is not constant-folded

    static int hash(byte[] b) {
        int h = 0x811c9dc5;
        for (int i = 0; i < b.length; i++) { h ^= b[i] & 0xff; h *= 16777619; }
        return h;
    }

    @Benchmark public void naive() { hash(d); }                       // result discarded
    @Benchmark public void blackhole(Blackhole bh) { bh.consume(hash(d)); }
    @Benchmark public int returned() { return hash(d); }
}
