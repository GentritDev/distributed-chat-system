package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer {
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    // Shared structures (protected with locks as requested)
    private final Map<String, ClientSession> clientsByUsername = new HashMap<>();
    private final ReentrantLock clientsLock = new ReentrantLock();

    private final Map<String, ChatRoom> roomsByName = new HashMap<>();
    private final ReentrantLock roomsLock = new ReentrantLock();

    public ChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = 5555;
        new ChatServer(port).start();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("ChatServer running on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            pool.execute(new ClientHandler(clientSocket, this));
        }
    }

    public boolean registerUsername(String username, ClientSession session) {
        clientsLock.lock();
        try {
            if (clientsByUsername.containsKey(username)) return false;
            clientsByUsername.put(username, session);
            return true;
        } finally {
            clientsLock.unlock();
        }
    }

    public void unregisterUsername(String username) {
        if (username == null) return;
        clientsLock.lock();
        try {
            clientsByUsername.remove(username);
        } finally {
            clientsLock.unlock();
        }
    }

    public ChatRoom createRoomIfAbsent(String roomName) {
        roomsLock.lock();
        try {
            return roomsByName.computeIfAbsent(roomName, ChatRoom::new);
        } finally {
            roomsLock.unlock();
        }
    }

    public ChatRoom getRoom(String roomName) {
        roomsLock.lock();
        try {
            return roomsByName.get(roomName);
        } finally {
            roomsLock.unlock();
        }
    }

    public List<String> listRooms() {
        roomsLock.lock();
        try {
            return new ArrayList<>(roomsByName.keySet());
        } finally {
            roomsLock.unlock();
        }
    }

    public void removeRoomIfEmpty(String roomName) {
//        roomsLock.lock();
//        try {
//            ChatRoom room = roomsByName.get(roomName);
//            if (room != null && room.memberCount() == 0) {
//                room.shutdown();
//                roomsByName.remove(roomName); // vetëm ky room
//            }
//        } finally {
//            roomsLock.unlock();
//        }
    }
}