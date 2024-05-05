package Service;

import WBSYS.WhiteBoard;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import static WBSYS.parameters.isValidPort;


public class WBServer {
    private static final String DEFAULT_WHITEBOARD_NAME = "unnamed whiteboard";
    private static final Logger logger = Logger.getLogger(WBServer.class.getName());
    private static String port;
    private final WhiteBoard wb;
    private Server server;

    WBServer(WhiteBoard wb) {
        this.wb = wb;
    }

    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4) {

            if (!isValidPort(args[1])) {
                System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
                System.out.println("valid port range : 1024-65535");
            } else {
                port = args[1];
                String name = (args.length == 4) ? args[2] : DEFAULT_WHITEBOARD_NAME;
                String IpAddress = args[0];


                try {
                    InetAddress inetAddress = InetAddress.getByName(IpAddress);
                    String Ip = inetAddress.getHostAddress();
                    WhiteBoard wb = new WhiteBoard();
                    final WBServer server = new WBServer(wb);

                    // 开启新线程启动服务器
                    new Thread(() -> {
                        try {
                            server.start();
                            server.blockUntilShutdown();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();

                    Thread.sleep(1000);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(Ip, Integer.parseInt(port)).usePlaintext().build();
                    wb.registerManager(Ip, port, name, channel);
                    //public ManagerGUI(WhiteBoard whiteBoard, String IpAddress, String port, String WBName)
//                    new ManagerGUI(wb, Ip, port, name);
                    logger.info("Manager GUI is created, welcome manager.");
                } catch (IOException e) {
                    logger.severe("IOException: Server failed to start");
                } catch (InterruptedException e) {
                    logger.severe("InterruptedException: Server failed to block until shutdown");
                }
            }
        } else {
            System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
        }
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(Integer.parseInt(port)).addService(new WhiteBoardServiceImpl(wb, logger)).build().start();
        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                WBServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
