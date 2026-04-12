package client.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TradeHouseApp extends Application {
    private final TextArea eventLog = new TextArea();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #10110d;");

        root.setTop(buildHeader());
        root.setLeft(buildLoginPanel());
        root.setCenter(buildMarketChart());
        root.setRight(buildOrderPanel());
        root.setBottom(buildEventLog());

        Scene scene = new Scene(root, 1120, 720);
        stage.setTitle("TradeHouse");
        stage.setMinWidth(920);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildHeader() {
        Label title = new Label("TradeHouse");
        title.setStyle("-fx-text-fill: #f6f0d0; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label status = new Label("GUI scaffold ready");
        status.setStyle("-fx-text-fill: #8fdc8f; -fx-font-size: 13px;");

        HBox header = new HBox(16, title, status);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.setStyle("-fx-background-color: #17180f; -fx-border-color: #3d3b24; -fx-border-width: 0 0 1 0;");
        HBox.setHgrow(status, Priority.ALWAYS);
        return header;
    }

    private VBox buildLoginPanel() {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle(inputStyle());
        TextField balanceField = new TextField("10000");
        balanceField.setPromptText("Starting balance");
        balanceField.setStyle(inputStyle());

        Button loginButton = new Button("Prepare Login");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(buttonStyle());
        loginButton.setOnAction(event -> eventLog.appendText("Login form ready for socket integration.\n"));

        VBox panel = new VBox(12,
                sectionTitle("Account"),
                fieldLabel("Username"),
                usernameField,
                fieldLabel("Starting Balance"),
                balanceField,
                loginButton,
                new Separator(),
                fieldLabel("Portfolio"),
                valueLabel("Cash: waiting for login"),
                valueLabel("Holdings: waiting for server"));
        panel.setPadding(new Insets(18));
        panel.setPrefWidth(260);
        panel.setStyle(panelStyle());
        return panel;
    }

    private VBox buildOrderPanel() {
        ComboBox<String> sideBox = new ComboBox<>();
        sideBox.getItems().addAll("BUY", "SELL");
        sideBox.setValue("BUY");
        sideBox.setMaxWidth(Double.MAX_VALUE);
        sideBox.setStyle(inputStyle());

        ComboBox<String> tickerBox = new ComboBox<>();
        tickerBox.getItems().addAll("AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "BTC");
        tickerBox.setValue("AAPL");
        tickerBox.setMaxWidth(Double.MAX_VALUE);
        tickerBox.setStyle(inputStyle());

        TextField priceField = new TextField("150.00");
        priceField.setStyle(inputStyle());
        TextField quantityField = new TextField("10");
        quantityField.setStyle(inputStyle());

        Button submitButton = new Button("Prepare Order");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setStyle(buttonStyle());
        submitButton.setOnAction(event -> eventLog.appendText("Order form ready for socket integration.\n"));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(fieldLabel("Side"), 0, 0);
        form.add(sideBox, 1, 0);
        form.add(fieldLabel("Ticker"), 0, 1);
        form.add(tickerBox, 1, 1);
        form.add(fieldLabel("Price"), 0, 2);
        form.add(priceField, 1, 2);
        form.add(fieldLabel("Quantity"), 0, 3);
        form.add(quantityField, 1, 3);

        VBox panel = new VBox(12, sectionTitle("Order Entry"), form, submitButton, new Separator(),
                fieldLabel("Connection"),
                valueLabel("Socket client lands in Commit 3"),
                valueLabel("Server: 127.0.0.1:8080"));
        panel.setPadding(new Insets(18));
        panel.setPrefWidth(280);
        panel.setStyle(panelStyle());
        return panel;
    }

    private VBox buildMarketChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tick");
        yAxis.setLabel("Price");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Live Market Preview");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setStyle("-fx-background-color: #10110d;");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>(1, 146));
        series.getData().add(new XYChart.Data<>(2, 149));
        series.getData().add(new XYChart.Data<>(3, 147));
        series.getData().add(new XYChart.Data<>(4, 151));
        series.getData().add(new XYChart.Data<>(5, 150));
        chart.getData().add(series);

        VBox wrapper = new VBox(12, chart);
        wrapper.setPadding(new Insets(18));
        wrapper.setStyle("-fx-background-color: #10110d;");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildEventLog() {
        eventLog.setText("TradeHouse GUI scaffold loaded.\nBackend socket integration comes next.\n");
        eventLog.setEditable(false);
        eventLog.setPrefRowCount(5);
        eventLog.setStyle("-fx-control-inner-background: #0c0d09; -fx-text-fill: #f6f0d0; -fx-font-family: Menlo;");

        VBox wrapper = new VBox(8, sectionTitle("Event Log"), eventLog);
        wrapper.setPadding(new Insets(0, 18, 18, 18));
        wrapper.setStyle("-fx-background-color: #10110d;");
        return wrapper;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #f6f0d0; -fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #c7b85a; -fx-font-size: 12px;");
        return label;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #efe7b4; -fx-font-size: 13px;");
        return label;
    }

    private String panelStyle() {
        return "-fx-background-color: #17180f; -fx-border-color: #3d3b24; -fx-border-width: 0 1 0 0;";
    }

    private String inputStyle() {
        return "-fx-background-color: #262616; -fx-text-fill: #f6f0d0; -fx-prompt-text-fill: #8d8556;";
    }

    private String buttonStyle() {
        return "-fx-background-color: #27b868; -fx-text-fill: #07100a; -fx-font-weight: bold;";
    }
}
