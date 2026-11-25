# HotSpot C2 (JDK 25): measured material for the compiler-optimizations article

Everything below was measured or read from a listing on this machine. Where C2 did not do what the C/Go sections lead you to expect, it says so.

## Environment

```text
$ java -version
openjdk version "25.0.5.1" 2026-11-17
OpenJDK Runtime Environment (build 25.0.5.1+2-1-26.04.1-Ubuntu)
OpenJDK 64-Bit Server VM (build 25.0.5.1+2-1-26.04.1-Ubuntu, mixed mode, sharing)

$ lscpu | grep 'Model name'
Model name: Intel(R) Core(TM) i7-14700KF        (AVX2, no AVX-512: UseAVX=2, MaxVectorSize=32)
```

- JMH 1.37 (`jmh-core` + `jmh-generator-annprocess`, Maven 3.9.12, shaded `jmh/target/benchmarks.jar`).
- Default GC is G1 (ergonomic); `LoopStripMiningIter=1000`, `LoopUnrollLimit=60`, `UseSuperWord=true` are the defaults.
- All JMH runs: `taskset -c 3 java -jar target/benchmarks.jar -f 5 -wi 5 -w 1s -i 5 -r 1s`. That is 5 forks x 5 measurement iterations = 25 samples; the `+-` is JMH's 99.9% confidence interval over them. Mode is average time, ns/op. The machine was idle while they ran (the first pass was disturbed by my other JVMs, so `virt`, `inline*`, `loop_*`, `vec_*` were re-run on the idle machine; those are the numbers below). CPU 3 is a hyperthread of a P-core; its sibling (CPU 2) was idle.
- Raw JMH output: `jmh/results/*.txt`. Raw standalone timings: `results/*.txt`.

### Commands

```bash
# listings (no hsdis): see jl.sh, a wrapper around jasm.py; it trims the output at the first `ret`
cd jopt && javac *.java
JVM="java -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation -XX:CompileCommand=quiet"
$JVM -XX:CompileCommand=print,Cls::method Cls 2>&1 | python3 ../jasm.py --last    # == ../jl.sh Cls method [flags]
javap -c -p Fold2.class Dce.class                                                   # bytecode (what javac did)
java -XX:+PrintCompilation Cls                                                      # tiers, OSR '%', 'made not entrant'
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -XX:CompileCommand=PrintInlining,Virt::total Virt bi
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintIntrinsics Intr
# JMH
cd ../jmh && mvn -q -B package && ./run.sh                  # all comparisons, flags are in run.sh
# standalone timings, pinned: taskset -c 3 java Warm | Naive | Cliff | Loops [volatile] ; java -Xint Osr ; java Tail
```

JVM flags used in the comparisons: `-XX:-UseSuperWord`, `-XX:LoopUnrollLimit=0`, `-XX:-DoEscapeAnalysis`, `-XX:CompileCommand=dontinline,jopt.InlineBench::square`, `-XX:-TieredCompilation`, `-XX:TieredStopAtLevel=1`, `-Xint`, `-Xss512m`.

Listing conventions: Intel syntax, offsets relative to the method start. The nmethod entry barrier (`cmp DWORD PTR [r15+0x20],..; jne`), the frame setup/teardown, the return poll (`cmp rsp,[r15+0x28]; ja`) and the out-of-line stubs after `ret` are removed and marked `...` unless they matter. A listing-based claim is only about the nmethod I printed.

**Caveat on `CompileCommand=print`.** In item 11a the loop left behind after the discarded call differs depending on whether `CompileCommand=print` is on (see there). Treat a printed listing as "what C2 can produce", and use a timing to confirm what ran. Where a timing is given next to a listing, the two agree.

---

## 1. Constant folding and propagation

Source (`jopt/Fold2.java`), same as the C example:

```java
static int secondsPerWeek() {
    int minutes = 60;
    int hours = 24 * minutes;
    return 7 * hours * 60;
}
static int secondsPerWeekFinal() {       // same, but with `final` locals
    final int minutes = 60;
    final int hours = 24 * minutes;
    return 7 * hours * 60;
}
static int viaParam(int x) { int k = 60; int h = 24 * k; return x + 7 * h * 60; }
```

javac (`javap -c -p Fold2`), no optimization flags exist for javac:

```text
secondsPerWeek():                        secondsPerWeekFinal():
  bipush 60 ; istore_0                     ldc #7   // int 604800
  bipush 24 ; iload_0 ; imul ; istore_1    ireturn
  bipush 7  ; iload_1 ; imul
  bipush 60 ; imul ; ireturn
```

C2:

```nasm
; Fold2::secondsPerWeek (C2)
  ...
  mov    eax,0x93a80        ; 604800
  ...
  ret
; Fold2::viaParam (C2)
  ...
  lea    eax,[rsi+0x93a80]  ; x + 604800
  ...
  ret
```

javac folds only constant expressions in the JLS sense: with `final` locals initialised from constants it emits `ldc 604800`; with plain (non-final) locals it emits the literal arithmetic, including the store/load of the locals, like gcc -O0. C2 folds both versions to `mov eax, 0x93a80`, and in `viaParam` propagates the constants through the locals so only the `x + 604800` remains (a single `lea`). The bytecode is "-O0 with constant-expression folding"; everything else is left to the JIT.

