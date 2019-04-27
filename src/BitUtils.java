class BitUtils {
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
        System.out.println();
    }

    /**
     * Prints the hex dump of the array
     * @param arr the byte array
     */
    static String getHexDump(byte[] arr) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < arr.length; i += 1) {
            res.append(String.format("%02x", (byte) arr[i])).append((i + 1) % 4 == 0 ? "\n" : "  -- ");
        }
        res.append("\n");
        return res.toString();
    }

    static byte setBitInByte(byte b, int index){
        return (byte) (b | 1 << index);
    }
}
