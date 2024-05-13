package Service;

import GUI.WhiteBoard;
import WBSYS.Properties;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import io.grpc.*;
import whiteboard.WhiteBoardSecuredServiceGrpc;
import whiteboard.WhiteBoardServiceGrpc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static WBSYS.Properties.isValidPort;


public class WBServer {
    private static final String DEFAULT_WHITEBOARD_NAME = "unnamed whiteboard";
    private static final Logger logger = Logger.getLogger(WBServer.class.getName());
    private static String port;
    private static int command_issue_rate;
    private static boolean showAll;
    private final WhiteBoard wb;
    private Server server;

    WBServer(WhiteBoard wb) {
        this.wb = wb;
    }

    public static void main(String[] args) {
        if (args.length >= 2) {

            if (!isValidPort(args[1])) {
                showUsage(2);
            } else {
                port = args[1];
                String IpAddress = args[0];
//                String name = args.length > 2 && !args[2].startsWith("-") ? args[2] : DEFAULT_WHITEBOARD_NAME;
                String name;int freeargStartIndex = 2;
                if(args.length > 2 && !args[2].startsWith("-")) {
                    name = args[2];
                    freeargStartIndex = 3;
                } else {
                    name = DEFAULT_WHITEBOARD_NAME;
                }
                System.out.println("Server init info: " + IpAddress + " " + port + " " + name);
                Map<String, String> extraParams = parseArguments(args, freeargStartIndex);
                command_issue_rate = Integer.parseInt(extraParams.getOrDefault("RCMD", "2000"));  // 从额外参数中获取命令发行率，或使用默认值
                showAll = Boolean.parseBoolean(extraParams.getOrDefault("SHOWALL", "false"));  // 从额外参数中获取是否显示所有请求，或使用默认值
                System.out.println("Server init RCMD: " + command_issue_rate);
                System.out.println("Server init showAll: " + showAll);
                int DDL = Integer.parseInt(extraParams.getOrDefault("DDL", "3"));  // 从额外参数中获取DDL，或使用默认值
                System.out.println("Server init DDL: " + DDL);
                try {
                    InetAddress inetAddress = InetAddress.getByName(IpAddress);
                    String Ip = inetAddress.getHostAddress();
                    WhiteBoard wb = new WhiteBoard();
                    wb.DDL = DDL;
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


                    logger.info("IP address set for sv: " + Ip);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(Ip, Integer.parseInt(port)).usePlaintext().build();
                    wb.registerManager(Ip, port, name, channel);
                    //public ManagerGUI(WhiteBoard whiteBoard, String IpAddress, String port, String WBName)
//                    new ManagerGUI(wb, Ip, port, name);
                    wb.setServerStub(WhiteBoardServiceGrpc.newStub(channel));
                    wb.setManagerSecuredStub(WhiteBoardSecuredServiceGrpc.newStub(channel));
                    wb.pushMessage(Properties.managerMessage("White Board Setup, Server IP: " + Ip + " Port: " + port + " Board Name: " + name));
                    logger.info("Manager GUI initialized, welcome message sent");
                } catch (IOException e) {
                    logger.severe("IOException: Server failed to start");
                }
            }
        } else {
            showUsage(1);
        }
    }

    private static Map<String, String> parseArguments(String[] args, int startIndex) {
        Map<String, String> params = new HashMap<>();
        for (int i = startIndex; i < args.length; i += 2) {
            if (i + 1 < args.length && args[i].startsWith("-")) {
                params.put(args[i].substring(1), args[i + 1]);
            }
        }
        return params;
    }

    private static void showUsage(int i) {
        switch (i) {
            case 1:
                System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
                break;
            case 2:
                System.out.println("Expected args : <serverIPAddress> <serverPort> boardname");
                System.out.println("valid port range : 1024-65535");
                break;
        }
    }

    public void start() throws IOException {
        initServerInvokingClientStubFlowQpsRule();
        server = ServerBuilder.forPort(Integer.parseInt(port)).
                addService(new WhiteBoardServiceImpl(wb, logger)).
                addService(new WhiteBoardClientImpl(wb, logger)).
                addService(ServerInterceptors.intercept(
                        new WhiteBoardSecuredServiceImpl(wb, logger),
                        new JwtServerInterceptor())).
                keepAliveTime(15, TimeUnit.MINUTES).
                permitKeepAliveWithoutCalls(true).
                maxConnectionAgeGrace(3, TimeUnit.SECONDS). // 允许3s的宽限期完成正在进行的RPC
        build().start();
        logger.info("grpc Server started, listening on " + port);

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
            wb.managerClose();
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    static void initServerInvokingClientStubFlowQpsRule() {
        ArrayList<FlowRule> rules = new ArrayList<>();
        FlowRule rule1 = new FlowRule();
//        rule1.setWarmUpPeriodSec(0);
        rule1.setResource("sbroadCastShape");  // 资源名，需要与 `SphU.entry` 中使用的资源名一致
        rule1.setCount(command_issue_rate);                // 平均每秒最多允许调用次数
        rule1.setGrade(RuleConstant.FLOW_GRADE_QPS); // 基于 QPS 的控制
        rule1.setControlBehavior(showAll ? RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER : RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
//        rule1.setMaxQueueingTimeMs(2500); // 排队等待时间
        rules.add(rule1);
        FlowRuleManager.loadRules(rules);
    }
}
