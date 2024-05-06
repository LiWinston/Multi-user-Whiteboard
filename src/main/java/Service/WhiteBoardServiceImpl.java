package Service;

import WBSYS.WhiteBoard;
import io.grpc.stub.StreamObserver;
import whiteboard.Whiteboard.*;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard._CanvasShape;

import whiteboard.Whiteboard.ChatMessage;

import java.util.logging.Logger;

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
            responseObserver.onNext(Response.newBuilder().setSuccess(false).build());
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
    public synchronized void synchronizeCanvas(_CanvasShape request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // 业务逻辑- 拆分出网络功能
        logger.severe("Received shape: " + request.getType());

        // 假设操作成功完成
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


}
