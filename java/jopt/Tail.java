public class Tail {
    static int depth;
    static long sum(long n, long acc) {          // tail position: nothing left to do after the call
        depth++;
        if (n == 0) return acc;
        return sum(n - 1, acc + n);
    }
    static long sumLoop(long n) { long acc = 0; for (; n > 0; n--) acc += n; return acc; }
    public static void main(String[] a) {
        try { System.out.println(sum(10_000_000L, 0)); }
        catch (StackOverflowError e) { System.out.println("StackOverflowError after depth " + depth); }
        System.out.println(sumLoop(10_000_000L));
        // after warm-up the method is C2 compiled; does it become a loop?
        for (int i = 0; i < 200_000; i++) { depth = 0; sum(50, 0); }
        depth = 0;
        try { sum(100_000_000L, 0); } catch (StackOverflowError e) { System.out.println("after JIT: StackOverflowError after depth " + depth); }
    }
}
