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

    private String username;
    private String currentRoom;

    private TextArea chatArea;
    private TextField inputField;
    private ListView<String> roomListView;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Distributed Chat Client");

        // Top - Login
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

        // Left - Rooms
        roomListView = new ListView<>();
        roomListView.setPrefWidth(180);

        TextField roomField = new TextField();
        roomField.setPromptText("room name");

        Button createRoomBtn = new Button("Create");
        Button joinRoomBtn = new Button("Join");
        Button leaveRoomBtn = new Button("Leave");
        Button refreshRoomsBtn = new Button("Refresh");

        VBox left = new VBox(8,
                new Label("Rooms"),
                roomListView,
                roomField,
                new HBox(6, createRoomBtn, joinRoomBtn),
                new HBox(6, leaveRoomBtn, refreshRoomsBtn)
        );
        left.setPadding(new Insets(10));

        // Center - Chat
        chatArea = new TextArea();
        chatArea.setEditable(false);

        inputField = new TextField();
        inputField.setPromptText("Type message...");
        Button sendBtn = new Button("Send");

        HBox sendBox = new HBox(8, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        statusLabel = new Label("Not connected.");

        VBox center = new VBox(8, chatArea, sendBox, statusLabel);
        center.setPadding(new Insets(10));
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setLeft(left);
        root.setCenter(center);

        Scene scene = new Scene(root, 900, 500);
        stage.setScene(scene);
        stage.show();

        // Network callbacks
        network.setOnResponse(resp -> Platform.runLater(() -> {
            appendSystem("RESP: " + (resp.isSuccess() ? "OK" : "ERR") + " - " + resp.getMessage());
        }));

        network.setOnEvent(event -> Platform.runLater(() -> {
            switch (event.getType()) {
                case SYSTEM_MESSAGE -> appendSystem(event.getText());
                case CHAT_MESSAGE -> appendChat(event.getMessage().format());
                case ROOM_LIST -> {
                    roomListView.getItems().setAll(event.getRooms());
                    appendSystem("Rooms updated.");
                }
            }
        }));

        network.setOnError(err -> Platform.runLater(() -> appendSystem(err)));

        // Actions
        connectBtn.setOnAction(e -> {
            try {
                if (username != null) {
                    appendSystem("Already connected.");
                    return;
                }
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                String u = usernameField.getText().trim();

                network.connect(host, port);
                network.send(new Request(RequestType.LOGIN, u, null, null));

                username = u;
                statusLabel.setText("Connected as: " + username);
                appendSystem("Connected.");
            } catch (Exception ex) {
                appendSystem("Connection error: " + ex.getMessage());
            }
        });

        createRoomBtn.setOnAction(e -> {
            String room = roomField.getText().trim();
            if (room.isEmpty()) return;
            sendReq(RequestType.CREATE_ROOM, room, null);
        });

        joinRoomBtn.setOnAction(e -> {
            String room = roomListView.getSelectionModel().getSelectedItem();
            if (room == null || room.isEmpty()) room = roomField.getText().trim();
            if (room == null || room.isEmpty()) return;

            sendReq(RequestType.JOIN_ROOM, room, null);
            currentRoom = room;
            statusLabel.setText("User: " + username + " | Room: " + currentRoom);
        });

        leaveRoomBtn.setOnAction(e -> {
            sendReq(RequestType.LEAVE_ROOM, null, null);
            currentRoom = null;
            statusLabel.setText("User: " + username + " | Room: -");
        });

        refreshRoomsBtn.setOnAction(e -> sendReq(RequestType.LIST_ROOMS, null, null));

        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        sendReq(RequestType.SEND_MESSAGE, null, text);
        inputField.clear();
    }

    private void sendReq(RequestType type, String room, String content) {
        if (username == null) {
            appendSystem("Login first.");
            return;
        }
        try {
            network.send(new Request(type, username, room, content));
        } catch (Exception e) {
            appendSystem("Send failed: " + e.getMessage());
        }
    }

    private void appendChat(String text) {
        chatArea.appendText(text + "\n");
    }

    private void appendSystem(String text) {
        chatArea.appendText("[SYSTEM] " + text + "\n");
    }

    @Override
    public void stop() {
        try {
            if (username != null) {
                network.send(new Request(RequestType.LOGOUT, username, null, null));
            }
        } catch (Exception ignored) {}
        network.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}