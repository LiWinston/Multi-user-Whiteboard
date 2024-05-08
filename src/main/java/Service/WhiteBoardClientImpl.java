package Service;

import GUI.IWhiteBoard;
import GUI.WhiteBoard;
import WBSYS.CanvasShape;
import whiteboard.WhiteBoardClientServiceGrpc;

import java.awt.*;
import java.util.ArrayList;
import java.util.logging.Logger;

import static Service.Utils.protoPointsToArrayList;

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
        CanvasShape shape;
        if(requestShape.getShapeString().equals("pen") || requestShape.getShapeString().equals("eraser")){
            shape = new CanvasShape(requestShape.getShapeString(), new Color(Integer.parseInt(requestShape.getColor())), requestShape.getUsername(), protoPointsToArrayList(requestShape.getPointsList().stream().toList()), requestShape.getStrokeInt());
        } else if (requestShape.getShapeString().equals("text")) {
            shape = new CanvasShape(requestShape.getShapeString(), new Color(Integer.parseInt(requestShape.getColor())), requestShape.getX(0), requestShape.getX(1), requestShape.getX(2), requestShape.getX(3), requestShape.getText(), requestShape.getFill(), requestShape.getUsername(), requestShape.getStrokeInt());
        } else {
            shape = new CanvasShape(requestShape.getShapeString(), new Color(Integer.parseInt(requestShape.getColor())), requestShape.getX(0), requestShape.getX(1), requestShape.getX(2), requestShape.getX(3), requestShape.getStrokeInt());
            shape.setUsername(requestShape.getUsername());
        }

        wb.SynchronizeCanvas(shape);
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
}