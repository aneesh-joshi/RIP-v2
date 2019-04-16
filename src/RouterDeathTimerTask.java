import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

/**
 * A timer task for when a Rover goes down
 */
public class RouterDeathTimerTask extends TimerTask {
    private InetAddress routerPrivateAddress, routerPublicAddress;
    private Rover rover;

    /**
     * Constructs a timer task for rover death
     * @param rover the Rover object running the timer
     * @param routerPrivateAddress the ip of the rover which is being checked
     */
    RouterDeathTimerTask(Rover rover, InetAddress routerPrivateAddress, InetAddress routerPublicAddress){
        this.routerPrivateAddress = routerPrivateAddress;
        this.routerPublicAddress = routerPublicAddress;
        this.rover = rover;
    }

    /**
     * Task which is run on the timer running out
     */
    @Override
    public void run() {
        try {
            rover.registerNeighborDeath(routerPrivateAddress, routerPublicAddress);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(42);
        }
    }
}
