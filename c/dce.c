int log_calls;
static int step(int x, int verbose) {
    int scaled = x * 1234;
    if (verbose) { log_calls++; return scaled; }
    return x + 1;
}
int compute(int x) { return step(x, 0); }
