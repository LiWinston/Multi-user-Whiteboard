package Main;

import GUI.PeerGUI;
import WBSYS.WhiteBoard;

import static WBSYS.parameters.isValidPort;

public class JoinWhiteBoard {
    private static final String DEFAULT_WHITEBOARD_NAME = "unnamed whiteboard";

    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4) {
            if (!isValidPort(args[1])) {
                System.out.println("Expected args : <serverIPAddress> <serverPort> username");
                System.out.println("optionally add <board name> to end of args");
                System.out.println("valid port range : 1024-65535");
            } else {
                String port = args[1];
                String name;
                if (args.length == 4) {
                    name = args[3];
                } else {
                    name = DEFAULT_WHITEBOARD_NAME;
                }

                String IpAddress = args[0];
                String address = "rmi://" + IpAddress + ":" + port + "/" + name;
                String username = args[2];


                WhiteBoard wb = new WhiteBoard();

//                    System.out.println("Successfully looked up to server.");
//                    if (WBServer.checkPeerName(username)) {
//                        if (WBServer.getApprove(username)) {
//                            System.out.println("Welcome " + username);
//                            WBServer.registerPeer(username);
//                            String wellcomeMessage =
//                                    chatMessageFormat(username,
//                                            WELLCOME_MESSAGE + " " + username);
//                            WBServer.SynchronizeMessage(wellcomeMessage);
//                            System.out.println("Peer GUI successfully created.");
//                        } else {
//                            System.out.println("you have been rejected by manager");
//                        }
//                    } else {
//                        System.out.println("This Username has been used, try again.");
//                    }


//                System.out.println("successfully looked up to - " + address);
//                System.out.println("trying to get approve from Manager...");
            }
        } else {
            System.out.println("Expected args : <serverIPAddress> <serverPort> username");
            System.out.println("optionally add <board name> to end of args");
        }
    }
}
