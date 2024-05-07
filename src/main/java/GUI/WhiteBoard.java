package GUI;


import WBSYS.CanvasShape;
import WBSYS.parameters;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class WhiteBoard implements IWhiteBoard {
    //    private final ArrayList<IClient> clientUIList = new ArrayList<>();
    private final ArrayList<String> userList = new ArrayList<>();

    public ConcurrentHashMap<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> userAgents = new ConcurrentHashMap<>();

    boolean isManager = false;
    //仅管理员存吧还是
    private WhiteBoardServiceGrpc.WhiteBoardServiceStub managerStub;
    private IClient selfUI;
    private ArrayList<CanvasShape> canvasShapeArrayList = new ArrayList<>();
    private ArrayList<String> messageArrayList = new ArrayList<>();

    public IClient getSelfUI() {
        return selfUI;
    }

    public void setSelfUI(IClient selfUI) {
        this.selfUI = selfUI;
    }

    public WhiteBoardServiceGrpc.WhiteBoardServiceStub getManagerStub() {
        return managerStub;
    }

    public void setManagerStub(WhiteBoardServiceGrpc.WhiteBoardServiceStub managerStub) {
        this.managerStub = managerStub;
    }


    //管理员only
    public synchronized void removePeer(String username) {
        String kickedClient = userList.stream().filter(client -> client.equals(username)).findFirst().orElse(null);

        if (!selfUI.getUsername().equals("Manager")) {
            return;
        }
        if (kickedClient != null) {
            userList.remove(kickedClient);
            userAgents.get(kickedClient).closeWindow(com.google.protobuf.Empty.newBuilder().build(), new StreamObserver<com.google.protobuf.Empty>() {
                @Override
                public void onNext(Empty empty) {
                    System.out.println("Remove peer success.");
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Remove peer failed.");
                }

                @Override
                public void onCompleted() {
                }
            });
            this.SynchronizeMessage(parameters.managerMessage(kickedClient + " have been removed"));
            this.SynchronizeUser("remove", username);
            userAgents.remove(username);
        } else {
            selfUI.updateChatBox("user not found.");
        }
    }


    public synchronized void peerExit(String username) {
        userList.removeIf(s -> s.equals(username));
        userAgents.remove(username);
        this.SynchronizeMessage(parameters.managerMessage(username + " has exited!\n"));
//        managerStub.synchronizeUser("remove", username);
    }


    public synchronized void newFile() {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.clearCanvas(com.google.protobuf.Empty.newBuilder().build(), new StreamObserver<com.google.protobuf.Empty>() {
                    @Override
                    public void onNext(Empty empty) {
                        System.out.println("Clear canvas success.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Clear canvas failed.");
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            } else {
                System.out.println("Cannot get stub for " + ent.getKey());
                System.out.println("UserAgents: " + userAgents);
            }
        }
        this.canvasShapeArrayList = new ArrayList<>();
    }


    public synchronized void openFile(ArrayList<CanvasShape> newShapes) {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.clearCanvas(com.google.protobuf.Empty.newBuilder().build(), new StreamObserver<com.google.protobuf.Empty>() {
                    @Override
                    public void onNext(Empty empty) {
                        System.out.println("Clear canvas success.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Clear canvas failed.");
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            } else {
                System.out.println("Cannot get stub for " + ent.getKey());
                System.out.println("UserAgents: " + userAgents);
            }
        }

        for (CanvasShape canvasShape : newShapes) {
            this.SynchronizeCanvas(canvasShape);
        }
    }


    public void managerClose() {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.closeWindow(com.google.protobuf.Empty.newBuilder().build(), new StreamObserver<com.google.protobuf.Empty>() {
                    @Override
                    public void onNext(Empty empty) {
                        System.out.println("Manager close success.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Manager close failed.");
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            } else {
                System.out.println("Cannot get stub for " + ent.getKey());
                System.out.println("UserAgents: " + userAgents);
            }
        }
    }


    public synchronized void registerManager(String IpAddress, String port, String name, ManagedChannel channel) {
        ManagerGUI managerGUI = null;
        isManager = true;
        managerGUI = new ManagerGUI(this, IpAddress, port, name, channel);
        userList.add("Manager");
        setSelfUI(managerGUI);
        userAgents.put("Manager", WhiteBoardClientServiceGrpc.newStub(channel));

    }


    public synchronized void registerPeer(String username, ManagedChannel channel) {
        PeerGUI peerGUI = new PeerGUI(this, username);
        peerGUI.Build();
        userList.add(username);
        setSelfUI(peerGUI);
//        userAgents.put(username, WhiteBoardClientServiceGrpc.newStub(channel));
        managerStub.synchronizeUser(Whiteboard.SynchronizeUserRequest.newBuilder().setOperation("add").setUsername(username).build(), new StreamObserver<Whiteboard.UserList>() {
            @Override
            public void onNext(Whiteboard.UserList userList) {
                System.out.println("Register peer success.");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Register peer failed.");
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    public synchronized void SynchronizeUser(String operation, String username) {

        if (isManager) {
            for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
                WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
                if (stb != null) {
                    System.out.println("UserAgents: " + userAgents);
                    stb.updatePeerList(Whiteboard.UserList.newBuilder().addAllUsernames(userList).build(), new StreamObserver<Whiteboard.Response>() {
                        @Override
                        public void onNext(Whiteboard.Response response) {
                            if (response.getSuccess()) {
                                System.out.println("Sync to peer success." + username);
                            } else {
                                System.out.println("Sync to peer failed." + username);
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
                } else {
                    System.out.println("Cannot get stub for " + ent.getKey());
                    System.out.println("UserAgents: " + userAgents);
                }
            }
        } else {
//            managerStub.synchronizeUser(
        }
    }


    public ArrayList<CanvasShape> getCanvasShapeArrayList() {
        return canvasShapeArrayList;
    }

    public synchronized void setCanvasShapeArrayList(ArrayList<CanvasShape> canvasShapeArrayList) {
        this.canvasShapeArrayList = canvasShapeArrayList;
    }


    public synchronized void SynchronizeEditing(String username) {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.showEditing(com.google.protobuf.StringValue.newBuilder().setValue(username).build(), new StreamObserver<com.google.protobuf.Empty>() {
                    @Override
                    public void onNext(Empty empty) {
                        System.out.println("Show editing success.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Show editing failed." + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            } else {
                System.out.println("Cannot get stub for " + ent.getKey());
                System.out.println("UserAgents: " + userAgents);
            }
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
        getSelfUI().updateShapes(canvasShape);
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.updateShapes(Whiteboard._CanvasShape.newBuilder().
                        setShapeString(canvasShape.getShapeString()).
                        setColor(String.valueOf(canvasShape.getColor().getRGB())).
                        addX(canvasShape.getX1()).addX(canvasShape.getX2()).addX(canvasShape.getY1()).addX(canvasShape.getY2()).
                        setText(!Objects.equals(canvasShape.getText(), "") ? canvasShape.getText() : "Nobody").
                        setFill(canvasShape.isFill()).
                        setUsername(canvasShape.getUsername()).
                        addAllPoints((ArrayList) canvasShape.getPoints().stream().toList()).
                        setStrokeInt(canvasShape.getStrokeInt()).
                        build(), new StreamObserver<com.google.protobuf.Empty>() {
                    @Override
                    public void onNext(Empty empty) {
                        System.out.println("Sync to peer success.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Sync to peer failed.");
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            } else {
                System.out.println("Cannot get stub for " + ent.getKey());
                System.out.println("UserAgents: " + userAgents);
            }
        }

    }


    public synchronized void SynchronizeMessage(String chatMessage) {
        messageArrayList.add(chatMessage);
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.updateChatBox(Whiteboard.ChatMessage.newBuilder().setMessage(chatMessage).build(), new StreamObserver<com.google.protobuf.Empty>() {
                    @Override
                    public void onNext(Empty empty) {
                        System.out.println("chatMessage to peer success.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("chatMessage to peer failed.");
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            } else {
                System.out.println("Cannot get stub for " + ent.getKey());
                System.out.println("UserAgents: " + userAgents);
            }
        }
    }


    public synchronized boolean getApproveFromUI(String request) {
        return isManager && getSelfUI().requestFromPeer(request);
    }


    public void addUser(String username) {
        userList.add(username);
    }

    public void removeUser(String username) {
        userList.remove(username);
    }
}
