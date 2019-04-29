import java.net.InetAddress;

/**
 * Packet class used in JCP
 */
public class JPacket {
    InetAddress destAddress, sourceAddress;
    int seqNumber, ackNumber;
    int totalSize;
    byte flags;
    byte[] payload;

    /**
     * Constructs a JPacket with the given values
     * @param destAddress the destination of the JPacket
     * @param sourceAddress the source of the JPacket
     * @param seqNumber the sequence number of the JPacket
     * @param ackNumber the acknowledgement number of the JPacket
     * @param flags the flags of the JPacket
     * @param payload the payload being carried by the JPacket
     * @param totalSize the total size of the file to be transferred
     */
    JPacket(InetAddress destAddress, InetAddress sourceAddress,
            int seqNumber, int ackNumber, byte flags, byte[] payload, int totalSize) {
        this.flags = flags;
        this.destAddress = destAddress;
        this.sourceAddress = sourceAddress;
        this.seqNumber = seqNumber;
        this.ackNumber = ackNumber;
        this.payload = payload;
        this.totalSize = totalSize;
    }

    /**
     * Constructs a JPacket with default values. Should be used for constructing a JPacket and initializing it later.
     */
    JPacket(){

    }

    @Override
    /**
     * Returns a string representation of the JPacket
     */
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("Flags : ").append(BitUtils.byteBitRepresentation(flags)).append("\n");
        if(JPacketUtil.isBitSet(flags, JPacketUtil.SYN_INDEX)) {
            res.append("Length of total payload ").append(JPacketUtil.isBitSet(flags, JPacketUtil.SYN_INDEX) ? totalSize : "").append("\n");
        }


        res.append("Destination Address : ").append(destAddress).append("\n");
        res.append("Source Address : ").append(sourceAddress).append("\n");

        res.append(JPacketUtil.isBitSet(flags, JPacketUtil.NORMAL_INDEX)?"Sequence Number : " + seqNumber + "\n":"");

        res.append(JPacketUtil.isBitSet(flags, JPacketUtil.ACK_INDEX)?"Acknowledgment Number : " + ackNumber + "\n" :"");

        if(payload != null) {
            res.append("Payload size ").append(payload.length).append("\n");
//            res.append("Payload is ").append(payload.length == 0 ? "empty" : "\n" + BitUtils.getHexDump(payload)).append("\n");
        }
        return res.toString();
    }
}
