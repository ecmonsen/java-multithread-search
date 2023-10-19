# Multithreaded file search

This is a small Java program that downloads files from S3 and searches them for a particular word, in parallel. 
I implemented something similar during a job interview. This version uses standard and open source libraries and a mock 
S3 server.

The provided Docker container in `s3mocking` artificially throttles the network so that I can benchmark the program 
with different numbers of I/O threads.

## Prerequisites
- Java 1.8+ (I used the Amazon Coretto JDK 11)
- Apache Maven
- Optional: Docker to run the S3 simulator
- Optional: Python 3.X to generate a word list
- Optional: gnuplot for benchmarking graphs

## Setup

### Mock S3
I tested this with [S3 Ninja](https://s3ninja.net/).

1. In the `s3mocking` directory, run `docker-compose up -d`.
2. Go to [http://localhost:9444](http://localhost:9444) in your browser.
3. Copy the key id and secret key into your `~/.aws/credentials` file under a new profile, say `[ninja]`.
4. Use this command to create a fake bucket: `aws --endpoint-url http://localhost:9444 --profile ninja s3 mb s3://BUCKET`

### Wordlist

A wordlist is provided, but if you wish to regenerate it, you can do so by tokenizing the complete works of Shakespeare.

1. Create a Python virtual environment
2. Activate it
3. `pip install nltk`
4. `python tokenize_shakespeare.txt`

## Running the search program
The first time you run the program, it will attempt to generate 100 files x 1MB each with random words from the word 
list, and upload them to the S3 bucket and prefix.

```
mvn package

# Get help
./search -h

# Print CSV header only (for example if using a script to run this repeatedly for benchmarking) 
./search -header

# Search. Only provide "s3endpoint" if using mock S3 server
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

`benchmark/benchmark.sh` runs the search at various CPU thread counts. Usage is

```bash
./benchmark.sh BENCHMARK_NAME SEARCH_TERM -s3endpoint ENDPOINT -profile PROFILE -bucket BUCKET -prefix PREFIX
# Then use gnuplot to view the effect of different CPU thread counts
./plot.sh BENCHMARK_NAME.csv
```

Coming soon: Run repeatedly with different `-cputhreads`, `-iothreads` and so on.

Coming soon: Artificial CPU usage inflation.