## 2. Strength reduction

Source (`jopt/Strength.java`):

```java
static int div10(int x)  { return x / 10; }
static int udiv10(int x) { return Integer.divideUnsigned(x, 10); }
static int mul9(int x)   { return x * 9; }
static int mod8(int x)   { return x % 8; }
static int div8(int x)   { return x / 8; }
static int shr3(int x)   { return x >> 3; }
```

C2 (arguments in `esi`):

```nasm
div10:                       ; signed: magic number, no idiv
  movsxd r10,esi
  sar    esi,0x1f           ; sign
  imul   r10,r10,0x66666667 ; ceil(2^34 / 10)
  sar    r10,0x22
  mov    eax,r10d
  sub    eax,esi            ; + 1 for negative x
mul9:   lea eax,[rsi+rsi*8]
mod8:                        ; signed x % 8
  mov r10d,esi ; sar r10d,0x1f ; shr r10d,0x1d ; add r10d,esi
  and r10d,0xfffffff8 ; mov eax,esi ; sub eax,r10d
div8:                        ; signed x / 8: bias negative values, then shift
  mov eax,esi ; sar eax,0x1f ; shr eax,0x1d ; add eax,esi ; sar eax,0x3
shr3:   mov eax,esi ; sar eax,0x3
udiv10:                      ; Integer.divideUnsigned(x, 10)
  mov    r10d,0xa
  mov    eax,esi
  cmp    r10d,0x0 ; jl ...  ; (divisor < 0 branch)
  xor    edx,edx
  div    r10d               ; a real div instruction
```

Multiplication by a constant becomes `lea`, signed division by a constant becomes a multiply-by-reciprocal and shifts, and signed `x / 8` is `sar/shr/add/sar` while `x >> 3` is one `sar`: for signed values they are not the same operation, so writing the shift by hand is wrong for negatives, as in the C section. Surprise: `Integer.divideUnsigned(x, 10)` was not strength-reduced; C2 emitted a real `div` with the generic negative-divisor check (gcc/clang turn the unsigned constant division into a multiply). I did not time it.

## 3. Dead code elimination

Source (`jopt/Dce.java`):

```java
static final boolean DEBUG = false;
static int logCalls;
static int compute(int x) {
    int unused = x * 1234;
    if (DEBUG) { logCalls++; }
    return x + 1;
}
static boolean dbgField = false;          // not a constant
static int compute2(int x) {
    if (dbgField) { logCalls++; }
    return x + 1;
}
```

javac (`javap -c -p Dce`):

```text
compute(int):                         compute2(int):
  iload_0 ; sipush 1234 ; imul          getstatic dbgField ; ifeq 14
  istore_1                              getstatic logCalls ; iconst_1 ; iadd ; putstatic logCalls
  iload_0 ; iconst_1 ; iadd ; ireturn   14: iload_0 ; iconst_1 ; iadd ; ireturn
```

C2:

```nasm
; Dce::compute
  ...
  lea    eax,[rsi+0x1]
  ...
  ret
; Dce::compute2
  ...
  movabs r10,0x455016c70
  movzx  r10d,BYTE PTR [r10+0x7d]   ; load dbgField
  test   r10d,r10d
  jne    0x41                        ; never taken so far: to the uncommon trap
  lea    eax,[rsi+0x1]
  ...
  ret
  ...                                ; 0x41:
  mov    esi,0xffffff45              ; trap reason code
  ...
  call   ...                         ; {runtime_call UncommonTrapBlob}
```

The `if (DEBUG)` block is removed by javac itself: no `getstatic DEBUG`, no branch, no `logCalls++` in the bytecode of `compute`. javac does not remove the dead `x * 1234` (the `imul; istore_1` is still in the bytecode); C2 does, leaving only `lea eax,[rsi+1]`. With a non-constant field (`dbgField`) nothing is removed at compile time; C2 keeps the load and the branch (the field can change), but the branch has never been taken, so it goes to an uncommon trap (`mov esi,0xffffff45; call` to `UncommonTrapBlob`): no `logCalls++` code is compiled at all, and the hot path is `lea`. If `dbgField` becomes true, the trap fires and the method is deoptimized (`../jl.sh Dce compute2` shows the listing up to the `ret`; the trap follows it).

## 4. Inlining

### 4a. Small static method

Source (`jopt/Inline.java`):

```java
static int square(int x) { return x * x; }
static int sumSquares(int a, int b) { return square(a) + square(b); }
```

```nasm
; Inline::sumSquares (C2)
  ...
  imul   esi,esi
  imul   edx,edx
  lea    eax,[rsi+rdx*1]
  ...
  ret
```

JMH (`InlineBench`, 1024-element `int[]`, `t += square(a[i])`), normal vs `-XX:CompileCommand=dontinline,jopt.InlineBench::square`:

