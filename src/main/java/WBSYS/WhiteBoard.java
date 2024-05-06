package WBSYS;


import GUI.ManagerGUI;
import GUI.PeerGUI;
import GUI.IClient;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.Whiteboard;
import whiteboard.Whiteboard.UserList;
import whiteboard.Whiteboard._CanvasShape;
import whiteboard.Whiteboard.point;
import whiteboard.Whiteboard.ChatMessage;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class WhiteBoard{
    private ArrayList<CanvasShape> canvasShapeArrayList = new ArrayList<>();
    private ArrayList<IClient> clientArrayList = new ArrayList<>();
    //这样就完全允许p2p操作了，真上线必须加鉴权
    public ConcurrentHashMap<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> userAgents = new ConcurrentHashMap<>();

    private ArrayList<String> messageArrayList = new ArrayList<>();

    public ArrayList<IClient> getClientArrayList() {
        return clientArrayList;
    }

    public synchronized void removePeer(String username){
        IClient kickedClient = clientArrayList.stream()
                                      .filter(client -> client.getUsername().equals(username))
                                      .findFirst()
                                      .orElse(null);

        if (kickedClient != null) {
            kickedClient.closeWindow();
            clientArrayList.remove(kickedClient);
            userAgents.remove(username);
            this.SynchronizeMessage(parameters.managerMessage(kickedClient.getUsername() + " have been removed"));
            this.SynchronizeUser();
        } else {
            clientArrayList.get(0).updateChatBox("user not found.");
        }
    }


    public synchronized void peerExit(String username){
    clientArrayList.removeIf(iClient -> iClient.getUsername().equals(username));
    userAgents.remove(username);
    this.SynchronizeMessage(parameters.managerMessage(username + " has exited!\n"));
    this.SynchronizeUser();
}


    public synchronized void newFile(){
        for (IClient iClient : clientArrayList) {
            iClient.clearCanvas();
        }
        this.canvasShapeArrayList = new ArrayList<>();
    }


    public synchronized void openFile(ArrayList<CanvasShape> newShapes){
        for (IClient iClient : clientArrayList) {
            iClient.clearCanvas();
        }

        for (CanvasShape canvasShape : newShapes) {
            this.SynchronizeCanvas(canvasShape);
        }
    }


    public void managerClose(){
        for (IClient iClient : clientArrayList) {
            if (!iClient.getUsername().equals("Manager")) {
                iClient.warningFromManager("Manager is closing Whiteboard...Window is closing...");
                iClient.closeWindow();
            }
        }
    }


    public synchronized void registerManager(String IpAddress, String port, String name, ManagedChannel channel){
        ManagerGUI managerGUI = new ManagerGUI(this, IpAddress, port, name, channel);
        clientArrayList.add(managerGUI);
        userAgents.put(name, WhiteBoardClientServiceGrpc.newStub(channel));
        this.SynchronizeUser();
    }


    public synchronized void registerPeer(String username, ManagedChannel channel){
        PeerGUI peerGUI = new PeerGUI(this, username);
        peerGUI.Build();
        clientArrayList.add(peerGUI);
        userAgents.put(username, WhiteBoardClientServiceGrpc.newStub(channel));
        this.SynchronizeUser();


    }


    public ArrayList<CanvasShape> getCanvasShapeArrayList(){
        return canvasShapeArrayList;
    }

    public synchronized void setCanvasShapeArrayList(ArrayList<CanvasShape> canvasShapeArrayList) {
        this.canvasShapeArrayList = canvasShapeArrayList;
    }


    public synchronized void SynchronizeEditing(String username){
        for (IClient client : clientArrayList) {
            client.showEditing(username);
        }
    }

    public ArrayList<String> getMessageArrayList() {
        return messageArrayList;
    }

    public void setMessageArrayList(ArrayList<String> messageArrayList) {
        this.messageArrayList = messageArrayList;
    }


    public synchronized void SynchronizeCanvas(CanvasShape canvasShape){
        canvasShapeArrayList.add(canvasShape);
        for (IClient client : clientArrayList) {
            client.updateShapes(canvasShape);
        }

    }


    public synchronized void SynchronizeMessage(String chatMessage){
        messageArrayList.add(chatMessage);
        for (IClient client : clientArrayList) {
            client.updateChatBox(chatMessage);
        }
    }


    public synchronized void SynchronizeUser(){
        ArrayList<String> peers = new ArrayList<>();
        for (IClient client : clientArrayList) {
            peers.add(client.getUsername());
        }

        UserList userList = UserList.newBuilder().addAllUsernames(peers).build();

        for (IClient client : clientArrayList) {
            userAgents.get(client.getUsername()).updatePeerList(userList, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty value) {
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                }
            });
        }

    }



    public synchronized boolean getApproveFromUI(String request){
        for (IClient client : clientArrayList) {
            if ("Manager".equals(client.getUsername())) {
                return client.requestFromPeer(request);
            }
        }
        return false;
    }


}
