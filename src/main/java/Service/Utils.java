package Service;

import WBSYS.CanvasShape;
import whiteboard.Whiteboard;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Utils {
    public static ArrayList<Point2D> protoPointsToArrayList(List<Whiteboard.point> list){
        ArrayList<Point2D> points = new ArrayList<>();
        for (Whiteboard.point point : list) {
            points.add(new Point2D.Double(point.getX(), point.getY()));
        }
        return points;
    };

    public static whiteboard.Whiteboard.point convertToPointProto(Point point) {
        return whiteboard.Whiteboard.point.newBuilder()
                .setX(point.getX())
                .setY(point.getY())
                .build();
    }

    public static Whiteboard._CanvasShape shape2ProtoShape(WBSYS.CanvasShape canvasShape) {
        return Whiteboard._CanvasShape.newBuilder().
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
                build();
    }

    static CanvasShape protoShape2Shape(Whiteboard._CanvasShape _canvasShape) {
        CanvasShape shape;
        if (_canvasShape.getShapeString().equals("pen") || _canvasShape.getShapeString().equals("eraser")) {
            shape = new CanvasShape(_canvasShape.getShapeString(), new Color(Integer.parseInt(_canvasShape.getColor())), _canvasShape.getUsername(), protoPointsToArrayList(_canvasShape.getPointsList().stream().toList()), _canvasShape.getStrokeInt());
        } else if (_canvasShape.getShapeString().equals("text")) {
            shape = new CanvasShape(_canvasShape.getShapeString(), new Color(Integer.parseInt(_canvasShape.getColor())), _canvasShape.getX(0), _canvasShape.getX(1), _canvasShape.getX(2), _canvasShape.getX(3), _canvasShape.getText(), _canvasShape.getFill(), _canvasShape.getUsername(), _canvasShape.getStrokeInt());
        } else {
            shape = new CanvasShape(_canvasShape.getShapeString(), new Color(Integer.parseInt(_canvasShape.getColor())), _canvasShape.getX(0), _canvasShape.getX(1), _canvasShape.getX(2), _canvasShape.getX(3), _canvasShape.getStrokeInt());
            shape.setUsername(_canvasShape.getUsername());
            shape.setFill(_canvasShape.getFill());
        }
        return shape;
    }
}
