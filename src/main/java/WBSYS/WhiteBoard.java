package WBSYS;


import GUI.ManagerGUI;
import GUI.PeerGUI;
import GUI.IClient;

import java.util.ArrayList;


public class WhiteBoard{
    private ArrayList<CanvasShape> canvasShapeArrayList = new ArrayList<CanvasShape>();
    private ArrayList<IClient> clientArrayList = new ArrayList<>();
    private ArrayList<String> messageArrayList = new ArrayList<>();


    public synchronized void removePeer(String username){
        int index = -1;
        for (int i = 0; i < clientArrayList.size(); i++) {
            if (clientArrayList.get(i).getUsername().equals(username)) {
                index = i;
            }
        }
        if (index >= 0 && clientArrayList.get(index).getUsername().equals(username)) {
            IClient kickedClient = clientArrayList.get(index);
            kickedClient.closeWindow();
            clientArrayList.remove(index);
            this.SynchronizeMessage(parameters.managerMessage(kickedClient.getUsername() + " have been removed"));
            this.SynchronizeUser();
        } else {
            clientArrayList.get(0).updateChatBox("user not found.");
        }
    }


    public synchronized void peerExit(String username){
        for (IClient iClient : clientArrayList) {
            if (iClient.getUsername().equals(username)) {
                clientArrayList.remove(iClient);
                break;
            }
        }
        this.SynchronizeMessage(parameters.managerMessage(username + " has exitd!\n"));
        this.SynchronizeUser();
    }


    public synchronized void newFile(){
        for (IClient iClient : clientArrayList) {
            iClient.clearCanvas();
        }
        this.canvasShapeArrayList = new ArrayList<CanvasShape>();
        for (CanvasShape canvasShape : this.canvasShapeArrayList) {
            this.SynchronizeCanvas(canvasShape);
        }
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


    public synchronized void registerManager(String IpAddress, String port, String name){
        ManagerGUI managerGUI = new ManagerGUI(this, IpAddress, port, name);
        clientArrayList.add(managerGUI);
        this.SynchronizeUser();
    }


    public synchronized void registerPeer(String username){
        PeerGUI peerGUI = new PeerGUI(this, username);
        peerGUI.Build();
        clientArrayList.add(peerGUI);
        this.SynchronizeUser();


    }


    public ArrayList<CanvasShape> getCanvasShapeArrayList(){
        return canvasShapeArrayList;
    }

    public synchronized void setCanvasShapeArrayList(ArrayList<CanvasShape> canvasShapeArrayList) {
        this.canvasShapeArrayList = canvasShapeArrayList;
    }


    public ArrayList<IClient> getClientArrayList(){
        return clientArrayList;
    }

    public void setClientArrayList(ArrayList<IClient> clientArrayList) {
        this.clientArrayList = clientArrayList;
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
        for (IClient client : clientArrayList) {
            client.updatePeerList(peers);
        }

    }


    public synchronized boolean checkPeerName(String peerName){
        for (IClient client : clientArrayList) {
            if (peerName.equals(client.getUsername())) {
                return false;
            }
        }
        return true;
    }


    public synchronized boolean getApprove(String request){
        for (IClient client : clientArrayList) {
            if ("Manager".equals(client.getUsername())) {
                return client.requestFromPeer(request);
            }
        }
        return false;
    }


}
