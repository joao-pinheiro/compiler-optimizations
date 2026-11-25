public class Fold {
    static int sum(int n) { int s = 0; for (int i = 0; i < n; i++) s += i * 4; return s; }
    public static void main(String[] a) {
        long t = 0;
        for (int i = 0; i < 200_000; i++) t += sum(i & 1023);
        System.out.println(t);
    }
}
