#!/bin/bash

# Scrappy script to generate some comparison charts using gnuplot via docker.
# Usage: $0 file1.csv file2.csv ...
# Input files were generated using ./benchmark.sh

OUTPUT_FILE=comparison.png
INIT="set ylabel 'Elapsed Time'; set xlabel 'CPU Thread Count';
       set grid;
       set datafile separator ',';
       set term png size 800,380;
       set output '$OUTPUT_FILE';
"

CMD=""
for CSV_FILE in $*
do
  if [[ "$CMD" != "" ]]
  then
    CMD="$CMD,"
  else
    CMD="plot"
  fi
  # Column 2 is numThreads, column 5 is elapsed time
  CMD="$CMD '$CSV_FILE' using 5:3 title '$(basename ${CSV_FILE%.*})' with lines linewidth 2"
done

CMD="$INIT $CMD;"

docker run --rm -v $(pwd):/work remuslazar/gnuplot -e  "$CMD"
open "$OUTPUT_FILE"

