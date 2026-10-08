import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Multi-threaded Data Processing System (Java version)
// Usage: java Main [outputFile]
public class Main {
    private static final int NUM_WORKERS = 4;
    private static final int NUM_TASKS = 20;

    public static void main(String[] args) {
        String outFile = args.length > 0 ? args[0] : "java_results.txt";

        TaskQueue queue = new TaskQueue();
        ResultStore store = new ResultStore();

        // fill the queue. Tasks 7 and 15 get negative data on purpose
        // so we can see the error handling work.
        for (int i = 1; i <= NUM_TASKS; i++) {
            int data = (i == 7 || i == 15) ? -i : i * 3;
            queue.addTask(new Task(i, data));
        }
        Log.info("Added " + NUM_TASKS + " tasks to the queue");
        queue.close(); // no more tasks coming

        // the executor manages the worker threads for us
        ExecutorService pool = Executors.newFixedThreadPool(NUM_WORKERS);
        for (int i = 0; i < NUM_WORKERS; i++) {
            pool.submit(new Worker(queue, store));
        }
        pool.shutdown(); // stop accepting new jobs, let the current ones finish

        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                Log.error("Workers took too long, forcing shutdown");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Log.error("Main thread interrupted while waiting for workers");
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            store.writeToFile(outFile);
            Log.info("Results written to " + outFile);
        } catch (IOException e) {
            Log.error("Could not write results file: " + e.getMessage());
        }

        Log.info("Summary: " + store.resultCount() + " succeeded, "
                + store.failureCount() + " failed, "
                + store.uniqueIds() + "/" + NUM_TASKS + " tasks accounted for");
        if (store.uniqueIds() == NUM_TASKS) {
            Log.info("Every task was handled exactly once");
        } else {
            Log.error("Some tasks were missed!");
        }
    }
}
