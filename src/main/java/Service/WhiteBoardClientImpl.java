package Service;

import GUI.IWhiteBoard;
import GUI.WhiteBoard;
import WBSYS.CanvasShape;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.Whiteboard;

import javax.swing.*;
import java.util.ArrayList;
import java.util.logging.Logger;

import static Service.Utils.protoShape2Shape;

public class WhiteBoardClientImpl extends WhiteBoardClientServiceGrpc.WhiteBoardClientServiceImplBase {
    public IWhiteBoard wb;
    public Logger logger;

    WhiteBoardClientImpl(WhiteBoard wb, Logger logger) {
        super();
        this.wb = wb;
        this.logger = logger;
    }

    @Override
    public void updatePeerList(whiteboard.Whiteboard.UserList request,
                               io.grpc.stub.StreamObserver<whiteboard.Whiteboard.Response> responseObserver) {
        logger.severe("Received peer list update list: " + request.getUsernamesList());
        wb.getSelfUI().updatePeerList(new ArrayList<>(request.getUsernamesList()));
        responseObserver.onNext(whiteboard.Whiteboard.Response.newBuilder().setSuccess(true).setMessage("Successfully upd usrlst").build());
        responseObserver.onCompleted();
    }

    @Override
    public void showEditing(com.google.protobuf.StringValue request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received show editing request: " + request.getValue());
        wb.getSelfUI().showEditing();
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void closeWindow(com.google.protobuf.Empty request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received close window request");
        wb.getSelfUI().closeWindow("Manager has closed the session");
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateShapes(whiteboard.Whiteboard._CanvasShape requestShape,
                             io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.info("Received update shape request: " + requestShape.getShapeString());
        CanvasShape shape = protoShape2Shape(requestShape);
        wb.getTempShapes().remove(shape.getUsername());
        wb.acceptRemoteShape(shape);
        //以多一次重绘的代价解决文字预览停驻问题
        //关联改动：注释掉了acceptRemoteShape中的updateShapes
        wb.getSelfUI().reDraw();
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void updEditing(whiteboard.Whiteboard.SynchronizeUserRequest request,
                                   io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received synchronize editing BCast: " + request.getOperation() +" "+ request.getUsername());
        wb.updEditing(request.getOperation(), request.getUsername());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateChatBox(whiteboard.Whiteboard.ChatMessage request,
                              io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received chat message: " + request.getMessage());
        wb.getMessageArrayList().add(request.getMessage());
        wb.getSelfUI().updateChatBox(request.getMessage());
    }

    @Override
    public void clearCanvas(com.google.protobuf.Empty request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received clear canvas request");
        wb.getSelfUI().clearCanvas();
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sPreviewShapes(Whiteboard._CanvasShape request,
                               StreamObserver<Empty> responseObserver) {
        logger.severe("Received preview shape request: " + request.getShapeString());
        CanvasShape shape = protoShape2Shape(request);
        SwingUtilities.invokeLater(() -> {
//            wb.getSelfUI().drawCanvasShape(shape);
            wb.getTempShapes().put(shape.getUsername(), shape);
            wb.getSelfUI().reDraw();
        });
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void forceClearTmp(whiteboard.Whiteboard.UserName request,
                              io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received force clear tmp BCast: " + request.getUsername());
        wb.getTempShapes().remove(request.getUsername());
        SwingUtilities.invokeLater(() -> {
            wb.getSelfUI().reDraw();
        });
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}