| Variant | default (SuperWord on) | `-XX:-UseSuperWord` |
|---|---|---|
| inlined | 39.75 +- 0.74 ns/op | 239.8 +- 19.2 ns/op |
| `dontinline` | 1719.7 +- 152.8 ns/op | 1490.4 +- 10.9 ns/op |

Same shape as the C and Go listings: both calls disappear into two `imul`s. The 43x gap in the first column is not call overhead alone: once `square` is inlined the loop body is plain arithmetic and SuperWord vectorizes it. With SuperWord off the gap is 6.2x, which is the part attributable to the call itself and to the lost loop optimizations together. Inlining is the optimization that unlocks the others.

### 4b. Virtual calls: monomorphic, bimorphic, megamorphic

Source (`jopt/Virt.java`, `jmh/.../VirtBench.java`): an interface `Shape` with final implementations `Sq`, `Rect`, `Tri`; `total(Shape[])` sums `s.area()` over 1024 elements. The array holds 1, 2 or 3 of the classes (round-robin); each JMH fork profiles only its own mix.

PrintInlining for `Virt::total` (`java -XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=PrintInlining,Virt::total Virt <mode>`). The JDK 25 inline-tree printer runs the lines together ("callee changed to ..."); the relevant parts:

```text
mono:   @ 27  Virt$Sq::area (10 bytes)   inline (hot)  ... TypeProfile (142327/142327 counts) = Virt$Sq
bi:     @ 27  Virt$Sq::area (10 bytes)   inline (hot)
              Virt$Rect::area (10 bytes) inline (hot)  ... TypeProfile (44908/89817 counts) = Virt$Rect
                                                       ... TypeProfile (44909/89817 counts) = Virt$Sq
mega:   @ 27  Virt$Shape::area (0 bytes)   failed to inline: virtual call
```

C2 code for the call site (selected lines):

```nasm
; mono: class guard, then the inlined body; the loop is unrolled
  cmp    r9d,0x1040618      ; compressed klass pointer of Virt$Sq
  jne    <uncommon trap>
  imul   r10d,r10d          ; s*s
  ...
; mega:
  call   <itable/vtable stub>     ; no guard, no inlining, no unrolling
```

JMH (`VirtBench`, ns per 1024 calls):

| Classes in the array | `interface Shape` call | `Sq[]`, final class, static dispatch |
|---|---|---|
| 1 (monomorphic) | 422.4 +- 58.6 | 309.6 +- 9.7 |
| 2 (bimorphic)   | 535.4 +- 58.3 | 319.3 +- 9.7 |
| 3 (megamorphic) | 2070.2 +- 199.8 | 306.0 +- 7.9 |

The column on the right does not depend on `kinds` (it always iterates the `Sq[]`) and is a control for run-to-run variation (about 4%). A monomorphic site is inlined behind a type guard, a bimorphic site inlines both targets with a guard each, and a third receiver type makes C2 give up: a real call per element and 4.9x the time of the monomorphic case (3.9x of bimorphic). Note the interface mono/bi error bars are wide (+-14% and +-11%), so mono vs bi (1.27x) is only marginally resolved; the cliff at 3 types is not in doubt.

### 4c. Deoptimization when a new receiver type appears

Source: `Virt shift` fills the array with `Sq` only, runs `total` 20000 times, then replaces the elements with all three classes and runs again. Tiered (default) `-XX:+PrintCompilation`, `Virt::total` lines only:

```text
   15   14 %     3       Virt::total @ 11 (43 bytes)
   16   15       3       Virt::total (43 bytes)
   16   16 %     4       Virt::total @ 11 (43 bytes)
   21   14 %     3       Virt::total @ 11 (43 bytes)   made not entrant: OSR invalidation of lower level
   21   17       4       Virt::total (43 bytes)
   24   15       3       Virt::total (43 bytes)   made not entrant: not used
-- introducing Rect/Tri
   32   17       4       Virt::total (43 bytes)   made not entrant: uncommon trap
   32   16 %     4       Virt::total @ 11 (43 bytes)   made not entrant: uncommon trap
   32   27       4       Virt::total (43 bytes)
```

The tier-4 code (and its OSR version) was compiled with a type guard for `Sq` and an uncommon trap for everything else. As soon as a `Rect` arrives, the trap fires, the nmethods are made not entrant ("uncommon trap"), execution falls back to the interpreter, and a new tier-4 `total` (id 27) is compiled from the updated profile. The other `made not entrant: ... lower level / not used` lines are the normal tier-3 to tier-4 hand-over, not deopts. See 11c for the cost.

## 5. Loop unrolling and range-check elimination

Source (`jopt/Unroll.java`):

```java
static int sum(int[] a) { int s = 0; for (int i = 0; i < a.length; i++) s += a[i]; return s; }
```

SuperWord is off in these listings and in the JMH runs so that vectorization does not mask the unrolling (`-XX:-UseSuperWord`).

Default (`LoopUnrollLimit=60`):

