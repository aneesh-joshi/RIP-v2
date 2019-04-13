import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * A Rover class which runs the RIPv2 protocol and updates it's tables accordingly.
 *
 * @author Aneesh Joshi
 */
public class Rover {
    byte id;
    private MulticastSocket socket;
    private InetAddress group;
    private byte[] buffer;
    private Map<InetAddress, RoutingTableEntry> routingTable;
    private Map<InetAddress, List<RoutingTableEntry>> neighborRoutingTableEntries;
    private Map<InetAddress, Timer> neighborTimers;
    private TimerTask timerTask;
    private InetAddress myAddress;
    private int multicastPort;


    private final static Logger LOGGER = Logger.getLogger("ROVER");
    private final static int LISTEN_WINDOW = 1024,
            ROUTE_UPDATE_TIME = 5,
            ROUTE_DELAY_TIME = 2,
            ROVER_OFFLINE_TIME_LIMIT = 10,
            ROVER_OFFLINE_TIMER_START_DELAY = 7,
            INFINITY = 16;
    private final static byte RIP_REQUEST = 1,
            RIP_UPDATE = 2,
            SUBNET_MASK = 24;

    /**
     * Constructs a rover with the given id with an IP ending in that id
     *
     * @param id
     */
    Rover(byte id, int multicastPort, InetAddress multicastIP) throws IOException {
        this.id = id;
        this.multicastPort = multicastPort;
        routingTable = new ConcurrentHashMap<>();
        neighborRoutingTableEntries = new HashMap<>();
        neighborTimers = new HashMap<>();

        myAddress = getMyInetAddress();

        LOGGER.info("Rover: " + id + " has IP address of " + myAddress);
        socket = new MulticastSocket(multicastPort);
        group = multicastIP;
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
    private void updateEntries(InetAddress sourceAddress, byte sourceRoverId, byte ripCommand, List<RoutingTableEntry> newEntries) throws IOException {

        // Drop your own table entries
        if (sourceAddress.equals(myAddress)) {
            return;
        }

        boolean updateHappened = false;

        // Cache the entries of neighbors to recalculate the path when a router dies
        neighborRoutingTableEntries.put(sourceAddress, newEntries);

        RoutingTableEntry tempEntry = new RoutingTableEntry(sourceAddress, (byte) 24, sourceAddress, (byte) 1);
        if (!routingTable.containsKey(sourceAddress) || !routingTable.get(sourceAddress).equals(tempEntry)) {
            routingTable.put(sourceAddress, tempEntry);
            updateHappened = true;
        }


        // restart the timer task since we have received the heart beat
        if (neighborTimers.containsKey(sourceAddress)) {
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
            updateHappened |= updateTableFromEntry(sourceAddress, entry);
        }

        // Send an update if
        if (updateHappened) {
            LOGGER.info(myAddress + "'s table is \n" + getStringRoutingTable());
            sendRIPUpdate();
        } else if (ripCommand == RIP_REQUEST) { // If a request was made, we have to send the update
            sendRIPUpdate();
        }
    }


    /**
     * Send update packets out
     */
    private void sendRIPUpdate() throws IOException {
        multicast(RIPPacketUtil.getRIPPacket(RIP_UPDATE, id, routingTable));
    }

    /**
     * Listens on the multicast ip and updates the routing table entries accordingly
     *
     * @throws IOException
     */
    private void listenMulticast() throws IOException {
        byte[] buf = new byte[LISTEN_WINDOW];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            List<RoutingTableEntry> entries = RIPPacketUtil.decodeRIPPacket(packet.getData(), packet.getLength());
            updateEntries(packet.getAddress(), packet.getData()[2], packet.getData()[0], entries);
        }
    }

