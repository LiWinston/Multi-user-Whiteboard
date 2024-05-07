package GUI;

import WBSYS.CanvasShape;
import WBSYS.WhiteBoard;
import io.grpc.ManagedChannel;
import whiteboard.WhiteBoardServiceGrpc;
import whiteboard.Whiteboard._CanvasShape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

import static WBSYS.parameters.chatMessageFormat;


public class ManagerGUI implements IClient, MouseListener, MouseMotionListener, ActionListener, WindowListener {
    private final JFrame managerFrame;
    private final String[] shapes = {"pen", "line", "circle", "oval", "rectangle", "eraser", "text"};
    private final String username;
    private final int portNumber;
    private final String WBName;
    private final WhiteBoard wb;
    private final String IpAddress;
    private final ManagedChannel channel;
    private final Object blockingStub;
    private int ipAddress;
    private JPanel managerPanel;
    private JPanel canvasPanel;
    private JPanel chatPanel;
    private JTextArea userTA;
    private JTextArea chatTA;
    private JScrollPane usersSP;
    private JScrollPane ChatSP;
    private JToolBar shapeColorBar;
    private JToolBar settingBar;
    private JButton newFileButton;
    private JButton openButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton fillButton;
    private JButton closeButton;
    private JButton colorButton;
    private JLabel colorLabel;
    private JButton penButton;
    private JButton lineButton;
    private JButton circleButton;
    private JButton ovalButton;
    private JButton rectButton;
    private JButton earserButton;
    private JButton textButton;
    private String shapeDrawing = "pen";
    private Color color = Color.BLACK;
    private int x1, x2, y1, y2;
    private JComboBox<Integer> strokeCB;
    private JLabel IpLabel;
    private JLabel portLabel;
    private JButton SendButton;
    private JButton kickButton;
    private JTextField sendTextField;
    private JLabel editingJLabel;
    private JLabel nameLabel;
    private ArrayList<Point> pointArrayList;
    private Graphics2D canvasGraphics;
    private boolean isFill = false;

