import java.util.NoSuchElementException;
import java.util.Random;

// A worker keeps pulling tasks off the shared queue until the queue
// is closed and empty, then it exits on its own.
public class Worker implements Runnable {
    private final TaskQueue queue;
    private final ResultStore store;
    private final Random random = new Random();

    public Worker(TaskQueue queue, ResultStore store) {
        this.queue = queue;
        this.store = store;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        Log.info("Worker started");
        int processed = 0;

        while (true) {
            Task task;
            try {
                task = queue.getTask();
            } catch (NoSuchElementException e) {
                // empty queue access, which here just means we're done
                Log.info("Queue is empty and closed, no more work");
                break;
            } catch (InterruptedException e) {
                Log.error("Interrupted while waiting for a task, stopping");
                Thread.currentThread().interrupt(); // keep the interrupt flag set
                break;
            }

            try {
                int value = process(task);
                store.addResult(task, value, name);
                processed++;
                Log.info("Finished " + task + " -> " + value);
            } catch (IllegalArgumentException e) {
                // bad data, log it and move on to the next task
                Log.error("Could not process " + task + ": " + e.getMessage());
                store.addFailure(task, e.getMessage(), name);
            } catch (InterruptedException e) {
                Log.error("Interrupted while processing " + task + ", stopping");
                store.addFailure(task, "interrupted", name);
                Thread.currentThread().interrupt();
                break;
            }
        }

        Log.info("Worker completed, processed " + processed + " task(s)");
    }

    // Pretend to do real work: sleep a bit, then square the number.
    private int process(Task task) throws InterruptedException {
        Thread.sleep(100 + random.nextInt(300)); // 100-399 ms of "computation"
        if (task.getData() < 0) {
            throw new IllegalArgumentException("negative input " + task.getData());
        }
        return task.getData() * task.getData();
    }
}
