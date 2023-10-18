package io.sunrisedata.pipeline1;

import org.apache.commons.cli.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A multi-threaded implementation of searching for a string in multiple S3 objects.
 */
public class App {
    private static final int DEFAULT_IO_THREAD_COUNT = 10;
    private static final int DEFAULT_CPU_THREAD_COUNT = 5;
    private static final double DEFAULT_NETWORK_FAILURE_RATE = 0.0025;
    private static final int DEFAULT_MAX_FILE_SIZE = 1024 * 1024; //1 MB
    private static final int DEFAULT_FILE_COUNT = 10;
    private static final int DEFAULT_MEM = 5;
    private static final int DEFAULT_MAX_FILE_RETRIES = 4;
    private static final Logger logger = Logger.getLogger("s3test");

    public static void main(String[] args) throws Exception {

        // Set up logging
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %5$s%6$s%n");
        logger.setLevel(Level.INFO);
        logger.addHandler(new ConsoleHandler());

        // Get command line options
        CommandLine cmd = getOptions(args);

        String searchTerm = cmd.getArgList().isEmpty() ? "hamlet" : cmd.getArgList().get(0);
        int numCpuThreads = getNumericOption(cmd, "cputhreads", DEFAULT_CPU_THREAD_COUNT).intValue();
        int numIoThreads = getNumericOption(cmd, "iothreads", DEFAULT_IO_THREAD_COUNT).intValue();
        int maxInMemoryFileCount = getNumericOption(cmd, "mem", DEFAULT_MEM).intValue();
        double failureRate = getNumericOption(cmd, "networkfailure", DEFAULT_NETWORK_FAILURE_RATE).doubleValue();

        // Set up resources
        S3Client s3Client = DependencyFactory.s3Client(cmd.getOptionValue("s3endpoint"),
                cmd.getOptionValue("profile"));
        DataGenerator dataGenerator = new DataGenerator(s3Client,
                cmd.getOptionValue("bucket"),
                cmd.getOptionValue("prefix"),
                "words.txt",
                DEFAULT_MAX_FILE_SIZE);
        dataGenerator.generate(DEFAULT_FILE_COUNT);

        Downloader downloader = new Downloader(
                s3Client,
                cmd.getOptionValue("bucket"),
                cmd.getOptionValue("prefix"));
        Searcher textSearcher = new Searcher(searchTerm);

        ParallelSearcher searcher = new ParallelSearcher(
                downloader,
                textSearcher,
                0,
                DEFAULT_FILE_COUNT,
                numCpuThreads,
                numIoThreads,
                DEFAULT_MAX_FILE_RETRIES,
                maxInMemoryFileCount
        );


        // Measure total clock time
        double startTime = System.nanoTime();
        Result result = searcher.search();
        double endTime = System.nanoTime();
        double elapsedSeconds = (endTime - startTime) / 1000000000.0;

        // searchTerm,count,elapsedSeconds,exceptionCount,numCpuThreads,numIoThreads,failureRate
        System.out.format("%s,%d,%d,%f,%f,%d%n",
                searchTerm,
                result.getCount(),
                elapsedSeconds,
                result.getExceptionCount(),
                numCpuThreads,
                numIoThreads,
                failureRate
        );
    }

    private static CommandLine getOptions(String[] args) {
        final Options options = new Options();
        options.addOption(Option.builder()
                .option("h")
                .desc("Show help message")
                .build());
        options.addOption(Option.builder()
                .option("header")
                .desc("Show CSV header for result output")
                .build());
        options.addOption(Option.builder()
                .option("bucket")
                .desc("Bucket")
                .hasArg()
                .build());
        options.addOption(Option.builder()
                .option("prefix")
                .desc("Prefix for all data files")
                .hasArg()
                .build());
        options.addOption(Option.builder()
                .option("s3endpoint")
                .desc("S3 endpoint, for example if using a local S3 simulator")
                .hasArg()
                .build());
        options.addOption(Option.builder()
                .option("profile")
                .desc("Named AWS profile whose credentials are stored in ~/.aws/credentials")
                .hasArg()
                .build());
        options.addOption(Option.builder()
                .option("cputhreads")
                .desc("Number of CPU threads")
                .hasArg()
                .type(Number.class)
                .build());
        options.addOption(Option.builder()
                .option("iothreads")
                .desc("Number of IO threads")
                .hasArg()
                .type(Number.class)
                .build());
        options.addOption(Option.builder()
                .option("networkfailure")
                .desc("Network failure rate")
                .hasArg()
                .type(Number.class)
                .build());
        options.addOption(Option.builder()
                .option("mem")
                .desc("Maximum number of files to allow in memory concurrently")
                .hasArg()
                .type(Number.class)
                .build());
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                showHelp(options);
                System.exit(2);
            }
            if (cmd.hasOption("header")) {
                System.out.println("searchTerm,count,elapsedSeconds,exceptionCount,numCpuThreads,numIoThreads,failureRate");
                System.exit(0);
            }
            return cmd;

        } catch (ParseException e) {
            System.err.println("Could not parse options. Message: " + e.getMessage());
            showHelp(options);
            System.exit(1);
        }
        return null;
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("pipeline1",
                "Perform a multithreaded search on downloaded files", options, null, true);
    }

    /**
     * Parse a numeric command line option, using a default value if the parsing fails.
     *
     * @param cmd          CommandLine containing the user-submitted options.
     * @param opt          Name of the option to parse.
     * @param defaultValue Default value to use if parsing fails.
     * @return Numeric value.
     */
    private static Number getNumericOption(CommandLine cmd, String opt, Number defaultValue) {
        Number value = defaultValue;
        if (cmd.hasOption(opt)) {
            try {
                value = (Number) cmd.getParsedOptionValue(opt);
            } catch (ParseException pe) {
                System.err.format("Error parsing option %s. Using default. Message: %s", opt, pe.getMessage());
            }
        }
        return value;
    }

}

