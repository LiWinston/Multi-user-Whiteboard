package GUI;

import WBSYS.CanvasShape;

import java.util.ArrayList;

public interface IClient{
    void warningFromManager(String message);

    void updateShapes(CanvasShape canvasShape);

    void updateChatBox(String chatMessage);

    boolean approveClientRequest(String username);

    void updatePeerList(ArrayList<String> userList);

    boolean requestFromPeer(String request);

    String getUsername();

    void reDraw();

    void closeWindow();

    void exit();

    void clearCanvas();

    void showEditing();

    void drawCanvasShape(CanvasShape canvasShape);
}
