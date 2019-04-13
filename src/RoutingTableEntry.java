import java.net.InetAddress;

/**
 * Class to keep track of routing table entries.
 * Should be passed to a RIPPacketUtil class which will convert it to a byte format
 */
public class RoutingTableEntry{
    InetAddress ipAddress;
    byte subnetMask; // I chose to keep it as byte since it's easier to form the packet and all valid
                     // entries will be in the range. Same for metric.
    InetAddress nextHop;
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
    RoutingTableEntry(InetAddress ipAddress, byte subnetMask, InetAddress nextHop, byte metric) {
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        this.nextHop = nextHop;
        this.metric = metric;
    }

    /**
     * Empty constructor which allows manual setting of member variables.
     */
    RoutingTableEntry(){

    }

    /**
     * Returns a string representation of the routing table entry
     * @return string representation of the routing table entry
     */
    @Override
    public String toString(){

        StringBuilder res = new StringBuilder();
        res.append(ipAddress.toString().substring(1)).append("/").append(subnetMask).append(" \t");
        res.append(nextHop.toString().substring(1) + " \t");
        res.append(metric + " \t");


        return res.toString();
    }

    /**
     * Checks whether 2 entries are the same.
     * @param otherObject the object to check equality with
     * @return true if the other object is equal to this one, false otherwise
     */
    @Override
    public boolean equals(Object otherObject) {
        if(!(otherObject instanceof RoutingTableEntry)){
            return false;
        }
        RoutingTableEntry other = (RoutingTableEntry)otherObject;
        return this.metric == other.metric && this.nextHop.equals(other.nextHop) &&
                this.ipAddress.equals(other.ipAddress) && this.subnetMask == other.subnetMask;
    }

    /**
     * Returns the same hashcode for "equal" objects
     * @return this object's hashcode
     */
    @Override
    public int hashCode(){
        return subnetMask * 100 + (int)Math.pow(metric, 3) + ipAddress.hashCode()*nextHop.hashCode();
    }
}
