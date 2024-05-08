package Service;

import GUI.WhiteBoard;
import WBSYS.CanvasShape;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard;
import whiteboard.Whiteboard.ChatMessage;
import whiteboard.Whiteboard.Response;
import whiteboard.Whiteboard._CanvasShape;

import java.awt.*;
import java.util.logging.Logger;

import static Service.Utils.protoPointsToArrayList;

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
    public void getApprove(com.google.protobuf.StringValue request,StreamObserver<Response> responseObserver) {
        logger.severe("Received getApprove request: " + request.getValue());
        if(wb.getApproveFromUI(request.getValue())) {
            responseObserver.onNext(Response.newBuilder().setSuccess(true).setMessage("Welcome " + request.getValue()).build());
//            chatStreamObservers.put(request.getValue(),
        } else {
            responseObserver.onNext(Response.newBuilder().setSuccess(false).setMessage("You have been rejected by manager").build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void registerPeer(Whiteboard.IP_Port ip_port,
                             io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver){
        logger.severe("Received registerPeer request: " + ip_port.getIp() + " " + ip_port.getPort());
        ManagedChannel channel = ManagedChannelBuilder.forAddress(ip_port.getIp(), Integer.parseInt(ip_port.getPort())).usePlaintext().build();
        wb.registerPeer(ip_port.getUsername(), ip_port.getIp(), ip_port.getPort(), channel);
        System.out.println("registerPeer generated channel" + channel);
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void synchronizeUser(whiteboard.Whiteboard.SynchronizeUserRequest request,
                                io.grpc.stub.StreamObserver<whiteboard.Whiteboard.UserList> responseObserver) {
        logger.severe("Received synchronizeUser request: " + request.getOperation() + " " + request.getUsername());
        if (request.getOperation().equals("add")) {
            wb.addUser(request.getUsername());
        } else if (request.getOperation().equals("remove")) {
            wb.removeUser(request.getUsername());
        }
        responseObserver.onNext(whiteboard.Whiteboard.UserList.newBuilder().addAllUsernames(wb.userAgents.keySet()).build());
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void synchronizeCanvas(_CanvasShape requestShape, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // 业务逻辑- 拆分出网络功能
        logger.severe("Received shape: " + requestShape.getShapeString());
        CanvasShape shape =
                requestShape.getShapeString() == "text" ?
                        new CanvasShape(requestShape.getShapeString(), new Color(Integer.parseInt(requestShape.getColor())),requestShape.getUsername(),protoPointsToArrayList(requestShape.getPointsList().stream().toList()), requestShape.getStrokeInt()) :
                new CanvasShape(requestShape.getShapeString(), new Color(Integer.parseInt(requestShape.getColor())), requestShape.getX(0), requestShape.getX(1), requestShape.getX(2), requestShape.getX(3), requestShape.getStrokeInt());
        wb.SynchronizeCanvas(shape);
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void synchronizeMessage(ChatMessage request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // 处理消息同步
        logger.severe("Received message: " + request.getMessage());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

//    @Override
//    public void registerPeer(AttributeContext.Peer request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
//        // 处理注册
//        System.out.println("Received registration request from: " + request.getName());
//        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
//        responseObserver.onCompleted();
//    }

    public void synchronizeEditing(whiteboard.Whiteboard.SynchronizeUserRequest request,
                                   io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received synchronize editing request: " + request.getOperation() +" "+ request.getUsername());
        wb.SynchronizeEditing(request.getOperation(), request.getUsername());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
