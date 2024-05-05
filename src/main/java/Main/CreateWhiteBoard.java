package Main;

import GUI.ManagerGUI;
import WBSYS.WhiteBoard;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static WBSYS.parameters.isValidPort;


public class CreateWhiteBoard {
    private static final String DEFAULT_WHITEBOARD_NAME = "whiteboard";

    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4) {

            if (!isValidPort(args[1])) {
                System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
                System.out.println("valid port range : 1024-65535");
            } else {
                String port = args[1];
                String name;
                if (args.length == 4) {
                    name = args[2];
                } else {
                    name = DEFAULT_WHITEBOARD_NAME;
                }
                String IpAddress = args[0];


                try {
                    InetAddress inetAddress = InetAddress.getByName(IpAddress);
                    String Ip = inetAddress.getHostAddress();
                    WhiteBoard wb = new WhiteBoard();
                    wb.registerManager(Ip, port, name);
                    //public ManagerGUI(WhiteBoard whiteBoard, String IpAddress, String port, String WBName)
                    new ManagerGUI(wb, Ip, port, name);
                    System.out.println("Manager GUI is created, welcome manager.");
                } catch (UnknownHostException unknownHostException) {
                    System.out.println("please provide correct ip address");
                    System.exit(0);
                }
            }
        } else {
            System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
        }
    }
}
