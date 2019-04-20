import java.net.InetAddress;

/**
 * Packet class used in JCP
 */
public class JPacket {
    InetAddress destAddress, sourceAddress;
    int seqNumber, ackNumber;
//    int length;
    byte flags;
    byte[] payload;

    public JPacket(InetAddress destAddress, InetAddress sourceAddress,
                   int seqNumber, int ackNumber, byte flags, byte[] payload) {
        this.flags = flags;
        this.destAddress = destAddress;
        this.sourceAddress = sourceAddress;
        this.seqNumber = seqNumber;
        this.ackNumber = ackNumber;
        this.payload = payload;
    }

    JPacket(){

    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("Flags : " + BitUtils.byteBitRepresentation(flags) + "\n");
        res.append("Destination Address : " + destAddress + "\n");
        res.append("Source Address : " + sourceAddress + "\n");
        res.append("Sequence Number : " + seqNumber + "\n");
        res.append("Acknowledgement Number : " + ackNumber + "\n");
        res.append("Payload size " + payload.length + "\n");
        return res.toString();
    }
}
