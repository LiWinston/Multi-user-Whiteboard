package GUI;

import Service.JwtCredential;
import WBSYS.CanvasShape;
import WBSYS.Properties;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import whiteboard.WhiteBoardClientServiceGrpc;
import whiteboard.WhiteBoardSecuredServiceGrpc;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard;
import whiteboard.Whiteboard.UserName;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static Service.Utils.shape2ProtoShape;
import static Service.Utils.shapes2ProtoShapes;

public class WhiteBoard implements IWhiteBoard {

	public int DDL = 0;

	// private final ArrayList<IClient> clientUIList = new ArrayList<>();
	private final ConcurrentHashMultiset<String> userList = ConcurrentHashMultiset.create();

	private final ConcurrentHashMultiset<String> editingUser = ConcurrentHashMultiset.create();

	public volatile ConcurrentHashMap<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> userAgents = new ConcurrentHashMap<>();

	boolean isManager = false;

	// 仅管理员存吧还是
	private WhiteBoardServiceGrpc.WhiteBoardServiceStub managerStub;

	private WhiteBoardSecuredServiceGrpc.WhiteBoardSecuredServiceStub managerSecuredStub;

	private CallCredentials callCredentials;

	private IClient selfUI;

	private volatile boolean allowConflict = false;

	public ConcurrentLinkedDeque<CanvasShape> getLocalShapeQ() {
		// System.out.println("Loc " + localShapeQ.size());
		return localShapeQ;
	}

	public volatile ConcurrentLinkedDeque<CanvasShape> localShapeQ = new ConcurrentLinkedDeque<>();

	public void setLocalShapeQ(Collection<CanvasShape> itShapes) {
		this.localShapeQ = new ConcurrentLinkedDeque<>(itShapes);
	}

	// public synchronized void setCanvasShapeArrayList(ArrayList<CanvasShape>
	// canvasShapeArrayList) {
	// this.canvasShapeArrayList = canvasShapeArrayList;
	// }

	public ConcurrentHashMap<String, CanvasShape> getTempShapes() {
		// System.out.println("Tmp " + tempShapes.size());
		return tempShapes;
	}

	public volatile ConcurrentHashMap<String, CanvasShape> tempShapes = new ConcurrentHashMap<>();

	private ConcurrentLinkedDeque<String> messageArrayList = new ConcurrentLinkedDeque<>();

	public ConcurrentHashMultiset<String> getEditingUser() {
		return editingUser;
	}

