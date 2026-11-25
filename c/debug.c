__attribute__((noinline)) void consume(int v) {
    __asm__ volatile("" : : "r"(v));
}

void scale(int x) {
    int doubled = x * 2;
    consume(doubled);
    consume(0);
}

int main(int argc, char **argv) {
    (void)argv;
    scale(argc);
    return 0;
}