```nasm
; Unroll::sum, -XX:-UseSuperWord
  mov    r10d,DWORD PTR [rsi+0xc]      ; a.length
  xor    r11d,r11d
  test   r10d,r10d ; jbe <exit>
  lea    r9d,[r10-0x1]
  cmp    r9d,r10d ; jae <range-check trap>      ; the ONLY bounds check, before the loops
  mov    eax,DWORD PTR [rsi+0x10]               ; pre-loop: first element, peeled (i = 0)
  lea    r9d,[r10-0xf] ; mov ecx,0x1
  cmp    r9d,0x1 ; jle <post>
  mov    ebx,0x3e80                            ; 16000 = 1000 (LoopStripMiningIter) x 16 (unroll)
  ... compute the end of this strip: min(limit, ecx+16000) -> r8d
 main:
  add    eax,DWORD PTR [rsi+rcx*4+0x10]       ; 16 adds, no bounds checks
  add    eax,DWORD PTR [rsi+rcx*4+0x14]
  ...  (16 in total, +0x10 ... +0x4c)
  add    ecx,0x10
  cmp    ecx,r8d ; jl main
  mov    r8,QWORD PTR [r15+0x30]              ; safepoint poll, once per strip
  test   DWORD PTR [r8],eax
  cmp    ecx,r9d ; jl <next strip>
 post:                                         ; post-loop: leftover 0..15 elements
  add    eax,DWORD PTR [rsi+rcx*4+0x10]
  inc    ecx ; cmp ecx,r10d ; jl post
```

`-XX:LoopUnrollLimit=0`:

```nasm
  ... same single range check before the loop ...
  mov    r8d,0x3e8                             ; 1000 iterations per strip
 loop:
  add    eax,DWORD PTR [rsi+rcx*4+0x10]       ; one add per iteration
  inc    ecx
  cmp    ecx,r9d ; jl loop
  mov    r8,QWORD PTR [r15+0x30]              ; safepoint poll every 1000 iterations
  test   DWORD PTR [r8],eax
  cmp    ecx,r10d ; jl <next strip>
```

JMH (`LoopBench`, `int[4096]`, both runs with `-XX:-UseSuperWord`):

| Benchmark | default | `-XX:LoopUnrollLimit=0` |
|---|---|---|
| `sum` (`s += a[i]`) | 848.7 +- 22.1 ns/op | 1162.0 +- 21.9 ns/op |
| `prefix` (`p = p*3 + x[i]`) | 2562.0 +- 57.8 ns/op | 2571.9 +- 30.1 ns/op |

What the listing shows: the range check is hoisted out of the loop entirely (one `cmp/jae` against `length - 1` before the loops; the main loop has none), the first iteration is peeled as a pre-loop, the main loop is unrolled 16x, a post-loop handles the remainder, and the safepoint poll is not in the iteration body but in a strip-mining outer loop that runs once per 1000 inner iterations (so 16000 elements here). Without unrolling the strip-mining and the range-check hoisting remain. Unrolling gives 1.37x on `sum`. It gives nothing on `prefix`, whose loop-carried dependency (`p*3 + x[i]`, a `lea` plus an `add` chain) limits it; C2 unrolled it 8x anyway (`lea`/`add` pairs in the listing), with no measurable effect. The `LoopUnrollLimit=0` listing has no pre-loop and no post-loop: those exist because of unrolling.

Also seen: when the index is not affine (`a[i & 1023]`, `jopt/Poll.java`) C2 keeps a bounds check per element (`cmp r9d,r8d; jae` in each unrolled copy), because the range-check elimination needs an index that is a linear function of the induction variable. The strip is still `0xfa0` = 1000 x 4 (unroll 4) with the poll after it.

## 6. Vectorization (SuperWord)

Source (`jopt/Vec.java`, `jopt/Vec2.java`, `jmh/.../VecBench.java`, n = 4096):

```java
for (int i = 0; i < n; i++) c[i] = a[i] + b[i];               // add
for (int i = 0; i < n; i++) s += a[i];                         // intSum   (int accumulator)
for (int i = 0; i < n; i++) s += a[i] * b[i];                  // intDot
for (int i = 0; i < n; i++) s += la[i];                        // longSum
for (int i = 0; i < n; i++) s += fa[i];                        // floatSum (float accumulator)
```

```nasm
; Vec::add (C2) - AVX2, 8 ints per instruction, unrolled
  vmovdqu ymm0,YMMWORD PTR [rdx+rbx*4+0x10]
  vpaddd  ymm0,ymm0,YMMWORD PTR [rsi+rbx*4+0x10]
  vmovdqu YMMWORD PTR [rcx+rbx*4+0x10],ymm0
  ...  (7 more; 8 triples, +0x10 ... +0xf0, 64 ints per iteration)

; Vec2::dot (C2)
  vpmulld ymm2,ymm0,YMMWORD PTR [rdx+rcx*4+0x10]    ; vector multiply, vector accumulators
  ...

; Vec::intSum (C2): no ymm/xmm instruction at all, scalar adds, 16x unrolled (see item 5)
  add    eax,DWORD PTR [rsi+rcx*4+0x10]
  ...

; Vec::floatSum (C2): scalar, one dependent chain
  vaddss xmm1,xmm0,DWORD PTR [rsi+r8*4+0x10]
  vaddss xmm0,xmm1,DWORD PTR [rsi+r8*4+0x14]
  ...
```

