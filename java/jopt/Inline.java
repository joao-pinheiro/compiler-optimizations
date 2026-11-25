public class Inline {
    static int square(int x) { return x * x; }
    static int sumSquares(int a, int b) { return square(a) + square(b); }
    public static void main(String[] args) {
        long t = 0;
        for (int i = 0; i < 3_000_000; i++) t += sumSquares(i, i + 1);
        System.out.println(t);
    }
}
