public class Fold2 {
    static int secondsPerWeek() {
        int minutes = 60;
        int hours = 24 * minutes;
        return 7 * hours * 60;
    }
    static int secondsPerWeekFinal() {
        final int minutes = 60;
        final int hours = 24 * minutes;
        return 7 * hours * 60;
    }
    static int viaParam(int x) { int k = 60; int h = 24 * k; return x + 7 * h * 60; }
    public static void main(String[] a) {
        long t = 0;
        for (int i = 0; i < 2_000_000; i++) t += secondsPerWeek() + secondsPerWeekFinal() + viaParam(i);
        System.out.println(t);
    }
}
