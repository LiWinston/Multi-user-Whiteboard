package GUI;

import WBSYS.CanvasShape;
import io.grpc.ManagedChannel;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public interface IWhiteBoard{
    void setDDL(int d);
    IClient getSelfUI();

    void pushShape(CanvasShape canvasShape);

    void broadCastShape(CanvasShape canvasShape);

    void acceptRemoteShape(CanvasShape canvasShape);

    void pushMessage(String chatMessage);

    void SynchronizeUser(String operation, String username);

    boolean getApproveFromUI(String request);

    void registerPeer(String username, String IpAddress, String port, ManagedChannel channel);

    void registerManager(String IpAddress, String port, String name, ManagedChannel channel);

    ConcurrentLinkedDeque<CanvasShape> getLocalShapeQ();

    void setLocalShapeQ(Collection<CanvasShape> localShapeQ);

    void reportUpdEditing(String operation, String username);

    void removePeer(String username);

    void peerExit(String username);

    void newFile();

    void openFile(Collection<CanvasShape> newShapes);

    void managerClose();

    void broadCastEditing(String operation, String username);

    void updEditing(String operation, String username);

    void broadCastChatMessage(String message);

    <E> Deque<E> getMessageArrayList();

    boolean checkConflictOk(CanvasShape shape);

    <K, V> Map<K,V> getTempShapes();

    void requestForceClearTmp();

    void broadCastForceClearTmp(String username);

    void setConIntersect(boolean b);
}