public class Warm {
    static final byte[] data = new byte[1024];
    static int hash(byte[] b) {
        int h = 0x811c9dc5;
        for (int i = 0; i < b.length; i++) { h ^= b[i] & 0xff; h *= 16777619; }
        return h;
    }
    public static void main(String[] args) {
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 7);
        int batch = 25, batches = 60;
        long[] ns = new long[batches]; int sink = 0;
        for (int b = 0; b < batches; b++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < batch; i++) sink += hash(data);
            ns[b] = (System.nanoTime() - t0) / batch;
        }
        for (int b = 0; b < batches; b++) System.out.printf("calls %4d-%4d: %8d ns/call%n", b * batch + 1, (b + 1) * batch, ns[b]);
        System.out.println(sink);
    }
}
