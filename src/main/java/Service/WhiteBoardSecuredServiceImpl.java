package Service;

import GUI.WhiteBoard;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardSecuredServiceGrpc;
import whiteboard.Whiteboard;

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
	public void synchronizeUser(whiteboard.Whiteboard.SynchronizeUserRequest request,
			io.grpc.stub.StreamObserver<whiteboard.Whiteboard.UserList> responseObserver) {
		logger.info("Received synchronizeUser request: " + request.getOperation() + " " + request.getUsername());
		String clientId = Constants.CLIENT_ID_CONTEXT_KEY.get(Context.current());
		// 若发起人为管理员，允许任何操作；若发起人为普通用户，只允许操作其自身
		if (!clientId.equals(request.getUsername()) && !clientId.equals("Manager")) {
			logger.severe("Unauthorized req: " + clientId);
			responseObserver.onError(new RuntimeException("Unauthorized"));
			return;
		}
		if (request.getOperation().equals("add")) {
			wb.addUser(request.getUsername());
		}
		else if (request.getOperation().equals("remove")) {
			wb.removeUser(request.getUsername());
		}
		var responseList = whiteboard.Whiteboard.UserList.newBuilder().addAllUsernames(wb.userAgents.keySet()).build();
		Context.current().fork().run(() -> {
			for (var me : wb.userAgents.entrySet()) {
				me.getValue().updatePeerList(responseList, new StreamObserver<Whiteboard.Response>() {
					@Override
					public void onNext(Whiteboard.Response value) {
						logger.info(value.getMessage());
					}

					@Override
					public void onError(Throwable t) {
						logger.severe(me + t.getMessage());
					}

					@Override
					public void onCompleted() {
					}
				});
			}
		});
		responseObserver.onNext(responseList);
		responseObserver.onCompleted();
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
