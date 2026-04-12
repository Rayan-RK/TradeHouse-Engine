package client.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import javafx.application.Application;
import javafx.application.Platform;
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
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static final int MAX_CHART_POINTS = 30;

    private final TextArea eventLog = new TextArea();
    private final Label statusLabel = new Label("Offline");
    private final Label cashLabel = valueLabel("Waiting");
    private final Label holdingsLabel = valueLabel("Server-owned");
    private final Label lastPriceLabel = valueLabel("--");
    private final Label activeTickerLabel = valueLabel("AAPL");
    private final Label feedStateLabel = valueLabel("Idle");
    private final TextField usernameField = new TextField();
    private final TextField balanceField = new TextField("10000");
    private final ComboBox<String> sideBox = new ComboBox<>();
    private final ComboBox<String> tickerBox = new ComboBox<>();
    private final TextField priceField = new TextField("150.00");
    private final TextField quantityField = new TextField("10");
    private final Button submitButton = new Button("Submit Order");
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private final Button themeButton = new Button();

    private BorderPane root;
    private ClientNetwork network;
    private int chartTick = 5;
    private boolean darkTheme;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        darkTheme = isSystemDarkTheme();

        root = new BorderPane();
        root.getStyleClass().add("app-root");

        root.setTop(buildHeader());
        root.setCenter(buildWorkspace());

        applyTheme(darkTheme);

        Scene scene = new Scene(root, 1180, 740);
        String stylesheet = getClass().getResource("/client/gui/tradehouse.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);

        stage.setTitle("TradeHouse");
        stage.setMinWidth(980);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (network != null) {
            network.close();
        }
    }

    private HBox buildHeader() {
        Label eyebrow = new Label("MARKET SIMULATION");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("TradeHouse");
        title.getStyleClass().add("app-title");

        VBox titleBlock = new VBox(4, eyebrow, title);

        statusLabel.getStyleClass().addAll("status-pill", "status-offline");

        themeButton.getStyleClass().add("theme-toggle");
        themeButton.setOnAction(event -> toggleTheme());
        updateThemeButtonLabel();

        HBox header = new HBox(18, titleBlock, spacer(), statusLabel, themeButton);
        header.getStyleClass().add("top-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox buildWorkspace() {
        VBox marketPanel = buildMarketPanel();
        HBox.setHgrow(marketPanel, Priority.ALWAYS);

        HBox workspace = new HBox(18, buildAccountPanel(), marketPanel, buildOrderPanel());
        workspace.getStyleClass().add("workspace");
        return workspace;
    }

    private VBox buildAccountPanel() {
        usernameField.setPromptText("Username");
        balanceField.setPromptText("Starting balance");

        Button loginButton = new Button("Connect and Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> connectAndLogin());

        VBox panel = new VBox(14,
                sectionTitle("Account"),
                formLabel("Username"),
                usernameField,
                formLabel("Starting Balance"),
                balanceField,
                loginButton,
                new Separator(),
                metricLine("Cash", cashLabel),
                metricLine("Holdings", holdingsLabel),
                metricLine("Server", valueLabel(SERVER_HOST + ":" + SERVER_PORT)));
        panel.getStyleClass().addAll("surface-panel", "side-panel");
        panel.setPrefWidth(272);
        return panel;
    }

    private VBox buildMarketPanel() {
        VBox chartPanel = buildMarketChart();
        VBox.setVgrow(chartPanel, Priority.ALWAYS);

        VBox marketPanel = new VBox(16, buildMetricStrip(), chartPanel, buildEventLog());
        marketPanel.getStyleClass().add("market-panel");
        return marketPanel;
    }

    private HBox buildMetricStrip() {
        HBox metrics = new HBox(12,
                metricTile("Last Price", lastPriceLabel),
                metricTile("Active Ticker", activeTickerLabel),
                metricTile("Feed", feedStateLabel),
                metricTile("Mode", valueLabel("Socket live")));
        metrics.getStyleClass().add("metric-strip");
        return metrics;
    }

    private VBox buildMarketChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tick");
        yAxis.setLabel("Price");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("market-chart");
        chart.setTitle("Live Market");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);

        priceSeries.getData().add(new XYChart.Data<>(1, 146));
        priceSeries.getData().add(new XYChart.Data<>(2, 149));
        priceSeries.getData().add(new XYChart.Data<>(3, 147));
        priceSeries.getData().add(new XYChart.Data<>(4, 151));
        priceSeries.getData().add(new XYChart.Data<>(5, 150));
        chart.getData().add(priceSeries);

        VBox chartPanel = new VBox(10, sectionTitle("Market Pulse"), chart);
        chartPanel.getStyleClass().add("surface-panel");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return chartPanel;
    }

    private VBox buildOrderPanel() {
        sideBox.getItems().addAll("BUY", "SELL");
        sideBox.setValue("BUY");
        sideBox.setMaxWidth(Double.MAX_VALUE);

        tickerBox.getItems().addAll("AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "BTC");
        tickerBox.setValue("AAPL");
        tickerBox.setMaxWidth(Double.MAX_VALUE);
        tickerBox.setOnAction(event -> activeTickerLabel.setText(tickerBox.getValue()));

        submitButton.getStyleClass().add("primary-button");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setDisable(true);
        submitButton.setOnAction(event -> submitOrder());

        GridPane form = new GridPane();
        form.getStyleClass().add("order-form");
        form.setHgap(10);
        form.setVgap(10);
        form.add(formLabel("Side"), 0, 0);
        form.add(sideBox, 1, 0);
        form.add(formLabel("Ticker"), 0, 1);
        form.add(tickerBox, 1, 1);
        form.add(formLabel("Price"), 0, 2);
        form.add(priceField, 1, 2);
        form.add(formLabel("Quantity"), 0, 3);
        form.add(quantityField, 1, 3);

        VBox panel = new VBox(14,
                sectionTitle("Order Ticket"),
                form,
                submitButton,
                new Separator(),
                formLabel("What happens next"),
                bodyText("Orders go to the server. Matching, balances, and persistence stay server-side."));
        panel.getStyleClass().addAll("surface-panel", "side-panel", "order-ticket");
        panel.setPrefWidth(300);
        return panel;
    }

    private VBox buildEventLog() {
        eventLog.setText("TradeHouse GUI loaded.\nUse Connect and Login to reach the server.\n");
        eventLog.setEditable(false);
        eventLog.setPrefRowCount(7);
        eventLog.getStyleClass().add("event-log");

        VBox wrapper = new VBox(10, sectionTitle("Event Feed"), eventLog);
        wrapper.getStyleClass().add("surface-panel");
        return wrapper;
    }

    private void connectAndLogin() {
        String username = usernameField.getText();
        String startingBalance = balanceField.getText();

        if (username == null || username.isBlank()) {
            appendEvent("Username is required before login.");
            return;
        }

        if (startingBalance == null || startingBalance.isBlank()) {
            appendEvent("Starting balance is required before login.");
            return;
        }

        try {
            ensureConnected();
            network.login(username, startingBalance);
            appendEvent("Sent login for " + username.trim().toUpperCase() + ".");
        } catch (RuntimeException | java.io.IOException e) {
            setOffline();
            appendEvent("Login failed: " + e.getMessage());
        }
    }

    private void submitOrder() {
        try {
            ensureConnected();
            network.submitOrder(sideBox.getValue(), tickerBox.getValue(), priceField.getText(),
                    quantityField.getText());
            appendEvent("Sent " + sideBox.getValue() + " order for " + tickerBox.getValue() + ".");
        } catch (RuntimeException | java.io.IOException e) {
            setOffline();
            appendEvent("Order failed: " + e.getMessage());
        }
    }

    private void ensureConnected() throws java.io.IOException {
        if (network != null && network.isConnected()) {
            return;
        }

        network = new ClientNetwork(SERVER_HOST, SERVER_PORT, this::handleServerMessage, this::handleNetworkError);
        network.connect();
        setStatus("Connected", true);
        appendEvent("Connected to TradeHouse server.");
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("LOGIN_SUCCESS:")) {
                String[] parts = message.split(":");
                if (parts.length >= 3) {
                    cashLabel.setText("$" + parts[2]);
                    holdingsLabel.setText("Waiting for trades");
                    setStatus("Logged in as " + parts[1], true);
                    submitButton.setDisable(false);
                }
            } else if (message.startsWith("SERVER_ERROR:")) {
                setStatus("Server error", false);
            } else if (message.startsWith("MARKET_UPDATE:")) {
                handleMarketUpdate(message);
            } else if ("PING".equalsIgnoreCase(message)) {
                feedStateLabel.setText("Heartbeat OK");
            }
            appendEvent("SERVER " + message);
        });
    }

    private void handleMarketUpdate(String message) {
        String[] parts = message.split(":");
        if (parts.length < 3) {
            return;
        }

        try {
            String ticker = parts[1];
            double price = Double.parseDouble(parts[2]);
            activeTickerLabel.setText(ticker);
            lastPriceLabel.setText(String.format("$%.2f", price));
            feedStateLabel.setText("Live");
            priceSeries.getData().add(new XYChart.Data<>(++chartTick, price));
            if (priceSeries.getData().size() > MAX_CHART_POINTS) {
                priceSeries.getData().remove(0);
            }
        } catch (NumberFormatException e) {
            appendEvent("Ignored malformed market update: " + message);
        }
    }

    private void handleNetworkError(String message) {
        Platform.runLater(() -> {
            setOffline();
            appendEvent(message);
        });
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        applyTheme(darkTheme);
    }

    private void applyTheme(boolean dark) {
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(dark ? "theme-dark" : "theme-light");
        updateThemeButtonLabel();
    }

    private void updateThemeButtonLabel() {
        themeButton.setText(darkTheme ? "Light" : "Dark");
    }

    private boolean isSystemDarkTheme() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        try {
            if (osName.contains("mac")) {
                return commandOutput("defaults", "read", "-g", "AppleInterfaceStyle").contains("Dark");
            }

            if (osName.contains("win")) {
                String output = commandOutput("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme");
                return !output.contains("0x1") && !output.contains("1");
            }

            String colorScheme = commandOutput("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
            if (colorScheme.toLowerCase(Locale.ROOT).contains("dark")) {
                return true;
            }

            String gtkTheme = commandOutput("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
            return gtkTheme.toLowerCase(Locale.ROOT).contains("dark");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String commandOutput(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!output.isEmpty()) {
                    output.append('\n');
                }
                output.append(line);
            }
        }

        process.waitFor();
        return output.toString().trim();
    }

    private void setOffline() {
        setStatus("Offline", false);
        submitButton.setDisable(true);
    }

    private void setStatus(String text, boolean online) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-online", "status-offline");
        statusLabel.getStyleClass().add(online ? "status-online" : "status-offline");
    }

    private void appendEvent(String message) {
        eventLog.appendText(message + "\n");
    }

    private HBox metricLine(String labelText, Label value) {
        Label label = formLabel(labelText);
        HBox row = new HBox(8, label, spacer(), value);
        row.getStyleClass().add("metric-line");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox metricTile(String labelText, Label value) {
        VBox tile = new VBox(6, formLabel(labelText), value);
        tile.getStyleClass().add("metric-tile");
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("value-label");
        return label;
    }

    private Label bodyText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("body-text");
        label.setWrapText(true);
        return label;
    }

    private HBox spacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
