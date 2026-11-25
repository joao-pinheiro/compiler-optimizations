#!/bin/bash
# usage: jl.sh Class method [extra JVM flags...]  -- C2 listing of Class::method up to the first ret (run from java/jopt)
c=$1; m=$2; shift 2
timeout ${JL_TIMEOUT:-60} java -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation -XX:CompileCommand=quiet -XX:CompileCommand=print,$c::$m "$@" $c $JL_ARGS 2>&1 | python3 "$(dirname "$0")/jasm.py" --last | awk '{print} /: ret/{exit}'
