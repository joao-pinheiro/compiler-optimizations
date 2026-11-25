public class Esc {
    record Point(int x, int y) {
        Point add(Point o) { return new Point(x + o.x, y + o.y); }
        int dist2() { return x * x + y * y; }
    }
    static Point keep;
    static int local(int x, int y) {
        Point p = new Point(x, y);
        Point q = p.add(new Point(1, 2));
        return q.dist2();
    }
    static int escapes(int x, int y) {
        Point p = new Point(x, y);
        keep = p;
        return p.add(new Point(1, 2)).dist2();
    }
    public static void main(String[] a) {
        long t = 0;
        for (int i = 0; i < 3_000_000; i++) t += local(i & 255, 4) + escapes(i & 255, 4);
        System.out.println(t);
    }
}
