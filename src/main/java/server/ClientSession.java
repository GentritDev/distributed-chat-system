package server;

import common.Event;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class ClientSession {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ReentrantLock sendLock = new ReentrantLock();

    private String username;
    private String currentRoom;

    public ClientSession(Socket socket, ObjectOutputStream out) {
        this.socket = socket;
        this.out = out;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }

    public void sendEvent(Event event) throws IOException {
        sendLock.lock();
        try {
            synchronized (out) {
                out.writeObject(event);
                out.flush();
                out.reset();
            }
        } finally {
            sendLock.unlock();
        }
    }
    public void sendResponse(common.Response response) throws IOException {
        sendLock.lock();
        try {
            out.writeObject(response);
            out.flush();
            out.reset();
        } finally {
            sendLock.unlock();
        }
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}