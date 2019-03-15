/**
 * Class to keep track of routing table entries.
 * Should be passed to a RIPPacket class which will convert it to a byte format
 */
public class RoutingTableEntry {
    String ipAddress;
    byte subnetMask; // I chose to keep it as byte since it's easier to form the packet and all valid
                     // entries will be in the range. Same for metric.
    String nextHop;
    byte metric;

    /**
     * Constructs a Routing Table entry
     *
     * @param ipAddress  The destination IP Address
     * @param subnetMask Subnet mask for the for `ipAddress`
     * @param nextHop    Immediate next hop IP address to which packets to
     *                   the destination specified by this route entry should be forwarded
     * @param metric     total cost of getting the datagram from host to destination
     */
    RoutingTableEntry(String ipAddress, byte subnetMask, String nextHop, byte metric) {
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        this.nextHop = nextHop;
        this.metric = metric;
    }

    RoutingTableEntry(){

    }

    @Override
    public String toString(){
        StringBuilder res = new StringBuilder();
        res.append("\n------------\n");
        res.append("IP Address : " + ipAddress + "\n");
        res.append("Subnet Mask : " + subnetMask + "\n");
        res.append("Next Hop : " + nextHop + "\n");
        res.append("Metric : " + metric + "\n");
        res.append("************\n");

        return res.toString();
    }
}
