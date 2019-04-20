public class BitUtils {
    static String byteBitRepresentation(byte byteToRepresent){
        StringBuilder res = new StringBuilder();
        int bitVector = 1;
        while(bitVector < 1<<8){
            res.append((byteToRepresent & bitVector) != 0 ? "1" : "0");
            bitVector = bitVector << 1;
        }
        return res.reverse().toString();
    }

    /**
     * Prints the hex dump of the packet
     * @param ripByteRepresentation the byte representation of the rip packet
     */
    static void printPacket(byte[] ripByteRepresentation) {
        for (int i = 0; i < ripByteRepresentation.length; i += 1) {
            System.out.printf("%02x" + ((i + 1) % 4 == 0 ? "\n" : "  -- "), (byte)ripByteRepresentation[i]);
        }
    }
}
