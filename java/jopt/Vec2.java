public class Vec2 {
    static int dot(int[] a, int[] b) { int s = 0; for (int i = 0; i < a.length; i++) s += a[i] * b[i]; return s; }
    public static void main(String[] x) {
        int n = 4096; int[] a = new int[n], b = new int[n];
        for (int i = 0; i < n; i++) { a[i] = i; b[i] = i; }
        long t = 0; for (int r = 0; r < 30000; r++) t += dot(a, b);
        System.out.println(t);
    }
}
