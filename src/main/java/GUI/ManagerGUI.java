package GUI;

import javax.swing.event.ChangeEvent;
import WBSYS.CanvasShape;
import WBSYS.Properties;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import io.grpc.ManagedChannel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static Service.Utils.shape2ProtoShape;
import static WBSYS.Properties.chatMessageFormat;
import static java.lang.System.err;

public class ManagerGUI implements IClient, MouseListener, MouseMotionListener, ActionListener, WindowListener {

	private final JFrame managerFrame;

	private final String[] shapes = { "pen", "line", "circle", "oval", "rectangle", "eraser", "text" };

	private final String username;

	private final int portNumber;

	private final String WBName;

	private final WhiteBoard wb;

	private final String IpAddress;

	private final ManagedChannel channel;

	private int ipAddress;

	private String currentShapeType = "pen";

	private Color color = Color.BLACK;

	private int x1, x2, y1, y2;

	private ConcurrentLinkedDeque<Point2D> pointQ;

	private Graphics2D canvasGraphics;

	private boolean isFill = false;

	volatile SettableFuture<Boolean> futurePreviewAccept;

	File canvasFile;

	public ManagerGUI(WhiteBoard whiteBoard, String IpAddress, String port, String WBName, ManagedChannel channel) {
		initComponents();
		canvasFile = null;
		this.wb = whiteBoard;
		username = "Manager";
		this.WBName = WBName;
		this.IpAddress = IpAddress;
		this.channel = channel;

		this.portNumber = Integer.parseInt(port);
		nameLabel.setText(WBName);
		IpLabel.setText(IpAddress);
		portLabel.setText(port);
		managerFrame = new JFrame();
		managerFrame.setBounds(150, 100, 1490, 800);
		managerFrame.setResizable(true);
		managerFrame.add(managerPanel);
		managerFrame.setResizable(false);
		managerPanel.setComponentZOrder(canvasPanel, 1);
		managerPanel.setComponentZOrder(chatPanel, 2);
		setColorChooser();
		setStrokeCB();
		setShapeButtons();
		setSendButton();
		setKickButton();
		setFillButton();
		setFileButtons();
		chatTA.setAutoscrolls(true);
		sendTextField.setAutoscrolls(true);
		userTA.setAutoscrolls(true);
		if (canvasGraphics == null) {
			canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
		}

		canvasPanel.addMouseListener(this);
		canvasPanel.addMouseMotionListener(this);
		managerFrame.addWindowListener(this);
		managerFrame.pack();
		managerFrame.setVisible(true);
	}

