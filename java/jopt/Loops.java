public class Loops {
    static void delay() { for (int i = 0; i < 1_000_000; i++) ; }
    static boolean ready;                 // plain field, set by another thread
    static volatile boolean readyV;
    static void waitReady()  { while (!ready) ; }
    static void waitReadyV() { while (!readyV) ; }

    public static void main(String[] a) throws Exception {
        for (int r = 0; r < 10; r++) {
            long t0 = System.nanoTime(); for (int i = 0; i < 100; i++) delay(); long t1 = System.nanoTime();
            if (r >= 8) System.out.printf("delay(): %.3f ns per call%n", (double) (t1 - t0) / 100);
        }
        if (a.length > 0 && a[0].equals("list")) return;
        boolean vol = a.length > 0 && a[0].equals("volatile");
        Thread t = new Thread(() -> { try { Thread.sleep(300); } catch (InterruptedException e) {} ready = true; readyV = true; });
        t.start();
        long t0 = System.nanoTime();
        if (vol) waitReadyV(); else waitReady();
        System.out.printf("%s wait finished after %d ms%n", vol ? "volatile" : "plain", (System.nanoTime() - t0) / 1_000_000);
    }
}
