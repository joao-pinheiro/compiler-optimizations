public class Cliff {
    interface Shape { int area(); }
    static final class Sq implements Shape { final int s; Sq(int s){this.s=s;} public int area(){ return s*s; } }
    static final class Rect implements Shape { final int w,h; Rect(int w,int h){this.w=w;this.h=h;} public int area(){ return w*h; } }
    static final class Tri implements Shape { final int b,h; Tri(int b,int h){this.b=b;this.h=h;} public int area(){ return b*h/2; } }
    static long total(Shape[] a) { long t = 0; for (Shape s : a) t += s.area(); return t; }

    public static void main(String[] args) {
        int n = 1024, window = 20000;
        Shape[] arr = new Shape[n];
        for (int i = 0; i < n; i++) arr[i] = new Sq(i);
        long t = 0;
        for (int w = 0; w < 16; w++) {
            if (w == 8) {                      // from here on the call site sees 3 receiver types
                System.out.println("-- 3 receiver types from here");
                for (int i = 0; i < n; i++) arr[i] = (i % 3 == 0) ? new Sq(i) : (i % 3 == 1 ? new Rect(i, 2) : new Tri(i, 4));
            }
            long t0 = System.nanoTime();
            for (int r = 0; r < window; r++) t += total(arr);
            System.out.printf("window %2d: %8.1f ns/round%n", w, (double) (System.nanoTime() - t0) / window);
        }
        System.out.println(t);
    }
}
