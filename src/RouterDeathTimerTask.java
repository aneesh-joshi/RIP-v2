import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

public class RouterDeathTimerTask extends TimerTask {
    InetAddress routerIp;
    Rover rover;

    RouterDeathTimerTask(Rover rover, InetAddress routerIp){
        this.routerIp = routerIp;
        this.rover = rover;
    }

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
