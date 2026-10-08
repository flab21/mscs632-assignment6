import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Shared queue that all the worker threads pull from.
// A ReentrantLock guards the LinkedList (LinkedList is NOT thread safe by itself).
// The Condition lets workers sleep while the queue is empty instead of spinning.
public class TaskQueue {
    private final Queue<Task> tasks = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private boolean closed = false;

    public void addTask(Task task) {
        lock.lock();
        try {
            if (closed) {
                throw new IllegalStateException("queue is closed, can't add " + task);
            }
            tasks.add(task);
            notEmpty.signal(); // wake up one waiting worker
        } finally {
            lock.unlock(); // always unlock, even if something above throws
        }
    }

    // Blocks until a task is available.
    // Throws NoSuchElementException when the queue is closed AND empty,
    // which is the signal for a worker to stop.
    public Task getTask() throws InterruptedException {
        lock.lock();
        try {
            while (tasks.isEmpty() && !closed) {
                // timed wait so a worker can never get stuck here forever
                notEmpty.await(500, TimeUnit.MILLISECONDS);
            }
            // remove() throws NoSuchElementException if the list is empty
            return tasks.remove();
        } finally {
            lock.unlock();
        }
    }

    // Tell the workers no more tasks are coming.
    public void close() {
        lock.lock();
        try {
            closed = true;
            notEmpty.signalAll(); // wake everyone so they can see it's closed
        } finally {
            lock.unlock();
        }
    }
}