class NeedHelpException extends Exception {
    NeedHelpException() {

    }
}

/**
 * Results of a search.
 */
class Result {
    private final int count;
    private final int exceptionCount;

    Result(int count, int exceptionCount) {
        this.count = count;
        this.exceptionCount = exceptionCount;
    }

    public int getCount() {
        return count;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }
}


class ParallelSearcher {
    private static final Logger logger = Logger.getLogger("s3test)");


    private final Searcher textSearcher;
    private final int endIndex;
    private final int numCpuThreads;

    private final AtomicInteger indexesQueued;
    private final int downloadRetries;
    private final int startIndex;
    private final Downloader downloader;
    private final ExecutorService cpuExecutor;
    private final ExecutorService ioExecutor;

    private int count;

    private int exceptionCount;
    // polling timeout in milliseconds
    private final long pollTimeout = 100;

    synchronized void addToCount(int v) {
        count += v;
    }

    synchronized void addToExceptionCount(int v) {
        exceptionCount += v;
    }

    // For limiting use of network bandwidth
    private final Semaphore inMemoryFileSemaphore;
    // For communication between the thread pools
    private final ArrayBlockingQueue<byte[]> inMemoryFileQueue;

    public ParallelSearcher(
            Downloader downloader,
            Searcher textSearcher,
            int startIndex,
            int endIndex,
            int numCpuThreads,
            int numIoThreads,
            int downloadRetries,
            int maxInMemoryFileCount) {
        this.cpuExecutor = Executors.newFixedThreadPool(numCpuThreads);
        this.ioExecutor = Executors.newFixedThreadPool(numIoThreads);

        this.textSearcher = textSearcher;
        this.endIndex = endIndex;
        this.numCpuThreads = numCpuThreads;
        this.downloader = downloader;
        this.indexesQueued = new AtomicInteger(startIndex);

        this.downloadRetries = downloadRetries;
        this.startIndex = startIndex;
        // manages load on memory by only downloading so many files at once.
        this.inMemoryFileSemaphore = new Semaphore(maxInMemoryFileCount, true);

        // capacity should be based on available memory
        this.inMemoryFileQueue = new ArrayBlockingQueue<>(maxInMemoryFileCount);
    }

    /**
     * Parallellized file search.
     *
     * @return
     */
    public Result search() {
        // CPU thread pool - grab queue messages as they arrive
        // I/O thread pool - download file and save (memory? disk?) and enqueue a message

        // TODO consider what to do when endIndex is very large - at what capacity does the executor's queue cause
        // slowdowns?
        for (int index = this.startIndex; index < this.endIndex; index++) {
            ioExecutor.submit(new CallableDownloader(index));
        }

        for (int t = 0; t < numCpuThreads; t++) {
            cpuExecutor.submit(
                    () -> {
                        int threadMatches = 0;
                        while (true) {
                            try {
                                byte[] indexData = inMemoryFileQueue.poll(pollTimeout, TimeUnit.MILLISECONDS);
                                if (indexData == null) {
                                    // Either we are waiting, or we are done.
                                    if (this.indexesQueued.get() >= endIndex) {
                                        // we are done
                                        break;
                                    }
                                    logger.fine("Polling timed out while waiting for new indexes");
                                } else {

                                    threadMatches += this.textSearcher.countMatches(indexData);
                                    // All done - submit my results
                                    this.addToCount(threadMatches);

                                    inMemoryFileSemaphore.release();
                                }
                            } catch (InterruptedException e) {
                                logger.warning("cpu thread interrupted");
                                break;
                            }
                        }
                    }

            );
        }

        cpuExecutor.shutdown();
        ioExecutor.shutdown();
        try {
            while (!cpuExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
                    || !ioExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                logger.fine("Still waiting...");
            }
        } catch (InterruptedException e) {
            logger.fine("Interrupted");
        }
        return new Result(count, exceptionCount);

    }

    class CallableDownloader implements Callable<Void> {
        private final int index;

        public CallableDownloader(int index) {
            this.index = index;

        }

        @Override
        public Void call() {
            for (int t = 0; t < downloadRetries; t++) {
                try {
                    // Semaphore controls number of downloaded files allowed in memory concurrently
                    inMemoryFileSemaphore.acquire();

                    byte[] indexData = downloader.download(index);
                    indexesQueued.incrementAndGet();
                    inMemoryFileQueue.add(indexData);
                    return null;
                } catch (IOException | InterruptedException e) {
                    inMemoryFileSemaphore.release();

                    addToExceptionCount(1);
                }
            }

            return null;
        }
    }
}