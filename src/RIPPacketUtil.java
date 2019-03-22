import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility for byte encoding and decoding RIP packets.
 */
public class RIPPacketUtil {
    static final byte VERSION = 2; // We will only support version 2

    /**
     * Returns a RIPPacketUtil
     *
     * @param command Either a request(0) or response(1)
     * @param entries an array of routing table entries to be filled
     */
    static byte[] getRIPPacket(byte command, Map<InetAddress, RoutingTableEntry> entries) {

        byte[] ripPacket = new byte[4 * 2 + entries.size() * 16]; //  4 * 2 bytes for the header
        ripPacket[0] = command;
        ripPacket[1] = VERSION;
        // skip 2 to keep empty
        ripPacket[4] = 0; // TODO check
        ripPacket[5] = 2; // 2 for IP
        // keep route tag as empty as we won't support anything but RIP

        int packetOffset = 8;
        for (RoutingTableEntry entry : entries.values()) {
            // Add IP entry
            packetOffset = addIpAddress(ripPacket, packetOffset, entry.ipAddress, entry);
            packetOffset += 3; // skip the first 3 bytes since they will be
            // 0 and the subnet mask will at max be 32

            ripPacket[packetOffset] = entry.subnetMask;
            packetOffset += 1;

            packetOffset = addIpAddress(ripPacket, packetOffset, entry.nextHop, entry);

            packetOffset += 3; // skip the first 3 bytes since they will be
            // 0 and the metric will at max be 15
            ripPacket[packetOffset] = entry.metric;
            packetOffset += 1;
        }
        return ripPacket;
    }

    public static List<RoutingTableEntry> decodeRIPPacket(byte[] packet, int packetLength) throws UnknownHostException{

        int totalEntries = (packetLength - 8) / 16;
        List<RoutingTableEntry> list = new ArrayList<>();
        RoutingTableEntry entry;
        int offset = 8;

        for (int count = 0; count < totalEntries; count++) {
            entry = new RoutingTableEntry();
            entry.ipAddress = getIpFromPacket(packet, offset);
            offset += 4;

            entry.subnetMask = Byte.parseByte(getNextNBytes(packet, offset, 4));
            offset += 4;
            entry.nextHop = getIpFromPacket(packet, offset);
            offset += 4;
            entry.metric = Byte.parseByte(getNextNBytes(packet, offset, 4));
            offset += 4;
            list.add(entry);
        }

        return list;
    }


    private static InetAddress getIpFromPacket(byte[] packet, int offset) throws UnknownHostException{
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            res.append(getNextNBytes(packet, offset + i, 1) + (i == 3 ? "" : "."));
        }
        return InetAddress.getByName(res.toString());
    }

    /**
     * Returns a string representation of the number made on N bytes.
     *
     * @param packet the packet array
     * @param offset the position to start reading from
     * @param N      the number of bytes to consider
     * @return string representation of the number made on N bytes
     */
    private static String getNextNBytes(byte[] packet, int offset, int N) {
        long res = (long) Byte.toUnsignedInt(packet[offset]);
        for (int i = 1; i < N; i++) {
            res = (res << 8) + (long) Byte.toUnsignedInt(packet[offset + i]);
        }
        return "" + res;
    }


    private static int addIpAddress(byte[] ripPacket, int packetOffset, InetAddress ipToAdd, RoutingTableEntry entry) {
        for (byte ipSubPart : ipToAdd.getAddress()) {
            ripPacket[packetOffset] = ipSubPart;
            packetOffset += 1;
        }
        return packetOffset;
    }

    public static void main(String[] args) throws Exception {
        // Test for RIP packet util
        RoutingTableEntry packet = new RoutingTableEntry(InetAddress.getByName("255.255.255.255"),
                (byte) 32, InetAddress.getByName("255.0.255.0"), (byte) 15);

        RoutingTableEntry packet1 = new RoutingTableEntry(InetAddress.getByName("123.221.1.55"),
                (byte) 11, InetAddress.getByName("1.0.1.1"), (byte) 29);

        Map<InetAddress, RoutingTableEntry> routingTableEntries = new HashMap<>();
        routingTableEntries.put(InetAddress.getByName("255.255.255.255"), packet);
        routingTableEntries.put(InetAddress.getByName("123.221.1.55"), packet1);
        byte[] ripByteRepresentation = getRIPPacket((byte) 1, routingTableEntries);

        printPacket(ripByteRepresentation);

        System.out.println(decodeRIPPacket(ripByteRepresentation, ripByteRepresentation.length));
    }

    private static void printPacket(byte[] ripByteRepresentation) {
        for (int i = 0; i < ripByteRepresentation.length; i += 1) {
            System.out.printf("%02x" + ((i + 1) % 4 == 0 ? "\n" : "  -- "), ripByteRepresentation[i]);
        }
    }
}
