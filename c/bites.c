#include <string.h>
#include <stddef.h>
void read_password(char *buf, size_t n);
int check(const char *pw);

int login(void) {
    char pw[64];
    read_password(pw, sizeof pw);
    int ok = check(pw);
    memset(pw, 0, sizeof pw);  /* swap for explicit_bzero to keep it */
    return ok;
}

int overflows(int x) {
    return x + 1 < x;
}

struct dev { int status; };
int get_status(struct dev *d) {
    int s = d->status;
    if (d == NULL)
        return -1;
    return s;
}

void delay(void) {
    for (int i = 0; i < 1000000; i++)
        ;
}

int ready;
void wait_ready(void) {
    while (!ready)
        ;
}
