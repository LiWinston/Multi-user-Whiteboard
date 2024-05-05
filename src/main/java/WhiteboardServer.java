import io.grpc.stub.StreamObserver;
import whiteboard.WhiteboardServiceGrpc;
import whiteboard.Whiteboard.DrawAction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WhiteboardServer extends WhiteboardServiceGrpc.WhiteboardServiceImplBase {
    private final ConcurrentHashMap<String, StreamObserver<DrawAction>> observers = new ConcurrentHashMap<>();

    @Override
    public StreamObserver<DrawAction> streamDrawAction(StreamObserver<DrawAction> responseObserver) {
        String userId = UUID.randomUUID().toString();
        observers.put(userId, responseObserver);

        return new StreamObserver<DrawAction>() {
            @Override
            public void onNext(DrawAction action) {
                broadcast(action);
            }

            @Override
            public void onError(Throwable t) {
                observers.remove(userId);
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                observers.remove(userId);
                responseObserver.onCompleted();
            }
        };
    }

    // 广播绘制动作到所有监听者
    private void broadcast(DrawAction action) {
        for (StreamObserver<DrawAction> observer : observers.values()) {
            observer.onNext(action);
        }
        System.out.println("Server is done sending messages");
    }

    public static void main(String[] args) {
        io.grpc.Server server = io.grpc.ServerBuilder.forPort(50051)
                .addService(new WhiteboardServer())
                .build();

        try {
            server.start();
            server.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
