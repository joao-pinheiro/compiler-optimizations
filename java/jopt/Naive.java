public class Naive {
    static final byte[] data = new byte[1024];
    static int sink;

    static int hash(byte[] b) {
        int h = 0x811c9dc5;
        for (int i = 0; i < b.length; i++) { h ^= b[i] & 0xff; h *= 16777619; }
        return h;
    }
    static void naive(int n)  { for (int i = 0; i < n; i++) hash(data); }          // result discarded
    static void fixed(int n)  { int s = 0; for (int i = 0; i < n; i++) s += hash(data); sink = s; }  // result kept alive

    static void time(String name, Runnable r) {
        long t0 = System.nanoTime(); r.run(); long t1 = System.nanoTime();
        System.out.printf("%-6s %10.3f ns/op%n", name, (double) (t1 - t0) / 1_000_000);
    }
    public static void main(String[] args) {
        for (int round = 0; round < 6; round++) {
            time("naive", () -> naive(1_000_000));
            time("fixed", () -> fixed(1_000_000));
        }
    }
}