	@Override
	public void setDDL(int d) {
		this.DDL = d;
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

	public void setManagerSecuredStub(WhiteBoardSecuredServiceGrpc.WhiteBoardSecuredServiceStub managerSecuredStub) {
		this.managerSecuredStub = managerSecuredStub;
	}

	public CountDownLatch connectionErrorLatch = new CountDownLatch(5);

	// only manager, thus no need for specific type check
	public synchronized void removePeer(String username) {
		String kickedClient = userList.stream().filter(client -> client.equals(username)).findFirst().orElse(null);

		if (!selfUI.getUsername().equals("Manager")) {
			return;
		}
		if (kickedClient != null) {
			if (kickedClient.equals("Manager")) {
				JOptionPane.showMessageDialog(null, "Manager cannot be removed.");
				return;
			}
			userList.remove(kickedClient);
			userAgents.get(kickedClient)
				.closeWindow(StringValue.newBuilder().setValue("You are kicked out by manager.").build(),
						new StreamObserver<Empty>() {
							@Override
							public void onNext(Empty empty) {
								System.out.println("Remove peer success.");
							}

							@Override
							public void onError(Throwable t) {
								System.out.println("Remove peer failed.");
								for (int i = 3; i > 0; i--) {
									System.out.println("Remove peer failed. Retry in " + i + " seconds.");
									try {
										Thread.sleep(1000);
									}
									catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}

							@Override
							public void onCompleted() {
							}
						});
			this.pushMessage(Properties.managerMessage("Manager kicks " + kickedClient.trim() + " out"));
			this.SynchronizeUser("remove", username);
			userAgents.remove(username);
		}
		else {
			selfUI.updateChatBox("user not found.");
		}
	}

	public synchronized void peerExit(String username) {
		userList.removeIf(s -> s.equals(username));
		// userAgents.remove(username); 没用 本地改这个没意义
		this.SynchronizeUser("remove", username);
		this.pushMessage(Properties.managerMessage(username + " exited."));
	}

	// only manager, thus no need for specific type check
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
			}
			else {
				System.out.println("Cannot get stub for " + ent.getKey());
				System.out.println("UserAgents: " + userAgents);
			}
		}
		this.localShapeQ = new ConcurrentLinkedDeque<>();
	}

	// only manager, thus no need for specific type check
	public synchronized void openFile(Collection<CanvasShape> newShapes) {
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
				stb.updateShapeList(shapes2ProtoShapes(newShapes),
						new StreamObserver<whiteboard.Whiteboard.Response>() {
							@Override
							public void onNext(whiteboard.Whiteboard.Response response) {
								if (response.getSuccess()) {
									System.out.println("updateShapeList success.");
								}
								else {
									System.out.println("updateShapeList failed.");
								}
							}

							@Override
							public void onError(Throwable t) {
								System.out.println("updateShapeList failed.");
							}

							@Override
							public void onCompleted() {
							}
						});
			}
			else {
				System.out.println("Cannot get stub for " + ent.getKey());
				System.out.println("UserAgents: " + userAgents);
			}
		}

		// for (CanvasShape canvasShape : newShapes) {
		// this.pushShape(canvasShape);
		// }
	}

	// only manager, thus no need for specific type check
	public void managerClose() {
		Context.current().fork().run(() -> {
			for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents
				.entrySet()) {
				WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
				if (stb != null) {
					stb.closeWindow(StringValue.newBuilder().setValue("Manager exited, closing now.").build(),
							new StreamObserver<Empty>() {
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
				}
				else {
					System.out.println("Cannot get stub for " + ent.getKey());
					System.out.println("UserAgents: " + userAgents);
				}
			}
		});
	}

	// only manager, thus no need for specific type check
	public synchronized void registerManager(String IpAddress, String port, String name, ManagedChannel channel) {
		ManagerGUI managerGUI = null;
		isManager = true;
		managerGUI = new ManagerGUI(this, IpAddress, port, name, channel);
		userList.add("Manager");
		String jwtToken = "Manager";
		callCredentials = new JwtCredential(jwtToken);
		setSelfUI(managerGUI);
		userAgents.put("Manager", WhiteBoardClientServiceGrpc.newStub(channel));
		SynchronizeUser("add", "Manager");
	}

	// 对管理员，调用从managerStub的实现发起，inputChannel是registerPeer调用中生成的
	// 对peer，直接调用,暂时没用到inputChannel
	public synchronized void registerPeer(String username, String peerServiceIP, String peerServicePort,
			ManagedChannel inputChannel) {
		if (isManager) {
			// 负责加入客户服务句柄 channel是用客户自己的ip port建立的
			userAgents.put(username, WhiteBoardClientServiceGrpc.newStub(inputChannel));

			Context.current().fork().run(() -> {
				userAgents.get(username)
					.withDeadlineAfter(2, TimeUnit.SECONDS)
					.syncDDL(Whiteboard.DDLS.newBuilder().setDDLS(DDL + 2).build(), new StreamObserver<>() {
						@Override
						public void onNext(Empty empty) {
							System.out.println("peer syncDDL = DDL + 2 success.");
						}

						@Override
						public void onError(Throwable t) {
							System.out.println("peer syncDDL failed." + t.getMessage());
						}

						@Override
						public void onCompleted() {
						}
					});
			});
			var sendOk = sendCanvasShapeListTo(username);
			if (sendOk != null) {
				sendOk.thenAcceptAsync((Boolean ok) -> {
					if (ok) {
						System.out.println("Send canvas shape list to " + username + " success.");
					}
					else {
						System.out.println("Send canvas shape list to " + username + " failed.");
					}
				}).exceptionally((Throwable t) -> {
					System.out.println("Send canvas shape list to " + username + " returned exceptionally, FOR:/n"
							+ t.getMessage());
					return null;
				});
			}

		}
		else {
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
			}
			catch (Exception e) {
				System.out.println("Error creating IP_Port object:");
				e.printStackTrace();
			}

			CompletableFuture<Boolean> callCredentialsGot = new CompletableFuture<>();

			Whiteboard.IP_Port finalIpp = ipp;
			Context.current().fork().run(() -> {
				managerStub.registerPeer(finalIpp, new StreamObserver<>() {
					@Override
					public void onNext(Whiteboard.Response response) {
						if (response.getSuccess()) {
							String token = response.getMessage();
							callCredentials = new JwtCredential(token);
							callCredentialsGot.complete(true);
						}
						else {
							callCredentialsGot.complete(false);
							System.out.println("C side registerPeer : grpc call 1 registerPeer failed.");
						}
					}

					@Override
					public void onError(Throwable t) {
						callCredentialsGot.complete(false);
						System.out.println("C side registerPeer : grpc call failed." + t.getMessage());
					}

					@Override
					public void onCompleted() {
						System.out.println("C side registerPeer : grpc call 1 registerPeer completed");
					}
				});
			});

			// 等待callCredentials的返回
			callCredentialsGot.thenAcceptAsync((Boolean ok) -> {
				if (ok) {

					Context.current().fork().run(() -> {
						// 已经包含wb.addUser(request.getUsername());
						managerSecuredStub.withCallCredentials(callCredentials)
							.synchronizeUser(Whiteboard.SynchronizeUserRequest.newBuilder()
								.setOperation("add")
								.setUsername(username)
								.build(), new StreamObserver<Whiteboard.UserList>() {
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
										System.out
											.println("C side registerPeer : grpc call 2 synchronizeUser completed");
										Context.current().fork().run(() -> {

											managerStub.pushMessage(Whiteboard.ChatMessage.newBuilder()
												.setMessage(Properties.chatMessageFormat(username, " joined."))
												.build(), new StreamObserver<Empty>() {
													@Override
													public void onNext(Empty empty) {
													}

													@Override
													public void onError(Throwable t) {
													}

													@Override
													public void onCompleted() {
													}
												});
										});
									}
								});
					});
					System.out.println("Register peer success.");
				}
				else {
					System.out.println("Register peer failed.");
				}
			}).exceptionally((Throwable t) -> {
				System.out.println("Register peer returned exceptionally, FOR:/n" + t.getMessage());
				return null;
			});

		}
	}

	public synchronized void SynchronizeUser(String operation, String username) {
		if (isManager) {
			for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents
				.entrySet()) {
				WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
				if (stb != null) {
					System.out.println("UserAgents: " + userAgents);
					stb.updatePeerList(Whiteboard.UserList.newBuilder().addAllUsernames(userList).build(),
							new StreamObserver<Whiteboard.Response>() {
								@Override
								public void onNext(Whiteboard.Response response) {
									if (response.getSuccess()) {
										System.out.println("peer updatePeerList success." + username);
									}
									else {
										System.out.println("peer updatePeerList failed." + response.getMessage());
									}
								}

								@Override
								public void onError(Throwable t) {
									// System.out.println("Sync manager to" +
									// client.getUsername() + " failed.");
								}

								@Override
								public void onCompleted() {
								}
							});
				}
				else {
					System.out.println("Cannot get stub for " + ent.getKey());
					System.out.println("UserAgents: " + userAgents);
				}
			}
		}
		else {
			managerSecuredStub.withCallCredentials(callCredentials)
				.synchronizeUser(Whiteboard.SynchronizeUserRequest.newBuilder()
					.setOperation(operation)
					.setUsername(username)
					.build(), new StreamObserver<Whiteboard.UserList>() {
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

	public synchronized void reportUpdEditing(String operation, String username) {
		// peer用户自身更改完毕后仅向manager同步
		managerStub.reportUpdEditing(
				Whiteboard.SynchronizeUserRequest.newBuilder().setOperation(operation).setUsername(username).build(),
				new StreamObserver<Empty>() {
					@Override
					public void onNext(Empty empty) {
						if (connectionErrorLatch.getCount() == 1)
							connectionErrorLatch = new CountDownLatch(5);
						System.out.println("manager synchronizeEditing success.");
					}

					@Override
					public void onError(Throwable t) {
						connectionErrorLatch.countDown();
						if (connectionErrorLatch.getCount() == 0) {
							JOptionPane.showMessageDialog(null, "Connection error, client will exit.");

						}
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
		// manager自身更改更改完毕后向所有peer同步 除了自身客户端
		// 也得除了username消息来源
		Context newContext = Context.current().fork();
		Context origContext = newContext.attach();
		try {
			for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents
				.entrySet()) {
				WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
				if (stb != null) {
					stb.updEditing(Whiteboard.SynchronizeUserRequest.newBuilder()
						.setOperation(operation)
						.setUsername(username)
						.build(), new StreamObserver<Empty>() {
							@Override
							public void onNext(Empty empty) {
								System.out.println(ent.getKey() + " synchronizeEditing success.");
							}

							@Override
							public void onError(Throwable t) {
								System.out.println(ent.getKey() + " synchronizeEditing failed." + t.getMessage());
							}

							@Override
							public void onCompleted() {
							}
						});
				}
				else {
					System.out.println("Cannot get stub for " + ent.getKey());
					System.out.println("UserAgents: " + userAgents);
				}
			}
		}
		finally {
			newContext.detach(origContext);
		}

	}

	public ConcurrentLinkedDeque<String> getMessageArrayList() {
		return messageArrayList;
	}

	public void setMessageArrayList(ConcurrentLinkedDeque<String> messageArrayList) {
		this.messageArrayList = messageArrayList;
	}

	public synchronized void pushShape(CanvasShape canvasShape) {
		Context.current().fork().run(() -> {
			managerStub.pushShape(shape2ProtoShape(canvasShape), new StreamObserver<Empty>() {
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
			for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents
				.entrySet()) {
				if (ent.getKey().equals(canvasShape.getUsername())) {
					continue;
				}
				WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
				if (stb != null) {
					Context.current().fork().run(() -> {
						stb.updateShapes(shape2ProtoShape(canvasShape), new StreamObserver<Empty>() {
							@Override
							public void onNext(Empty empty) {
								System.out.println("peer updateShapes success.");
							}

							@Override
							public void onError(Throwable t) {
								System.out.println(ent.getKey() + "peer updateShapes failed." + t.getMessage());
							}

							@Override
							public void onCompleted() {
							}
						});
					});
				}
				else {
					System.out.println("Cannot get stub for " + ent.getKey());
					System.out.println("UserAgents: " + userAgents);
				}
			}
		}
	}

	public CompletableFuture<Boolean> sendCanvasShapeListTo(String username) {
		if (!isManager) {
			return null;
		}
		CompletableFuture<Boolean> futureOK = new CompletableFuture<>();
		Context.current().fork().run(() -> {
			userAgents.get(username)
				.updateShapeList(shapes2ProtoShapes(localShapeQ), new StreamObserver<whiteboard.Whiteboard.Response>() {
					@Override
					public void onNext(whiteboard.Whiteboard.Response response) {
						if (response.getSuccess()) {
							futureOK.complete(true);
							futureOK.resultNow();
						}
						else {
							futureOK.complete(false);
							futureOK.resultNow();
						}
						// System.out.println(futureOK);
					}

					@Override
					public void onError(Throwable t) {
						System.out.println("peer updateShapeList failed." + t.getMessage());
						futureOK.completeExceptionally(t);
					}

					@Override
					public void onCompleted() {
					}
				});
		});

		return futureOK;
	}

	public void acceptRemoteShape(CanvasShape canvasShape) {
		tempShapes.remove(canvasShape.getUsername());
		if (canvasShape.getShapeString().equals("text") && canvasShape.getText().isBlank()) {
			return;
		}
		localShapeQ.add(canvasShape);
		// getSelfUI().updateShapes(canvasShape);
	}

	public synchronized void pushMessage(String chatMessage) {
		managerStub.pushMessage(Whiteboard.ChatMessage.newBuilder().setMessage(chatMessage).build(),
				new StreamObserver<Empty>() {
					@Override
					public void onNext(Empty empty) {
						System.out.println(getSelfUI().getUsername() + "pushMessage success.");
					}

					@Override
					public void onError(Throwable t) {
						System.out.println(getSelfUI().getUsername() + "pushMessage failed." + t.getMessage());
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
					stb.updateChatBox(Whiteboard.ChatMessage.newBuilder().setMessage(message).build(),
							new StreamObserver<Empty>() {
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
				});

			}
			else {
				System.out.println("Cannot get stub for " + ent.getKey());
				System.out.println("UserAgents: " + userAgents);
			}
		}
	}

	volatile StreamObserver<Whiteboard._CanvasShape> previewTmpStream = null;

	// 推送本用户的临时预览图形
	// in ConcurrentHashMap<String, CanvasShape> tempShapes -- to Server
	public SettableFuture<Boolean> sBeginPushShape() {
		// allows UI Sync function awaits Async stream response obtained from sPushShape
		SettableFuture<Boolean> futureOK = SettableFuture.create();
		StreamObserver<whiteboard.Whiteboard.Response> response = new StreamObserver<>() {
			@Override
			public void onNext(whiteboard.Whiteboard.Response res) {
				if (res.getSuccess()) {
					futureOK.set(true);
					// futureOK.resultNow();
					System.out.println("set OK to true.");
				}
				else {
					System.out.println(res.getMessage());
					futureOK.set(false);
					// futureOK.resultNow();
				}
			}

			@Override
			public void onError(Throwable t) {
				futureOK.set(false);
				if (t.getMessage().contains("Conflict")) {
					// JOptionPane.showMessageDialog(null, "Conflict detected with another
					// user, please try again.");

				}
				else {
					System.out.println(t.getMessage());
				}
				System.out.println(t.getMessage());
			}

			@Override
			public void onCompleted() {
				requestForceClearTmp();
				// futureOK.set(true);
			}
		};
		previewTmpStream = managerStub.withDeadlineAfter(DDL, TimeUnit.SECONDS).sPushShape(response);

		return futureOK;
	}

	@SentinelResource(value = "sbroadCastShape")
	public void sbroadCastShape(Whiteboard._CanvasShape _canvasShape) {
		for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
			if (ent.getKey().equals(_canvasShape.getUsername())) {
				continue;
			}
			WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
			if (stb != null) {
				Context.current().fork().run(() -> {
					stb.sPreviewShapes(_canvasShape, new StreamObserver<Empty>() {
						@Override
						public void onNext(Empty empty) {
							// System.out.println("sbroadCastShape TO: "+ ent.getKey() + "
							// success.");
						}

						@Override
						public void onError(Throwable t) {
							System.out.println(ent.getKey() + "peer sbroadCastShape failed." + t.getMessage());
						}

						@Override
						public void onCompleted() {
						}
					});
				});
			}
			else {
				System.out.println("Cannot get stub for " + ent.getKey());
				System.out.println("UserAgents: " + userAgents);
			}
		}
	}

	@Override
	public void requestForceClearTmp() {
		Context.current().fork().run(() -> {
			managerSecuredStub.withCallCredentials(callCredentials)
				.forceClearTmp(UserName.newBuilder().setUsername(getSelfUI().getUsername()).build(),
						new StreamObserver<Empty>() {
							@Override
							public void onNext(Empty empty) {
								System.out.println("requestForceClearTmp success.");
							}

							@Override
							public void onError(Throwable t) {
								System.out.println("requestForceClearTmp failed." + t.getMessage());
							}

							@Override
							public void onCompleted() {
							}
						});
		});
	}

	@Override
	public void broadCastForceClearTmp(String username) {
		for (Map.Entry<String, WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub> ent : userAgents.entrySet()) {
			WhiteBoardClientServiceGrpc.WhiteBoardClientServiceStub stb = ent.getValue();
			if (stb != null) {
				Context.current().fork().run(() -> {
					stb.forceClearTmp(UserName.newBuilder().setUsername(username).build(), new StreamObserver<Empty>() {
						@Override
						public void onNext(Empty empty) {
						}

						@Override
						public void onError(Throwable t) {
							System.out.println("peer clearTmpShapes failed." + t.getMessage());
						}

						@Override
						public void onCompleted() {
						}
					});
				});
			}
			else {
				System.out.println("Cannot get stub for " + ent.getKey());
				System.out.println("UserAgents: " + userAgents);
			}
		}
	}

	@Override
	public void setConIntersect(boolean b) {
		allowConflict = b;
	}

	// public void sPreviewShapes(Whiteboard._CanvasShape prvShape) {
	// getSelfUI().previewShape(prvShape);
	// }
	private static final Set<String> pointsShape = Set.of("pen", "eraser");

	@Override
	public boolean checkConflictOk(CanvasShape newShape) {
		if (allowConflict) {
			return true;
		}
		for (CanvasShape editingShape : tempShapes.values()) {
			if (editingShape.getUsername().equals(newShape.getUsername()))
				continue;
			// 使用边界框来初步判断重叠
			if (overlapBoundingBox(newShape, editingShape)) {
				// if (newShape.getShapeString().equals("pen") &&
				// editingShape.getShapeString().equals("eraser"))

				if (pointsShape.contains(newShape.getShapeString())
						&& pointsShape.contains(editingShape.getShapeString())) {
					// 逐点比较
					if (checkPointByPoint(newShape, editingShape)) {
						return false;
					}
				}
				else if (newShape.getShapeString().equals("text")) {
					return false;
				}
				else {
					// 如果是其他情形，进一步用Area交叉检查
					if (checkShapeIntersection(newShape, editingShape)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean overlapBoundingBox(CanvasShape shape1, CanvasShape shape2) {
		// 计算两个形状的边界框是否重叠
		Rectangle2D rect1 = shape1.toShape().getBounds2D();
		Rectangle2D rect2 = shape2.toShape().getBounds2D();
		// System.out.println(rect1);
		// System.out.println(rect2);

		return rect1.intersects(rect2) || rect2.intersects(rect1);
	}

	private boolean checkPointByPoint(CanvasShape newShape, CanvasShape existingShape) {
		// 逐点比较，适用于双方均是纯点型(“笔画”和“橡皮”)的冲突检测
		// Shape shape1Shape = newShape.toShape();
		Shape shape2Shape = existingShape.toShape();
		for (Point2D point : newShape.getPoints()) {
			if (shape2Shape.contains(point)) {
				return false;
			}
		}

		return true; // 示例中默认返回false
	}

	private boolean checkShapeIntersection(CanvasShape shape1, CanvasShape shape2) {
		// 使用Area类来判断形状间的几何交叉(二者即使是全包含关系也不允许)
		Area area1 = new Area(shape1.toShape());
		Area area2 = new Area(shape2.toShape());
		area1.intersect(area2);
		return !area1.isEmpty();
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
