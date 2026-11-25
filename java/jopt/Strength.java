public class Strength {
    static int div10(int x)       { return x / 10; }
    static int udiv10(int x)      { return Integer.divideUnsigned(x, 10); }
    static int mul9(int x)        { return x * 9; }
    static int mod8(int x)        { return x % 8; }
    static int div8(int x)        { return x / 8; }
    static int shr3(int x)        { return x >> 3; }
    static int umod8(int x)       { return x & 7; }
    public static void main(String[] a) {
        long t = 0;
        for (int i = -1_000_000; i < 1_000_000; i++)
            t += div10(i) + udiv10(i) + mul9(i) + mod8(i) + div8(i) + shr3(i);
        System.out.println(t);
    }
}
