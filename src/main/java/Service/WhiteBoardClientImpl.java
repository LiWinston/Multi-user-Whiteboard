package Service;

import WBSYS.WhiteBoard;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.Whiteboard._CanvasShape;
import whiteboard.Whiteboard.UserList;
import whiteboard.Whiteboard.ChatMessage;

import java.util.ArrayList;
import java.util.logging.Logger;

public class WhiteBoardClientImpl extends WhiteBoardClientServiceGrpc.WhiteBoardClientServiceImplBase {
    public WhiteBoard wb;
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
        wb.getSelfUI().updatePeerList((ArrayList<String>)request.getUsernamesList());
        responseObserver.onNext(whiteboard.Whiteboard.Response.newBuilder().setSuccess(true).setMessage("Successfully upd usrlst").build());
        responseObserver.onCompleted();
    }

}