JMH (default vs `-XX:-UseSuperWord`), ns/op for 4096 elements:

| Benchmark | SuperWord on | `-XX:-UseSuperWord` | ratio |
|---|---|---|---|
| `add` (`c = a + b`) | 159.8 +- 2.9 | 785.7 +- 13.4 | 4.9x |
| `intDot` (`s += a[i]*b[i]`) | 151.1 +- 1.9 | 1258.2 +- 40.9 | 8.3x |
| `intSum` (`s += a[i]`) | 825.7 +- 19.0 | 961.1 +- 6.1 | ~1x (same scalar code; difference is noise) |
| `longSum` | 948.1 +- 4.3 | 964.8 +- 6.9 | 1.02x (not vectorized) |
| `floatSum` | 1635.3 +- 21.1 | 1886.0 +- 20.8 | ~1x (same scalar code; difference is noise) |

The element-wise add vectorizes to `vpaddd ymm` and is 4.9x faster. The plain int sum reduction does not vectorize on this JDK/CPU (no vector instruction in the listing; timing matches the scalar loop; both versions are scalar, and the C2 code is identical with and without SuperWord, so the 16% difference is run-to-run noise; a short re-run gave about 1.0x); the same holds for `long` and for `float`. A dot product (reduction plus a multiply) does vectorize, at 8.3x. I did not look into why a pure reduction is rejected; it is what this JVM does. `floatSum` staying scalar is expected: float addition is not associative and Java requires the source order (the `vaddss` chain above is that order). The Vector API (`jdk.incubator.vector`) is relevant only as the way to ask for the vectorization C2 declined to do for reductions; I did not measure it.

## 7. Idiom recognition and intrinsics

Source (`jopt/Intr.java`):

```java
static int pop(int x)             { return Integer.bitCount(x); }
static int popLoop(int x)         { int c = 0; while (x != 0) { x &= x - 1; c++; } return c; }
static int nlz(int x)             { return Integer.numberOfLeadingZeros(x); }
static double sqrt(double x)      { return Math.sqrt(x); }
static int abs(int x)             { return Math.abs(x); }
static int max(int a, int b)      { return Math.max(a, b); }
static long mulHi(long a, long b) { return Math.multiplyHigh(a, b); }
static void fill(int[] a)         { Arrays.fill(a, 7); }
static void fillLoop(int[] a)     { for (int i = 0; i < a.length; i++) a[i] = 7; }
static void copy(int[] s, int[] d){ System.arraycopy(s, 0, d, 0, s.length); }
```

```nasm
pop:      popcnt eax,esi
nlz:      lzcnt  eax,esi
sqrt:     vsqrtsd xmm0,xmm0,xmm0
abs:      xor eax,eax ; sub eax,esi ; cmovl eax,esi
max:      cmp esi,edx ; mov eax,esi ; cmovl eax,edx
mulHi:    mov rax,rdx ; imul rsi ; mov rax,rdx           ; one-operand imul, high half in rdx
popLoop:  ... blsr esi,esi ... inc eax ... test [poll],eax ... jne     ; still a loop, one bit per iteration
copy:     ... vzeroupper ; movabs r10,<addr> ; call r10  ; "runtime_call Stub::jint_disjoint_arraycopy"
fill:     vpbroadcastd ymm0,... ; vmovdqu YMMWORD PTR [rsi+rdx*4+0x10],ymm0 ... (identical to fillLoop)
```

`-XX:+PrintIntrinsics` lists `Integer::bitCount`, `Integer::numberOfLeadingZeros`, `Math::abs`, `Math::sqrt`, `Math::max`, `Math::multiplyHigh` and `System::arraycopy` as `(intrinsic)`.

`Integer.bitCount`, `numberOfLeadingZeros`, `Math.sqrt`, `abs`, `max` and `multiplyHigh` compile to single instructions or short branch-free sequences instead of the Java bodies. `System.arraycopy` becomes a call into a runtime stub (`jint_disjoint_arraycopy`) after one length check; it is a call, not inline code. Two things did not go as the task framing suggested. First, `Arrays.fill` did not become a stub: it is inlined and compiled to the same AVX2 store loop as the hand-written loop (no intrinsic is reported for it, so this is the loop being vectorized). Second, the hand-written popcount loop was not recognized as an idiom (it uses `blsr`, but still loops once per set bit), unlike `Integer.bitCount` which is a named intrinsic: Java gets idiom recognition by method name, not by pattern-matching loops.

## 8. Tail calls

Source (`jopt/Tail.java`):

```java
static long sum(long n, long acc) {          // tail position
    depth++;
    if (n == 0) return acc;
    return sum(n - 1, acc + n);
}
```

Results (default stack, `java Tail`):

```text
interpreter only (-Xint):                    StackOverflowError after depth 7869
default, first call (cold):                  StackOverflowError after depth 11558
after 200,000 warm-up calls (C2-compiled):   StackOverflowError after depth 59022
-Xss512m, first call:                        completes: sum(10_000_000) = 50000005000000
-Xss512m, after warm-up, sum(100_000_000):   StackOverflowError after depth 33547918
```

