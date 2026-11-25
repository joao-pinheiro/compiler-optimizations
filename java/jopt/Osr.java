public class Osr {
    public static void main(String[] args) {
        long s = 0;
        for (int i = 0; i < 2_000_000_000; i++) s += (i ^ (i >> 3)) & 7;   // one long-running loop in main(): it is entered once
        System.out.println(s);
    }
}
