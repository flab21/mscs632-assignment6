import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Shared place where workers save their results.
// Every method that touches the lists is synchronized so only one thread
// can be inside at a time.
public class ResultStore {
    private final List<String> results = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private final Set<Integer> seenIds = new HashSet<>();

    public synchronized void addResult(Task task, int value, String worker) {
        checkDuplicate(task.getId());
        results.add("Task " + task.getId() + " | input " + task.getData()
                + " | result " + value + " | by " + worker);
    }

    public synchronized void addFailure(Task task, String reason, String worker) {
        checkDuplicate(task.getId());
        failures.add("Task " + task.getId() + " | FAILED: " + reason + " | by " + worker);
    }

    private void checkDuplicate(int id) {
        if (!seenIds.add(id)) {
            Log.error("Duplicate result for task " + id + "! This should never happen.");
        }
    }

    public synchronized int resultCount() {
        return results.size();
    }

    public synchronized int failureCount() {
        return failures.size();
    }

    public synchronized int uniqueIds() {
        return seenIds.size();
    }

    // try-with-resources closes the writer even if a write fails
    public synchronized void writeToFile(String path) throws IOException {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            out.write("=== Java Data Processing Results ===");
            out.newLine();
            for (String line : results) {
                out.write(line);
                out.newLine();
            }
            out.write("=== Failed Tasks ===");
            out.newLine();
            for (String line : failures) {
                out.write(line);
                out.newLine();
            }
        }
    }
}
