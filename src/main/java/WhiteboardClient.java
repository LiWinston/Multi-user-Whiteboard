import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import whiteboard.Whiteboard.DrawAction;
import whiteboard.WhiteboardServiceGrpc;

public class WhiteboardClient {
    private final WhiteboardServiceGrpc.WhiteboardServiceStub asyncStub;

    public WhiteboardClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = WhiteboardServiceGrpc.newStub(channel);
    }

    public void startDrawing() {
        StreamObserver<DrawAction> requestObserver = asyncStub.streamDrawAction(new StreamObserver<DrawAction>() {
            @Override
            public void onNext(DrawAction action) {
                // 更新本地画板
                updateCanvas(action);
            }

            private void updateCanvas(DrawAction action) {

            }

            @Override
            public void onError(Throwable t) {
                // 错误处理
            }

            @Override
            public void onCompleted() {
                // 完成处理
            }
        });

        // 发送动作流到服务器
        requestObserver.onNext(DrawAction.newBuilder().build());
    }

    public static void main(String[] args) {
        WhiteboardClient client = new WhiteboardClient("localhost", 50051);
        client.startDrawing();
    }
}
