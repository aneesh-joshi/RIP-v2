import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class which will be run per rover
 */
public class Rover {
    int id;
    private DatagramSocket socket;
    private InetAddress group;
    private byte[] buffer;

    private final static Logger LOGGER = Logger.getLogger("ROVER");
    private final static int LISTEN_WINDOW = 1024;
    private final static String MULTICAST_ADDRESS = "230.0.0.0";
    private final static int MULTICAST_PORT = 4446;

    /**
     * Constructs a rover with the given id with an IP ending in that id
     *
     * @param id
     */
    Rover(int id) {
        this.id = id;
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

    void startRIP() {

    }

    void advertiseEntry() {
        try {
            multicast("Hey all, this is Rover " + id + " signing in! My IP is " + InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            System.err.println(e);
        }
    }

    void listenMulticast(String multicastAddress, int port) {
        try {
            MulticastSocket socket = new MulticastSocket(port);
            InetAddress group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group);
            byte[] buf = new byte[LISTEN_WINDOW];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                System.out.println("Got message " + received);
                if ("end".equals(received)) {
                    break;
                }
            }
            socket.leaveGroup(group);
            socket.close();
        } catch (SocketException e) {
            System.err.println(e);
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    void multicast(String message) { // TODO obfs

        try {
            socket = new DatagramSocket();
            group = InetAddress.getByName("230.0.0.0");
            buffer = message.getBytes();

            DatagramPacket packet
                    = new DatagramPacket(buffer, buffer.length, group, 4446);
            socket.send(packet);

            LOGGER.info("Message sent");
            socket.close();
        } catch (SocketException e) {
            System.err.println(e);
        } catch (UnknownHostException e) {
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