C2 listing of `Tail::sum` (calls and branches only): one `call 0x0`, i.e. a call back into the method itself (offset 0 relative to the start), with the usual frame setup before it. HotSpot does not eliminate tail calls: the recursion overflows the stack in the interpreter and in C2 code; after compilation it gets 5x deeper before overflowing, and a bigger `-Xss` just moves the limit (33.5 million frames with 512 MB). Unlike gcc and clang at -O2 in the C section, there is no loop produced. The bytecode (`javap -c`) has a plain recursive `invokestatic`.

## 9. Escape analysis and scalar replacement

Source (`jmh/.../EscBench.java`, `jopt/Esc.java`):

```java
record Point(int x, int y) {
    Point add(Point o) { return new Point(x + o.x, y + o.y); }
    int dist2() { return x * x + y * y; }
}
int local()   { Point p = new Point(x, y); Point q = p.add(new Point(1, 2)); return q.dist2(); }  // nothing escapes
int escapes() { Point p = new Point(x, y); keep = p; return p.add(new Point(1, 2)).dist2(); }    // p stored in a static
```

C2 listing of `local(x, y)`:

```nasm
; default
  ...
  inc    esi            ; x + 1
  imul   esi,esi
  add    edx,0x2        ; y + 2
  imul   edx,edx
  lea    eax,[rsi+rdx*1]
  ...
  ret

; -XX:-DoEscapeAnalysis (abridged; three allocations, all with the TLAB bump sequence)
  mov    r11,QWORD PTR [r15+0x1c8]      ; TLAB top
  mov    r10,r11 ; add r10,0x18         ; 24-byte object
  cmp    r10,QWORD PTR [r15+0x1d8]      ; TLAB end
  jae    <slow path>
  mov    QWORD PTR [r15+0x1c8],r10
  prefetchw BYTE PTR [r10+0xc0]
  mov    QWORD PTR [r11],0x1            ; mark word
  mov    DWORD PTR [r11+0x8],0x1040210  ; klass
  mov    DWORD PTR [r11+0xc],ebp        ; x
  ...  (the same again for the other two Points, then loads of x and y back from memory)
  imul   eax,eax ; imul r10d,r10d ; add eax,r10d
```

`escapes()` with escape analysis on: one allocation remains (the stored `p`), plus the G1 post-write barrier for the static-field store; the temporary `new Point(1, 2)` and the result of `add` are gone (only `add r11d,2; imul; inc ebp; imul` arithmetic remains).

JMH `-prof gc`:

| Benchmark | EA on (default) | `-XX:-DoEscapeAnalysis` |
|---|---|---|
| `local` time | 0.420 +- 0.005 ns/op | 11.599 +- 1.390 ns/op |
| `local` alloc | ~0 B/op (`gc.alloc.rate` 0.006 MB/s) | 72 B/op, 6028 MB/s |
| `escapes` time | 2.223 +- 0.071 ns/op | 7.230 +- 1.387 ns/op |
| `escapes` alloc | 24 B/op, 10298 MB/s | 72 B/op, 9906 MB/s |

With escape analysis the non-escaping `Point`s are scalar-replaced: no allocation instruction in the listing, 0 B/op, and the whole method is five arithmetic instructions. Without it all three Points are allocated (3 x 24 B = 72 B/op, and 27x the time). In `escapes` only the stored Point is allocated (24 B/op, 1 of 3). One oddity: with `-XX:-DoEscapeAnalysis` `escapes` (7.2 ns) was faster than `local` (11.6 ns) even though both allocate 72 B; both have large error bars (+-19% / +-12%) and I did not investigate.

## 10. JIT-specific behaviour

### 10a. Tiered compilation and warm-up

Source (`jopt/Warm.java`): FNV-1a over a 1024-byte array, `hash(data)` called 25 times per batch, 60 batches, time per call for each batch. Run pinned to CPU 3 (`results/warm_*.txt`).

Default (tiered), selected batches:

```text
calls    1-  25:    14756 ns/call      interpreter
calls   26-  50:    15141 ns/call
calls   51-  75:    16773 ns/call
calls   76- 100:   125402 ns/call      slow batch right before tier 3 (cause not investigated)
calls  101- 125:     3313 ns/call      tier 3 (C1 with profiling)
calls  126- 200:     2867-3068 ns/call  (2821-3407 up to call 825)
calls  726- 750:     3212 ns/call
calls  826- 850:   108426 ns/call      slow batch right before tier 4 (cause not investigated)
calls  851- 875:     1026 ns/call      tier 4 (C2) from here
calls  876- 900:     1021 ns/call
calls 1476-1500:      997 ns/call
```

```text
$ java -XX:+PrintCompilation Warm | grep Warm::hash
   18    8 %     3       Warm::hash @ 5 (34 bytes)               C1, profiled, OSR (the loop inside hash)
   18    9       3       Warm::hash (34 bytes)                   C1, profiled
   18   10 %     4       Warm::hash @ 5 (34 bytes)               C2, OSR
   21    8 %     3       Warm::hash @ 5 (34 bytes)   made not entrant: OSR invalidation of lower level
   23   11       4       Warm::hash (34 bytes)                   C2
   ... 9  3 Warm::hash   made not entrant: not used           (tier-3 version retired)
```

