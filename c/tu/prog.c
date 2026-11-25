void double_all(int *a, int n);
int buf[1024];
int main(int argc, char **argv) { double_all(buf, argc * 100); return buf[3]; }
