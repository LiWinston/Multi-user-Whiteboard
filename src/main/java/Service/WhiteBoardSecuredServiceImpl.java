package Service;

import GUI.WhiteBoard;
import io.grpc.Context;
import whiteboard.WhiteBoardSecuredServiceGrpc;

import java.util.logging.Logger;

public class WhiteBoardSecuredServiceImpl extends WhiteBoardSecuredServiceGrpc.WhiteBoardSecuredServiceImplBase {
    public WhiteBoard wb;
    public Logger logger;

    WhiteBoardSecuredServiceImpl(WhiteBoard wb, Logger logger) {
        super();
        this.wb = wb;
        this.logger = logger;
    }

    @Override
    public void forceClearTmp(whiteboard.Whiteboard.UserName request,
                              io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        String clientId = Constants.CLIENT_ID_CONTEXT_KEY.get(Context.current());
        if (!clientId.equals(request.getUsername())) {
            logger.severe("Received force clear tmp request from unauthorized user: " + clientId);
            responseObserver.onError(new RuntimeException("Unauthorized"));
            return;
        }
        logger.severe("Received force clear tmp request: " + request.getUsername());
        wb.broadCastForceClearTmp(request.getUsername());
        responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }


}
