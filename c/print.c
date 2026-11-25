#include <stdio.h>

void by_hand(void) {
    printf("the number is %d\n", 1);
    printf("the number is %d\n", 2);
    printf("the number is %d\n", 3);
    printf("the number is %d\n", 4);
    printf("the number is %d\n", 5);
    printf("the number is %d\n", 6);
    printf("the number is %d\n", 7);
}

void with_loop(void) {
    for (int i = 1; i < 8; i++)
        printf("the number is %d\n", i);
}

void one_line(void) {
    printf("the number is 1\n");
}
