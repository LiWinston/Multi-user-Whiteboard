package Service;

import whiteboard.Whiteboard;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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

}
