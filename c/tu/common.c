void apply(int *a, int n, int (*f)(int)) {
    for (int i = 0; i < n; i++)
        a[i] = f(a[i]);
}
