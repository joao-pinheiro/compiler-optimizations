#include <stdbool.h>
typedef void *SemaphoreHandle_t;
#define portMAX_DELAY 0xffffffffu
#define pdTRUE 1
int xSemaphoreTake(SemaphoreHandle_t s, unsigned ticks);        /* opaque, in another file */
int xTaskCreate(void (*fn)(void *), const char *name, unsigned stack, void *arg, int prio, void *handle);
int recvfrom_frame(void);                                        /* stands in for the socket read */
void draw_animation(unsigned frame);
void show(void);

static SemaphoreHandle_t frame_semaphore;
static _Atomic bool udp_frame_mode = false;

static void udp_server_task(void *arg) {                         /* writer */
    (void)arg;
    for (;;) {
        if (recvfrom_frame() > 0)
            udp_frame_mode = true;
    }
}

static void simple_led_task(void *arg) {                         /* reader, as in main.c */
    (void)arg;
    unsigned frame = 0;
    while (1) {
        if (xSemaphoreTake(frame_semaphore, portMAX_DELAY) == pdTRUE) {
            if (!udp_frame_mode)
                draw_animation(frame);
            show();
            frame++;
        }
    }
}

void wait_for_udp(void) {                                        /* the spin-loop variant */
    while (!udp_frame_mode)
        ;
}

void app_main(void) {
    xTaskCreate(simple_led_task, "simple_led", 8192, 0, 10, 0);
    xTaskCreate(udp_server_task, "udp_server", 12288, 0, 5, 0);
}
