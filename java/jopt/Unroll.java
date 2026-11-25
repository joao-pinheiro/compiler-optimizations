public class Unroll {
    static int sum(int[] a) { int s = 0; for (int i = 0; i < a.length; i++) s += a[i]; return s; }
    static int prefix(int[] a) { int p = 0; for (int i = 0; i < a.length; i++) p = p * 3 + a[i]; return p; }
    static int sumMasked(int[] a) { int s = 0; for (int i = 0; i < a.length; i++) s += a[i & 1023]; return s; }
    public static void main(String[] x) {
        int[] a = new int[4096];
        for (int i = 0; i < a.length; i++) a[i] = i;
        long t = 0;
        for (int r = 0; r < 20000; r++) t += sum(a) + prefix(a) + sumMasked(a);
        System.out.println(t);
    }
}
