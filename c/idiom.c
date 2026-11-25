unsigned sum_to(unsigned n) {
    unsigned s = 0;
    for (unsigned i = 1; i <= n; i++)
        s += i;
    return s;
}
int popcount(unsigned x) {
    int c = 0;
    while (x) { x &= x - 1; c++; }
    return c;
}
