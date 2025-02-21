import java.io.*;
import java.lang.ProcessBuilder;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {

    private Circle selectedVertex = null; // Для хранения выбранной вершины
    private Pane graphPane; // Панель для размещения вершин и ребер
    private List<Circle> vertices = new ArrayList<>(); // Список вершин
    private List<EdgeInfo> edges = new ArrayList<>(); // Список ребер
    private double vertexSize = 10; // Начальный размер вершин
    private Stack<GraphState> undoStack = new Stack<>(); // Стэк для сохранения предыдущего состояния графа
    private Stack<GraphState> redoStack = new Stack<>(); // Стэк для возвращения в последнее состояние графа

    @Override
    public void start(Stage primaryStage) {
        // Основной контейнер
        BorderPane root = new BorderPane();

        // Панель для графа
        graphPane = new Pane();
        root.setCenter(graphPane);

        // Кнопка для решения задачи о разбиении графа
        Button createMatrixButton = new Button("Показать решение");
        createMatrixButton.setOnAction(event -> {
            // Проверка кратности количества вершин 3
            if (vertices.size() % 3 != 0) {
                showAlert(
                        "Ошибка",
                        "Некорректное количество вершин",
                        "Количество вершин должно быть кратно 3!\nТекущее количество: " + vertices.size()
                );
                return; // Прерываем выполнение
            }

            // Если проверка пройдена, продолжаем
            writeMatrixToFile(createAdjacencyMatrix(vertices), "javaOut.txt");
            openExeAndFindSolution();
        });


        // Кнопка для очищения экрана
        Button cleanScene = new Button("Очистка экрана");
        cleanScene.setOnAction(event -> clearScene());


        // Кнопка для вывода справки
        Image helpIcon = new Image(getClass().getResourceAsStream("/notepad.png")); // Загружаем изображение
        ImageView helpIconView = new ImageView(helpIcon);
        Button helpButton = new Button();
        helpButton.setGraphic(helpIconView);
        helpIconView.setFitWidth(20); // Устанавливаем размер изображения
        helpIconView.setFitHeight(20);
        helpButton.setOnAction(event -> showHelp());

        // Кнопка для установления размера вершин
        Button vertexSizeButton = new Button("Выбрать размер вершины");
        vertexSizeButton.setOnAction(event -> vertexSize = chooseVertexSize());

        // Кнопки для возврашения к предыдущему и последнему состоянию графа
        Button undoButton = new Button("←");
        undoButton.setOnAction(event -> undo());

        Button redoButton = new Button("→");
        redoButton.setOnAction(event -> redo());


        // Всплывающие уведомления при наведении на кнопку
        createMatrixButton.setTooltip(new Tooltip("Кликни на кнопку, \nчтобы увидеть \nна какие треугольники\n разбивается граф"));
        cleanScene.setTooltip(new Tooltip("Кликни на кнопку, \nчтобы очистить экран\nот рёбер и вершин"));
        helpButton.setTooltip(new Tooltip("Кликни на кнопку, \nчтобы посмотреть \nна справочный матриал"));
        vertexSizeButton.setTooltip(new Tooltip("Кликни на кнопку, \nчтобы изменить размер вершин"));
        undoButton.setTooltip(new Tooltip("Отменить последнее действие"));
        redoButton.setTooltip(new Tooltip("Вернуть отмененное действие"));


        // Создаем контейнер для кнопок
        HBox topToolbar = new HBox(10);
        topToolbar.setPadding(new Insets(10)); // Устанавливаем дополнения для вставок
        topToolbar.getChildren().addAll( // Добавляем кнопки в контейнер
                createMatrixButton,
                cleanScene,
                vertexSizeButton,
                undoButton,
                redoButton,
                helpButton
        );
        root.setTop(topToolbar); // Добавляем в основной контейнер



        // Обработчик кликов по экрану
        graphPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) { // Если нажата левая кнопка мыши
                Circle vertex = findVertexAt(event.getX(), event.getY(), vertices);
                if (vertex == null) {
                    saveState(); // Добавляем состояние графа в стэк undo
                    // Создаем новую вершину (круг) в месте клика
                    vertex = new Circle(event.getX(), event.getY(), vertexSize); // (x, y, радиус)
                    vertices.add(vertex); // Добавляем вершину в список
                    graphPane.getChildren().add(vertex); // Добавляем вершину в панель графа(выводим на экран)
                    enableVertexDragging(vertex); // Добавляем возможность передвигать вершину
                }
            } else if (event.getButton() == MouseButton.SECONDARY) { // Если нажата правая кнопка мыши
                // Удаление вершины, если она есть в месте клика
                Circle vertexToDelete = findVertexAt(event.getX(), event.getY(), vertices);
                if (vertexToDelete != null) {
                    deleteVertex(vertexToDelete);
                }
            } else if(event.getButton() == MouseButton.MIDDLE) { // Если нажато колесико мыши
                Circle vertex = findVertexAt(event.getX(), event.getY(), vertices); // Сохраняем вершину в месте клика
                if (vertex != null){
                    if (selectedVertex == null) {
                        // Если вершина не выбрана, выбираем текущую
                        selectedVertex = vertex;
                        System.out.println("Вершина выбрана: " + vertex);
                    } else {
                        saveState();
                        // Если вершина уже выбрана, создаем ребро между выбранной и текущей вершиной
                        // Находим индексы выбранных вершин в списке вершин
                        int startIndex = vertices.indexOf(selectedVertex);
                        int endIndex = vertices.indexOf(vertex);
                        if(startIndex != endIndex){
                            // Проверяем, существует ли ребро
                            boolean edgeExists = isEdgeExists(startIndex, endIndex);
                            if (!edgeExists){
                            Line edge = new Line( // Cоздаём линию
                                    selectedVertex.getCenterX(), selectedVertex.getCenterY(),
                                    vertex.getCenterX(), vertex.getCenterY()
                            );
                            edges.add(new EdgeInfo(startIndex, endIndex,edge)); // Добавляем ребро в список
                            graphPane.getChildren().add(edge); // Выводим ребра на экран
                            System.out.println("Ребро создано между " + selectedVertex + " и " + vertex);
                        } else {
                                showAlert(
                                        "Ошибка",
                                        "Ребро уже существует",
                                        "Между этими вершинами уже есть соединение."
                                );
                            }}

                        selectedVertex = null; // Сбрасываем выбор вершины
                    }
                }
            }
        });


        // Создаем сцену и отображаем ее
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Interactive Graph with Matrix");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Методы для работы с историей
    // Начало

    // Cохраняем сцену
    private void saveState() {
        undoStack.push(new GraphState(vertices, edges));
        redoStack.clear();
    }

    // Востанавливаем сцену
    private void restoreState(GraphState state) {
        clearSceneWithoutSaving();

        // Восстанавливаем вершины
        for (GraphState.VertexData vData : state.verticesData) {
            Circle vertex = new Circle(vData.x, vData.y, vData.radius);
            vertices.add(vertex);
            graphPane.getChildren().add(vertex);
            enableVertexDragging(vertex);
        }

        // Восстанавливаем ребра
        for (GraphState.EdgeData eData : state.edgesData) {
            if (eData.startIndex < vertices.size() && eData.endIndex < vertices.size()) {
                Circle start = vertices.get(eData.startIndex);
                Circle end = vertices.get(eData.endIndex);
                Line line = new Line(
                        start.getCenterX(),
                        start.getCenterY(),
                        end.getCenterX(),
                        end.getCenterY()
                );
                line.setStroke(eData.color);
                line.setStrokeWidth(2);
                edges.add(new EdgeInfo(eData.startIndex, eData.endIndex, line));
                graphPane.getChildren().add(line);
            }
        }
    }

    // Очищаем сцену без сохранения в истории
    private void clearSceneWithoutSaving() {
        graphPane.getChildren().removeAll(edges.stream()
                .map(EdgeInfo::getLine)
                .collect(Collectors.toList()));
        edges.clear();
        graphPane.getChildren().removeAll(vertices);
        vertices.clear();
    }

    // Обработчики для кнопок
    private void undo() { // назад
        if (!undoStack.isEmpty()) {
            redoStack.push(new GraphState(vertices, edges));
            restoreState(undoStack.pop());
        }
    }

    private void redo() { // вперёд
        if (!redoStack.isEmpty()) {
            undoStack.push(new GraphState(vertices, edges));
            restoreState(redoStack.pop());
        }
    }
    // Конец

    // Функция для выбора размера вершин
    private double chooseVertexSize() {
        // Создаем диалоговое окно
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Выбор размера вершин");
        dialog.setHeaderText("Выберите новый размер для всех вершин");

        // Настраиваем кнопки
        ButtonType applyButtonType = new ButtonType("Применить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        // Создаем слайдер
        Slider slider = new Slider(5, 30, vertexSize);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(5);
        slider.setBlockIncrement(1);

        // Добавляем элементы в окно
        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setSpacing(10);
        content.getChildren().addAll(new Label("Размер вершин:"), slider);
        dialog.getDialogPane().setContent(content);

        // Обработчик результата
        dialog.setResultConverter(buttonType -> {
            if (buttonType == applyButtonType) {
                return slider.getValue();
            }
            return null;
        });
        saveState();
        // Показываем окно и обрабатываем результат
        Optional<Double> result = dialog.showAndWait();
        if (result.isPresent()) {
            double newSize = result.get();
            // Обновляем размер всех существующих вершин
            for (Circle vertex : vertices) {
                vertex.setRadius(newSize);
            }
            return newSize;
        }
        return vertexSize; // Возвращаем текущий размер, если изменение отменено
    }

    // Очищаем сцену с сохранением в истории
    private void clearScene(){
        saveState();
        for(EdgeInfo edge : edges){
            graphPane.getChildren().removeAll(edge.getLine());
        }
        edges.clear();
        graphPane.getChildren().removeAll(vertices);
        vertices.clear();
    }

    // Метод для включения перемещения вершины
    private void enableVertexDragging(Circle vertex) {
        final double[] offsetX = new double[1];
        final double[] offsetY = new double[1];

        vertex.setOnMousePressed(event -> {
            // Запоминаем смещение курсора относительно центра вершины
            offsetX[0] = vertex.getCenterX() - event.getSceneX();
            offsetY[0] = vertex.getCenterY() - event.getSceneY();
        });

        vertex.setOnMouseDragged(event -> {
            // Перемещаем вершину
            double newX = event.getSceneX() + offsetX[0];
            double newY = event.getSceneY() + offsetY[0];
            vertex.setCenterX(newX);
            vertex.setCenterY(newY);

            // Обновляем связанные ребра
            updateConnectedEdges(vertex);
            event.consume();
        });
    }

    // Метод для обновления связанных ребер
    private void updateConnectedEdges(Circle vertex) {
        int vertexIndex = vertices.indexOf(vertex);
        for (EdgeInfo edge : edges) {
            if (edge.getStartIndex() == vertexIndex) {
                // Обновляем начало ребра
                Line line = edge.getLine();
                line.setStartX(vertex.getCenterX());
                line.setStartY(vertex.getCenterY());
            }
            if (edge.getEndIndex() == vertexIndex) {
                // Обновляем конец ребра
                Line line = edge.getLine();
                line.setEndX(vertex.getCenterX());
                line.setEndY(vertex.getCenterY());
            }
        }
    }

    // Функция для удаления вершины и связанных с ней рёбер
    private void deleteVertex(Circle vertex) {
        saveState();
        // Удаляем все ребра, связанные с этой вершиной
        int vertexIndex = vertices.indexOf(vertex);

        List<EdgeInfo> edgesToRemove = new ArrayList<>(); // Новый список для ребёр, которые мы хотим удалить
        for (EdgeInfo edge : edges) {
            if (edge.getStartIndex() == vertexIndex || edge.getEndIndex() == vertexIndex) {
                edgesToRemove.add(edge);
            }
        }

        edges.removeAll(edgesToRemove); // Удаляем рёбра из нового списка
        graphPane.getChildren().removeAll(edgesToRemove.stream().map(EdgeInfo::getLine).collect(Collectors.toList()));

        // Удаляем вершину
        vertices.remove(vertex);
        graphPane.getChildren().remove(vertex);

        // Обновляем индексы в оставшихся ребрах
        for (EdgeInfo edge : edges) {
            int start = edge.getStartIndex();
            int end = edge.getEndIndex();

            if (start > vertexIndex) {
                edge.setStartIndex(start - 1);
            }
            if (end > vertexIndex) {
                edge.setEndIndex(end - 1);
            }
        }
        System.out.println("Вершина удалена: " + vertex);
    }

    // Метод для создания матрицы смежности
    private List<List<Integer>> createAdjacencyMatrix(List<Circle> vertices) {
        int size = vertices.size();
        List<List<Integer>> adjacencyMatrix = new ArrayList<>();

        // Инициализация матрицы смежности
        for (int i = 0; i < size; i++) {
            adjacencyMatrix.add(new ArrayList<>(Collections.nCopies(size, 0)));
        }

        // Заполняем матрицу смежности
        for (EdgeInfo edge : edges) {
            int i = edge.getStartIndex();
            int j = edge.getEndIndex();

            adjacencyMatrix.get(i).set(j, 1);
            adjacencyMatrix.get(j).set(i, 1); // Если граф неориентированный
        }


        return adjacencyMatrix;

    }

    // Метод для отображения справки
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Справка");
        alert.setHeaderText("Возможности программы");

        // Текст справки
        String helpText = """
            Программа позволяет работать с графами. Основные функции:
            1. Создание вершин:
               - Левый клик на пустом месте создает новую вершину.
            2. Перемещение вершин:
               - Зажмите левую кнопку мыши на вершине и перетащите её.
            3. Создание ребер:
               - Средний клик (колесо мыши) на одной вершине, затем на другой создает ребро.
            4. Удаление вершин:
               - Правый клик на вершине удаляет её и связанные ребра.
            5. Очищение экрана:
               - Нажмите кнопку "Очистка экрана",чтобы удалить все вершины и рёбра .
            6. Разбиение графа на треугольники:
               - Нажмите кнопку "Показать решение", чтобы разбить граф на треугольники, ребра которого окрашиваются в случайные цвета.
               ВНИМАНИЕ! Количество вершин должно быть кратно 3 .
            7. Изменение размера вершин:
               - Кнопка открывает слайдер для выбора радиуса вершин (от 5 до 30 пикселей).
            8. История действий (кнопки "<-" и "->"):
               - "<-" отменяет последнее действие(удаление, создание, изменение размера вершин, решение)
               - "->" возвращает отменённое действие
            """;

        alert.setContentText(helpText);
        alert.showAndWait();
    }

    // Записываем матрицу в текстовый фаил javaOut
    private void writeMatrixToFile(List<List<Integer>> matrix, String filePath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            // Формируем данные матрицы
            for (List<Integer> row : matrix) {
                for (int value : row) {
                    writer.write(value + " ");
                }
                writer.newLine();
            }
            writer.flush();
            writer.close();

            System.out.println("Матрица успешно записана в файл: " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для поиска вершины по координатам
    private Circle findVertexAt(double x, double y, List<Circle> vertices) {
        for (Circle vertex : vertices) {
            // Проверяем, находятся ли координаты внутри круга (вершины)
            if (Math.sqrt(Math.pow(x - vertex.getCenterX(), 2) + Math.pow(y - vertex.getCenterY(), 2)) <= vertex.getRadius()) {
                return vertex;
            }
        }
        return null; // Если вершина не найдена
    }

    // Запускается скомпилированный с++ код, который решает задачу триангуляции графа
    private void openExeAndFindSolution() {
        try {
            saveState();
            Process process = new ProcessBuilder("Сoursework IT 31").start();

            // Чтение вывода программы
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<List<Integer>> newMatrix = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                List<Integer> row = Arrays.stream(line.split("\\s+"))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                if (!row.isEmpty()) {
                    newMatrix.add(row);
                }
            }

            // Удаление лишних рёбер и окрашивание
            List<EdgeInfo> toRemove = new ArrayList<>();
            for (EdgeInfo edge : edges) {
                int i = edge.getStartIndex();
                int j = edge.getEndIndex();
                if (i >= newMatrix.size() || j >= newMatrix.size() || newMatrix.get(i).get(j) == 0) {
                    toRemove.add(edge);
                }
            }
            graphPane.getChildren().removeAll(
                    toRemove.stream().map(EdgeInfo::getLine).collect(Collectors.toList())
            );
            edges.removeAll(toRemove);

            colorTriangles(newMatrix);
            process.waitFor();

        } catch (IOException e) {
            showAlert(
                    "Ошибка запуска",
                    "Файл не найден",
                    "Исполняемый файл 'Сoursework IT 31' не обнаружен.\nПроверьте путь: " +
                            new File("Сoursework IT 31").getAbsolutePath()
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Метод для окрашивания треугольников
    private void colorTriangles(List<List<Integer>> matrix) {
        Random rand = new Random();
        List<Integer> visitedVertices = new ArrayList<>(); // Список посещённых вершин

        for (int i = 0; i < matrix.size(); i++) {
            if (visitedVertices.contains(i)) {
                continue; // Пропускаем уже обработанные вершины
            }

            // Находим две вершины, связанные с текущей вершиной
            List<Integer> connectedVertices = new ArrayList<>();
            for (int j = 0; j < matrix.size(); j++) {
                if (matrix.get(i).get(j) == 1 && !visitedVertices.contains(j)) {
                    connectedVertices.add(j);
                    if (connectedVertices.size() == 2) {
                        break; // Нашли две вершины, выходим из цикла
                    }
                }
            }

            // Если нашли две вершины, окрашиваем рёбра
            if (connectedVertices.size() == 2) {
                Color color = Color.color(
                        rand.nextDouble(),
                        rand.nextDouble(),
                        rand.nextDouble()
                );

                // Окрашиваем рёбра треугольника
                colorEdge(i, connectedVertices.get(0), color);
                colorEdge(i, connectedVertices.get(1), color);
                colorEdge(connectedVertices.get(0), connectedVertices.get(1), color);

                // Добавляем вершины в список посещённых
                visitedVertices.add(i);
                visitedVertices.add(connectedVertices.get(0));
                visitedVertices.add(connectedVertices.get(1));
            }
        }
    }

    // Вспомогательный метод для окраски конкретного ребра
    private void colorEdge(int start, int end, Color color) {
        for (EdgeInfo edge : edges) {
            if ((edge.getStartIndex() == start && edge.getEndIndex() == end) ||
                    (edge.getStartIndex() == end && edge.getEndIndex() == start)) {
                edge.getLine().setStroke(color);
                edge.getLine().setStrokeWidth(2); // Делаем рёбра толще для наглядности
                edge.setColor(color);
            }
        }
    }

    // Вспомогательный метод для проверки рёбер
    private boolean isEdgeExists(int start, int end) {
        return edges.stream().anyMatch(edge ->
                (edge.getStartIndex() == start && edge.getEndIndex() == end) ||
                        (edge.getStartIndex() == end && edge.getEndIndex() == start)
        );
    }

    // Метод для показа ошибок
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
