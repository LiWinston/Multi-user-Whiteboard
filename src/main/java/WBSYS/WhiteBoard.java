package WBSYS;


import GUI.IClient;
import GUI.ManagerGUI;
import GUI.PeerGUI;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.Whiteboard;
import whiteboard.Whiteboard.UserList;

import javax.swing.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class WhiteBoard {
    //这样就完全允许p2p操作了，真上线必须加鉴权
    public ConcurrentHashMap<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> userAgents = new ConcurrentHashMap<>();

    public IClient getSelfUI() {
        return selfUI;
    }

    public void setSelfUI(IClient selfUI) {
        this.selfUI = selfUI;
    }

    private IClient selfUI;
    private ArrayList<CanvasShape> canvasShapeArrayList = new ArrayList<>();
    private final ArrayList<IClient> clientArrayList = new ArrayList<>();
    private ArrayList<String> messageArrayList = new ArrayList<>();

    public ArrayList<IClient> getClientArrayList() {
        return clientArrayList;
    }

    public synchronized void removePeer(String username) {
        IClient kickedClient = clientArrayList.stream().filter(client -> client.getUsername().equals(username)).findFirst().orElse(null);

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


    public synchronized void peerExit(String username) {
        clientArrayList.removeIf(iClient -> iClient.getUsername().equals(username));
        userAgents.remove(username);
        this.SynchronizeMessage(parameters.managerMessage(username + " has exited!\n"));
        this.SynchronizeUser();
    }


    public synchronized void newFile() {
        for (IClient iClient : clientArrayList) {
            iClient.clearCanvas();
        }
        this.canvasShapeArrayList = new ArrayList<>();
    }


    public synchronized void openFile(ArrayList<CanvasShape> newShapes) {
        for (IClient iClient : clientArrayList) {
            iClient.clearCanvas();
        }

        for (CanvasShape canvasShape : newShapes) {
            this.SynchronizeCanvas(canvasShape);
        }
    }


    public void managerClose() {
        for (IClient iClient : clientArrayList) {
            if (!iClient.getUsername().equals("Manager")) {
                iClient.warningFromManager("Manager is closing Whiteboard...Window is closing...");
                iClient.closeWindow();
            }
        }
    }


    public synchronized void registerManager(String IpAddress, String port, String name, ManagedChannel channel) {
        SwingUtilities.invokeLater(() -> {
            ManagerGUI managerGUI = null;
            managerGUI = new ManagerGUI(this, IpAddress, port, name, channel);
            clientArrayList.add(managerGUI);
            setSelfUI(managerGUI);
            userAgents.put("Manager", WhiteBoardClientServiceGrpc.newStub(channel));
            this.SynchronizeUser();
        });
    }


    public synchronized void registerPeer(String username, ManagedChannel channel) {
        PeerGUI peerGUI = new PeerGUI(this, username);
        peerGUI.Build();
        clientArrayList.add(peerGUI);
        setSelfUI(peerGUI);
        userAgents.put(username, WhiteBoardClientServiceGrpc.newStub(channel));
        this.SynchronizeUser();


    }


    public ArrayList<CanvasShape> getCanvasShapeArrayList() {
        return canvasShapeArrayList;
    }

    public synchronized void setCanvasShapeArrayList(ArrayList<CanvasShape> canvasShapeArrayList) {
        this.canvasShapeArrayList = canvasShapeArrayList;
    }


    public synchronized void SynchronizeEditing(String username) {
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


    public synchronized void SynchronizeCanvas(CanvasShape canvasShape) {
        canvasShapeArrayList.add(canvasShape);
        for (IClient client : clientArrayList) {
            client.updateShapes(canvasShape);
        }

    }


    public synchronized void SynchronizeMessage(String chatMessage) {
        messageArrayList.add(chatMessage);
        for (IClient client : clientArrayList) {
            client.updateChatBox(chatMessage);
        }
    }


    public synchronized void SynchronizeUser() {
        ArrayList<String> peers = new ArrayList<>();
        for (IClient client : clientArrayList) {
            peers.add(client.getUsername());
        }

        UserList userList = UserList.newBuilder().addAllUsernames(peers).build();

        for (IClient client : clientArrayList) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = userAgents.get(client.getUsername());
            if (stb != null) {
                System.out.println("UserAgents: " + userAgents);
                stb.updatePeerList(userList, new StreamObserver<Whiteboard.Response>() {
                    @Override
                    public void onNext(Whiteboard.Response response) {
                        if (response.getSuccess()) {
                            System.out.println("Sync manager to peer success." + client.getUsername());
                        }else {
                            System.out.println("Sync manager to peer failed." + client.getUsername());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
//                        System.out.println("Sync manager to" + client.getUsername() + " failed.");
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }else {
                System.out.println("Cannot get stub for " + client.getUsername());
                System.out.println("UserAgents: " + userAgents);
            }
        }

    }


    public synchronized boolean getApproveFromUI(String request) {
        for (IClient client : clientArrayList) {
            if ("Manager".equals(client.getUsername())) {
                return client.requestFromPeer(request);
            }
        }
        return false;
    }


}
