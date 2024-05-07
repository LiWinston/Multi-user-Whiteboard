package Service;

import GUI.WhiteBoard;
import com.google.protobuf.StringValue;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.LoggerFactory;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static WBSYS.parameters.isValidPort;

public class WBClient {
    private static final String DEFAULT_WHITEBOARD_NAME = "unnamed whiteboard";
    private static final Logger logger = Logger.getLogger(WBClient.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WBClient.class);
    private static String destPort;
    private static String selfIp;
    private static int selfPort;
    private WhiteBoard wb;
    Server clientServer;
    private final ManagedChannel channel;
    private WhiteBoardServiceGrpc.WhiteBoardServiceStub stub;


    public WBClient(WhiteBoard wb, String destHost,int destPort) {
        this.wb = wb;
        channel = ManagedChannelBuilder.forAddress(destHost, destPort)
                .keepAliveTime(1, TimeUnit.MINUTES)  // 每分钟发送一次心跳
                .keepAliveTimeout(10, TimeUnit.SECONDS)  // 如果10秒内没有响应，认为连接失败
                .keepAliveWithoutCalls(true)  // 即使没有调用也发送心跳
                .usePlaintext()
                .build();
        stub = WhiteBoardServiceGrpc.newStub(channel);
    }
    public static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();  // Automatically finds a free port
        } catch (IOException e) {
            throw new IOException("Failed to find a free port", e);
        }
    }

    public void start() throws IOException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        selfIp = inetAddress.getHostAddress();
        selfPort = findFreePort();
        if(selfPort == -1) {
            logger.info("Cannot find free port, please check.");
            return;
        }else {
            logger.info("Successfully find free port: " + selfPort);
        }
        clientServer = ServerBuilder.forPort(selfPort).
                addService(new WhiteBoardClientImpl(wb, logger)).
                build().start();
        logger.info("grpc Server started, listening on " + selfPort);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                WBClient.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (clientServer != null) {
            clientServer.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (clientServer != null) {
            clientServer.awaitTermination();
        }
    }

    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4) {
            if (!isValidPort(args[1])) {
                System.out.println("Expected args : <serverIPAddress> <serverPort> username");
                System.out.println("optionally add <board name> to end of args");
                System.out.println("valid port range : 1024-65535");
            } else {
                CountDownLatch clientDownLatch = new CountDownLatch(1);
                destPort = args[1];
                String name = (args.length == 4) ? args[3] : DEFAULT_WHITEBOARD_NAME;
                String destIpAddress = args[0];
                String username = args[2];



                WhiteBoard wb = new WhiteBoard();
                WBClient client = new WBClient(wb, destIpAddress, Integer.parseInt(destPort));
                WhiteBoardServiceGrpc.WhiteBoardServiceStub ServerStub = client.stub;

                com.google.protobuf.StringValue checkPeerNameRequest = StringValue.newBuilder().setValue(username).build();
                StreamObserver<Response> checkPeerNameResponseObserver = new StreamObserver<Response>() {
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
                                        CountDownLatch latch = new CountDownLatch(1);
                                        new Thread(() -> {
                                            try {
                                                client.start();
                                                latch.countDown();
                                                client.blockUntilShutdown();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }).start();
                                        try {
                                            latch.await();
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (ServerStub == null) {
                                            logger.info("Cannot get constructed stub, please check.");
                                            return;
                                        }else {
                                            logger.info("Successfully get stub, channel is connected." + client.channel.toString());
                                            wb.setServerStub(ServerStub);
                                        }
                                        wb.registerPeer(username, selfIp, String.valueOf(selfPort), null);
//
                                        System.out.println("Peer GUI successfully created. ______From WBClient____");
                                    } else {
                                        System.out.println(response.getMessage());
                                        clientDownLatch.countDown();
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
                            ServerStub.getApprove(GetApproveRequest, GetApproveResponseObserver);
                        } else {
                            logger.info(response.getMessage() + ", Please try again.");
                            if (null != wb.getSelfUI()) {
                                wb.getSelfUI().closeWindow();
                            };
                            clientDownLatch.countDown();
//                            exit(0);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
                            // 检查具体的状态码
                            if (statusRuntimeException.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                logger.severe("Timeout, please check your network connection.");
                            }
                        } else {
                            // 可以处理其他类型的错误
                            logger.severe("RPC statusRuntimeException: " + throwable.getCause().getMessage());
                        }
                        System.out.println("Cannot connect to whiteboard, please use valid Ip, port and name");
                        clientDownLatch.countDown();
//                        exit(-100);
                    }

                    @Override
                    public void onCompleted() {

                    }
                };
                try{
                    ServerStub.withDeadlineAfter(5, TimeUnit.SECONDS).checkPeerName(checkPeerNameRequest, checkPeerNameResponseObserver);
                    clientDownLatch.await();
//                    client.channel.awaitTermination(10, TimeUnit.SECONDS);

                } catch (Exception e) {
                    System.out.println("Connection failed, please check your args or network connection.");
                }
            }
        } else {
            System.out.println("Expected args : <serverIPAddress> <serverPort> username");
            System.out.println("optionally add <board name> to end of args");
        }
    }

}
