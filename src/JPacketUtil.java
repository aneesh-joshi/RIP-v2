import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Utility class to encode and decode JPackets used in JCP (Joshi Control Protocol)
 */
public class JPacketUtil {

    final static int ACK_INDEX = 0,
            SYN_INDEX = 1,
            NORMAL_INDEX = 2;

    /**
     * @param srcAddress the source of the JPacket
     * @param seqNumber  the sequence number of the JPacket
     * @param ackNumber  the acknowledgement number of the JPacket
     * @param flags      the flags of the JPacket
     * @param payload    the payload being carried by the JPacket
     * @param totalSize  the total size of the file to be transferred
     * @return the byte array packet representation of the JPacket
     */
    static byte[] jPacket2Arr(InetAddress destAddress, InetAddress srcAddress, int seqNumber, int ackNumber, byte flags,
                              byte[] payload, int totalSize) {
        return jPacket2Arr(new JPacket(destAddress, srcAddress, seqNumber, ackNumber, flags, payload, totalSize));
    }


    /**
     * Converts the given JPacket into a byte array.
     *
     * @return JPacket byte array
     */
    static byte[] jPacket2Arr(JPacket jPacket) {
        int packetSize = 1 + 3 + 3 + // flags + srcIP + dest IP
                (isBitSet(jPacket.flags, SYN_INDEX) ? 4 : 0) + // Total payload size in bytes
                (isBitSet(jPacket.flags, ACK_INDEX) ? 4 : 0) +
                // If it's not a SYN or an ACK, it's a normal transfer packet
                (isBitSet(jPacket.flags, NORMAL_INDEX) ? 4 : 0) +
                (isBitSet(jPacket.flags, ACK_INDEX) ? 0 : jPacket.payload.length); // No payload from an ACK

        byte[] packet = new byte[packetSize];

        int index = 0;
        packet[index] = jPacket.flags;
        index += 1;

        // If it's a SYN, add totalSize in the packet
        if (isBitSet(jPacket.flags, SYN_INDEX)) {
            for (byte b : ByteBuffer.allocate(4).putInt(jPacket.totalSize).array()) {
                packet[index++] = b;
            }
        }

        byte[] destAddress = jPacket.destAddress.getAddress(),
                srcAddress = jPacket.sourceAddress.getAddress();

        // Put destination address
        packet[index++] = destAddress[1];
        packet[index++] = destAddress[2];
        packet[index++] = destAddress[3];

        // Put source address
        packet[index++] = srcAddress[1];
        packet[index++] = srcAddress[2];
        packet[index++] = srcAddress[3];


        // It's a normal packet, with only a payload
        if (isBitSet(jPacket.flags, NORMAL_INDEX)) {
            byte[] seqNumber = ByteBuffer.allocate(4).putInt(jPacket.seqNumber).array();

            packet[index++] = seqNumber[0];
            packet[index++] = seqNumber[1];
            packet[index++] = seqNumber[2];
            packet[index++] = seqNumber[3];
        }

        // It's an ACK packet
        if (isBitSet(jPacket.flags, ACK_INDEX)) {
            byte[] ackNumber = ByteBuffer.allocate(4).putInt(jPacket.ackNumber).array();
            packet[index++] = ackNumber[0];
            packet[index++] = ackNumber[1];
            packet[index++] = ackNumber[2];
            packet[index++] = ackNumber[3];
        } else { // If it's not an ACK, it'll definitely have a payload
            for (int payLoadIndex = 0; payLoadIndex < jPacket.payload.length; payLoadIndex++) {
                packet[index++] = jPacket.payload[payLoadIndex];
            }
        }

        return packet;
    }


