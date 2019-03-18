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
    private MulticastSocket socket;
    private InetAddress group;
    private byte[] buffer;
    private List<RoutingTableEntry> routingTableEntries;
    private TimerTask timerTask;
    private InetAddress myAddress;

    private final static Logger LOGGER = Logger.getLogger("ROVER");
    private final static int LISTEN_WINDOW = 1024;
    private final static String MULTICAST_ADDRESS = "230.0.0.0";
    private final static int MULTICAST_PORT = 4446;
    private final static int MAX_ROVERS = 12;
    private final static int ROUTE_UPDATE_TIME = 5,
            ROUTE_DELAY_TIME = 2,
            ROVER_OFFLINE_TIME_LIMIT = 5;

    /**
     * Constructs a rover with the given id with an IP ending in that id
     *
     * @param id
     */
    Rover(int id) {
        this.id = id;
        routingTableEntries = new ArrayList<>(); // since we can have a maximum of
        // 10 entries
        try {
            myAddress = InetAddress.getLocalHost();
            System.out.println("This is my address!!!" + myAddress);
            socket = new MulticastSocket(MULTICAST_PORT);
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
        } catch (SocketException | UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
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
                listenMulticast();
            }
        }).start();
    }

    /**
     * Send update packets out
     */
    void sendRIPUpdate() {
        multicast(RIPPacketUtil.getRIPPacket((byte) 1, routingTableEntries)); // TODO check if request or response
    }


    void listenMulticast() {
        try {
            byte[] buf = new byte[LISTEN_WINDOW];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                List<RoutingTableEntry> entries = RIPPacketUtil.decodeRIPPacket(packet.getData(), packet.getLength());
                updateEntries(packet.getAddress(), entries);
            }
//        socket.leaveGroup(group);
//        socket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Updates entries as per the Distance Vector Algorithm when new entries are received
     *
     * @param newEntries The entries received
     */
    void updateEntries(InetAddress sourceAddress, List<RoutingTableEntry> newEntries) {
        if(sourceAddress.equals(myAddress)){
            return;
        }
        if (getEntryForThisIP(sourceAddress) == -1) {
            System.out.println("Adding " + sourceAddress + " to table entries");
            routingTableEntries.add(new RoutingTableEntry(sourceAddress, (byte) 24, sourceAddress, (byte) 1));
        }
        for (RoutingTableEntry entry : newEntries) {
            if (myAddress.equals(entry.ipAddress)) {
                System.out.println("IS EQUAL");
                continue;
            } else {
                System.out.println(sourceAddress.getHostAddress() + " -> " + myAddress.getHostAddress());
            }
            int myEntryIndex = getEntryForThisIP(entry.ipAddress);
            if (myEntryIndex == -1) {
                routingTableEntries.add(new RoutingTableEntry(entry.ipAddress,
                        entry.subnetMask,
                        sourceAddress,
                        (byte) (1 + entry.metric)));
            } else if (routingTableEntries.get(myEntryIndex).metric > 1 + entry.metric) {
                routingTableEntries.get(myEntryIndex).metric = (byte) (1 + entry.metric);
                routingTableEntries.get(myEntryIndex).nextHop = sourceAddress;
                routingTableEntries.get(myEntryIndex).subnetMask = entry.subnetMask;
            }
        }
        System.out.println("Routing Table of  " + myAddress + " : " + routingTableEntries);
    }

    /**
     * Returns the index of the entry which has the given IP
     * <p>
     * Note: would've been better to use a Map but I don't want that complexity right now
     * Possible future TODO use Map
     *
     * @param ipAddress the ipAdress whose index you want
     * @return the index if it exists in the entries, -1 otherwise
     */
    int getEntryForThisIP(InetAddress ipAddress) {
        for (int index = 0; index < routingTableEntries.size(); index += 1) {
            if (routingTableEntries.get(index).ipAddress.equals(ipAddress)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Mulicasts the given byte over the network
     * <p>
     * Note: I am not closing the socket since it is intended to be used often. TODO check
     *
     * @param buffer packet to be sent
     */
    void multicast(byte[] buffer) { // TODO obfs
        try {
            DatagramPacket packet
                    = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            System.out.println("Sending this : " + RIPPacketUtil.decodeRIPPacket(buffer, packet.getLength()));
            socket.send(packet);

            LOGGER.info("Message sent");
        } catch (SocketException | UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    public static void main(String[] args) {
        new Rover(Integer.parseInt(args[0]));
//        Rover rover = new Rover(12);
    }

}
