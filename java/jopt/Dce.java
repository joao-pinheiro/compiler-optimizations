public class Dce {
    static final boolean DEBUG = false;
    static int logCalls;
    static int compute(int x) {
        int unused = x * 1234;
        if (DEBUG) {
            logCalls++;
        }
        return x + 1;
    }
    static boolean dbgField = false;   // not constant
    static int compute2(int x) {
        if (dbgField) { logCalls++; }
        return x + 1;
    }
    public static void main(String[] a) {
        long t = 0;
        for (int i = 0; i < 3_000_000; i++) t += compute(i) + compute2(i);
        System.out.println(t);
    }
}
