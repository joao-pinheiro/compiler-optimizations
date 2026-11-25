import java.util.Arrays;
public class Intr {
    static int pop(int x)               { return Integer.bitCount(x); }
    static int popLoop(int x)           { int c = 0; while (x != 0) { x &= x - 1; c++; } return c; }
    static int nlz(int x)               { return Integer.numberOfLeadingZeros(x); }
    static double sqrt(double x)        { return Math.sqrt(x); }
    static int abs(int x)               { return Math.abs(x); }
    static int max(int a, int b)        { return Math.max(a, b); }
    static long mulHi(long a, long b)   { return Math.multiplyHigh(a, b); }
    static void fill(int[] a)           { Arrays.fill(a, 7); }
    static void fillLoop(int[] a)       { for (int i = 0; i < a.length; i++) a[i] = 7; }
    static void copy(int[] s, int[] d)  { System.arraycopy(s, 0, d, 0, s.length); }
    static int[] copyOf(int[] s)        { return Arrays.copyOf(s, s.length); }
    public static void main(String[] x) {
        int[] a = new int[1000], b = new int[1000];
        long t = 0;
        for (int i = 0; i < 300_000; i++) {
            t += pop(i) + popLoop(i) + nlz(i) + (long) sqrt(i) + abs(-i) + max(i, 5) + mulHi(i, i * 3L);
            fill(a); fillLoop(b); copy(a, b); t += copyOf(a).length + a[i % 1000];
        }
        System.out.println(t);
    }
}
