import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;

public class GraphState {
    public final List<VertexData> verticesData;
    public final List<EdgeData> edgesData;

    public GraphState(List<Circle> vertices, List<EdgeInfo> edges) {
        this.verticesData = new ArrayList<>();
        for (Circle vertex : vertices) {
            verticesData.add(new VertexData(
                    vertex.getCenterX(),
                    vertex.getCenterY(),
                    vertex.getRadius()
            ));
        }
        this.edgesData = new ArrayList<>();
        for (EdgeInfo edge : edges) {
            edgesData.add(new EdgeData(
                    edge.getStartIndex(),
                    edge.getEndIndex(),
                    (Color) edge.getLine().getStroke() // сохраняем цвет ребра
            ));
        }
    }

    public static class VertexData {
        public double x, y, radius;
        public VertexData(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    public static class EdgeData {
        public int startIndex, endIndex;
        public Color color;

        public EdgeData(int start, int end, Color color) {
            this.startIndex = start;
            this.endIndex = end;
            this.color = color;
        }
    }
}
