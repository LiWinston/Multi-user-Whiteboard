package Service;

import WBSYS.CanvasShape;
import whiteboard.Whiteboard;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;

public class Utils {

    //构造点-protobuf点的缓存，加速编码
    private static final HashMap<Point2D, Whiteboard.point> pointCache = new HashMap<>();

    public static ArrayList<Point2D> protoPointsToArrayList(List<Whiteboard.point> list){
        ArrayList<Point2D> points = new ArrayList<>(list.size());
        for (Whiteboard.point point : list) {
            points.add(new Point2D.Double(point.getX(), point.getY()));
        }
        return points;
    };

    public static whiteboard.Whiteboard.point convertToPointProto(Point2D point) {
        return pointCache.computeIfAbsent(point, p -> whiteboard.Whiteboard.point.newBuilder()
                .setX(p.getX())
                .setY(p.getY())
                .build());
    }

    public static Whiteboard._CanvasShape shape2ProtoShape(WBSYS.CanvasShape canvasShape) {
        var points = canvasShape.getPoints();
        return Whiteboard._CanvasShape.newBuilder().
                setShapeString(canvasShape.getShapeString()).
                setColor(String.valueOf(canvasShape.getColor().getRGB())).
                addX(canvasShape.getX1()).addX(canvasShape.getX2()).addX(canvasShape.getY1()).addX(canvasShape.getY2()).
                setText(canvasShape.getText() == null ? "" : canvasShape.getText()).
                setFill(canvasShape.isFill()).
                setUsername(canvasShape.getUsername()).
                addAllPoints(points == null ? List.of() : points
                        .stream()
                        .map(Utils::convertToPointProto)
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

    public static Whiteboard._CanvasShapeList shapes2ProtoShapes(Iterable<CanvasShape> shapes) {
        Whiteboard._CanvasShapeList.Builder builder = Whiteboard._CanvasShapeList.newBuilder();
        for (CanvasShape shape : shapes) {
            builder.addShapes(shape2ProtoShape(shape));
        }
        return builder.build();
    }

    public static Collection<CanvasShape> protoShapes2Shapes(Whiteboard._CanvasShapeList _shapes) {
        return _shapes.getShapesList().stream().map(Utils::protoShape2Shape).toList();
    }
}
