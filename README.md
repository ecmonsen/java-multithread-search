# Multithreaded file search

This is a small Java program that downloads files from S3 and searches them for a particular word, in parallel. 
I implemented something similar during a job interview. This version uses standard and open source libraries.

## Prerequisites
- Java 1.8+ (I used the Amazon Coretto JDK 11)
- Apache Maven
- Optional: Docker to run an S3 simulator
- Optional: Python 3.X to generate a word list

## Setup

A wordlist is provided, but if you wish to regenerate it, you can do so by tokenizing the complete works of shakespeare.

1. Create a Python virtual environment
2. Activate it
3. `pip install nltk`
4. `python tokenize_shakespeare.txt`

## Running the search program
The first time you run the program, it will attempt to generate 100 files x 1MB each with random words from the word 
list, and upload them to the S3 bucket and prefix.

```
mvn package
# Only provide "s3endpoint" if using mock S3 server
./search -bucket BUCKET \
  -prefix PREFIX \
  -cputhreads N \
  -iothreads M \
  -profile my_aws_profile \
  -s3endpoint http://localhost:9444 \
  hamlet
```

## Benchmark
Output is in CSV format. Run `./search -header` to see the column headers.

Run repeatedly with different `-cputhreads`, `-iothreads` and so on.

Coming soon: Artificial network throttling and artificial CPU usage inflation.

Coming soon: gnuplot commands to plot your benchmarks CSV.