Steady-state ns/call at calls 1476-1500 under other settings:

| Setting | calls 1-25 | calls 1476-1500 |
|---|---|---|
| default tiered | 14756 | 997 |
| `-Xint` | 13315 | 13229 |
| `-XX:TieredStopAtLevel=1` (C1 only) | 13489 | 985 |
| `-XX:-TieredCompilation` (C2 only) | 14488 | 973 |

The first ~100 calls (about 1.5 ms) run in the interpreter at ~15 us per call; tier-3 code is ~5x faster; tier-4 is another ~3x. Tier 3 is C1 with profiling; with `TieredStopAtLevel=1` (tier 1, no profiling) the same loop runs at 0.99 us instead of 3 us. Single runs; the C2-only run has a slow batch at calls 176-200 (190689 ns/call), the C1-only run one at 226-250 (22902). Possibly the JIT threads sharing CPU 3 with the benchmark under taskset; not investigated. `PrintCompilation` shows the C2 nmethod installed 23 ms after JVM start. On this simple loop C1 alone reaches the same steady state as C2 (985 vs 973 ns/call, 1.2% apart), so C2's advantage here is zero; it matters when it can inline, vectorize or remove allocations (items 4, 6, 9).

### 10b. On-stack replacement

Source (`jopt/Osr.java`): a single 2-billion-iteration loop inside `main`, entered once (no method call can be compiled and swapped in, so only OSR can speed it up).

```text
$ java -XX:+PrintCompilation Osr
   17    8 %     3       Osr::main @ 4 (36 bytes)         % = OSR, bci 4 is the loop header
   17    9       3       Osr::main (36 bytes)
   18   10 %     4       Osr::main @ 4 (36 bytes)
   19    8 %     3       Osr::main @ 4 (36 bytes)   made not entrant: OSR invalidation of lower level
  836   10 %     4       Osr::main @ 4 (36 bytes)   made not entrant: uncommon trap
```

Wall time for the whole program: `java Osr` 0.78 s, `java -Xint Osr` 13.2 s. The `%` entries are compilations entered in the middle of a running invocation at the loop's back-edge: the interpreter hands its live state (locals) to the compiled loop. The loop gets there 17-18 ms after start, after which the program is C2 speed. The last line is the tier-4 OSR nmethod being invalidated by an uncommon trap at 836 ms, near the end of the run (I did not look at which trap).

## 11. When it bites, Java versions

### 11a. A naive `System.nanoTime` benchmark measures nothing

Source (`jopt/Naive.java`, an FNV-1a hash over 1024 bytes; `hash.c` and the Go version use `h*31+c`):

```java
static void naive(int n) { for (int i = 0; i < n; i++) hash(data); }                       // result discarded
static void fixed(int n) { int s = 0; for (int i = 0; i < n; i++) s += hash(data); sink = s; }
// timing: t0 = nanoTime(); naive(1_000_000); t1 = nanoTime();  ns/op = (t1 - t0) / 1e6
```

`java Naive` (round 1, 2, then steady state):

```text
naive      104.927 ns/op   fixed 935.076 ns/op       <- round 1
naive       46.586 ns/op   fixed 903.392 ns/op       <- round 2
naive        0.003 ns/op   fixed 904.628 ns/op       <- rounds 3-6: 0.003, 0.002, 0.001, 0.001
```

`java -Xint Naive` (same code, no JIT): `naive` 10685 ns/op, `fixed` 11544 ns/op. So the number the naive version reports is 0.001 ns/op once C2 has compiled it: 1 ps for work that costs ~900 ns, and 1e6 calls in ~1-3 us total. The first two rounds (105 and 47 ns/op) are the loop still partially running while the compiler is working: the naive benchmark does not even give one stable wrong number.

With JMH (`DceBench`, `-prof` off, `jmh/results/dce.txt`):

```java
@Benchmark public void naive()                    { hash(d); }               // discarded
@Benchmark public void blackhole(Blackhole bh)    { bh.consume(hash(d)); }
@Benchmark public int  returned()                 { return hash(d); }
```

| Benchmark | ns/op |
|---|---|
| `naive` | 0.381 +- 0.006 |
| `blackhole` | 860.9 +- 52.3 |
| `returned` | 812.8 +- 16.7 |

Listing of the JMH-generated loop for `naive` (`DceBench_naive_jmhTest::naive_avgt_jmhStub`, C2, loop only):

```nasm
 loop:
  movzx  r8d,BYTE PTR [rdi+0x94]     ; JMH's `isDone` flag
  mov    r11,QWORD PTR [r15+0x30]    ; safepoint poll
  inc    r13                          ; operations counter
  test   DWORD PTR [r11],eax
  test   r8d,r8d
  jne    <exit>
  mov    r10d,DWORD PTR [rcx+0xc]     ; the array `d` is still loaded and its length checked ...
  mov    r8d,DWORD PTR [r12+r10*8+0xc]
  test   r8d,r8d ; jbe loop
  lea    r9d,[r8-0x1] ; cmp r9d,r8d ; jb loop   ; ... but nothing is read from it
  ...                                 ; (fallthrough: out-of-line trap)
```

