package Service;

import WBSYS.WhiteBoard;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static WBSYS.parameters.isValidPort;

public class WBClient {
    private static final String DEFAULT_WHITEBOARD_NAME = "unnamed whiteboard";
    private static final Logger logger = Logger.getLogger(WBClient.class.getName());
    private static String port;
    private WhiteBoard wb;
    private final ManagedChannel channel;
    private WhiteBoardServiceGrpc.WhiteBoardServiceStub stub;

    public WBClient(WhiteBoard wb, String host,int port) {
        this.wb = wb;
        channel = ManagedChannelBuilder.forAddress(host,port)
                .usePlaintext()
                .build();
        stub = WhiteBoardServiceGrpc.newStub(channel);
    }



    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4) {
            if (!isValidPort(args[1])) {
                System.out.println("Expected args : <serverIPAddress> <serverPort> username");
                System.out.println("optionally add <board name> to end of args");
                System.out.println("valid port range : 1024-65535");
            } else {
                port = args[1];
                String name = (args.length == 4) ? args[3] : DEFAULT_WHITEBOARD_NAME;
                String IpAddress = args[0];
                String username = args[2];


                WhiteBoard wb = new WhiteBoard();
                WBClient client = new WBClient(wb, IpAddress, Integer.parseInt(port));
                WhiteBoardServiceGrpc.WhiteBoardServiceStub stub = client.stub;
                if (stub == null) {
                    logger.info("Cannot get constructed stub, please check.");
                    return;
                }else {
                    logger.info("Successfully get stub, channel is connected." + client.channel.toString());
                }
                com.google.protobuf.StringValue request = StringValue.newBuilder().setValue(username).build();
                StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                    @Override
                    public void onNext(Response response) {
                        if (response.getSuccess()) {
                            logger.info("trying to get approve from Manager...");

                            // 另一个请求
                            StringValue GetApproveRequest = StringValue.newBuilder().setValue(username).build();

                            // 另一个StreamObserver处理新的RPC调用的响应
                            StreamObserver<Response> GetApproveResponseObserver = new StreamObserver<>() {
                                @Override
                                public void onNext(Response response) {
                                    if (response.getSuccess()) {
                                        System.out.println(response.getMessage());
                                        wb.registerPeer(username, client.channel);
                                        System.out.println("Peer GUI successfully created.");
                                    } else {
                                        System.out.println(response.getMessage());
                                    }

                                }
                                @Override
                                public void onError(Throwable t) {
                                }
                                @Override
                                public void onCompleted() {
                                }
                            };

                            // 进行另一个RPC调用
                            stub.getApprove(GetApproveRequest, GetApproveResponseObserver);
                        } else {
                            logger.info("Existing username, try again.");
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Cannot connect to whiteboard, " +
                                "please use valid Ip, port and name");
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("completed");
                    }
                };
                try{
                    stub.checkPeerName(request, responseObserver);
                    client.channel.awaitTermination(10L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.out.println("Cannot connect to whiteboard, " +
                            "please use valid Ip, port and name");
                }
            }
        } else {
            System.out.println("Expected args : <serverIPAddress> <serverPort> username");
            System.out.println("optionally add <board name> to end of args");
        }
    }

//    private void blockUntilShutdown() throws InterruptedException {
//        if (channel != null) {
//            channel.awaitTermination(50L, TimeUnit. SECONDS);
//        }
//    }
}
