package client.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX trading dashboard that connects to the socket server and renders live market state.
 */
public class TradeHouseApp extends Application {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static final String DEFAULT_TICKER = "AAPL";
    private static final String[] WATCHLIST = {"AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "BTC"};
    private static final Map<String, Double> STARTING_PRICES = Map.of(
            "AAPL", 150.00,
            "MSFT", 420.00,
            "TSLA", 250.00,
            "GOOGL", 175.00,
            "AMZN", 185.00,
            "BTC", 65000.00);
    private static final int MAX_CHART_POINTS = 48;
    private static final int SEEDED_CHART_POINTS = 18;
    private static final double MIN_AXIS_PADDING_RATIO = 0.018;
    private static final double MIN_AXIS_PADDING_PRICE = 0.75;
    private static final int MAX_ACTIVITY_ROWS = 12;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Shared dashboard controls are fields so socket callbacks can update live UI state.
    private final Label statusLabel = new Label("Offline");
    private final Label cashLabel = valueLabel("Waiting");
    private final Label availableCashLabel = valueLabel("--");
    private final Label holdingsLabel = valueLabel("None");
    private final Label profitLossLabel = valueLabel("$0.00");
    private final Label lastPriceLabel = valueLabel(moneyLabel(STARTING_PRICES.get(DEFAULT_TICKER)));
    private final Label activeTickerLabel = valueLabel(DEFAULT_TICKER);
    private final Label feedStateLabel = valueLabel("Idle");
    private final Label orderFeedbackLabel = bodyText("Login before submitting an order.");
    private final TextField usernameField = new TextField();
    private final TextField balanceField = new TextField("10000");
    private final TextField fundsField = new TextField("1000");
    private final ComboBox<String> sideBox = new ComboBox<>();
    private final ComboBox<String> tickerBox = new ComboBox<>();
    private final TextField priceField = new TextField();
    private final TextField quantityField = new TextField("5");
    private final Button submitButton = new Button("Submit Order");
    private final Button addFundsButton = new Button("Add Funds");
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private final ObservableList<ActivityRow> activityRows = FXCollections.observableArrayList();
    private final TableView<ActivityRow> activityTable = new TableView<>(activityRows);
    private final Map<String, Double> latestPrices = new HashMap<>(STARTING_PRICES);
    private final Map<String, List<Double>> priceHistory = new HashMap<>();
    private final RadioMenuItem normalVolatilityItem = new RadioMenuItem("Normal");
    private final RadioMenuItem wildVolatilityItem = new RadioMenuItem("Wild");
    private final CheckMenuItem soundMenuItem = new CheckMenuItem("Sound On");
    private final Button themeButton = new Button();
    private final Map<String, Integer> currentHoldings = new HashMap<>();

    private BorderPane root;
    private LineChart<Number, Number> marketChart;
    private NumberAxis chartXAxis;
    private NumberAxis chartYAxis;
    private Stage primaryStage;
    private Stage aboutStage;
    private VBox aboutRoot;
    private ClientNetwork network;
    private PendingOrder pendingOrder;
    private int chartTick = 5;
    private String selectedTicker = DEFAULT_TICKER;
    private double startingBalanceBaseline;
    private double currentCash;
    private boolean sessionBaselineReady;
    private boolean darkTheme;
    private boolean soundEnabled = true;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        startingBalanceBaseline = 0;
        currentCash = startingBalanceBaseline;
        darkTheme = isSystemDarkTheme();

        root = new BorderPane();
        root.getStyleClass().add("app-root");

        root.setTop(buildTopArea());
        root.setCenter(buildWorkspace());
        selectTicker(DEFAULT_TICKER);

        applyTheme();

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

    private VBox buildTopArea() {
        return new VBox(buildMenuBar(), buildHeader());
    }

    // AI assisted with menu placement ideas; the JavaFX wiring was reviewed and tested by the team.
    private MenuBar buildMenuBar() {
        MenuItem aboutItem = new MenuItem("About TradeHouse");
        aboutItem.setOnAction(event -> showAboutWindow());

        Menu appMenu = new Menu("About");
        appMenu.getItems().add(aboutItem);

        ToggleGroup volatilityGroup = new ToggleGroup();
        normalVolatilityItem.setToggleGroup(volatilityGroup);
        wildVolatilityItem.setToggleGroup(volatilityGroup);
        normalVolatilityItem.setSelected(true);
        normalVolatilityItem.setOnAction(event -> sendVolatilitySetting());
        wildVolatilityItem.setOnAction(event -> sendVolatilitySetting());

        Menu volatilityMenu = new Menu("Volatility");
        volatilityMenu.getItems().addAll(normalVolatilityItem, wildVolatilityItem);

        soundMenuItem.setSelected(soundEnabled);
        soundMenuItem.setOnAction(event -> setSoundEnabled(soundMenuItem.isSelected()));
        updateSoundMenuLabel();

        Menu volumeMenu = new Menu("Volume");
        volumeMenu.getItems().add(soundMenuItem);

        MenuBar menuBar = new MenuBar(appMenu, volatilityMenu, volumeMenu);
        menuBar.setUseSystemMenuBar(true);
        menuBar.getStyleClass().add("app-menu");
        return menuBar;
    }

    // Header keeps status visible while secondary app settings live in the menu bar.
    private HBox buildHeader() {
        Label eyebrow = new Label("MARKET SIMULATION");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("TradeHouse");
        title.getStyleClass().add("app-title");

        VBox titleBlock = new VBox(4, eyebrow, title);

        statusLabel.getStyleClass().addAll("status-pill", "status-offline");

        themeButton.getStyleClass().add("utility-button");
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

    // Left panel handles login and shows the server-owned portfolio snapshot.
    // AI helped compare compact account-panel layouts; server state remains the source of truth.
    private VBox buildAccountPanel() {
        usernameField.setPromptText("Username");
        balanceField.setPromptText("Starting balance");
        fundsField.setPromptText("Amount");
        profitLossLabel.getStyleClass().add("pnl-flat");

        Button loginButton = new Button("Connect and Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> connectAndLogin());

        addFundsButton.getStyleClass().add("utility-button");
        addFundsButton.setDisable(true);
        addFundsButton.setOnAction(event -> addFunds());
        HBox fundsRow = new HBox(8, fundsField, addFundsButton);
        HBox.setHgrow(fundsField, Priority.ALWAYS);

        VBox panel = new VBox(14,
                sectionTitle("Account"),
                formLabel("Username"),
                usernameField,
                formLabel("Starting Balance"),
                balanceField,
                loginButton,
                formLabel("Add Funds"),
                fundsRow,
                new Separator(),
                metricLine("Cash", cashLabel),
                metricLine("Available", availableCashLabel),
                metricLine("Holdings", holdingsLabel),
                metricLine("Profit / Loss", profitLossLabel),
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

    // Center panel shows the live market chart and starts with demo-friendly seed points.
    private VBox buildMarketChart() {
        chartXAxis = new NumberAxis();
        chartYAxis = new NumberAxis();
        chartXAxis.setLabel("Ticks");
        chartYAxis.setLabel("Price");
        chartXAxis.setAutoRanging(false);
        chartXAxis.setForceZeroInRange(false);
        chartXAxis.setLowerBound(1);
        chartXAxis.setUpperBound(MAX_CHART_POINTS);
        chartXAxis.setTickUnit(6);
        chartYAxis.setAutoRanging(false);
        chartYAxis.setForceZeroInRange(false);
        chartYAxis.setTickUnit(1);

        marketChart = new LineChart<>(chartXAxis, chartYAxis);
        marketChart.getStyleClass().add("market-chart");
        marketChart.setTitle(DEFAULT_TICKER + " Market Pulse");
        marketChart.setAnimated(false);
        marketChart.setLegendVisible(false);
        marketChart.setCreateSymbols(true);
        marketChart.getData().add(priceSeries);

        VBox chartPanel = new VBox(10, sectionTitle("Market Pulse"), marketChart);
        chartPanel.getStyleClass().add("surface-panel");
        VBox.setVgrow(marketChart, Priority.ALWAYS);
        return chartPanel;
    }

    // Right panel builds the order ticket and inline validation feedback.
    private VBox buildOrderPanel() {
        sideBox.getItems().addAll("BUY", "SELL");
        sideBox.setValue("BUY");
        sideBox.setMaxWidth(Double.MAX_VALUE);

        tickerBox.getItems().addAll(WATCHLIST);
        tickerBox.setValue(DEFAULT_TICKER);
        tickerBox.setMaxWidth(Double.MAX_VALUE);
        tickerBox.setOnAction(event -> selectTicker(tickerBox.getValue()));

        sideBox.setOnAction(event -> updateSuggestedOrderPrice(true));

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
            startingBalanceBaseline = startingBalance;
            currentCash = startingBalance;
            currentHoldings.clear();
            sessionBaselineReady = false;
            addFundsButton.setDisable(true);
            updateProfitLoss();
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

    private void addFunds() {
        Double amount = parsePositiveMoney(trimmed(fundsField));
        if (amount == null) {
            rejectInput("Add funds amount must be greater than 0.");
            return;
        }

        try {
            ensureConnected();
            network.send("ADD_FUNDS:" + moneyText(amount));
            updateOrderFeedback("Add funds request sent for " + moneyLabel(amount) + ".", "feedback-info");
            addActivity("Funds", "Account", "Requested " + moneyLabel(amount));
        } catch (RuntimeException | java.io.IOException e) {
            setOffline();
            updateOrderFeedback("Add funds failed: " + e.getMessage(), "feedback-error");
            addActivity("Error", "Funds", e.getMessage());
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
        sendVolatilitySetting();
    }

    // Server messages are routed onto the JavaFX thread before touching controls.
    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("LOGIN_SUCCESS:")) {
                String[] parts = message.split(":");
                if (parts.length >= 3) {
                    cashLabel.setText("$" + parts[2]);
                    holdingsLabel.setText("Waiting for trades");
                    addFundsButton.setDisable(false);
                    Double loginCash = parseMoney(parts[2]);
                    if (loginCash != null) {
                        currentCash = loginCash;
                        updateProfitLoss();
                    }
                    setStatus("Logged in as " + parts[1], true);
                    submitButton.setDisable(false);
                    updateOrderFeedback("Logged in. Order ticket is ready.", "feedback-success");
                    addActivity("Accepted", parts[1], "Login confirmed with cash $" + parts[2]);
                    playSuccessCue();
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
            } else if (message.startsWith("VOLATILITY_UPDATE:")) {
                handleVolatilityUpdate(message);
            } else if (message.startsWith("FUNDS_ADDED:")) {
                handleFundsAdded(message);
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
        Double cash = parseMoney(parts[2]);
        if (cash != null) {
            currentCash = cash;
        }
        currentHoldings.clear();
        currentHoldings.putAll(parseHoldings(parts[4]));
        cashLabel.setText("$" + parts[2]);
        availableCashLabel.setText("$" + parts[3]);
        holdingsLabel.setText(holdings);
        if (!sessionBaselineReady) {
            startingBalanceBaseline = portfolioValue();
            sessionBaselineReady = true;
        }
        updateProfitLoss();
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
            double previousPrice = latestPrices.getOrDefault(ticker, price);
            latestPrices.put(ticker, price);
            recordPrice(ticker, price);
            updateProfitLoss();

            if (!ticker.equals(selectedTicker)) {
                feedStateLabel.setText("Live");
                return;
            }

            lastPriceLabel.setText(String.format("$%.2f", price));
            feedStateLabel.setText("Live " + ticker);
            updateSuggestedOrderPrice(false);
            if (chartTick % 5 == 0) {
                addActivity("Market", ticker, "Live price " + moneyLabel(price));
            }
            priceSeries.getData().add(createChartPoint(++chartTick, price));
            if (priceSeries.getData().size() > MAX_CHART_POINTS) {
                priceSeries.getData().remove(0);
            }
            refreshChartViewport();
            applyChartDirection(price, previousPrice);
        } catch (NumberFormatException e) {
            addActivity("Ignored", "Market", "Malformed update: " + message);
        }
    }

    private void handleVolatilityUpdate(String message) {
        String mode = message.substring("VOLATILITY_UPDATE:".length()).trim().toUpperCase(Locale.ROOT);
        feedStateLabel.setText(displayVolatilityMode(mode) + " volatility");
        addActivity("Volatility", "Bot", displayVolatilityMode(mode));
    }

    private void handleFundsAdded(String message) {
        String[] parts = message.split(":");
        if (parts.length < 3) {
            return;
        }

        Double amount = parseMoney(parts[2]);
        if (amount == null) {
            return;
        }

        startingBalanceBaseline += amount;
        currentCash += amount;
        updateProfitLoss();
        updateOrderFeedback("Added " + moneyLabel(amount) + " to account.", "feedback-success");
        addActivity("Funds", parts[1], "Added " + moneyLabel(amount));
        playSuccessCue();
    }

    private void showAboutWindow() {
        if (aboutStage == null) {
            aboutRoot = buildAboutContent();
            Scene scene = new Scene(aboutRoot, 460, 360);
            scene.getStylesheets().add(getClass().getResource("/client/gui/tradehouse.css").toExternalForm());

            aboutStage = new Stage();
            aboutStage.setTitle("About TradeHouse");
            aboutStage.initOwner(primaryStage);
            aboutStage.setScene(scene);
        }

        applyThemeTo(aboutRoot);
        aboutStage.show();
        aboutStage.toFront();
    }

    // AI assisted with concise About-window wording; the project details were verified by the team.
    private VBox buildAboutContent() {
        Label title = sectionTitle("About TradeHouse");
        Label description = bodyText("A JavaFX trading dashboard backed by a multi-threaded socket server, live bot market makers, account tracking, and CSV persistence.");
        Label members = bodyText("Karan Bhaskar, Mohammad Ali Jafari, Rayan Khan");

        VBox content = new VBox(14,
                title,
                description,
                new Separator(),
                formLabel("Made by"),
                members,
                new Separator(),
                formLabel("Application notes"),
                bodyText("Use the application menu bar for About, Volatility, and Volume. Normal is the default live-market mode. Wild makes bot prices move more aggressively for demos."));
        content.getStyleClass().add("app-root");
        content.setPadding(new Insets(20));
        return content;
    }

    private void handleNetworkError(String message) {
        Platform.runLater(() -> {
            setOffline();
            updateOrderFeedback(message, "feedback-error");
            addActivity("Network", "Server", message);
            playErrorCue();
        });
    }

    private void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        AudioManager.setEnabled(enabled);
        updateSoundMenuLabel();
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        applyTheme();
    }

    private void applyTheme() {
        applyThemeTo(root);
        if (aboutRoot != null) {
            applyThemeTo(aboutRoot);
        }
        updateThemeButtonLabel();
    }

    private void applyThemeTo(VBox container) {
        container.getStyleClass().removeAll("theme-dark", "theme-light");
        container.getStyleClass().add(darkTheme ? "theme-dark" : "theme-light");
    }

    private void applyThemeTo(BorderPane container) {
        container.getStyleClass().removeAll("theme-dark", "theme-light");
        container.getStyleClass().add(darkTheme ? "theme-dark" : "theme-light");
    }

    private void updateThemeButtonLabel() {
        themeButton.setText(darkTheme ? "Light" : "Dark");
    }

    private void updateSoundMenuLabel() {
        soundMenuItem.setSelected(soundEnabled);
        soundMenuItem.setText(soundEnabled ? "Sound On" : "Muted");
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

    private void selectTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return;
        }

        selectedTicker = ticker;
        activeTickerLabel.setText(ticker);
        double price = latestPrices.getOrDefault(ticker, STARTING_PRICES.getOrDefault(ticker, 150.00));
        lastPriceLabel.setText(moneyLabel(price));
        feedStateLabel.setText("Watching " + ticker);
        renderSelectedTickerChart(price);
        updateSuggestedOrderPrice(true);
    }

    private void renderSelectedTickerChart(double fallbackPrice) {
        priceSeries.getData().clear();
        chartTick = 0;
        if (marketChart != null) {
            marketChart.setTitle(selectedTicker + " Market Pulse");
        }

        List<Double> history = ensureHistoryForTicker(selectedTicker, fallbackPrice);

        int start = Math.max(0, history.size() - MAX_CHART_POINTS);
        for (int i = start; i < history.size(); i++) {
            priceSeries.getData().add(createChartPoint(++chartTick, history.get(i)));
        }
        refreshChartViewport();
        applyChartDirection(history.get(history.size() - 1), history.get(Math.max(0, history.size() - 2)));
    }

    private void recordPrice(String ticker, double price) {
        List<Double> history = priceHistory.computeIfAbsent(ticker, key -> new ArrayList<>());
        history.add(price);
        if (history.size() > MAX_CHART_POINTS) {
            history.remove(0);
        }
    }

    private List<Double> ensureHistoryForTicker(String ticker, double anchorPrice) {
        List<Double> history = priceHistory.computeIfAbsent(ticker, key -> new ArrayList<>());
        if (!history.isEmpty()) {
            return history;
        }

        double basePadding = Math.max(MIN_AXIS_PADDING_PRICE, anchorPrice * 0.02);
        for (int i = 0; i < SEEDED_CHART_POINTS; i++) {
            double wave = Math.sin(i * 0.72) * basePadding;
            double pullback = Math.cos(i * 0.31) * basePadding * 0.45;
            double shapedPrice = Math.max(0.01, anchorPrice + wave + pullback);
            history.add(roundPrice(shapedPrice));
        }
        return history;
    }

    private void applyChartDirection(double price, double previousPrice) {
        var line = priceSeries.getNode();
        if (line == null) {
            return;
        }
        line.getStyleClass().removeAll("chart-up", "chart-down");
        line.getStyleClass().add(price < previousPrice ? "chart-down" : "chart-up");
    }

    private void refreshChartViewport() {
        if (chartXAxis == null || chartYAxis == null || priceSeries.getData().isEmpty()) {
            return;
        }

        int visiblePoints = priceSeries.getData().size();
        chartXAxis.setLowerBound(Math.max(1, chartTick - MAX_CHART_POINTS + 1));
        chartXAxis.setUpperBound(Math.max(MAX_CHART_POINTS, chartTick));

        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (XYChart.Data<Number, Number> point : priceSeries.getData()) {
            double value = point.getYValue().doubleValue();
            minPrice = Math.min(minPrice, value);
            maxPrice = Math.max(maxPrice, value);
        }

        if (visiblePoints == 1) {
            minPrice -= MIN_AXIS_PADDING_PRICE;
            maxPrice += MIN_AXIS_PADDING_PRICE;
        }

        double span = Math.max(maxPrice - minPrice, MIN_AXIS_PADDING_PRICE);
        double padding = Math.max(span * MIN_AXIS_PADDING_RATIO * 5, MIN_AXIS_PADDING_PRICE);
        chartYAxis.setLowerBound(Math.max(0.01, minPrice - padding));
        chartYAxis.setUpperBound(maxPrice + padding);
        chartYAxis.setTickUnit(Math.max(span / 5.0, MIN_AXIS_PADDING_PRICE / 2.0));
    }

    private XYChart.Data<Number, Number> createChartPoint(int tick, double price) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(tick, price);
        point.nodeProperty().addListener((observable, oldNode, newNode) -> installPointTooltip(newNode, tick, price));
        return point;
    }

    private void installPointTooltip(Node node, int tick, double price) {
        if (node == null) {
            return;
        }

        Tooltip tooltip = new Tooltip(String.format(Locale.US,
                "%s%nTick %d%nPrice %s",
                selectedTicker,
                tick,
                moneyLabel(price)));
        tooltip.getStyleClass().add("chart-tooltip");
        Tooltip.install(node, tooltip);
        node.setOnMouseClicked(event -> {
            tooltip.show(node, event.getScreenX() + 10, event.getScreenY() - 10);
            event.consume();
        });
        node.setOnMouseExited(event -> tooltip.hide());
    }

    private double roundPrice(double price) {
        return Math.round(price * 100.0) / 100.0;
    }

    private void updateSuggestedOrderPrice(boolean force) {
        if (!force && priceField.isFocused()) {
            return;
        }

        double marketPrice = latestPrices.getOrDefault(selectedTicker, STARTING_PRICES.getOrDefault(selectedTicker, 150.00));
        double spread = Math.max(0.25, marketPrice * 0.002);
        double suggestedPrice = "SELL".equals(sideBox.getValue()) ? marketPrice - spread : marketPrice + spread;
        priceField.setText(moneyText(Math.max(0.01, suggestedPrice)));
    }

    private void updateProfitLoss() {
        double portfolioValue = portfolioValue();
        double profitLoss = portfolioValue - startingBalanceBaseline;
        profitLossLabel.setText(signedMoneyLabel(profitLoss));
        profitLossLabel.getStyleClass().removeAll("pnl-profit", "pnl-loss", "pnl-flat");
        if (profitLoss > 0.005) {
            profitLossLabel.getStyleClass().add("pnl-profit");
        } else if (profitLoss < -0.005) {
            profitLossLabel.getStyleClass().add("pnl-loss");
        } else {
            profitLossLabel.getStyleClass().add("pnl-flat");
        }
    }

    private double portfolioValue() {
        double portfolioValue = currentCash;
        for (Map.Entry<String, Integer> entry : currentHoldings.entrySet()) {
            double price = latestPrices.getOrDefault(entry.getKey(), STARTING_PRICES.getOrDefault(entry.getKey(), 0.0));
            portfolioValue += entry.getValue() * price;
        }
        return portfolioValue;
    }

    private void sendVolatilitySetting() {
        if (network == null || !network.isConnected()) {
            feedStateLabel.setText(displayVolatilityMode(selectedVolatilityMode()) + " volatility");
            return;
        }

        try {
            network.send("VOLATILITY:" + selectedVolatilityMode());
        } catch (RuntimeException e) {
            addActivity("Volatility", "Bot", "Could not update volatility: " + e.getMessage());
        }
    }

    private String selectedVolatilityMode() {
        return wildVolatilityItem.isSelected() ? "WILD" : "NORMAL";
    }

    private String displayVolatilityMode(String mode) {
        return "WILD".equals(mode) ? "Wild" : "Normal";
    }

    // Local validation keeps obvious bad orders out of the socket protocol.
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
        playErrorCue();
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

    private void playSuccessCue() {
        if (soundEnabled) {
            AudioManager.playSuccess();
        }
    }

    private void playErrorCue() {
        if (soundEnabled) {
            AudioManager.playError();
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

    private String signedMoneyLabel(double value) {
        if (Math.abs(value) < 0.005) {
            return "$0.00";
        }
        return String.format(Locale.US, "%s$%.2f", value > 0 ? "+" : "-", Math.abs(value));
    }

    private String displayHoldings(String rawHoldings) {
        return "NONE".equals(rawHoldings) ? "None" : rawHoldings.replace("|", ", ").replace("=", " ");
    }

    private Map<String, Integer> parseHoldings(String rawHoldings) {
        Map<String, Integer> holdings = new HashMap<>();
        if (rawHoldings == null || rawHoldings.isBlank() || "NONE".equals(rawHoldings)) {
            return holdings;
        }

        for (String position : rawHoldings.split("\\|")) {
            String[] parts = position.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                holdings.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return holdings;
    }

    // Small UI factory helpers keep the layout readable without separate component classes.
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
