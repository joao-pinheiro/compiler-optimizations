#!/bin/bash
# Benchmark run: performance governor, pin to the quietest physical core, sibling left idle.
# (On the benchmark host, other workloads were paused for the duration of the run.)
set -u
cd "$(dirname "$0")"
OLDGOV=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor)
restore() {
  for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo "$OLDGOV" > "$g"; done
}
trap restore EXIT INT TERM
for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > "$g"; done
sleep 10
# per-CPU busy over 5 s, then pick the physical core (cpu N and N+8) with the lowest combined load
grep '^cpu[0-9]' /proc/stat > s0; sleep 5; grep '^cpu[0-9]' /proc/stat > s1
paste s0 s1 | awk '{n=NF/2; ta=0; tb=0; for(i=2;i<=n;i++){ta+=$i; tb+=$(i+n)}; printf "%d %.1f\n", substr($1,4), 100*(1-(($(5+n)+$(6+n))-($5+$6))/(tb-ta))}' > busy.txt
CPU=$(awk '{b[$1]=$2} END {best=-1; h=NR/2; for(c=0;c<h;c++){s=b[c]+b[c+h]; if(best<0||s<bs){best=c; bs=s}}; print best}' busy.txt)
{ echo "cpu: $(lscpu | sed -n 's/^Model name: *//p' | head -1)  pinned: cpu $CPU (sibling $((CPU+$(nproc)/2)) left idle)"
  echo "date: $(date -Is)  kernel: $(uname -r)  governor: performance  load: $(cut -d' ' -f1-3 /proc/loadavg)"
  echo "per-cpu busy % before the run:"; tr '\n' ' ' < busy.txt; echo; } > env.txt
cat env.txt
T="taskset -c $CPU"
for i in $(seq 10); do for b in gcc-O2 clang-O2 gcc-O0 clang-O0; do echo "== $b"; $T ./bench-$b; done; done > c_bench.txt
$T ./goopt.test -test.run '^$' -test.bench . -test.count 10 > goopt_bench.txt
$T ./godry.test -test.run '^$' -test.bench . -test.count 10 > godry_bench.txt
$T ./goifs.test -test.run '^$' -test.bench . -test.count 10 > goifs_bench.txt
echo "load after: $(cut -d' ' -f1-3 /proc/loadavg)" >> env.txt
echo done
