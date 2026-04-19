package client;

import common.*;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected = false;

    private Consumer<Response> onResponse;
    private Consumer<Event> onEvent;
    private Consumer<String> onError;

    public void setOnResponse(Consumer<Response> onResponse) { this.onResponse = onResponse; }
    public void setOnEvent(Consumer<Event> onEvent) { this.onEvent = onEvent; }
    public void setOnError(Consumer<String> onError) { this.onError = onError; }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;

        Thread receiver = new Thread(this::receiveLoop, "ClientReceiver");
        receiver.setDaemon(true);
        receiver.start();
    }

    private void receiveLoop() {
        try {
            while (connected) {
                Object obj = in.readObject();
                if (obj instanceof Response resp) {
                    if (onResponse != null) onResponse.accept(resp);
                } else if (obj instanceof Event event) {
                    if (onEvent != null) onEvent.accept(event);
                }
            }
        } catch (Exception e) {
            if (onError != null) onError.accept("Disconnected: " + e.getMessage());
        } finally {
            close();
        }
    }

    public synchronized void send(Request req) throws IOException {
        out.writeObject(req);
        out.flush();
    }

    public synchronized void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}