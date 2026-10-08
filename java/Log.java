import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Small logger so every line shows the time and which thread printed it.
// synchronized so two threads can't print on top of each other.
public class Log {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static synchronized void info(String msg) {
        print("INFO ", msg);
    }

    public static synchronized void error(String msg) {
        print("ERROR", msg);
    }

    private static void print(String level, String msg) {
        String time = LocalTime.now().format(FMT);
        String thread = Thread.currentThread().getName();
        System.out.println("[" + time + "] [" + level + "] [" + thread + "] " + msg);
    }
}