    public ManagerGUI(WhiteBoard whiteBoard, String IpAddress, String port, String WBName, ManagedChannel channel) {
        initComponents();
        this.wb = whiteBoard;
        username = "Manager";
        this.WBName = WBName;
        this.IpAddress = IpAddress;
        this.channel = channel;
        this.blockingStub = WhiteBoardServiceGrpc.newBlockingStub(channel);

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
                File canvasFile = null;

                if (JFileChooser.APPROVE_OPTION == jFileChooser.showOpenDialog(managerFrame)) {
                    canvasFile = jFileChooser.getSelectedFile();
                    if (canvasFile != null) {
                        try {
                            FileInputStream fileInputStream = new FileInputStream(canvasFile);
                            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                            ArrayList<CanvasShape> shapes = (ArrayList<CanvasShape>) objectInputStream.readObject();
                            wb.openFile(shapes);
                            objectInputStream.close();
                            fileInputStream.close();
                            JOptionPane.showMessageDialog(managerFrame, "open file successfully");
                        } catch (ClassNotFoundException | IOException fileNotFoundException) {
                            JOptionPane.showMessageDialog(managerFrame, "can not open this file, try a correct file.");
                        }
                    }
                }
            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                jFileChooser.setDialogTitle("please choose file path to save");
                File canvasFile = null;
                if (JFileChooser.APPROVE_OPTION == jFileChooser.showSaveDialog(managerFrame)) {
                    canvasFile = jFileChooser.getSelectedFile();
                    if (canvasFile != null) {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(canvasFile);
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                            objectOutputStream.writeObject(wb.getCanvasShapeArrayList());
                            objectOutputStream.close();
                            fileOutputStream.close();
                            JOptionPane.showMessageDialog(managerFrame, "file saved.");
                        } catch (IOException fileNotFoundException) {
                            JOptionPane.showMessageDialog(managerFrame, "File save error, try again..");
                        }
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
                File canvasFile = null;
                if (JFileChooser.APPROVE_OPTION == jFileChooser.showSaveDialog(managerFrame)) {
                    canvasFile = jFileChooser.getSelectedFile();
                    if (canvasFile != null) {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(canvasFile);
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                            objectOutputStream.writeObject(wb.getCanvasShapeArrayList());
                            objectOutputStream.close();
                            fileOutputStream.close();
                            JOptionPane.showMessageDialog(managerFrame, "file saved.");
                        } catch (IOException fileNotFoundException) {
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
        strokeCB.addItem(3);
        strokeCB.addItem(5);
        strokeCB.addItem(7);
        strokeCB.addItem(9);
        strokeCB.addItem(11);
        strokeCB.addItem(15);
        strokeCB.addItem(20);
        strokeCB.addItem(25);
    }

    private void setSendButton() {
        SendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = sendTextField.getText();
                if (message != null) {

                    wb.SynchronizeMessage(chatMessageFormat(username, message));
                    sendTextField.setText(null);

                } else {
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
                } else {
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
                String username = JOptionPane.showInputDialog(managerFrame, "input a username", "kick", JOptionPane.PLAIN_MESSAGE, null, null, null).toString();

                wb.removePeer(username);

            }
        });
    }

    private void drawCanvasShape(CanvasShape canvasShape) {
        String shapeType = canvasShape.getShapeString();
        int x1 = canvasShape.getX1();
        int y1 = canvasShape.getY1();
        int x2 = canvasShape.getX2();
        int y2 = canvasShape.getY2();
        Color shapeColor = canvasShape.getColor();
        String username = canvasShape.getUsername();
        int strokeInt = canvasShape.getStrokeInt();
        Stroke stroke = new BasicStroke(strokeInt);
        canvasGraphics = (Graphics2D) canvasPanel.getGraphics();

        canvasGraphics.setPaint(shapeColor);
        if (shapeType.equals("line")) {
            canvasGraphics.setStroke(stroke);
            canvasGraphics.drawLine(x1, y1, x2, y2);
        } else if (shapeType.equals("circle") || shapeType.equals("oval") || shapeType.equals("rectangle")) {
            canvasGraphics.setStroke(stroke);
            int height = Math.abs(y2 - y1);
            int width = Math.abs(x2 - x1);
            if (canvasShape.isFill()) {
                switch (shapeType) {
                    case "circle":
                        canvasGraphics.fillOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, height), Math.max(width, height));
                        break;
                    case "oval":
                        canvasGraphics.fillOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
                        break;
                    case "rectangle":
                        canvasGraphics.fillRect(Math.min(x1, x2), Math.min(y1, y2), width, height);
                        break;
                }
            } else {
                switch (shapeType) {
                    case "circle":
                        canvasGraphics.drawOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, height), Math.max(width, height));
                        break;
                    case "oval":
                        canvasGraphics.drawOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
                        break;
                    case "rectangle":
                        canvasGraphics.drawRect(Math.min(x1, x2), Math.min(y1, y2), width, height);
                        break;
                }
            }
        } else if (shapeType.equals("text")) {
            int size = canvasShape.getStrokeInt();
            canvasGraphics.setFont(new Font("Times New Roman", Font.PLAIN, size * 2 + 10));
            canvasGraphics.drawString(canvasShape.getText(), x1, y1);
        } else if (shapeType.equals("pen") || shapeType.equals("eraser")) {
            ArrayList<Point> points = canvasShape.getPoints();
            canvasGraphics.setStroke(stroke);
            for (int i = 1; i < points.size(); i++) {
                canvasGraphics.drawLine(points.get(i - 1).x, points.get(i - 1).y, points.get(i).x, points.get(i).y);
            }
        }

    }

    private void setShapeButtons() {
        JButton[] buttons = {penButton, lineButton, circleButton, ovalButton, rectButton, earserButton, textButton};
        for (int i = 0; i < buttons.length; i++) {
            int index = i; // 使用 final 局部变量来解决 Lambda 表达式中的访问问题
            buttons[i].addActionListener(e -> {
                chooseToolButton(index);
                shapeDrawing = shapes[index];
            });
        }
    }

    private void chooseToolButton(int index) {
        JButton[] buttons = {penButton, lineButton, circleButton, ovalButton, rectButton, earserButton, textButton};
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

        wb.SynchronizeEditing(username);

        x1 = e.getX();
        y1 = e.getY();
        if (shapeDrawing.equals("pen") || shapeDrawing.equals("eraser")) {
            pointArrayList = new ArrayList<Point>();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        wb.SynchronizeEditing(null);

        x2 = e.getX();
        y2 = e.getY();

        int strokeInShape = Integer.parseInt(strokeCB.getSelectedItem().toString());

        CanvasShape canvasShape;
        if (shapeDrawing.equals("pen") || shapeDrawing.equals("eraser")) {
            Color tempColor = color;
            if (shapeDrawing.equals("eraser")) {
                tempColor = Color.white;
            }
            canvasShape = new CanvasShape(shapeDrawing, tempColor, pointArrayList, strokeInShape);
        } else if (shapeDrawing.equals("text")) {
            canvasShape = new CanvasShape(shapeDrawing, color, x1, x2, y1, y2, strokeInShape);
            String texts = JOptionPane.showInputDialog(managerFrame, "input your text", "text", JOptionPane.PLAIN_MESSAGE, null, null, null).toString();
            canvasShape.setText(texts);
            canvasShape.setStrokeInt(Integer.parseInt(strokeCB.getSelectedItem().toString()));
        } else {
            canvasShape = new CanvasShape(shapeDrawing, color, x1, x2, y1, y2, strokeInShape);
        }

        canvasShape.setFill(isFill);
        wb.SynchronizeCanvas(canvasShape);
//        blockingStub.synchronizeCanvas(_CanvasShape.newBuilder().setShapeString(canvasShape.getShapeString()).setColor(canvasShape.getColor().getRGB()).setX1(canvasShape.getX1()).setX2(canvasShape.getX2()).setY1(canvasShape.getY1()).setY2(canvasShape.getY2()).setFill(canvasShape.isFill()).setUsername(canvasShape.getUsername()).setStrokeInt(canvasShape.getStrokeInt()).build());

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int x3 = e.getX();
        int y3 = e.getY();
        int x4 = x1;
        int y4 = y1;
        canvasGraphics = (Graphics2D) canvasPanel.getGraphics();
        Stroke tempStroke = new BasicStroke(Integer.parseInt(strokeCB.getSelectedItem().toString()));
        if (shapeDrawing.equals("pen") || shapeDrawing.equals("eraser")) {
            if (pointArrayList.size() > 0) {
                x4 = pointArrayList.get(pointArrayList.size() - 1).x;
                y4 = pointArrayList.get(pointArrayList.size() - 1).y;
            }
            Color tempColor = color;
            if (shapeDrawing.equals("eraser")) {
                tempColor = Color.white;
            }
            canvasGraphics.setPaint(tempColor);
            canvasGraphics.setStroke(tempStroke);
            canvasGraphics.drawLine(x4, y4, x3, y3);
            pointArrayList.add(new Point(x3, y3));
        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void warningFromManager(String message) {

    }

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
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(managerFrame, request + " wants to join your whiteboard.", "Request from Client", JOptionPane.YES_NO_OPTION);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void reDraw() {
        new Thread() {
            @Override
            public void run() {
                ArrayList<CanvasShape> shapeArrayList = null;

                shapeArrayList = wb.getCanvasShapeArrayList();

                for (CanvasShape shape : shapeArrayList) {
                    drawCanvasShape(shape);
                }
            }
        }.start();
    }


    @Override
    public void closeWindow() {

    }

    @Override
    public void clearCanvas() {
        canvasPanel.repaint();
    }

    @Override
    public void showEditing(String username) {
        new Thread() {
            @Override
            public void run() {
                if (username != null) {
                    editingJLabel.setText(username + " is editing.");
                } else {
                    editingJLabel.setText(null);
                }
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
    }

    @Override
    public void exit() {
        managerFrame.setVisible(false);
    }

    @Override
    public void windowIconified(WindowEvent e) {

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
        // not use it
    }
}
