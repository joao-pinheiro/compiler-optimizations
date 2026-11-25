#!/bin/bash
# Runs the JMH comparisons pinned to CPU 3. Output: results/*.txt
# usage: ./run.sh [name ...]   (after `mvn -q package`; no args = everything). Keep the machine idle while it runs.
cd "$(dirname "$0")"; mkdir -p results
JMH="taskset -c 3 java -jar target/benchmarks.jar -f 5 -wi 5 -w 1s -i 5 -r 1s"
want() { [ $# -eq 0 ] && return 0; for n in "${NAMES[@]}"; do [ "$n" = "$1" ] && return 0; done; [ ${#NAMES[@]} -eq 0 ]; }
NAMES=("$@")
run() { out=$1; shift; want "$out" || return 0; echo ">> $out"; "$@" > results/$out.txt 2>&1; }
run virt              $JMH VirtBench
run inline_on         $JMH InlineBench
run inline_off        $JMH InlineBench -jvmArgsAppend -XX:CompileCommand=dontinline,jopt.InlineBench::square
run inline_on_nosw    $JMH InlineBench -jvmArgsAppend -XX:-UseSuperWord
run inline_off_nosw   $JMH InlineBench -jvmArgsAppend "-XX:CompileCommand=dontinline,jopt.InlineBench::square -XX:-UseSuperWord"
run loop_unroll       $JMH LoopBench -jvmArgsAppend -XX:-UseSuperWord
run loop_nounroll     $JMH LoopBench -jvmArgsAppend "-XX:-UseSuperWord -XX:LoopUnrollLimit=0"
run vec_on            $JMH VecBench
run vec_off           $JMH VecBench -jvmArgsAppend -XX:-UseSuperWord
run esc_on            $JMH EscBench -prof gc
run esc_off           $JMH EscBench -prof gc -jvmArgsAppend -XX:-DoEscapeAnalysis
run dce               $JMH DceBench
