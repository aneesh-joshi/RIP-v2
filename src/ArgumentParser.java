import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class to parse all the arguments
 */

class ArgumentParser {
    // Set the default values in case it is not specified by the user
    InetAddress multicastAddress = InetAddress.getByName("233.0.0.0");
    int multicastPort = 5200;
    byte roverId = 10;
    boolean success=false;
    String fileToSend;

    /**
     * Constructs the argument parser object using the arguments which are
     * passed
     *
     * @param args passed string arguments
     */
    ArgumentParser(String args[]) throws UnknownHostException {

        if(args.length == 0 || args.length == 1 || args[0].equals("-h") || args[0].equals("--help")){
            displayHelp();
            return;
        }

        if(args.length % 2 != 0){
            throw new IllegalArgumentException("You've probably thrown an Illegal exception. " +
                    "Please run `java Rover --help` for the correct options");
        }

        int index = 0;
        while(index < args.length){
            if(args[index].contains("-")){
                switch(args[index]){
                    case "-p":
                    case "--port":
                        multicastPort = Integer.parseInt(args[index + 1]);
                        index += 2;
                        break;
                    case "-m":
                    case "--multicastIp":
                        multicastAddress = InetAddress.getByName(args[index + 1]);
                        index += 2;
                        break;
                    case "-i":
                    case "--id":
                        roverId = Byte.parseByte(args[index + 1]);
                        index += 2;
                        break;
                    case "-f":
                    case "--file":
                        fileToSend = args[index + 1];
                        index += 2;
                        break;
                    default:
                            throw new IllegalArgumentException("You've probably provided an Illegal exception. " +
                                    "Please run `java Rover --help` for the correct options");
                }
            }
            success = true;
        }
    }

    /**
     * Displays the help in case an incorrect argument list is passed
     */
    void displayHelp(){
        System.out.println("DESCRIPTION :\n Initializes a Rover on a given multicast ip, port and with a given id\n" +
                "USAGE :\n " +
                "- java Rover [-h | --help]\n"+
                "- java Rover [-p | --port] 520 [-m | --multicastIp] 233.0.0.0  [-i | --id] 10" +
                " [-f | --file] fileToSend\n" +
                "\nEXAMPLE:\n" +
                "java Rover --port 520 --multicastIp 233.0.0.0 --id 10 --file path/to/file");
    }
}