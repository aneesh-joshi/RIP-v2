import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Utility class to encode and decode JPackets used in JCP (Joshi Control Protocol)
 */
public class JPacketUtil {

    private final static int ACK_INDEX = 0;

    /**
     * Converts the given JPacket into a byte array.
     *
     * @return JPacket byte array
     */
    static byte[] jpacket2Arr(JPacket jPacket){
        int packetSize = 1 + 3 + 3 + 4 + (isBitSet(jPacket.flags, ACK_INDEX)?4:0) + jPacket.payload.length;
        byte[] packet = new byte[packetSize];
        int index = 0;
        byte[] destAddress = jPacket.destAddress.getAddress(),
                srcAddress = jPacket.sourceAddress.getAddress();
        byte[] seqNumber = ByteBuffer.allocate(4).putInt(jPacket.seqNumber).array();


        packet[index] = jPacket.flags; index += 1;


        packet[index] = destAddress[1]; index += 1;
        packet[index] = destAddress[2]; index += 1;
        packet[index] = destAddress[3]; index += 1;

        packet[index] = srcAddress[1]; index += 1;
        packet[index] = srcAddress[2]; index += 1;
        packet[index] = srcAddress[3]; index += 1;


        System.out.println(Byte.toUnsignedInt(seqNumber[3]) + " <<<<<<<< " + seqNumber[2] + " <<<<<<<< " + seqNumber[1] + " <<<<<<<< " + seqNumber[0] + " <<<<<<<< ");

        packet[index] = seqNumber[0]; index += 1;
        packet[index] = seqNumber[1]; index += 1;
        packet[index] = seqNumber[2]; index += 1;
        packet[index] = seqNumber[3]; index += 1;

        if(isBitSet(jPacket.flags, ACK_INDEX)){
            byte[] ackNumber = ByteBuffer.allocate(4).putInt(jPacket.ackNumber).array();
            packet[index] = ackNumber[0]; index += 1;
            packet[index] = ackNumber[1]; index += 1;
            packet[index] = ackNumber[2]; index += 1;
            packet[index] = ackNumber[3]; index += 1;
        }

        for(int payLoadIndex = 0; payLoadIndex < jPacket.payload.length; payLoadIndex++){
            packet[index++] = jPacket.payload[payLoadIndex];
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
        jPacket.flags = packet[index]; index += 1;

        byte[] destAddr = new byte[4], srcAddr = new byte[4];
        destAddr[0] = (byte)10;
        srcAddr[0] = (byte)10;

        destAddr[1] = packet[index++];
        destAddr[2] = packet[index++];
        destAddr[3] = packet[index++];

        srcAddr[1] = packet[index++];
        srcAddr[2] = packet[index++];
        srcAddr[3] = packet[index++];

        jPacket.destAddress = InetAddress.getByAddress(destAddr);
        jPacket.sourceAddress = InetAddress.getByAddress(srcAddr);

        byte[] seqNumber = Arrays.copyOfRange(packet, index, index + 4);
        index += 4;

        jPacket.seqNumber =  ByteBuffer.wrap(seqNumber).getInt();

        if(isBitSet(jPacket.flags, ACK_INDEX)){
            byte[] ackNumber = Arrays.copyOfRange(packet, index, index + 4);
            index += 4;
            jPacket.ackNumber =  ByteBuffer.wrap(ackNumber).getInt();
        }

        jPacket.payload = Arrays.copyOfRange(packet, index, packet.length);

        return jPacket;
    }

    /**
     * Returns true if the give bitIndex of the byteToCheck is set, false otherwise
     * @param byteToCheck the byte to check
     * @param bitIndex the bit index to check
     * @return true if the give bitIndex of the byteToCheck is set, false otherwise
     */
    static boolean isBitSet(byte byteToCheck, int bitIndex){
        return (byteToCheck & (1 << bitIndex)) == 1;
    }

    public static void main(String[] args) throws UnknownHostException {
        byte[] payload = new byte[]{1,2,3,4,5,32};
        JPacket jPacket = new JPacket(InetAddress.getByName("10.7.2.65"), InetAddress.getByName("10.54.63.23"),
                152, 19, (byte)(1<<0), payload);

        System.out.println(jPacket);

        byte[] packet = jpacket2Arr(jPacket);
        BitUtils.printPacket(packet);

        System.out.println(arr2JPacket(packet));
    }
}