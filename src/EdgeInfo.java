import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class EdgeInfo {
    private int startIndex;
    private int endIndex;
    private Line line;
    private Color color;

    public EdgeInfo(int startIndex, int endIndex, Line line) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.line = line;
        this.color = Color.BLACK;
    }

    // сеттеры
    public void setStartIndex(int startIndex) { this.startIndex = startIndex; }
    public void setEndIndex(int endIndex) { this.endIndex = endIndex; }
    public void setColor(Color color) {this.color = color; }

    // геттеры
    public int getStartIndex() { return startIndex; }
    public int getEndIndex() { return endIndex; }
    public Line getLine() { return line; }

}