There is no byte load from `d`, no `imul`, no xor: the 1024-iteration hash loop is gone and only the harness loop is left. The 0.381 ns/op is the cost of one harness iteration (about 2 cycles); it is not hash cost. Both `blackhole` and `returned` keep the hash alive and report 813-861 ns/op, which agrees with the ~900 ns/op `fixed` figure from the standalone version. Unlike the Go case in the article, where 157 vs 839 ns/op looked plausible, this one is off by three orders of magnitude and is obviously wrong.

Caveat on the listing for the standalone `naive(int)` (the bare loop left by the compiler): in `java Naive` it is gone entirely (0.001 ns/op over 3 runs), but under `-XX:CompileCommand=print,Naive::naive` the loop survives as an empty counted loop (`add r10d,0x10`, 16x unrolled, with a safepoint poll per iteration) and the reported time goes to 0.015-0.021 ns/op (about 18 us per 1e6 calls, which fits that loop). So whether the empty loop is also deleted varied with how the VM was started; the hash call is gone in both. I did not find out why, and I would not claim in the article that C2 always removes the empty loop. (The JMH listing above was printed with `-XX:-TieredCompilation` and the print option; the JMH `naive` result is 0.38 ns/op with and without them, so the listing matches the timing.)

### 11b. A loop without side effects

Source (`jopt/Loops.java`):

```java
static void delay() { for (int i = 0; i < 1_000_000; i++) ; }

static boolean ready;                    // set by another thread after 300 ms
static volatile boolean readyV;
static void waitReady()  { while (!ready) ; }
static void waitReadyV() { while (!readyV) ; }
```

`delay()`:

```nasm
; Loops::delay (C2)
  ...                                  ; only the entry barrier
  ret                                  ; nothing in between: the loop is deleted
```

Timing: `-Xint` 4,153,131 ns per call to `delay()` (4.2 ms); compiled the call costs ~8-24 ns, which is the call itself from the interpreted caller loop. Java, like the C compilers, removes a counted loop with no side effects completely.

`waitReady()` (plain field), the OSR-compiled loop:

```nasm
; Loops::waitReady @ 0 (C2, OSR)
  ...
  movabs r10,<address of Loops.class mirror>
  movzx  r11d,BYTE PTR [r10+0x78]     ; `ready`, read ONCE
  test   r11d,r11d
  jne    <done>
 spin:
  mov    r10,QWORD PTR [r15+0x30]     ; poll
  test   DWORD PTR [r10],eax
  jmp    spin                          ; never reads `ready` again
```

Program behaviour: with the plain field, `java Loops` never finishes (killed by `timeout 8` after the writer thread had set `ready` at 300 ms; exit status 124). With `volatile` (`java Loops volatile`): `volatile wait finished after 300 ms`. With `-Xint` the plain version also finishes (303 ms): the interpreter re-reads the field every time. So this is the gcc behavior from the C section, not the clang one: C2 hoists the read once and spins forever (the loop still contains the safepoint poll), and it only happens after the loop gets compiled; a debug/interpreted run hides the bug. The fix is the same, `volatile` (shown working above). Different from the C section, Java did not delete the `while(!ready);` loop the way clang does.

### 11c. Deoptimization cliff: a call site goes megamorphic mid-run

Source (`jopt/Cliff.java`): `total(Shape[])` as in 4b, 1024 shapes, windows of 20000 rounds. Windows 0-7 only have `Sq`; before window 8 the array is refilled with all three classes. `java Cliff` pinned to CPU 3 (`results/cliff.txt`):

```text
window  0:    915.2 ns/round     (warm-up)
window  1-7:  ~400 ns/round      (window 5: 747, an outlier I did not explain)
-- 3 receiver types from here
window  8:   2016.9 ns/round
window  9:   1601.2 ns/round
window 10:   1975.7 ns/round
window 11-15: 1744-1799 ns/round
```

`-XX:+PrintCompilation` (`results/cliff_printcompilation.txt`) shows `Cliff::total` (tier 4 and its OSR version) `made not entrant: uncommon trap` at 127 ms, then a fresh tier-3 and tier-4 compile of `total` a few ms later.

After the third class appears the steady-state cost per round goes from about 400 ns to about 1800 ns (4.4x) and stays there: the recompiled code has the megamorphic call (4b). The window in which the trap fires and the code is rebuilt is also one of the slowest (2017 ns/round), but the recompile itself takes a few ms (the new tier-3 and tier-4 `total` are installed within about 10 ms of the trap in `PrintCompilation`), small against the steady-state loss. In the same program a nearby window (window 9, 1601) is faster than steady state, within the noise of single runs; I ran this once for the table and once for PrintCompilation, with the same picture (window 0-7 ~400, 8-15 ~1800-2000).
