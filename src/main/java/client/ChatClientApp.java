package client;

import common.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ChatClientApp extends Application {
    private final NetworkClient network = new NetworkClient();

    private String username;          // vendoset vetem pas LOGIN success
    private String pendingUsername;   // ruhet para login
    private String currentRoom;

    private RequestType lastRequestType;

    private TextArea chatArea;
    private TextField inputField;
    private ListView<String> roomListView;
    private Label statusLabel;
    private Label currentRoomLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Distributed Chat Client");

        // Top
        TextField hostField = new TextField("localhost");
        hostField.setPrefWidth(120);

        TextField portField = new TextField("5555");
        portField.setPrefWidth(70);

        TextField usernameField = new TextField();
        usernameField.setPromptText("username");

        Button connectBtn = new Button("Connect/Login");

        HBox top = new HBox(8,
                new Label("Host:"), hostField,
                new Label("Port:"), portField,
                new Label("Username:"), usernameField,
                connectBtn
        );
        top.setPadding(new Insets(10));

        // Left
        roomListView = new ListView<>();
        roomListView.setPrefWidth(180);

        TextField roomField = new TextField();
        roomField.setPromptText("room name");

        Button createRoomBtn = new Button("Create");
        Button joinRoomBtn = new Button("Join");
        Button leaveRoomBtn = new Button("Leave");

        VBox left = new VBox(8,
                new Label("Rooms"),
                roomListView,
                roomField,
                new HBox(6, createRoomBtn, joinRoomBtn),
                leaveRoomBtn
        );
        left.setPadding(new Insets(10));

        // Center
        chatArea = new TextArea();
        chatArea.setEditable(false);

        inputField = new TextField();
        inputField.setPromptText("Type message...");
        Button sendBtn = new Button("Send");

        HBox sendBox = new HBox(8, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        statusLabel = new Label("Not connected.");
        currentRoomLabel = new Label("Room aktiv: -");
        currentRoomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0a84ff;");

        VBox center = new VBox(8, currentRoomLabel, chatArea, sendBox, statusLabel);
        center.setPadding(new Insets(10));
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setLeft(left);
        root.setCenter(center);

        stage.setScene(new Scene(root, 900, 500));
        stage.show();

        // callbacks
        network.setOnResponse(resp -> Platform.runLater(() -> {
            if (!resp.isSuccess()) {
                appendSystem("Gabim: " + resp.getMessage());
                return;
            }

            switch (lastRequestType) {
                case LOGIN -> {
                    username = pendingUsername;
                    statusLabel.setText("I lidhur si: " + username);
                    appendSystem("U lidhe si " + username + ".");
                    refreshRoomsAuto();
                }
                case CREATE_ROOM, JOIN_ROOM, LEAVE_ROOM -> refreshRoomsAuto();
                default -> { }
            }
        }));

        network.setOnEvent(event -> Platform.runLater(() -> {
            switch (event.getType()) {
                case SYSTEM_MESSAGE -> appendSystem(event.getText());
                case CHAT_MESSAGE -> appendChat(event.getMessage().format());
                case ROOM_LIST -> roomListView.getItems().setAll(event.getRooms());
            }
        }));

        network.setOnError(err -> Platform.runLater(() ->
                appendSystem("Lidhja u nderpre: " + err)));

        // actions
        connectBtn.setOnAction(e -> {
            if (username != null) {
                appendSystem("Tashme je i lidhur.");
                return;
            }

            String u = usernameField.getText().trim();
            if (u.isEmpty()) {
                appendSystem("Shkruaj nje username.");
                return;
            }

            try {
                int port = Integer.parseInt(portField.getText().trim());
                network.connect(hostField.getText().trim(), port);

                pendingUsername = u;
                sendReq(RequestType.LOGIN, null, null); // LOGIN vetem ketu
            } catch (NumberFormatException ex) {
                appendSystem("Port i pavlefshem.");
            } catch (Exception ex) {
                appendSystem("Gabim lidhje: " + ex.getMessage());
            }
        });

        createRoomBtn.setOnAction(e -> {
            String room = roomField.getText().trim();
            if (!room.isEmpty()) sendReq(RequestType.CREATE_ROOM, room, null);
        });

        joinRoomBtn.setOnAction(e -> {
            String room = roomListView.getSelectionModel().getSelectedItem();
            if (room == null || room.isEmpty()) room = roomField.getText().trim();
            if (room == null || room.isEmpty()) return;

            sendReq(RequestType.JOIN_ROOM, room, null);

            // UI update lokale (server zakonisht e pranon nese room ekziston)
            currentRoom = room;
            currentRoomLabel.setText("Room aktiv: " + currentRoom);
            statusLabel.setText("Perdoruesi: " + username + " | Room: " + currentRoom);
        });

        leaveRoomBtn.setOnAction(e -> {
            sendReq(RequestType.LEAVE_ROOM, null, null);
            currentRoom = null;
            currentRoomLabel.setText("Room aktiv: -");
            statusLabel.setText("Perdoruesi: " + username + " | Room: -");
        });

        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
    }

    private void refreshRoomsAuto() {
        sendReq(RequestType.LIST_ROOMS, null, null);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        sendReq(RequestType.SEND_MESSAGE, null, text);
        inputField.clear();
    }

    private void sendReq(RequestType type, String room, String content) {
        if (type != RequestType.LOGIN && username == null) {
            appendSystem("Hyr fillimisht.");
            return;
        }

        try {
            lastRequestType = type;
            String userToSend = (type == RequestType.LOGIN) ? pendingUsername : username;
            network.send(new Request(type, userToSend, room, content));
        } catch (Exception e) {
            appendSystem("Dergimi deshtoi: " + e.getMessage());
        }
    }

    private void appendChat(String text) {
        chatArea.appendText(text + "\n");
    }

    private void appendSystem(String text) {
        chatArea.appendText("[SISTEM] " + text + "\n");
    }

    @Override
    public void stop() {
        try {
            if (username != null) {
                network.send(new Request(RequestType.LOGOUT, username, null, null));
            }
        } catch (Exception ignored) { }
        network.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}