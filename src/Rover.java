import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * The class which will be run per rover
 */
public class Rover {
    int id;
    private DatagramSocket socket;
    private InetAddress group;
    private byte[] buffer;
    private List<RoutingTableEntry> routingTableEntries;
    private TimerTask timerTask;

    private final static Logger LOGGER = Logger.getLogger("ROVER");
    private final static int LISTEN_WINDOW = 1024;
    private final static String MULTICAST_ADDRESS = "230.0.0.0";
    private final static int MULTICAST_PORT = 4446;
    private final static int MAX_ROVERS = 12;
    private final static int ROUTE_UPDATE_TIME = 5, ROUTE_DELAY_TIME = 2;

    /**
     * Constructs a rover with the given id with an IP ending in that id
     *
     * @param id
     */
    Rover(int id) {
        this.id = id;
        routingTableEntries = new ArrayList<>(); // since we can have a maximum of
                                                            // 10 entries
        routingTableEntries.add(new RoutingTableEntry("1.1.1.1", (byte)2, "15.15.2.2", (byte)12));
        try {
            socket = new DatagramSocket();
            group = InetAddress.getByName("230.0.0.0");
        }catch (SocketException | UnknownHostException e){
            System.err.println(e);
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                sendRIPUpdate();
            }
        };

        Timer routeUpdateTimer = new Timer("RIP Route Update Timer");
        routeUpdateTimer.scheduleAtFixedRate(timerTask, ROUTE_DELAY_TIME * 1000, ROUTE_UPDATE_TIME * 1000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                listenMulticast(MULTICAST_ADDRESS, MULTICAST_PORT);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                startRIP();
            }
        }).start();
    }

    /**
     * Performs the sending and updating functions of RIP
     */
    void startRIP() {

    }

    /**
     * Send update packets out
     */
    void sendRIPUpdate(){
        multicast(RIPPacketUtil.getRIPPacket((byte)1, routingTableEntries)); // TODO check if request or response
    }

    void advertiseEntry() {
//        try {
//            multicast("Hey all, this is Rover " + id + " signing in! My IP is " + NetworkInterface.getByName("eth0").InetAddress.getLocalHost());
//        } catch (UnknownHostException e) {
//            System.err.println(e);
//        }catch (SocketException e){
//            System.err.println(z);
//        }
    }

    void listenMulticast(String multicastAddress, int port) {
        try {
            MulticastSocket socket = new MulticastSocket(port);
            InetAddress group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group);
            byte[] buf = new byte[LISTEN_WINDOW];
            check(socket, group, buf);
        } catch (IOException  e) {
            System.err.println(e);
        }
    }

    static void check(MulticastSocket socket, InetAddress group, byte[] buf) throws IOException {
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            List<RoutingTableEntry> entries = RIPPacketUtil.decodeRIPPacket(packet.getData(), packet.getLength());
            System.out.println("Got this " + entries.size() + "\n");
            for(int count = 0; count < entries.size(); count++){
                System.out.println(entries.get(count));
            }
        }
//        socket.leaveGroup(group);
//        socket.close();
    }

    /**
     * Mulicasts the given byte over the network
     *
     * Note: I am not closing the socket since it is intended to be used often. TODO check
     *
     * @param buffer packet to be sent
     */
    void multicast(byte[] buffer) { // TODO obfs
        try {
            DatagramPacket packet
                    = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            socket.send(packet);

            LOGGER.info("Message sent");
        } catch (SocketException | UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    public static void main(String[] args) {
//        new Rover(Integer.parseInt(args[0])).advertiseEntry();
        Rover rover = new Rover(12);
        rover.advertiseEntry();
    }

}
