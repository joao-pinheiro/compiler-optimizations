public class Poll {
    static long sumLong(int[] a, long n) { long s = 0; for (long i = 0; i < n; i++) s += a[(int) (i & 1023)]; return s; }
    static int sumInt(int[] a, int n) { int s = 0; for (int i = 0; i < n; i++) s += a[i & 1023] * 3; return s; }
    public static void main(String[] x) {
        int[] a = new int[1024];
        for (int i = 0; i < a.length; i++) a[i] = i;
        long t = 0;
        for (int r = 0; r < 20000; r++) t += sumLong(a, 5000) + sumInt(a, 5000);
        System.out.println(t);
    }
}
