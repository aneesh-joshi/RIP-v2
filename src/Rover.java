import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * The class which will be run per rover
 */
public class Rover {
    int id;
    private MulticastSocket socket;
    private InetAddress group;
    private byte[] buffer;
    private Map<InetAddress, RoutingTableEntry> routingTable;
    private Map<InetAddress, List<RoutingTableEntry>> neighborRoutingTableEntries;
    private Map<InetAddress, Timer> neighborTimers;
    private TimerTask timerTask;
    private InetAddress myAddress;


    private final static Logger LOGGER = Logger.getLogger("ROVER");
    private final static String MULTICAST_ADDRESS = "230.0.0.0";
    private final static int LISTEN_WINDOW = 1024,
                            MULTICAST_PORT = 4446,
                            ROUTE_UPDATE_TIME = 5,
                            ROUTE_DELAY_TIME = 2,
                            ROVER_OFFLINE_TIME_LIMIT = 10,
                            ROVER_OFFLINE_TIMER_START_DELAY = 7,
                            INFINITY = 16;

    /**
     * Constructs a rover with the given id with an IP ending in that id
     *
     * @param id
     */
    Rover(int id) throws IOException {
        this.id = id;
        routingTable = new HashMap<>();
        neighborRoutingTableEntries = new HashMap<>();
        neighborTimers = new HashMap<>();

        myAddress = InetAddress.getLocalHost(); // TODO stop relying on getLocalHost()
        LOGGER.info("Rover: " + id + " has IP address of " + myAddress);
        socket = new MulticastSocket(MULTICAST_PORT);
        group = InetAddress.getByName(MULTICAST_ADDRESS);
        socket.joinGroup(group);

        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    sendRIPUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };


        Timer routeUpdateTimer = new Timer("RIP Route Update Timer");
        routeUpdateTimer.scheduleAtFixedRate(timerTask, ROUTE_DELAY_TIME * 1000, ROUTE_UPDATE_TIME * 1000);

        new Thread(() -> {
            try {
                listenMulticast();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }).start();

    }

    /**
     * Updates entries as per the Distance Vector Algorithm when new entries are received
     *
     * @param newEntries The entries received
     */
    private void updateEntries(InetAddress sourceAddress, List<RoutingTableEntry> newEntries) {
        // Drop your own table entries
        if (sourceAddress.equals(myAddress)) {
            return;
        }

        // Cache the entries of neighbors to recalculate the path when a router dies
        neighborRoutingTableEntries.put(sourceAddress, newEntries);

//        if (!routingTable.containsKey(sourceAddress)) {
            routingTable.put(sourceAddress, new RoutingTableEntry(sourceAddress, (byte) 24, sourceAddress, (byte) 1)); // TODO Fix subnet
//        }

        // restart the timer task since we have received the heart beat
        if(neighborTimers.containsKey(sourceAddress)) {
            neighborTimers.get(sourceAddress).cancel();
        }
        neighborTimers.put(sourceAddress, new Timer(sourceAddress + " Death Timer"));
        neighborTimers.get(sourceAddress).scheduleAtFixedRate(
                new RouterDeathTimerTask(this, sourceAddress),
                ROVER_OFFLINE_TIMER_START_DELAY * 1000, ROVER_OFFLINE_TIME_LIMIT * 1000);


        for (RoutingTableEntry entry : newEntries) {
            // skip your own multicast
            if (myAddress.equals(entry.ipAddress)) {
                continue;
            }

            // If we've never seen the entry's IP before, we immediately add it
            if (!routingTable.containsKey(entry.ipAddress)) {
                routingTable.put(entry.ipAddress, new RoutingTableEntry(entry.ipAddress,
                        entry.subnetMask,
                        sourceAddress,
                        (byte) ((1 + entry.metric) >= INFINITY ? INFINITY : 1 + entry.metric)));
            }
            // If the entry is this tables next hop, we will trust it
            // Or if the entry is shorter, we update our entry
            else if(routingTable.get(entry.ipAddress).nextHop.equals(sourceAddress) ||
                    routingTable.get(entry.ipAddress).metric > 1 + entry.metric){

                routingTable.get(entry.ipAddress).metric =
                                (byte) ((1 + entry.metric) >= INFINITY ? INFINITY : 1 + entry.metric);
                routingTable.get(entry.ipAddress).nextHop = sourceAddress;
                routingTable.get(entry.ipAddress).subnetMask = entry.subnetMask;
            }
        }
    }


    /**
     * Send update packets out
     */
    private void sendRIPUpdate() throws IOException {
        LOGGER.info(myAddress + "'s table is \n" + routingTable.values());
        multicast(RIPPacketUtil.getRIPPacket((byte) 1, routingTable)); // TODO check if request or response
    }


    private void listenMulticast() throws IOException {
        byte[] buf = new byte[LISTEN_WINDOW];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            List<RoutingTableEntry> entries = RIPPacketUtil.decodeRIPPacket(packet.getData(), packet.getLength());
            updateEntries(packet.getAddress(), entries);
        }
//        socket.leaveGroup(group);
//        socket.close();
    }

    /**
     * Called by RouterDeathTimerTask object when
     * @param roverIp
     */
    void registerNeighborDeath(InetAddress roverIp){
        LOGGER.info(roverIp + " just died :(\n\n\n");
        neighborTimers.get(roverIp).cancel();
        routingTable.get(roverIp).metric = INFINITY;
        for(InetAddress inetAddress: routingTable.keySet()){
            if(routingTable.get(inetAddress).nextHop.equals(roverIp)){
                routingTable.get(inetAddress).metric = INFINITY;
            }
        }

    }

    /**
     * Mulicasts the given byte over the network
     * <p>
     * Note: I am not closing the socket since it is intended to be used often. TODO check
     *
     * @param buffer packet to be sent
     */
    private void multicast(byte[] buffer) throws IOException { // TODO obfs
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
        socket.send(packet);
    }


    public static void main(String[] args) throws IOException {
        new Rover(Integer.parseInt(args[0]));
    }
}
