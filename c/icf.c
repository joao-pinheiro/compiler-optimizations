#include <stdio.h>
__attribute__((noinline)) int checksum_orders(const unsigned char *p, int n) {
    int s = 0; for (int i = 0; i < n; i++) s = s * 31 + p[i]; return s;
}
__attribute__((noinline)) int checksum_invoices(const unsigned char *p, int n) {
    int s = 0; for (int i = 0; i < n; i++) s = s * 31 + p[i]; return s;
}
int main(void) {
    printf("%p\n%p\n", (void*)checksum_orders, (void*)checksum_invoices);
}
