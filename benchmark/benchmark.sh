#!/bin/bash

# Benchmark the program by running with various numbers of CPU threads.
set -euo pipefail

BENCHMARK_ID=$1
shift
SEARCH_TERM=$1
shift
ARGS=$*

OUTPUT_FILE="${BENCHMARK_ID}.csv"
echo "Writing results to $OUTPUT_FILE"
EXE=$(dirname $0)/../search

$EXE -header | tee "$OUTPUT_FILE"

for THREADS in $(seq 100 -10 50)  $(seq 45 -5 20) $(seq 15 -1 1)
do
   "$EXE" "$SEARCH_TERM"  -cputhreads $THREADS $ARGS
done | tee -a "$OUTPUT_FILE"

