#include <stdio.h>
#include <time.h>
static unsigned hash(const unsigned char *s, int n) {
    unsigned h = 0;
    for (int i = 0; i < n; i++)
        h = h * 31 + s[i];
    return h;
}
static unsigned char data[1024];
static double now(void) {
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec * 1e9 + t.tv_nsec;
}
int main(void) {
    double t0 = now();
    for (int i = 0; i < 1000000; i++)
        hash(data, sizeof data);               /* result discarded */
    double t1 = now();
    for (int i = 0; i < 1000000; i++) {
        const unsigned char *p = data;
        __asm__ volatile("" : "+r"(p));        /* hide the input: p could point anywhere */
        unsigned h = hash(p, sizeof data);
        __asm__ volatile("" : : "r"(h));       /* consume the result */
    }
    double t2 = now();
    printf("result discarded:              %.3f ns/op\n", (t1 - t0) / 1e6);
    printf("result consumed, input hidden: %.3f ns/op\n", (t2 - t1) / 1e6);
}
