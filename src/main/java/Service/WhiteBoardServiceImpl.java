package Service;

import WBSYS.WhiteBoard;
import io.grpc.stub.StreamObserver;
import whiteboard.Whiteboard.*;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard._CanvasShape;
import com.google.protobuf.*;

import java.util.logging.Logger;

public class WhiteBoardServiceImpl extends WhiteBoardServiceGrpc.WhiteBoardServiceImplBase {
    public WhiteBoard wb;
    public Logger logger;
    WhiteBoardServiceImpl(WhiteBoard wb) {
        super();
        this.wb = wb;
        logger = Logger.getLogger(WhiteBoardServiceImpl.class.getName());
    }
    WhiteBoardServiceImpl(WhiteBoard wb, Logger logger) {
        super();
        this.wb = wb;
        this.logger = logger;
    }
    @Override
    public synchronized void synchronizeCanvas(_CanvasShape request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // 业务逻辑- 拆分出网络功能
        System.out.println("Received shape: " + request.getType());

        // 假设操作成功完成
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void synchronizeMessage(ChatMessage request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // 处理消息同步
        System.out.println("Received message: " + request.getMessage());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

}
