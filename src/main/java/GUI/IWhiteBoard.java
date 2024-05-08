package GUI;

import WBSYS.CanvasShape;
import io.grpc.ManagedChannel;

import java.util.ArrayList;
import java.util.List;

public interface IWhiteBoard{
    IClient getSelfUI();
    void pushShape(CanvasShape canvasShape);

    void broadCastShape(CanvasShape canvasShape);

    void acceptRemoteShape(CanvasShape canvasShape);

    void pushMessage(String chatMessage);

    void SynchronizeUser(String operation, String username);

    boolean getApproveFromUI(String request);

    void registerPeer(String username, String IpAddress, String port, ManagedChannel channel);

    void registerManager(String IpAddress, String port, String name, ManagedChannel channel);

    ArrayList<CanvasShape> getCanvasShapeArrayList();

    void reportUpdEditing(String operation, String username);

    void removePeer(String username);

    void peerExit(String username);

    void newFile();

    void openFile(ArrayList<CanvasShape> newShapes);

    void managerClose();

    void broadCastEditing(String operation, String username);

    void updEditing(String operation, String username);

    void broadCastChatMessage(String message);

    <E> List<E> getMessageArrayList();

    boolean checkConflictOk(CanvasShape shape);
}