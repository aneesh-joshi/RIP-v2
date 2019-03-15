import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerTest {
    public void givenUsingTimer_whenSchedulingRepeatedTask_thenCorrect() {
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                System.out.println("Task performed on " + new Date());
            }
        };
        Timer timer = new Timer("Timer");

        long delay = 1000L;
        long period = 1000L;
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    public static void main(String[] args) {
        new TimerTest().givenUsingTimer_whenSchedulingRepeatedTask_thenCorrect();
    }
}
