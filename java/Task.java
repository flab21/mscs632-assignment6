// One unit of work that sits in the shared queue.
public class Task {
    private final int id;
    private final int data;

    public Task(int id, int data) {
        this.id = id;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public int getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Task#" + id + "(data=" + data + ")";
    }
}
