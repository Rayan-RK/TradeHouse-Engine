package client.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private static final int MAX_ACTIVITY_ROWS = 12;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Label statusLabel = new Label("Offline");
    private final Label cashLabel = valueLabel("Waiting");
    private final Label availableCashLabel = valueLabel("--");
    private final Label holdingsLabel = valueLabel("None");
    private final Label lastPriceLabel = valueLabel("--");
    private final Label activeTickerLabel = valueLabel("AAPL");
    private final Label feedStateLabel = valueLabel("Idle");
    private final Label orderFeedbackLabel = bodyText("Login before submitting an order.");
    private final TextField usernameField = new TextField();
    private final TextField balanceField = new TextField("10000");
    private final ComboBox<String> sideBox = new ComboBox<>();
    private final ComboBox<String> tickerBox = new ComboBox<>();
    private final TextField priceField = new TextField("150.00");
    private final TextField quantityField = new TextField("10");
    private final Button submitButton = new Button("Submit Order");
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private final ObservableList<ActivityRow> activityRows = FXCollections.observableArrayList();
    private final TableView<ActivityRow> activityTable = new TableView<>(activityRows);
    private final Button themeButton = new Button();

    private BorderPane root;
    private ClientNetwork network;
    private PendingOrder pendingOrder;
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
                metricLine("Available", availableCashLabel),
                metricLine("Holdings", holdingsLabel),
                metricLine("Server", valueLabel(SERVER_HOST + ":" + SERVER_PORT)));
        panel.getStyleClass().addAll("surface-panel", "side-panel");
        panel.setPrefWidth(272);
        return panel;
    }

    private VBox buildMarketPanel() {
        VBox chartPanel = buildMarketChart();
        VBox.setVgrow(chartPanel, Priority.ALWAYS);

        VBox marketPanel = new VBox(16, buildMetricStrip(), chartPanel, buildActivityHistory());
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

        orderFeedbackLabel.getStyleClass().addAll("feedback-label", "feedback-info");

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
                orderFeedbackLabel,
                new Separator(),
                formLabel("What happens next"),
                bodyText("Orders go to the server. Matching, balances, and persistence stay server-side."));
        panel.getStyleClass().addAll("surface-panel", "side-panel", "order-ticket");
        panel.setPrefWidth(300);
        return panel;
    }

    private VBox buildActivityHistory() {
        activityTable.getStyleClass().add("activity-table");
        activityTable.setPlaceholder(bodyText("Order and trade updates will appear here."));
        activityTable.setPrefHeight(220);
        activityTable.setMaxHeight(260);
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ActivityRow, String> timeColumn = tableColumn("Time", 76, ActivityRow::timeProperty);
        TableColumn<ActivityRow, String> eventColumn = tableColumn("Event", 92, ActivityRow::eventProperty);
        TableColumn<ActivityRow, String> tickerColumn = tableColumn("Ticker", 78, ActivityRow::tickerProperty);
        TableColumn<ActivityRow, String> detailsColumn = tableColumn("Details", 180, ActivityRow::detailsProperty);

        activityTable.getColumns().clear();
        activityTable.getColumns().add(timeColumn);
        activityTable.getColumns().add(eventColumn);
        activityTable.getColumns().add(tickerColumn);
        activityTable.getColumns().add(detailsColumn);
        addActivity("Ready", "GUI", "Connect and login to reach the server.");

        VBox wrapper = new VBox(10, sectionTitle("Activity"), activityTable);
        wrapper.getStyleClass().add("surface-panel");
        return wrapper;
    }

    private void connectAndLogin() {
        String username = trimmed(usernameField);

        if (username.isBlank()) {
            rejectInput("Username is required before login.");
            return;
        }

        Double startingBalance = parseNonNegativeMoney(trimmed(balanceField));
        if (startingBalance == null) {
            rejectInput("Starting balance must be a number greater than or equal to 0.");
            return;
        }

        try {
            ensureConnected();
            network.login(username, moneyText(startingBalance));
            updateOrderFeedback("Login sent. Waiting for server confirmation.", "feedback-info");
            addActivity("Login", username.toUpperCase(Locale.ROOT), "Starting cash " + moneyLabel(startingBalance));
        } catch (RuntimeException | java.io.IOException e) {
            setOffline();
            updateOrderFeedback("Login failed: " + e.getMessage(), "feedback-error");
            addActivity("Error", "Login", e.getMessage());
        }
    }

    private void submitOrder() {
        PendingOrder order = readPendingOrder();
        if (order == null) {
            return;
        }

        try {
            ensureConnected();
            pendingOrder = order;
            network.submitOrder(order.side(), order.ticker(), moneyText(order.price()), String.valueOf(order.quantity()));
            addActivity("Submitted", order.ticker(), order.summary());
            updateOrderFeedback("Submitted " + order.summary() + ". Waiting for server.", "feedback-info");
        } catch (RuntimeException | java.io.IOException e) {
            setOffline();
            updateOrderFeedback("Order failed: " + e.getMessage(), "feedback-error");
            addActivity("Error", order.ticker(), e.getMessage());
        }
    }

    private void ensureConnected() throws java.io.IOException {
        if (network != null && network.isConnected()) {
            return;
        }

        network = new ClientNetwork(SERVER_HOST, SERVER_PORT, this::handleServerMessage, this::handleNetworkError);
        network.connect();
        setStatus("Connected", true);
        addActivity("Connected", "Server", SERVER_HOST + ":" + SERVER_PORT);
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
                    updateOrderFeedback("Logged in. Order ticket is ready.", "feedback-success");
                    addActivity("Accepted", parts[1], "Login confirmed with cash $" + parts[2]);
                }
            } else if (message.startsWith("SERVER_ACK:")) {
                acknowledgePendingOrder();
            } else if (message.startsWith("SERVER_ERROR:")) {
                setStatus("Server error", false);
                rejectPendingOrder(message.substring("SERVER_ERROR:".length()).trim());
            } else if (message.startsWith("ACCOUNT_UPDATE:")) {
                handleAccountUpdate(message);
            } else if (message.startsWith("MARKET_UPDATE:")) {
                handleMarketUpdate(message);
            } else if (message.startsWith("CONNECTION_SUCCESS:")) {
                addActivity("Connected", "Server", message.substring("CONNECTION_SUCCESS:".length()).trim());
            } else if ("PING".equalsIgnoreCase(message)) {
                feedStateLabel.setText("Heartbeat OK");
            }
        });
    }

    private void handleAccountUpdate(String message) {
        String[] parts = message.split(":", 5);
        if (parts.length < 5) {
            addActivity("Ignored", "Account", "Malformed update: " + message);
            return;
        }

        String holdings = displayHoldings(parts[4]);
        cashLabel.setText("$" + parts[2]);
        availableCashLabel.setText("$" + parts[3]);
        holdingsLabel.setText(holdings);
        addActivity("Portfolio", parts[1], "Available $" + parts[3] + ", holdings " + holdings);
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
            addActivity("Market", ticker, "Trade printed at " + moneyLabel(price));
            priceSeries.getData().add(new XYChart.Data<>(++chartTick, price));
            if (priceSeries.getData().size() > MAX_CHART_POINTS) {
                priceSeries.getData().remove(0);
            }
        } catch (NumberFormatException e) {
            addActivity("Ignored", "Market", "Malformed update: " + message);
        }
    }

    private void handleNetworkError(String message) {
        Platform.runLater(() -> {
            setOffline();
            updateOrderFeedback(message, "feedback-error");
            addActivity("Network", "Server", message);
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
        updateOrderFeedback("Login before submitting an order.", "feedback-info");
    }

    private void setStatus(String text, boolean online) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-online", "status-offline");
        statusLabel.getStyleClass().add(online ? "status-online" : "status-offline");
    }

    private PendingOrder readPendingOrder() {
        String side = sideBox.getValue();
        String ticker = tickerBox.getValue();

        if (side == null || side.isBlank()) {
            rejectInput("Choose BUY or SELL before submitting.");
            return null;
        }

        if (ticker == null || ticker.isBlank()) {
            rejectInput("Choose a ticker before submitting.");
            return null;
        }

        Double price = parsePositiveMoney(trimmed(priceField));
        if (price == null) {
            rejectInput("Price must be a number greater than 0.");
            return null;
        }

        Integer quantity = parsePositiveInteger(trimmed(quantityField));
        if (quantity == null) {
            rejectInput("Quantity must be a whole number greater than 0.");
            return null;
        }

        return new PendingOrder(side, ticker, price, quantity);
    }

    private Double parsePositiveMoney(String rawValue) {
        Double value = parseMoney(rawValue);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private Double parseNonNegativeMoney(String rawValue) {
        Double value = parseMoney(rawValue);
        if (value == null || value < 0) {
            return null;
        }
        return value;
    }

    private Double parseMoney(String rawValue) {
        try {
            double value = Double.parseDouble(rawValue);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parsePositiveInteger(String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void acknowledgePendingOrder() {
        if (pendingOrder == null) {
            updateOrderFeedback("Server accepted the request.", "feedback-success");
            return;
        }

        updateOrderFeedback("Server accepted " + pendingOrder.summary() + ".", "feedback-success");
        addActivity("Accepted", pendingOrder.ticker(), pendingOrder.summary());
        pendingOrder = null;
    }

    private void rejectPendingOrder(String reason) {
        String displayReason = reason.isBlank() ? "Server rejected the request." : reason;
        if (pendingOrder == null) {
            updateOrderFeedback(displayReason, "feedback-error");
            addActivity("Rejected", "Server", displayReason);
            return;
        }

        updateOrderFeedback(displayReason, "feedback-error");
        addActivity("Rejected", pendingOrder.ticker(), pendingOrder.summary() + " - " + displayReason);
        pendingOrder = null;
    }

    private void rejectInput(String message) {
        updateOrderFeedback(message, "feedback-error");
        addActivity("Input", "Local", message);
    }

    private void updateOrderFeedback(String message, String stateClass) {
        orderFeedbackLabel.setText(message);
        orderFeedbackLabel.getStyleClass().removeAll("feedback-info", "feedback-success", "feedback-error");
        orderFeedbackLabel.getStyleClass().add(stateClass);
    }

    private void addActivity(String event, String ticker, String details) {
        activityRows.add(0, new ActivityRow(LocalTime.now().format(TIME_FORMAT), event, ticker, details));
        if (activityRows.size() > MAX_ACTIVITY_ROWS) {
            activityRows.remove(MAX_ACTIVITY_ROWS, activityRows.size());
        }
    }

    private String trimmed(TextField field) {
        String text = field.getText();
        return text == null ? "" : text.trim();
    }

    private String moneyText(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String moneyLabel(double value) {
        return "$" + moneyText(value);
    }

    private String displayHoldings(String rawHoldings) {
        return "NONE".equals(rawHoldings) ? "None" : rawHoldings.replace("|", ", ").replace("=", " ");
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

    private TableColumn<ActivityRow, String> tableColumn(String title, double width,
            java.util.function.Function<ActivityRow, StringProperty> property) {
        TableColumn<ActivityRow, String> column = new TableColumn<>(title);
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setReorderable(false);
        column.setCellValueFactory(cell -> property.apply(cell.getValue()));
        return column;
    }

    private record PendingOrder(String side, String ticker, double price, int quantity) {
        private String summary() {
            return String.format(Locale.US, "%s %d %s @ $%.2f", side, quantity, ticker, price);
        }
    }

    private static final class ActivityRow {
        private final StringProperty time;
        private final StringProperty event;
        private final StringProperty ticker;
        private final StringProperty details;

        private ActivityRow(String time, String event, String ticker, String details) {
            this.time = new SimpleStringProperty(time);
            this.event = new SimpleStringProperty(event);
            this.ticker = new SimpleStringProperty(ticker);
            this.details = new SimpleStringProperty(details);
        }

        private StringProperty timeProperty() {
            return time;
        }

        private StringProperty eventProperty() {
            return event;
        }

        private StringProperty tickerProperty() {
            return ticker;
        }

        private StringProperty detailsProperty() {
            return details;
        }
    }
}