    /**
     * Converts the given byte array to a JPacket.
     *
     * @return JPacket for the given byte array
     */
    static JPacket arr2JPacket(byte[] packet) throws UnknownHostException {

        JPacket jPacket = new JPacket();

        int index = 0;
        jPacket.flags = packet[index++];

        // Add the length field
        if (isBitSet(jPacket.flags, SYN_INDEX)) {
            byte[] temp = Arrays.copyOfRange(packet, index, index + 4);
            index += 4;
            jPacket.totalSize = ByteBuffer.wrap(temp).getInt();

        }

        // Add destination and source address
        byte[] destAddr = new byte[4], srcAddr = new byte[4];
        destAddr[0] = (byte) 10;
        srcAddr[0] = (byte) 10;

        destAddr[1] = packet[index++];
        destAddr[2] = packet[index++];
        destAddr[3] = packet[index++];

        srcAddr[1] = packet[index++];
        srcAddr[2] = packet[index++];
        srcAddr[3] = packet[index++];

        jPacket.destAddress = InetAddress.getByAddress(destAddr);
        jPacket.sourceAddress = InetAddress.getByAddress(srcAddr);

        if (isBitSet(jPacket.flags, ACK_INDEX)) {
            byte[] ackNumber = Arrays.copyOfRange(packet, index, index + 4);
            index += 4;
            jPacket.ackNumber = ByteBuffer.wrap(ackNumber).getInt();
        }

        if (isBitSet(jPacket.flags, NORMAL_INDEX)) {
            byte[] seqNumber = Arrays.copyOfRange(packet, index, index + 4);
            index += 4;
            jPacket.seqNumber = ByteBuffer.wrap(seqNumber).getInt();
        }

        if (!isBitSet(jPacket.flags, ACK_INDEX)) {
            jPacket.payload = Arrays.copyOfRange(packet, index, packet.length);
        }

        return jPacket;
    }

    /**
     * Returns true if the give bitIndex of the byteToCheck is set, false otherwise
     *
     * @param byteToCheck the byte to check
     * @param bitIndex    the bit index to check
     * @return true if the give bitIndex of the byteToCheck is set, false otherwise
     */
    static boolean isBitSet(byte byteToCheck, int bitIndex) {
        return (byteToCheck & (1 << bitIndex)) != 0;
    }

    /**
     * Driver program which tests the class
     * @param args optional user args
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException {

        // Test 1 : Send an ACK
        JPacket jPacket = new JPacket(InetAddress.getByName("10.7.2.65"), InetAddress.getByName("10.54.63.23"),
                152, 19, BitUtils.setBitInByte((byte) 0, ACK_INDEX), new byte[0], 0);
        System.out.println(jPacket);

        byte[] arr = jPacket2Arr(jPacket);
        System.out.println("Byte representation : ");
        BitUtils.printPacket(arr);

        jPacket = arr2JPacket(arr);

        System.out.println("\nConverted back");
        System.out.println(jPacket);

        System.out.println("=================================");

        // Test 2: SYN packet
        byte[] payload = new byte[]{1, 2, 3, 4, 5, 32};
        checkFlag(payload, SYN_INDEX);

        System.out.println("=================================");

        // Test 3: NORMAL packet
        checkFlag(payload, NORMAL_INDEX);

    }

    /**
     * Utility which checks if the flag is set. (Just for testing, can be ignored)
     * @param payload the payload of the packet
     * @param synIndex the index of the flags where SYN should be set
     * @throws UnknownHostException
     */
    private static void checkFlag(byte[] payload, int synIndex) throws UnknownHostException {
        JPacket jPacket;
        byte[] arr;
        jPacket = new JPacket(InetAddress.getByName("10.7.2.65"), InetAddress.getByName("10.54.63.23"),
                152, 19, BitUtils.setBitInByte((byte) 0, synIndex), payload, payload.length);
        System.out.println(jPacket);

        arr = jPacket2Arr(jPacket);
        System.out.println("Byte representation : ");
        BitUtils.printPacket(arr);

        jPacket = arr2JPacket(arr);

        System.out.println("\nConverted back");
        System.out.println(jPacket);
    }
}