	private void setFileButtons() {
		newFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				wb.newFile();

			}
		});
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				jFileChooser.setDialogTitle("please choose file path to open");

				if (JFileChooser.APPROVE_OPTION == jFileChooser.showOpenDialog(managerFrame)) {
					File openFile = jFileChooser.getSelectedFile();
					if (openFile != null) {

						try (FileInputStream fileInputStream = new FileInputStream(openFile);
								ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

							Object readObject = objectInputStream.readObject();
							if (readObject instanceof ConcurrentLinkedDeque<?> deque) {
								if (!deque.isEmpty() && deque.peek() instanceof CanvasShape) {
									@SuppressWarnings("unchecked")
									ConcurrentLinkedDeque<CanvasShape> shapes = (ConcurrentLinkedDeque<CanvasShape>) deque;
									wb.openFile(shapes);
									wb.pushMessage(Properties.managerMessage("Open new file: " + openFile.getName()));
									canvasFile = openFile;
									JOptionPane.showMessageDialog(managerFrame, "Open file successfully.");
								}
								else {
									JOptionPane.showMessageDialog(managerFrame,
											"The file does not contain valid shapes.");
								}
							}
							else {
								JOptionPane.showMessageDialog(managerFrame, "The file format is not supported.");
							}
						}
						catch (ClassNotFoundException | IOException ex) {
							JOptionPane.showMessageDialog(managerFrame, "Cannot open this file, try a correct file.");
						}

						// try {
						// FileInputStream fileInputStream = new
						// FileInputStream(canvasFile);
						// ObjectInputStream objectInputStream = new
						// ObjectInputStream(fileInputStream);
						// ConcurrentLinkedDeque<CanvasShape> shapes =
						// (ConcurrentLinkedDeque<CanvasShape>)
						// objectInputStream.readObject();
						// wb.openFile(shapes);
						// objectInputStream.close();
						// fileInputStream.close();
						// JOptionPane.showMessageDialog(managerFrame, "open file
						// successfully");
						// } catch (ClassNotFoundException | IOException
						// fileNotFoundException) {
						// JOptionPane.showMessageDialog(managerFrame, "can not open this
						// file, try a correct file.");
						// }
					}
				}
			}
		});
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (canvasFile == null) {
					// save to a new file and set value to canvasFile
					JFileChooser jFileChooser = new JFileChooser();
					jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					jFileChooser.setDialogTitle("please choose file path to save");
					if (JFileChooser.APPROVE_OPTION == jFileChooser.showSaveDialog(managerFrame)) {
						canvasFile = jFileChooser.getSelectedFile();
						if (canvasFile != null) {
							if (canvasFile.exists()) {
								int response = JOptionPane.showConfirmDialog(managerFrame,
										"The file already exists. Do you want to replace it?", "Confirm Overwrite",
										JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
								if (response != JOptionPane.YES_OPTION) {
									return;
								}
							}
							try {
								FileOutputStream fileOutputStream = new FileOutputStream(canvasFile);
								ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
								objectOutputStream.writeObject(wb.getLocalShapeQ());
								objectOutputStream.close();
								fileOutputStream.close();
								JOptionPane.showMessageDialog(managerFrame, "file saved.");
							}
							catch (IOException fileNotFoundException) {
								JOptionPane.showMessageDialog(managerFrame, "File save error, try again..");
							}
						}
					}
				}
				else {
					// save to the originally opened file
					try {
						FileOutputStream fileOutputStream = new FileOutputStream(canvasFile);
						ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
						objectOutputStream.writeObject(wb.getLocalShapeQ());
						objectOutputStream.close();
						fileOutputStream.close();
						JOptionPane.showMessageDialog(managerFrame, canvasFile.getName() + " : Saved.");
					}
					catch (IOException fileNotFoundException) {
						JOptionPane.showMessageDialog(managerFrame, "File save error, try again..");
					}

				}
			}
		});
		saveAsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				jFileChooser.setDialogTitle("please choose file path to save");
				File saveAsFile = null;
				if (JFileChooser.APPROVE_OPTION == jFileChooser.showSaveDialog(managerFrame)) {
					saveAsFile = jFileChooser.getSelectedFile();
					if (saveAsFile != null) {
						if (saveAsFile.exists()) {
							System.out.println("saveAsFile = " + saveAsFile);
							System.out.println("canvasFile = " + canvasFile);
							// if (saveAsFile.getAbsoluteFile() ==
							// canvasFile.getAbsoluteFile()) {
							try {
								if (Files.isSameFile(saveAsFile.toPath(), canvasFile.toPath())) {
									// Select current file
									JOptionPane.showMessageDialog(managerFrame,
											"Selected current file, it will be replaced.");
								}
								else {
									// Select a file that already exists other than
									// current file
									int response = JOptionPane.showConfirmDialog(managerFrame,
											"The file already exists. Do you want to replace it?", "Confirm Overwrite",
											JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
									if (response != JOptionPane.YES_OPTION) {
										return;
									}
								}
							}
							catch (IOException ex) {
								throw new RuntimeException(ex);
							}
						}
						try {
							FileOutputStream fileOutputStream = new FileOutputStream(saveAsFile);
							ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
							objectOutputStream.writeObject(wb.getLocalShapeQ());
							objectOutputStream.close();
							fileOutputStream.close();
							JOptionPane.showMessageDialog(managerFrame, "file saved.");
						}
						catch (IOException fileNotFoundException) {
							JOptionPane.showMessageDialog(managerFrame, "File save error, try again..");
						}
					}

				}
			}
		});
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				wb.managerClose();
				System.exit(0);

			}
		});
	}

	private void setColorChooser() {
		colorLabel.setOpaque(true);
		colorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				color = JColorChooser.showDialog(managerFrame, "choose color", null);
				if (color == null) {
					return;
				}
				colorLabel.setBackground(color);
			}
		});
	}

	private void setStrokeCB() {
		strokeCB.addItem(1);
		strokeCB.addItem(2);
		strokeCB.addItem(3);
		strokeCB.addItem(5);
		strokeCB.addItem(7);
		strokeCB.addItem(9);
		strokeCB.addItem(11);
		strokeCB.addItem(26);
		strokeCB.addItem(52);
	}

	private void setSendButton() {
		SendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String message = sendTextField.getText();
				if (message != null) {

					wb.pushMessage(chatMessageFormat(username, message));
					sendTextField.setText(null);

				}
				else {
					JOptionPane.showMessageDialog(managerFrame, "plz input your message to send");
				}

			}
		});
	}

	private void setFillButton() {
		fillButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isFill) {
					isFill = false;
					fillButton.setBackground(Color.WHITE);
				}
				else {
					isFill = true;
					fillButton.setBackground(Color.LIGHT_GRAY);
				}
			}
		});
	}

	private void setKickButton() {
		kickButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String username = JOptionPane
					.showInputDialog(managerFrame, "input a username", "kick", JOptionPane.PLAIN_MESSAGE, null, null,
							null)
					.toString();

				wb.removePeer(username);

			}
		});
	}

	@SuppressWarnings("SynchronizeOnNonFinalField")
	public void drawCanvasShape(CanvasShape canvasShape) {
		SwingUtilities.invokeLater(() -> {

			if (canvasGraphics == null) {
				canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
			}

			String shapeType = canvasShape.getShapeString();
			int x1 = canvasShape.getX1();
			int y1 = canvasShape.getY1();
			int x2 = canvasShape.getX2();
			int y2 = canvasShape.getY2();
			Color shapeColor = canvasShape.getColor();
			String username = canvasShape.getUsername();
			int strokeInt = canvasShape.getStrokeInt();
			Stroke stroke = new BasicStroke(strokeInt);

			canvasGraphics.setPaint(shapeColor);
			switch (shapeType) {
				case "line" -> {
					canvasGraphics.setStroke(stroke);
					canvasGraphics.drawLine(x1, y1, x2, y2);
				}
				case "circle", "oval", "rectangle" -> {
					canvasGraphics.setStroke(stroke);
					int height = Math.abs(y2 - y1);
					int width = Math.abs(x2 - x1);
					if (canvasShape.isFill()) {
						switch (shapeType) {
							case "circle":
								canvasGraphics.fillOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, height),
										Math.max(width, height));
								break;
							case "oval":
								canvasGraphics.fillOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
								break;
							case "rectangle":
								canvasGraphics.fillRect(Math.min(x1, x2), Math.min(y1, y2), width, height);
								break;
						}
					}
					else {
						switch (shapeType) {
							case "circle":
								canvasGraphics.drawOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, height),
										Math.max(width, height));
								break;
							case "oval":
								canvasGraphics.drawOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
								break;
							case "rectangle":
								canvasGraphics.drawRect(Math.min(x1, x2), Math.min(y1, y2), width, height);
								break;
						}
					}
				}
				case "text" -> {
					int fontSize = canvasShape.getStrokeInt() * 2 + 10;
					drawString(canvasShape.getText(), x1, y1, 1080, fontSize);// hardcoded
																				// width
				}
				case "pen", "eraser" -> {
					ArrayList<Point2D> points = canvasShape.getPoints();
					canvasGraphics.setStroke(stroke);
					if (points != null && points.size() > 1) {
						for (int i = 0; i < points.size() - 1; i++) {
							int x3 = (int) points.get(i).getX();
							int y3 = (int) points.get(i).getY();
							int x4 = (int) points.get(i + 1).getX();
							int y4 = (int) points.get(i + 1).getY();
							canvasGraphics.setPaint(shapeColor);
							canvasGraphics.drawLine(x3, y3, x4, y4);
						}
					}
					else if ((points != null && points.size() == 1)) {
						int x = (int) points.getFirst().getX();
						int y = (int) points.getFirst().getY();
						canvasGraphics.drawLine(x, y, x, y);
					}
				}
			}

			// restore to chosen
			canvasGraphics.setStroke(new BasicStroke(Integer.parseInt(strokeCB.getSelectedItem().toString())));

		});
	}

	public void drawString(String text, int x, int y, int canvasWidth, int fontSize) {
		if (text == null || text.trim().isEmpty()) {
			return; // 避免处理空字符串或只有空格的字符串
		}
		text = text.substring(0, Math.min(text.length(), 20000)).trim(); // 限制文本长度

		Font font = new Font("Arial", Font.PLAIN, fontSize);
		canvasGraphics.setFont(font);
		canvasGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		FontRenderContext frc = canvasGraphics.getFontRenderContext();
		int availableWidth = canvasWidth - x;

		String[] words = text.split("\\s+");
		ArrayList<String> lines = new ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		double lineWidth = 0;

		for (String word : words) {
			// 计算当前单词的宽度
			double wordWidth = new TextLayout(word, font, frc).getBounds().getWidth();

			// 检查是否应该新起一行
			if (lineWidth + (currentLine.length() > 0 ? font.getStringBounds(" ", frc).getWidth() : 0)
					+ wordWidth > availableWidth) {
				if (currentLine.length() > 0) {
					lines.add(currentLine.toString());
					currentLine = new StringBuilder();
				}
				// 处理长单词换行问题
				if (wordWidth > availableWidth) {
					// 如果单词长度超过整行，拆分单词
					lines.add(word.substring(0, word.length() / 2));
					word = word.substring(word.length() / 2);
					wordWidth = new TextLayout(word, font, frc).getBounds().getWidth();
				}
				currentLine.append(word);
				lineWidth = wordWidth;
			}
			else {
				if (currentLine.length() > 0) {
					currentLine.append(" ");
					lineWidth += font.getStringBounds(" ", frc).getWidth();
				}
				currentLine.append(word);
				lineWidth += wordWidth;
			}
		}

		if (currentLine.length() > 0) {
			lines.add(currentLine.toString());
		}

		// 绘制文本
		int lineHeight = canvasGraphics.getFontMetrics().getHeight();
		int currentY = y;
		for (String line : lines) {
			canvasGraphics.drawString(line, x, currentY);
			currentY += lineHeight;
		}
	}

	private void setShapeButtons() {
		JButton[] buttons = { penButton, lineButton, circleButton, ovalButton, rectButton, earserButton, textButton };
		for (int i = 0; i < buttons.length; i++) {
			int index = i;
			buttons[i].addActionListener(e -> {
				chooseToolButton(index);
				currentShapeType = shapes[index];
			});
		}
	}

	private void chooseToolButton(int index) {
		JButton[] buttons = { penButton, lineButton, circleButton, ovalButton, rectButton, earserButton, textButton };
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].setBackground(i == index ? Color.lightGray : Color.WHITE);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (canvasGraphics == null) {
			canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
		}
		wb.reportUpdEditing("add", username);

		x1 = e.getX();
		y1 = e.getY();
		if (pointQ == null) {
			pointQ = new ConcurrentLinkedDeque<>();
		}
		else {
			pointQ.clear();
		}

		if (currentShapeType.equals("text")) {
			if (Properties.RepeatTextPreviewSending) {
				// new Thread(() -> { // 创建新线程以避免UI阻塞
				// CanvasShape tmp = new CanvasShape(currentShapeType, Color.GRAY, x1 - 5,
				// x1 + 5, y1 - 5, y1 + 5,
				// Integer.parseInt(strokeCB.getSelectedItem().toString()));
				// tmp.setText("Other user is typing...");
				// if (wb.previewTmpStream == null) {
				// futurePreviewAccept = wb.sBeginPushShape();
				// }
				// for (int i = 0; i < 100; i++) { // 循环发送10次
				// if (wb.previewTmpStream != null) {
				// wb.previewTmpStream.onNext(shape2ProtoShape(tmp)); // 发送消息
				// }
				// try {
				// Thread.sleep(20); // 等待200毫秒
				// } catch (InterruptedException f) {
				// System.err.println("Sending thread was interrupted.");
				// break;
				// }
				// }
				// }).start(); // 启动线程
			}
			else {
				int strokeInShape = Integer.parseInt(Objects.requireNonNull(strokeCB.getSelectedItem()).toString());
				CanvasShape tmp = new CanvasShape(currentShapeType, color, x1 - strokeInShape / 2,
						x1 + strokeInShape / 2, y1 - strokeInShape / 2, y1 + strokeInShape / 2,
						Integer.parseInt(strokeCB.getSelectedItem().toString()));
				tmp.setText("User is typing...");
				tmp.setStrokeInt(strokeInShape);
				if (wb.previewTmpStream == null) {
					futurePreviewAccept = wb.sBeginPushShape();
				}
				wb.previewTmpStream.onNext(shape2ProtoShape(tmp)); // 发送消息

			}
		}
		else {
			if (currentShapeType.equals("pen") || currentShapeType.equals("eraser")) {
				pointQ.add(new Point(x1, y1));
				Color tempColor = currentShapeType.equals("pen") ? color : Color.white;
				CanvasShape tmp = new CanvasShape(currentShapeType, tempColor, username, new ArrayList<>(pointQ),
						Integer.parseInt(strokeCB.getSelectedItem().toString()));
				drawCanvasShape(tmp);
				if (wb.previewTmpStream == null) {
					futurePreviewAccept = wb.sBeginPushShape();
				}
				wb.previewTmpStream.onNext(shape2ProtoShape(tmp));
			}

			if (currentShapeType.equals("oval") || currentShapeType.equals("circle")
					|| currentShapeType.equals("rectangle")) {
				if (wb.previewTmpStream == null) {
					futurePreviewAccept = wb.sBeginPushShape();
				}
				int strokeInShape = Integer.parseInt(Objects.requireNonNull(strokeCB.getSelectedItem()).toString());
				CanvasShape tmp = new CanvasShape(currentShapeType, color, x1, x1 + strokeInShape, y1,
						y1 + strokeInShape, strokeInShape);
				drawCanvasShape(tmp);
				if (wb.previewTmpStream == null) {
					futurePreviewAccept = wb.sBeginPushShape();
				}
				wb.previewTmpStream.onNext(shape2ProtoShape(tmp));
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (wb.previewTmpStream != null) {
			wb.previewTmpStream.onCompleted();
			wb.previewTmpStream = null;
		}
		Futures.addCallback(futurePreviewAccept, new FutureCallback<Boolean>() {
			@Override
			public void onSuccess(Boolean result) {
				// 调用 SwingUtilities 确保 UI 更新在正确的线程执行
				SwingUtilities.invokeLater(() -> {
					if (Boolean.TRUE.equals(result)) {
						System.out.println("Preview accepted");
						conductPushShape();
					}
					else {
						System.out.println("Preview rejected");
						// 进行拒绝后的操作，如不 pushShape
						reDraw();
					}
				});
			}

			private void conductPushShape() {
				try {
					if (futurePreviewAccept.get()) {
						System.out.println("preview accepted");
						// 如果被接受，就pushShape
						wb.reportUpdEditing("remove", username);

						x2 = e.getX();
						y2 = e.getY();

						int strokeInShape = Integer.parseInt(strokeCB.getSelectedItem().toString());

						CanvasShape canvasShape;
						if (currentShapeType.equals("pen") || currentShapeType.equals("eraser")) {
							Color tempColor = color;
							if (currentShapeType.equals("eraser")) {
								tempColor = Color.white;
							}
							canvasShape = new CanvasShape(currentShapeType, tempColor, username,
									new ArrayList<>(pointQ), strokeInShape);
						}
						else if (currentShapeType.equals("text")) {
							try {
								String texts = JOptionPane
									.showInputDialog(managerFrame, "input your text", "text", JOptionPane.PLAIN_MESSAGE,
											null, null, null)
									.toString();
								// if (texts.isEmpty()) {
								// wb.requestForceClearTmp();
								// return;
								// }
								canvasShape = new CanvasShape(currentShapeType, color, x1, x2, y1, y2, strokeInShape);
								canvasShape.setText(texts);
								canvasShape.setStrokeInt(Integer.parseInt(strokeCB.getSelectedItem().toString()));
							}
							catch (Exception ex) {
								canvasShape = new CanvasShape(currentShapeType, color, x1, x2, y1, y2, strokeInShape);
								canvasShape.setText("");
								// wb.requestForceClearTmp();
								// return;
							}
						}
						else {
							// 起终点可界定的图形
							// 若按下没动，设为笔触大小的相应形
							x2 = x2 == x1 ? x1 + strokeInShape : x2;
							y2 = y2 == y1 ? y1 + strokeInShape : y2;
							canvasShape = new CanvasShape(currentShapeType, color, x1, x2, y1, y2, strokeInShape);
						}

						canvasShape.setFill(isFill);
						if (canvasShape.getShapeString().equals("text") && canvasShape.getText().isEmpty()) {
							reDraw();// 让pen落实到画布上
							wb.pushShape(canvasShape);
						}
						else {
							wb.getLocalShapeQ().add(canvasShape);
							reDraw();// 让pen落实到画布上
							wb.pushShape(canvasShape);
						}
					}
					else {
						System.out.println("preview rejected");
						wb.requestForceClearTmp();
						// 如果被拒绝，就不pushShape
						// 且请求删除临时图形
						reDraw();
					}
				}
				catch (InterruptedException ex) {
					err.println(ex);
				}
				catch (ExecutionException ex) {
					err.println(ex);
				}
				futurePreviewAccept = null;
			}

			@Override
			public void onFailure(Throwable t) {
				// 处理错误
				System.err.println("Error during preview: " + t.getMessage());
				// 可以在这里进行错误处理相关的 UI 更新
			}
		}, Executors.newSingleThreadExecutor());
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		CanvasShape tmp;
		canvasPanel.repaint();
		reDraw();
		int x3 = e.getX();
		int y3 = e.getY();
		int x4 = x1;
		int y4 = y1;
		// canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
		Stroke tempStroke = new BasicStroke(Integer.parseInt(strokeCB.getSelectedItem().toString()));
		if (currentShapeType.equals("pen") || currentShapeType.equals("eraser")) {
			if (!pointQ.isEmpty()) {
				x4 = (int) pointQ.getLast().getX();
				y4 = (int) pointQ.getLast().getX();
			}
			Color tempColor;
			if (currentShapeType.equals("eraser")) {
				tempColor = Color.gray;
			}
			else {
				tempColor = color;
			}
			// int finalX = x4, finalY = y4;
			// SwingUtilities.invokeLater(() -> {
			// canvasGraphics.setPaint(tempColor);
			// canvasGraphics.setStroke(tempStroke);
			// canvasGraphics.drawLine(finalX, finalY, x3, y3);
			// });
			pointQ.add(new Point(x3, y3));

			tmp = new CanvasShape(currentShapeType, tempColor, username, new ArrayList<>(pointQ),
					Integer.parseInt(strokeCB.getSelectedItem().toString()));
			// SwingUtilities.invokeLater(() -> drawCanvasShape(tmp));
			// wb.tempShapes.put(username, tmp);
		}
		else {
			tmp = new CanvasShape(currentShapeType, color, x1, x3, y1, y3,
					Integer.parseInt(strokeCB.getSelectedItem().toString()));
			SwingUtilities.invokeLater(() -> drawCanvasShape(tmp));
			// wb.tempShapes.put(username, tmp);
		}

		if (currentShapeType.equals("text"))
			return;
		if (wb.previewTmpStream == null) {
			futurePreviewAccept = wb.sBeginPushShape();
		}
		else {
			wb.previewTmpStream.onNext(shape2ProtoShape(tmp));
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void warningFromManager(String message) {
		// JOptionPane.showMessageDialog(managerFrame, message);
	}

	// 不带redraw
	@Override
	public void updateShapes(CanvasShape canvasShape) {
		new Thread() {
			@Override
			public void run() {
				drawCanvasShape(canvasShape);
			}
		}.start();
	}

	@Override
	public void updateChatBox(String chatMessage) {
		new Thread() {
			@Override
			public void run() {
				chatTA.append(chatMessage + "\n");
			}
		}.start();
	}

	@Override
	public boolean approveClientRequest(String username) {
		return false;
	}

	@Override
	public void updatePeerList(ArrayList<String> userlist) {
		new Thread() {
			@Override
			public void run() {
				userTA.setText(null);
				for (String user : userlist) {
					userTA.append(user + "\n");
				}

			}
		}.start();
	}

	public boolean requestFromPeer(String request) {
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(managerFrame,
				request + " wants to join your whiteboard.", "Request from Client", JOptionPane.YES_NO_OPTION);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void reDraw() {
		SwingUtilities.invokeLater(() -> {
			canvasPanel.repaint();
			new Thread(() -> {
				for (CanvasShape shape : wb.getTempShapes().values()) {
					drawCanvasShape(shape);
				}
				for (CanvasShape shape : wb.getLocalShapeQ()) {
					drawCanvasShape(shape);
				}
			}).start();
		});
	}

	@Override
	public void closeWindow(String message) {

	}

	@Override
	public void clearCanvas() {
		wb.getLocalShapeQ().clear();
		wb.getTempShapes().clear();
		canvasPanel.repaint();
	}

	@Override
	public void showEditing() {
		new Thread() {
			@Override
			public void run() {
				StringBuilder stringBuilder = new StringBuilder();
				for (String str : wb.getEditingUser()) {
					stringBuilder.append(str).append(",");
				}

				if (stringBuilder.isEmpty()) {
					stringBuilder.append("Nobody");
				}
				else {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}
				stringBuilder.append(" is editing.");
				editingJLabel.setText(stringBuilder.toString());
			}
		}.start();
	}

	@Override
	public void windowOpened(WindowEvent e) {

	}

	@Override
	public void windowClosing(WindowEvent e) {

		wb.managerClose();
		System.exit(0);

	}

	@Override
	public void windowClosed(WindowEvent e) {
		managerFrame.setVisible(false);
		wb.managerClose();
	}

	@Override
	public void exit() {
		managerFrame.setVisible(false);
	}

	@Override
	public void windowIconified(WindowEvent e) {
		loseFocus();
		reDraw();
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		reDraw();
	}

	@Override
	public void windowActivated(WindowEvent e) {
		reDraw();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		loseFocus();
		reDraw();
	}

	public void loseFocus() {
		if (wb.previewTmpStream != null) {
			wb.previewTmpStream.onCompleted();
			wb.previewTmpStream = null;
		}
		if (futurePreviewAccept != null) {
			futurePreviewAccept.cancel(false);
			futurePreviewAccept = null;
		}
		if (wb.getTempShapes().containsKey(username)) {
			// JOptionPane.showMessageDialog(managerFrame, "Lose focus, your preview will
			// be canceled on your next action.");
			wb.requestForceClearTmp();
		}
	}

	private void ConIntersectCB(ActionEvent e) {
		if (ConIntersectCB.isSelected()) {
			wb.setConIntersect(true);
			JOptionPane.showMessageDialog(managerFrame, "Concurrent intersection is enabled.");
		}
		else {
			wb.setConIntersect(false);
			JOptionPane.showMessageDialog(managerFrame, "Concurrent intersection is disabled.");
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Evaluation license - yongchun
        managerPanel = new JPanel();
        canvasPanel = new JPanel();
        chatPanel = new JPanel();
        usersSP = new JScrollPane();
        userTA = new JTextArea();
        ChatSP = new JScrollPane();
        chatTA = new JTextArea();
        settingBar = new JToolBar();
        newFileButton = new JButton();
        openButton = new JButton();
        saveButton = new JButton();
        saveAsButton = new JButton();
        closeButton = new JButton();
        ConIntersectCB = new JCheckBox();
        hSpacer19 = new JPanel(null);
        var label1 = new JLabel();
        IpLabel = new JLabel();
        var label2 = new JLabel();
        portLabel = new JLabel();
        hSpacer1 = new JPanel(null);
        var label3 = new JLabel();
        nameLabel = new JLabel();
        shapeColorBar = new JToolBar();
        colorButton = new JButton();
        colorLabel = new JLabel();
        penButton = new JButton();
        lineButton = new JButton();
        circleButton = new JButton();
        ovalButton = new JButton();
        rectButton = new JButton();
        earserButton = new JButton();
        textButton = new JButton();
        fillButton = new JButton();
        var label4 = new JLabel();
        strokeCB = new JComboBox();
        kickButton = new JButton();
        editingJLabel = new JLabel();
        SendButton = new JButton();
        sendTextField = new JTextField();

        //======== managerPanel ========
        {
            managerPanel.setMaximumSize(new Dimension(1490, 850));
            managerPanel.setMinimumSize(new Dimension(1490, 850));
            managerPanel.setPreferredSize(new Dimension(1490, 850));
            managerPanel.setBackground(Color.white);
            managerPanel.setName("managerPanel");
            managerPanel.setBorder ( new javax . swing. border .CompoundBorder ( new javax . swing. border .TitledBorder ( new
            javax . swing. border .EmptyBorder ( 0, 0 ,0 , 0) ,  "JF\u006frmDes\u0069gner \u0045valua\u0074ion" , javax
            . swing .border . TitledBorder. CENTER ,javax . swing. border .TitledBorder . BOTTOM, new java
            . awt .Font ( "D\u0069alog", java .awt . Font. BOLD ,12 ) ,java . awt
            . Color .red ) ,managerPanel. getBorder () ) ); managerPanel. addPropertyChangeListener( new java. beans .
            PropertyChangeListener ( ){ @Override public void propertyChange (java . beans. PropertyChangeEvent e) { if( "\u0062order" .
            equals ( e. getPropertyName () ) )throw new RuntimeException( ) ;} } );
            managerPanel.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));

            //======== canvasPanel ========
            {
                canvasPanel.setBackground(Color.white);
                canvasPanel.setBorder(new TitledBorder(new EtchedBorder(), "Canvas area", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                    UIManager.getFont("TitledBorder.font").deriveFont(14f)));
                canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                canvasPanel.setName("canvasPanel");
                canvasPanel.setLayout(new GridLayoutManager(1, 1, new Insets(1, 1, 1, 1), -1, -1));
            }
            managerPanel.add(canvasPanel, new GridConstraints(2, 0, 2, 1,
                GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                new Dimension(1130, 700), new Dimension(1130, 700), new Dimension(1130, 700)));

            //======== chatPanel ========
            {
                chatPanel.setInheritsPopupMenu(false);
                chatPanel.setName("chatPanel");
                chatPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));

                //======== usersSP ========
                {
                    usersSP.setBorder(new TitledBorder(null, "Users connecting", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                        UIManager.getFont("TitledBorder.font").deriveFont(14f), Color.black));
                    usersSP.setName("usersSP");

                    //---- userTA ----
                    userTA.setEditable(false);
                    userTA.setMargin(new Insets(10, 10, 10, 10));
                    userTA.setFocusable(false);
                    userTA.setName("userTA");
                    usersSP.setViewportView(userTA);
                }
                chatPanel.add(usersSP, new GridConstraints(0, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    new Dimension(350, 220), new Dimension(350, 220), new Dimension(350, 220)));

                //======== ChatSP ========
                {
                    ChatSP.setBorder(new TitledBorder(null, "Chat area", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                        UIManager.getFont("TitledBorder.font").deriveFont(14f)));
                    ChatSP.setName("ChatSP");

                    //---- chatTA ----
                    chatTA.setEditable(false);
                    chatTA.setMaximumSize(new Dimension(350, 460));
                    chatTA.setMinimumSize(new Dimension(350, 460));
                    chatTA.setPreferredSize(new Dimension(350, 460));
                    chatTA.setLineWrap(true);
                    chatTA.setWrapStyleWord(true);
                    chatTA.setName("chatTA");
                    ChatSP.setViewportView(chatTA);
                }
                chatPanel.add(ChatSP, new GridConstraints(1, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                    new Dimension(350, 460), new Dimension(350, 460), null));
            }
            managerPanel.add(chatPanel, new GridConstraints(0, 1, 3, 1,
                GridConstraints.ANCHOR_SOUTHEAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                new Dimension(350, 700), new Dimension(350, 700), new Dimension(350, 700)));

            //======== settingBar ========
            {
                settingBar.setBackground(Color.white);
                settingBar.setEnabled(true);
                settingBar.setFloatable(false);
                settingBar.setMargin(new Insets(5, 5, 5, 5));
                settingBar.setBorder(new TitledBorder(LineBorder.createBlackLineBorder(), ""));
                settingBar.setName("settingBar");

                //---- newFileButton ----
                newFileButton.setText("newFile");
                newFileButton.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                newFileButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                newFileButton.setPreferredSize(new Dimension(61, 18));
                newFileButton.setName("newFileButton");
                settingBar.add(newFileButton);

                //---- openButton ----
                openButton.setText("open");
                openButton.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                openButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                openButton.setPreferredSize(new Dimension(61, 18));
                openButton.setName("openButton");
                settingBar.add(openButton);

                //---- saveButton ----
                saveButton.setText("save");
                saveButton.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                saveButton.setPreferredSize(new Dimension(61, 18));
                saveButton.setName("saveButton");
                settingBar.add(saveButton);

                //---- saveAsButton ----
                saveAsButton.setText("saveAs");
                saveAsButton.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                saveAsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                saveAsButton.setPreferredSize(new Dimension(61, 18));
                saveAsButton.setName("saveAsButton");
                settingBar.add(saveAsButton);

                //---- closeButton ----
                closeButton.setText("close");
                closeButton.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                closeButton.setPreferredSize(new Dimension(61, 18));
                closeButton.setName("closeButton");
                settingBar.add(closeButton);

                //---- ConIntersectCB ----
                ConIntersectCB.setText("Allow Concurrent Intersection");
                ConIntersectCB.setName("ConIntersectCB");
                ConIntersectCB.addActionListener(e -> ConIntersectCB(e));
                settingBar.add(ConIntersectCB);

                //---- hSpacer19 ----
                hSpacer19.setForeground(Color.white);
                hSpacer19.setBackground(Color.white);
                hSpacer19.setName("hSpacer19");
                settingBar.add(hSpacer19);

                //---- label1 ----
                label1.setText("Host : ");
                label1.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                label1.setName("label1");
                settingBar.add(label1);

                //---- IpLabel ----
                IpLabel.setText("");
                IpLabel.setName("IpLabel");
                settingBar.add(IpLabel);

                //---- label2 ----
                label2.setText("  Port : ");
                label2.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                label2.setName("label2");
                settingBar.add(label2);

                //---- portLabel ----
                portLabel.setText("");
                portLabel.setName("portLabel");
                settingBar.add(portLabel);

                //---- hSpacer1 ----
                hSpacer1.setForeground(Color.white);
                hSpacer1.setBackground(Color.white);
                hSpacer1.setName("hSpacer1");
                settingBar.add(hSpacer1);

                //---- label3 ----
                label3.setText(" ");
                label3.setFont(new Font("Droid Sans Mono", Font.BOLD, 12));
                label3.setName("label3");
                settingBar.add(label3);

                //---- nameLabel ----
                nameLabel.setText("");
                nameLabel.setName("nameLabel");
                settingBar.add(nameLabel);
            }
            managerPanel.add(settingBar, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                new Dimension(1130, 30), new Dimension(1130, 30), new Dimension(1130, 30)));

            //======== shapeColorBar ========
            {
                shapeColorBar.setBackground(Color.white);
                shapeColorBar.setMargin(new Insets(5, 5, 5, 5));
                shapeColorBar.setBorder(new TitledBorder(LineBorder.createBlackLineBorder(), ""));
                shapeColorBar.setFloatable(false);
                shapeColorBar.setMaximumSize(new Dimension(802, 32));
                shapeColorBar.setMinimumSize(new Dimension(802, 32));
                shapeColorBar.setPreferredSize(new Dimension(794, 32));
                shapeColorBar.setName("shapeColorBar");

                //---- colorButton ----
                colorButton.setMaximumSize(new Dimension(108, 40));
                colorButton.setMinimumSize(new Dimension(108, 40));
                colorButton.setPreferredSize(new Dimension(108, 40));
                colorButton.setText("choose Color");
                colorButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                colorButton.setName("colorButton");
                shapeColorBar.add(colorButton);

                //---- colorLabel ----
                colorLabel.setBackground(Color.black);
                colorLabel.setMaximumSize(new Dimension(40, 40));
                colorLabel.setMinimumSize(new Dimension(40, 40));
                colorLabel.setOpaque(true);
                colorLabel.setPreferredSize(new Dimension(40, 40));
                colorLabel.setText("");
                colorLabel.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                colorLabel.setName("colorLabel");
                shapeColorBar.add(colorLabel);

                //---- penButton ----
                penButton.setText("pen");
                penButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                penButton.setMaximumSize(new Dimension(54, 30));
                penButton.setMinimumSize(new Dimension(54, 27));
                penButton.setPreferredSize(new Dimension(54, 30));
                penButton.setName("penButton");
                shapeColorBar.add(penButton);

                //---- lineButton ----
                lineButton.setText("line");
                lineButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                lineButton.setPreferredSize(new Dimension(54, 30));
                lineButton.setMaximumSize(new Dimension(54, 30));
                lineButton.setMinimumSize(new Dimension(54, 27));
                lineButton.setName("lineButton");
                shapeColorBar.add(lineButton);

                //---- circleButton ----
                circleButton.setText("circle");
                circleButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                circleButton.setPreferredSize(new Dimension(54, 30));
                circleButton.setMaximumSize(new Dimension(54, 30));
                circleButton.setName("circleButton");
                shapeColorBar.add(circleButton);

                //---- ovalButton ----
                ovalButton.setText("oval");
                ovalButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                ovalButton.setPreferredSize(new Dimension(54, 30));
                ovalButton.setMaximumSize(new Dimension(54, 30));
                ovalButton.setMinimumSize(new Dimension(54, 27));
                ovalButton.setName("ovalButton");
                shapeColorBar.add(ovalButton);

                //---- rectButton ----
                rectButton.setText("rect");
                rectButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                rectButton.setPreferredSize(new Dimension(54, 30));
                rectButton.setMaximumSize(new Dimension(54, 30));
                rectButton.setMinimumSize(new Dimension(54, 27));
                rectButton.setName("rectButton");
                shapeColorBar.add(rectButton);

                //---- earserButton ----
                earserButton.setText("earser");
                earserButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                earserButton.setPreferredSize(new Dimension(54, 30));
                earserButton.setMaximumSize(new Dimension(54, 30));
                earserButton.setName("earserButton");
                shapeColorBar.add(earserButton);

                //---- textButton ----
                textButton.setText("text");
                textButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                textButton.setPreferredSize(new Dimension(54, 30));
                textButton.setMaximumSize(new Dimension(54, 30));
                textButton.setMinimumSize(new Dimension(54, 27));
                textButton.setName("textButton");
                shapeColorBar.add(textButton);

                //---- fillButton ----
                fillButton.setText("Fill");
                fillButton.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                fillButton.setPreferredSize(new Dimension(54, 30));
                fillButton.setMaximumSize(new Dimension(54, 30));
                fillButton.setMinimumSize(new Dimension(54, 27));
                fillButton.setName("fillButton");
                shapeColorBar.add(fillButton);

                //---- label4 ----
                label4.setText("Stroke");
                label4.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                label4.setName("label4");
                shapeColorBar.add(label4);

                //---- strokeCB ----
                strokeCB.setMaximumSize(new Dimension(78, 30));
                strokeCB.setMinimumSize(new Dimension(78, 30));
                strokeCB.setToolTipText("stroke");
                strokeCB.setFont(new Font("Droid Sans Mono Dotted", Font.BOLD, 12));
                strokeCB.setName("strokeCB");
                shapeColorBar.add(strokeCB);

                //---- kickButton ----
                kickButton.setText("kick");
                kickButton.setMaximumSize(new Dimension(100, 28));
                kickButton.setMinimumSize(new Dimension(100, 28));
                kickButton.setPreferredSize(new Dimension(100, 28));
                kickButton.setName("kickButton");
                shapeColorBar.add(kickButton);

                //---- editingJLabel ----
                editingJLabel.setText("");
                editingJLabel.setName("editingJLabel");
                shapeColorBar.add(editingJLabel);
            }
            managerPanel.add(shapeColorBar, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                new Dimension(1130, 48), new Dimension(1130, 48), new Dimension(1130, 50)));

            //---- SendButton ----
            SendButton.setText("Send");
            SendButton.setName("SendButton");
            managerPanel.add(SendButton, new GridConstraints(4, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null));

            //---- sendTextField ----
            sendTextField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            sendTextField.setName("sendTextField");
            managerPanel.add(sendTextField, new GridConstraints(3, 1, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                new Dimension(350, 100), new Dimension(350, 100), new Dimension(350, 100)));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on

	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Evaluation license - yongchun
    private JPanel managerPanel;
    private JPanel canvasPanel;
    private JPanel chatPanel;
    private JScrollPane usersSP;
    private JTextArea userTA;
    private JScrollPane ChatSP;
    private JTextArea chatTA;
    private JToolBar settingBar;
    private JButton newFileButton;
    private JButton openButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton closeButton;
    private JCheckBox ConIntersectCB;
    private JPanel hSpacer19;
    private JLabel IpLabel;
    private JLabel portLabel;
    private JPanel hSpacer1;
    private JLabel nameLabel;
    private JToolBar shapeColorBar;
    private JButton colorButton;
    private JLabel colorLabel;
    private JButton penButton;
    private JButton lineButton;
    private JButton circleButton;
    private JButton ovalButton;
    private JButton rectButton;
    private JButton earserButton;
    private JButton textButton;
    private JButton fillButton;
    private JComboBox strokeCB;
    private JButton kickButton;
    private JLabel editingJLabel;
    private JButton SendButton;
    private JTextField sendTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

}
