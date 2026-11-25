unsigned hash(const unsigned char *s, int n) {
    unsigned h = 0;
    for (int i = 0; i < n; i++)
        h = h * 31 + s[i];
    return h;
}
