import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

/**
 * A timer task for when a Rover goes down
 */
public class RouterDeathTimerTask extends TimerTask {
    InetAddress routerIp;
    Rover rover;

    /**
     * Constructs a timer task for rover death
     * @param rover the Rover object running the timer
     * @param routerIp the ip of the rover which is being checked
     */
    RouterDeathTimerTask(Rover rover, InetAddress routerIp){
        this.routerIp = routerIp;
        this.rover = rover;
    }

    /**
     * Task which is run on the timer running out
     */
    @Override
    public void run() {
        try {
            rover.registerNeighborDeath(routerIp);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(42);
        }
    }
}
