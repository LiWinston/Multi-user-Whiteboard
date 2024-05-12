package Service;

import GUI.WhiteBoard;
import WBSYS.CanvasShape;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard;
import whiteboard.Whiteboard.Response;

import java.awt.*;
import java.util.logging.Logger;

import static Service.Utils.protoShape2Shape;

public class WhiteBoardServiceImpl extends WhiteBoardServiceGrpc.WhiteBoardServiceImplBase {
    public WhiteBoard wb;
    public Logger logger;

    WhiteBoardServiceImpl(WhiteBoard wb, Logger logger) {
        super();
        this.wb = wb;
        this.logger = logger;
    }

    @Override
    public void checkPeerName(com.google.protobuf.StringValue request, StreamObserver<Response> responseObserver) {
        logger.severe("Received checkPeerName request: " + request.getValue());
        if (wb.userAgents.containsKey(request.getValue())) {
            responseObserver.onNext(Response.newBuilder().setSuccess(false).setMessage("Username Already exists").build());
        } else {
            responseObserver.onNext(Response.newBuilder().setSuccess(true).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getApprove(com.google.protobuf.StringValue request, StreamObserver<Response> responseObserver) {
        logger.severe("Received getApprove request: " + request.getValue());
        if (wb.getApproveFromUI(request.getValue())) {
            responseObserver.onNext(Response.newBuilder().setSuccess(true).setMessage("Welcome " + request.getValue()).build());
//            chatStreamObservers.put(request.getValue(),
        } else {
            responseObserver.onNext(Response.newBuilder().setSuccess(false).setMessage("You have been rejected by manager").build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void registerPeer(Whiteboard.IP_Port ip_port,
                             io.grpc.stub.StreamObserver<Response> responseObserver) {
        logger.severe("Received registerPeer request: " + ip_port.getIp() + " " + ip_port.getPort());

        // 生成channel，有配置文件则使用配置文件
        ManagedChannel channel = null;
        channel = ManagedChannelBuilder.forAddress(ip_port.getIp(), Integer.parseInt(ip_port.getPort()))
                .usePlaintext()
                .build();
//        String configFilePath = "grpc_service_config.json";
//        File configFile = new File(configFilePath);
//        String jsonConfig = null;
//
//        try {
//            if (configFile.exists()) {
//                jsonConfig = new String(Files.readAllBytes(configFile.toPath()));
//            } else {
//                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
//                if (inputStream == null) {
//                    throw new IOException("Configuration file not found in classpath");
//                }
//                byte[] bytes = inputStream.readAllBytes();
//                jsonConfig = new String(bytes);
//            }
//
//            JsonObject configObj = JsonParser.parseString(jsonConfig).getAsJsonObject();
//            Type type = new TypeToken<Map<String, Object>>(){}.getType();
//            Map<String, Object> grpcServiceConfig = new Gson().fromJson(configObj, type);
//            logger.info("Using grpc_service_config.json: " + grpcServiceConfig);
////            channel.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS); // 关闭旧的 channel
//            channel = ManagedChannelBuilder.forAddress(ip_port.getIp(), Integer.parseInt(ip_port.getPort()))
//                    .defaultServiceConfig(grpcServiceConfig)
//                    .usePlaintext()
//                    .build();
//        } catch (Exception e) {
//            logger.severe("Failed to load grpc_service_config.json: " + e.getMessage());
////            channel = ManagedChannelBuilder.forAddress(ip_port.getIp(), Integer.parseInt(ip_port.getPort()))
////                    .usePlaintext()
////                    .build();
//        }


//        String grpc_service_config = null;
//        try {
//            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("grpc_service_config.json");
//            byte[] bytes = inputStream.readAllBytes();
//            grpc_service_config = Arrays.toString(bytes);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        if (grpc_service_config == null || grpc_service_config.isEmpty() || grpc_service_config.isBlank()) {
//            logger.severe("grpc_service_config.json not found");
//            channel = ManagedChannelBuilder.forAddress(ip_port.getIp(), Integer.parseInt(ip_port.getPort())).usePlaintext().build();
//        }else{
//            channel = ManagedChannelBuilder.forAddress(ip_port.getIp(), Integer.parseInt(ip_port.getPort())).
//                    defaultServiceConfig(JsonParser.parseString(grpc_service_config).getAsJsonObject().asMap()).usePlaintext().build();
//        }
        wb.registerPeer(ip_port.getUsername(), ip_port.getIp(), ip_port.getPort(), channel);

        //新用户同步显示“当前编辑中”用户列表
        Context.current().fork().run(() -> {
            for (String editingUser : wb.getEditingUser()) {
                wb.userAgents.get(ip_port.getUsername()).updEditing(
                        Whiteboard.SynchronizeUserRequest.newBuilder().setOperation("add").setUsername(editingUser).build(),
                        new StreamObserver<com.google.protobuf.Empty>() {
                            @Override
                            public void onNext(com.google.protobuf.Empty value) {}
                            @Override
                            public void onError(Throwable t) {logger.severe(t.getMessage());}
                            @Override
                            public void onCompleted() {}
                        });
            }
        });


        System.out.println("registerPeer generated channel" + channel);
//        String jwtToken = Jwts.builder().setSubject(ip_port.getUsername()).signWith(Constants.JWT_KEY).compact();
        String jwtToken = ip_port.getUsername();
        responseObserver.onNext(Response.newBuilder().setSuccess(true).setMessage(jwtToken).build());
        responseObserver.onCompleted();
    }


    @Override
    public void pushMessage(whiteboard.Whiteboard.ChatMessage request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received message: " + request.getMessage());
        wb.broadCastChatMessage(request.getMessage());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }


    @Override
    public void reportUpdEditing(whiteboard.Whiteboard.SynchronizeUserRequest request,
                                 io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received synchronize editing request: " + request.getOperation() + " " + request.getUsername());
        wb.broadCastEditing(request.getOperation(), request.getUsername());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }


    @Override
    public synchronized void pushShape(Whiteboard._CanvasShape requestShape, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // 业务逻辑- 拆分出网络功能
        logger.severe("Received shape: " + requestShape.getShapeString());
        CanvasShape shape = protoShape2Shape(requestShape);

        new CanvasShape(requestShape.getShapeString(), new Color(Integer.parseInt(requestShape.getColor())), requestShape.getX(0), requestShape.getX(1), requestShape.getX(2), requestShape.getX(3), requestShape.getStrokeInt());
        shape.setFill(requestShape.getFill());
        wb.broadCastShape(shape);
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    //客户端流式 没有请求参数 而是返回一个输入流供应用层操作
    @Override
    public io.grpc.stub.StreamObserver<whiteboard.Whiteboard._CanvasShape> sPushShape(
            io.grpc.stub.StreamObserver<whiteboard.Whiteboard.Response> responseObserver) {

        return new io.grpc.stub.StreamObserver<whiteboard.Whiteboard._CanvasShape>() {
            boolean isOk = true;
            @Override
            public void onNext(Whiteboard._CanvasShape _canvasShape) {
                logger.info(" Stream_IN Shape: " + _canvasShape.getShapeString() + "### PREVIEW ###");
                CanvasShape shape = protoShape2Shape(_canvasShape);
                //shape间的交叠冲突检测，委派wb处理
//                if(wb.checkConflictOk(shape)){
                if(true){
                    wb.tempShapes.put(_canvasShape.getUsername(), shape);
                    wb.sbroadCastShape(_canvasShape);
                }else{
                    //中止接收，发送失败消息
                    if(isOk) {
                        responseObserver.onNext(Response.newBuilder().setSuccess(false).setMessage("Conflict with other editing").build());
                    }
                    isOk = false;

                    responseObserver.onError(new Throwable("Conflict"));
                }

            }

            @Override
            public void onError(Throwable throwable) {
                logger.severe("sPushShape中断：" + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Response.newBuilder().setSuccess(isOk).build());
                responseObserver.onCompleted();//没有这句则绘制不落实 待调研onNext里的responseObserver onnext是否会自动调用onCompleted
            }

        };
    }

}
