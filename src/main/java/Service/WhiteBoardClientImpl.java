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
        wb.getSelfUI().closeWindow();
    }

    @Override
    public void updateShapes(whiteboard.Whiteboard._CanvasShape requestShape,
                             io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.info("Received update shape request: " + requestShape.getShapeString());
        CanvasShape shape = protoShape2Shape(requestShape);

        wb.acceptRemoteShape(shape);
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void updEditing(whiteboard.Whiteboard.SynchronizeUserRequest request,
                                   io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received synchronize editing request: " + request.getOperation() +" "+ request.getUsername());
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
        wb.getTempShapes().put(shape.getUsername(), shape);
        SwingUtilities.invokeLater(() -> {
            wb.getSelfUI().drawCanvasShape(shape);
            wb.getSelfUI().reDraw();
        });
    }
}