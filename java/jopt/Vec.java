public class Vec {
    static void add(int[] a, int[] b, int[] c) { for (int i = 0; i < c.length; i++) c[i] = a[i] + b[i]; }
    static int intSum(int[] a) { int s = 0; for (int i = 0; i < a.length; i++) s += a[i]; return s; }
    static float floatSum(float[] a) { float s = 0; for (int i = 0; i < a.length; i++) s += a[i]; return s; }
    public static void main(String[] x) {
        int n = 4096;
        int[] a = new int[n], b = new int[n], c = new int[n]; float[] f = new float[n];
        for (int i = 0; i < n; i++) { a[i] = i; b[i] = i; f[i] = i; }
        long t = 0;
        for (int r = 0; r < 30000; r++) { add(a, b, c); t += c[r & 1023] + intSum(a) + (long) floatSum(f); }
        System.out.println(t);
    }
}