    /**
     * Called by RouterDeathTimerTask object when
     *
     * @param deadRoverIp IP of the rover which died/is offline
     */
    void registerNeighborDeath(InetAddress deadRoverIp) throws IOException {
        LOGGER.info(deadRoverIp + " just died :(\n\n\n");
        neighborTimers.get(deadRoverIp).cancel();
        routingTable.get(deadRoverIp).metric = INFINITY;

        for (InetAddress inetAddress : routingTable.keySet()) {
            if (routingTable.get(inetAddress).nextHop.equals(deadRoverIp)) {
                routingTable.get(inetAddress).metric = INFINITY;
            }
        }

        neighborRoutingTableEntries.remove(deadRoverIp);
        for (InetAddress neighborIp : neighborRoutingTableEntries.keySet()) {
            for (RoutingTableEntry entry : neighborRoutingTableEntries.get(neighborIp)) {
                if (entry.ipAddress.equals(myAddress) ||
                        entry.ipAddress.equals(deadRoverIp) ||
                        entry.nextHop.equals(myAddress) ||
                        entry.nextHop.equals(deadRoverIp)) {
                    continue;
                }


                routingTable.put(neighborIp, new RoutingTableEntry(neighborIp, (byte) SUBNET_MASK, neighborIp, (byte) 1));
                // If we've never seen the entry's IP before, we immediately add it
                updateTableFromEntry(neighborIp, entry);
            }
        }

        LOGGER.info(myAddress + "'s table is \n" + getStringRoutingTable());
        // send a triggered update
        sendRIPUpdate();

    }

    /**
     * Update the routing table based on the given entry.
     * Note: this function was separated from updateRoutingTable since it is also used when a neighbor dies
     *
     * @param neighborIp the ip of the neighbor who sent this entry
     * @param entry      the entry in that neighbor's table
     * @return true if the entry updates something, false otherwise
     */
    private boolean updateTableFromEntry(InetAddress neighborIp, RoutingTableEntry entry) {
        int entryVal = entry.nextHop.equals(myAddress) ? INFINITY : entry.metric;

        if (!routingTable.containsKey(entry.ipAddress)) {
            routingTable.put(entry.ipAddress, new RoutingTableEntry(entry.ipAddress,
                    entry.subnetMask,
                    neighborIp,
                    (byte) ((1 + entryVal) >= INFINITY ? INFINITY : 1 + entryVal)));
        }
        // If the entry is this tables next hop, we will trust it
        // Or if the entry is shorter, we update our entry
        else if (routingTable.get(entry.ipAddress).nextHop.equals(neighborIp) ||
                routingTable.get(entry.ipAddress).metric > 1 + entryVal) {

            routingTable.get(entry.ipAddress).metric =
                    (byte) ((1 + entryVal) >= INFINITY ? INFINITY : 1 + entryVal);
            routingTable.get(entry.ipAddress).nextHop = neighborIp;
            routingTable.get(entry.ipAddress).subnetMask = entry.subnetMask;
        } else {
            return false; // if none of the above conditions hit, we didn't update anything
        }
        return true;
    }

    /**
     * Mulicasts the given byte over the network
     * <p>
     * Note: I am not closing the socket since it is intended to be used often.
     *
     * @param buffer packet to be sent
     */
    private void multicast(byte[] buffer) throws IOException {
        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, group, multicastPort);
        socket.send(packet);
    }

    /**
     * Ping Google's DNS server in order to get your own IP address on the correct interface
     *
     * @return this machine's IP on the outgoing interface
     */
    private InetAddress getMyInetAddress() throws IOException {

        DatagramSocket tempSocket = new DatagramSocket();
        tempSocket.connect(InetAddress.getByName("8.8.8.8"), 20800);
        return tempSocket.getLocalAddress();
    }


    /**
     * Returns a neat representation of the routing table
     *
     * @return a neat representation of the routing table
     */
    private String getStringRoutingTable() {
        StringBuilder res = new StringBuilder("IP Address\tNextHop\t\tMetric\n");
        for (RoutingTableEntry entry : routingTable.values()) {
            res.append(entry.toString() + " \n");
        }
        return res.toString();
    }


    /**
     * Driver function for the Rover class
     *
     * @param args the arguments passed to the Rover
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ArgumentParser argsParser = new ArgumentParser(args);
        if (argsParser.success) {
            new Rover(argsParser.roverId, argsParser.multicastPort, argsParser.multicastAddress);
        }
    }
}
