package Service;

import GUI.IWhiteBoard;
import GUI.WhiteBoard;
import whiteboard.WhiteBoardClientServiceGrpc;

import java.util.ArrayList;
import java.util.logging.Logger;

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
        logger.severe("Received peer list update request: " + request.getUsernamesList());
        System.out.println((ArrayList<String>) request.getUsernamesList().stream().toList());
        wb.getSelfUI().updatePeerList((ArrayList<String>) request.getUsernamesList().stream().toList());
        responseObserver.onNext(whiteboard.Whiteboard.Response.newBuilder().setSuccess(true).setMessage("Successfully upd usrlst").build());
        responseObserver.onCompleted();
    }

    @Override
    public void showEditing(com.google.protobuf.StringValue request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received show editing request: " + request.getValue());
        wb.getSelfUI().showEditing(request.getValue());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void closeWindow(com.google.protobuf.Empty request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.severe("Received close window request");
        wb.getSelfUI().closeWindow();
    }

}