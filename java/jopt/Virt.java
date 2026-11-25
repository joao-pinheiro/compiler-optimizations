public class Virt {
    interface Shape { int area(); }
    static final class Sq implements Shape { final int s; Sq(int s){this.s=s;} public int area(){ return s*s; } }
    static final class Rect implements Shape { final int w,h; Rect(int w,int h){this.w=w;this.h=h;} public int area(){ return w*h; } }
    static final class Tri implements Shape { final int b,h; Tri(int b,int h){this.b=b;this.h=h;} public int area(){ return b*h/2; } }

    static long total(Shape[] a) { long t = 0; for (Shape s : a) t += s.area(); return t; }

    // usage: Virt <mono|bi|mega|shift>
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "mono";
        int n = 1024;
        Shape[] arr = new Shape[n];
        int kinds = switch (mode) { case "mono", "shift" -> 1; case "bi" -> 2; default -> 3; };
        for (int i = 0; i < n; i++) arr[i] = switch (i % kinds) { case 0 -> new Sq(i); case 1 -> new Rect(i, 2); default -> new Tri(i, 4); };
        long t = 0;
        for (int r = 0; r < 20000; r++) t += total(arr);
        if (mode.equals("shift")) {   // a new receiver type shows up after the code was compiled for 1 type
            System.out.println("-- introducing Rect/Tri");
            for (int i = 0; i < n; i++) arr[i] = (i % 3 == 0) ? new Sq(i) : (i % 3 == 1 ? new Rect(i, 2) : new Tri(i, 4));
            for (int r = 0; r < 20000; r++) t += total(arr);
        }
        System.out.println(t);
    }
}
