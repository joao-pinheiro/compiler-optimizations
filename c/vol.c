#include <stdatomic.h>
void delay(void) {
    for (volatile int i = 0; i < 1000000; i++)
        ;
}
atomic_int ready;
void wait_ready(void) {
    while (!atomic_load(&ready))
        ;
}
