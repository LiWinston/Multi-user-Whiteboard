package GUI;


import WBSYS.CanvasShape;
import WBSYS.parameters;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.protobuf.Empty;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class WhiteBoard implements IWhiteBoard {
    //    private final ArrayList<IClient> clientUIList = new ArrayList<>();
    private final ArrayList<String> userList = new ArrayList<>();
    private final ConcurrentHashMultiset<String> editingUser = ConcurrentHashMultiset.create();
    public ConcurrentHashMap<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> userAgents = new ConcurrentHashMap<>();
    boolean isManager = false;
    //仅管理员存吧还是
    private WhiteBoardServiceGrpc.WhiteBoardServiceStub managerStub;
    private IClient selfUI;
    private ArrayList<CanvasShape> canvasShapeArrayList = new ArrayList<>();
    private ArrayList<String> messageArrayList = new ArrayList<>();

    public ConcurrentHashMultiset<String> getEditingUser() {
        return editingUser;
    }

    public IClient getSelfUI() {
        return selfUI;
    }

    public void setSelfUI(IClient selfUI) {
        this.selfUI = selfUI;
    }

    public WhiteBoardServiceGrpc.WhiteBoardServiceStub getManagerStub() {
        return managerStub;
    }

    public void setServerStub(WhiteBoardServiceGrpc.WhiteBoardServiceStub managerStub) {
        this.managerStub = managerStub;
    }


    //only manager, thus no need for specific type check
    public synchronized void removePeer(String username) {
        String kickedClient = userList.stream().filter(client -> client.equals(username)).findFirst().orElse(null);

        if (!selfUI.getUsername().equals("Manager")) {
            return;
        }
        if (kickedClient != null) {
            userList.remove(kickedClient);
            userAgents.get(kickedClient).closeWindow(Empty.newBuilder().build(),
                    new StreamObserver<Empty>() {
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
            this.pushMessage(parameters.managerMessage(kickedClient + " have been removed"));
            this.SynchronizeUser("remove", username);
            userAgents.remove(username);
        } else {
            selfUI.updateChatBox("user not found.");
        }
    }


    public synchronized void peerExit(String username) {
        userList.removeIf(s -> s.equals(username));
//        userAgents.remove(username); 没用 本地改这个没意义
        this.SynchronizeUser("remove", username);
        this.pushMessage(parameters.managerMessage(username + " has exited!\n"));
    }


    //only manager, thus no need for specific type check
    public synchronized void newFile() {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.clearCanvas(Empty.newBuilder().build(), new StreamObserver<Empty>() {
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

    //only manager, thus no need for specific type check
    public synchronized void openFile(ArrayList<CanvasShape> newShapes) {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.clearCanvas(Empty.newBuilder().build(), new StreamObserver<Empty>() {
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
            this.pushShape(canvasShape);
        }
    }

    //only manager, thus no need for specific type check
    public void managerClose() {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                stb.closeWindow(Empty.newBuilder().build(), new StreamObserver<Empty>() {
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

    //only manager, thus no need for specific type check
    public synchronized void registerManager(String IpAddress, String port, String name, ManagedChannel channel) {
        ManagerGUI managerGUI = null;
        isManager = true;
        managerGUI = new ManagerGUI(this, IpAddress, port, name, channel);
        userList.add("Manager");
        setSelfUI(managerGUI);
        userAgents.put("Manager", WhiteBoardClientServiceGrpc.newStub(channel));
        SynchronizeUser("add", "Manager");
    }


    //对管理员，调用从managerStub的实现发起，inputChannel是registerPeer调用中生成的
    //对peer，直接调用,暂时没用到inputChannel
    public synchronized void registerPeer(String username, String peerServiceIP, String peerServicePort, ManagedChannel inputChannel) {
        if (isManager) {
            //负责加入客户服务句柄 channel是用客户自己的ip port建立的
            userAgents.put(username, WhiteBoardClientServiceGrpc.newStub(inputChannel));
        } else {
            PeerGUI peerGUI = new PeerGUI(this, username);
            peerGUI.Build();
            userList.add(username);
            setSelfUI(peerGUI);
            System.out.println(managerStub + "  **  " + managerStub.getChannel().toString());
            Whiteboard.IP_Port ipp = null;
            try {
                ipp = Whiteboard.IP_Port.newBuilder()
                        .setUsername(username)
                        .setIp(peerServiceIP)
                        .setPort(peerServicePort)
                        .build();
                System.out.println("C side registerPeer : grpc call beginning");
            } catch (Exception e) {
                System.out.println("Error creating IP_Port object:");
                e.printStackTrace();
            }


            managerStub.registerPeer(ipp, new StreamObserver<>() {
                @Override
                public void onNext(Empty empty) {
                    System.out.println("C side registerPeer : grpc call success.");
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("C side registerPeer : grpc call failed." + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("C side registerPeer : grpc call 1 registerPeer completed");
                }
            });
            //已经包含wb.addUser(request.getUsername());
            managerStub.synchronizeUser(Whiteboard.SynchronizeUserRequest.newBuilder().setOperation("add").
                    setUsername(username).build(), new StreamObserver<Whiteboard.UserList>() {
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
                    System.out.println("C side registerPeer : grpc call 2 synchronizeUser completed");
                }
            });
        }
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
                                System.out.println("peer updatePeerList success." + username);
                            } else {
                                System.out.println("peer updatePeerList failed." + response.getMessage());
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
            managerStub.synchronizeUser(Whiteboard.SynchronizeUserRequest.newBuilder().setOperation(operation).
                    setUsername(username).build(), new StreamObserver<Whiteboard.UserList>() {
                @Override
                public void onNext(Whiteboard.UserList userList) {
                    System.out.println("Manager synchronizeUser success.");
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Manager synchronizeUser failed." + t.getMessage());
                }

                @Override
                public void onCompleted() {
                }
            });
        }
    }


    public ArrayList<CanvasShape> getCanvasShapeArrayList() {
        return canvasShapeArrayList;
    }

    public synchronized void setCanvasShapeArrayList(ArrayList<CanvasShape> canvasShapeArrayList) {
        this.canvasShapeArrayList = canvasShapeArrayList;
    }


    public synchronized void reportUpdEditing(String operation, String username) {
        //peer用户自身更改完毕后仅向manager同步
        managerStub.reportUpdEditing(Whiteboard.SynchronizeUserRequest.newBuilder().setOperation(operation).
                setUsername(username).build(), new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty empty) {
                System.out.println("manager synchronizeEditing success.");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("manager synchronizeEditing failed." + t.getMessage());
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    public void updEditing(String operation, String username) {

        switch (operation) {
            case "add":
                if (editingUser.count(username) == 0)
                    editingUser.add(username);
                break;
            case "remove":
                editingUser.removeIf(s -> s.equals(username));
                break;
        }
        getSelfUI().showEditing();
    }


    public void broadCastEditing(String operation, String username) {
        System.out.println(userAgents);
        //manager自身更改更改完毕后向所有peer同步 除了自身客户端
        //也得除了username消息来源
        Context newContext = Context.current().fork();
        Context origContext = newContext.attach();
        try {
            for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
                WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
                if (stb != null) {
                    stb.updEditing(Whiteboard.SynchronizeUserRequest.newBuilder().setOperation(operation).
                            setUsername(username).build(), new StreamObserver<Empty>() {
                        @Override
                        public void onNext(Empty empty) {
                            System.out.println("peer synchronizeEditing success.");
                        }

                        @Override
                        public void onError(Throwable t) {
                            System.out.println("peer synchronizeEditing failed." + t.getMessage());
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
        } finally {
            newContext.detach(origContext);
        }

    }

    public ArrayList<String> getMessageArrayList() {
        return messageArrayList;
    }

    public void setMessageArrayList(ArrayList<String> messageArrayList) {
        this.messageArrayList = messageArrayList;
    }


    public synchronized void pushShape(CanvasShape canvasShape) {
        Context.current().fork().run(() -> {
            managerStub.pushShape(Whiteboard._CanvasShape.newBuilder().
                    setShapeString(canvasShape.getShapeString()).
                    setColor(String.valueOf(canvasShape.getColor().getRGB())).
                    addX(canvasShape.getX1()).addX(canvasShape.getX2()).addX(canvasShape.getY1()).addX(canvasShape.getY2()).
                    setText(canvasShape.getText() == null ? "" : canvasShape.getText()).
                    setFill(canvasShape.isFill()).
                    setUsername(canvasShape.getUsername()).
                    addAllPoints(Optional.ofNullable(canvasShape.getPoints())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(point -> Whiteboard.point.newBuilder()
                                    .setX(point.getX())
                                    .setY(point.getY())
                                    .build())
                            .toList()).
                    setStrokeInt(canvasShape.getStrokeInt()).
                    build(), new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty empty) {
                    System.out.println("manager synchronizeCanvas success.");
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("manager synchronizeCanvas failed." + t.getMessage());
                }

                @Override
                public void onCompleted() {
                }
            });
        });
    }

    public void broadCastShape(CanvasShape canvasShape) {
        if (isManager) {
            for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
                if (ent.getKey().equals(canvasShape.getUsername())) {
                    continue;
                }
                WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
                if (stb != null) {
                    Context.current().fork().run(() -> {
                        stb.updateShapes(Whiteboard._CanvasShape.newBuilder().
                                setShapeString(canvasShape.getShapeString()).
                                setColor(String.valueOf(canvasShape.getColor().getRGB())).
                                addX(canvasShape.getX1()).addX(canvasShape.getX2()).addX(canvasShape.getY1()).addX(canvasShape.getY2()).
                                setText(canvasShape.getText() == null ? "" : canvasShape.getText()).
                                setFill(canvasShape.isFill()).
                                setUsername(canvasShape.getUsername()).
                                addAllPoints(Optional.ofNullable(canvasShape.getPoints())
                                        .orElse(new ArrayList<>())
                                        .stream()
                                        .map(point -> Whiteboard.point.newBuilder()
                                                .setX(point.getX())
                                                .setY(point.getY())
                                                .build())
                                        .toList()).
                                setStrokeInt(canvasShape.getStrokeInt()).
                                build(), new StreamObserver<Empty>() {
                            @Override
                            public void onNext(Empty empty) {
                                System.out.println("peer updateShapes success.");
                            }

                            @Override
                            public void onError(Throwable t) {
                                System.out.println("peer updateShapes failed." + t.getMessage());
                            }

                            @Override
                            public void onCompleted() {
                            }
                        });
                    });
                } else {
                    System.out.println("Cannot get stub for " + ent.getKey());
                    System.out.println("UserAgents: " + userAgents);
                }
            }
        }
    }

    public void acceptRemoteShape(CanvasShape canvasShape) {
        canvasShapeArrayList.add(canvasShape);
        getSelfUI().updateShapes(canvasShape);
    }


    public synchronized void pushMessage(String chatMessage) {
        managerStub.pushMessage(Whiteboard.ChatMessage.newBuilder().setMessage(chatMessage).build(), new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty empty) {
                System.out.println("manager pushMessage success.");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("manager pushMessage failed." + t.getMessage());
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    public void broadCastChatMessage(String message) {
        for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
            WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
            if (stb != null) {
                Context.current().fork().run(() -> {
                            stb.updateChatBox(Whiteboard.ChatMessage.newBuilder().setMessage(message).build(), new StreamObserver<Empty>() {
                                @Override
                                public void onNext(Empty empty) {
                                    System.out.println("peer updateChatBox success.");
                                }

                                @Override
                                public void onError(Throwable t) {
                                    System.out.println("peer updateChatBox failed." + t.getMessage());
                                }

                                @Override
                                public void onCompleted() {
                                }
                            });
                        }
                );

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
        userAgents.remove(username);
    }
}
