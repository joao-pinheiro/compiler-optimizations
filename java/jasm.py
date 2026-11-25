"""Disassemble HotSpot JIT output without hsdis: read `java -XX:CompileCommand=print,...` output on stdin, take the
raw [MachCode] bytes of each compiled method and disassemble them with objdump (Intel syntax).
usage: java -XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=print,Cls::method ... Cls 2>&1 | python3 jasm.py [--last]"""
import re, subprocess, sys, tempfile

blocks, cur, start, header = [], None, None, None
for line in sys.stdin:
    if line.startswith("Compiled method"):
        header = line.strip()
    if line.startswith("[MachCode]"):
        cur, start = bytearray(), None
        continue
    if line.startswith("[/MachCode]") and cur is not None:
        blocks.append((header, start, bytes(cur))); cur = None
        continue
    if cur is None:
        continue
    m = re.match(r"\s*0x([0-9a-f]+): ((?:[0-9a-f]{2,4}\s?\|?\s?)+)$", line.rstrip())
    if m and ";" not in line:
        if start is None:
            start = int(m.group(1), 16)
        cur += bytes.fromhex(re.sub(r"[\s|]", "", m.group(2)))

if "--last" in sys.argv:
    blocks = blocks[-1:]
for header, start, code in blocks:
    with tempfile.NamedTemporaryFile(suffix=".bin") as f:
        f.write(code); f.flush()
        out = subprocess.run(["objdump", "-D", "-b", "binary", "-m", "i386:x86-64", "-M", "intel",
                              f"--adjust-vma={start}", f.name], capture_output=True, text=True).stdout
    print("==", header)
    for l in out.splitlines()[7:]:
        m = re.match(r"^\s*([0-9a-f]+):\t(?:[0-9a-f]{2} )+\s*\t(.*)$", l)
        if not m:
            continue  # continuation bytes of a long instruction
        text = re.sub(r"0x([0-9a-f]{9,})", lambda a: hex(int(a.group(1), 16) - start) if 0 <= int(a.group(1), 16) - start < len(code) else a.group(0), m.group(2))
        print(f"  {int(m.group(1), 16) - start:4x}: {text}")
