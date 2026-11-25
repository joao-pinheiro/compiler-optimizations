unsigned long fact(unsigned long n, unsigned long acc) {
    if (n <= 1) return acc;
    return fact(n - 1, acc * n);
}
