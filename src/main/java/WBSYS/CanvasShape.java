package WBSYS;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;

public class CanvasShape implements Serializable {
    private String shapeString = "";
    private Color color = Color.BLACK;
    private int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
    private String text = "";
    private boolean fill = false;
    private String username = "";
    private ArrayList<Point2D> points;
    private int strokeInt = 1;


    public CanvasShape(String shapeString, Color color, int x1, int x2, int y1, int y2, int strokeInt) {
        this.shapeString = shapeString;
        this.color = color;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.strokeInt = strokeInt;
    }

    public CanvasShape(String shapeString, Color color, int x1, int x2, int y1, int y2, boolean fill, String username, int strokeInt) {
        this.shapeString = shapeString;
        this.color = color;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.fill = fill;
        this.username = username;
        this.strokeInt = strokeInt;
    }


    public CanvasShape(String shapeString, Color color, String username, ArrayList<Point2D> points, int strokeInt) {
        this.shapeString = shapeString;
        this.color = color;
        this.username = username;
        this.points = points;
        this.strokeInt = strokeInt;
    }

    public String getShapeString() {
        return shapeString;
    }

    public void setShapeString(String shapeString) {
        this.shapeString = shapeString;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFill() {
        return fill;
    }

    public void setFill(boolean fill) {
        this.fill = fill;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ArrayList<Point2D> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<Point2D> points) {
        this.points = points;
    }

    public int getStrokeInt() {
        return strokeInt;
    }

    public void setStrokeInt(int strokeInt) {
        this.strokeInt = strokeInt;
    }
}
