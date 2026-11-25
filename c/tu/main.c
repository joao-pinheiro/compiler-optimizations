void apply(int *a, int n, int (*f)(int));
static int dbl(int x) { return x * 2; }
void double_all(int *a, int n) { apply(a, n, dbl); }
