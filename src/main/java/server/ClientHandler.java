package server;

import common.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private ClientSession session;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            session = new ClientSession(socket, out);

            while (running) {
                Object obj = in.readObject();
                if (!(obj instanceof Request)) continue;

                Request req = (Request) obj;
                handle(req);
            }
        } catch (EOFException ignored) {
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handle(Request req) throws IOException, InterruptedException {
        switch (req.getType()) {
            case LOGIN -> handleLogin(req);
            case CREATE_ROOM -> handleCreateRoom(req);
            case JOIN_ROOM -> handleJoinRoom(req);
            case LEAVE_ROOM -> handleLeaveRoom();
            case LIST_ROOMS -> handleListRooms();
            case SEND_MESSAGE -> handleSendMessage(req);
            case LOGOUT -> {
                sendResponse(true, "Logout successful.");
                running = false;
            }
        }
    }

    private void handleLogin(Request req) throws IOException {
        String username = req.getUsername();
        if (username == null || username.trim().isEmpty()) {
            sendResponse(false, "Username invalid.");
            return;
        }

        boolean ok = server.registerUsername(username, session);
        if (!ok) {
            sendResponse(false, "Username already exists.");
            return;
        }

        session.setUsername(username);
        sendResponse(true, "Login successful.");
    }

    private void handleCreateRoom(Request req) throws IOException {
        if (!isLoggedIn()) return;
        String roomName = req.getRoomName();
        if (roomName == null || roomName.trim().isEmpty()) {
            sendResponse(false, "Room name invalid.");
            return;
        }
        server.createRoomIfAbsent(roomName);
        sendResponse(true, "Room ready: " + roomName);
    }

    private void handleJoinRoom(Request req) throws IOException {
        if (!isLoggedIn()) return;
        String roomName = req.getRoomName();
        ChatRoom room = server.getRoom(roomName);
        if (room == null) {
            sendResponse(false, "Room does not exist.");
            return;
        }

        leaveCurrentRoomIfAny();

        room.addMember(session);
        session.setCurrentRoom(roomName);

        sendResponse(true, "Joined room: " + roomName);
        room.broadcast(Event.system(session.getUsername() + " joined room."));
    }

    private void handleLeaveRoom() throws IOException {
        if (!isLoggedIn()) return;

        String oldRoom = session.getCurrentRoom();
        if (oldRoom == null) {
            sendResponse(false, "You are not in a room.");
            return;
        }

        ChatRoom room = server.getRoom(oldRoom);
        if (room != null) {
            room.removeMember(session);
            room.broadcast(Event.system(session.getUsername() + " left room."));
        }
        session.setCurrentRoom(null);
        server.removeRoomIfEmpty(oldRoom);

        sendResponse(true, "Left room: " + oldRoom);
    }

    private void handleListRooms() throws IOException {
        if (!isLoggedIn()) return;
        List<String> rooms = server.listRooms();
        session.sendEvent(Event.roomList(rooms));
        sendResponse(true, "Rooms listed.");
    }

    private void handleSendMessage(Request req) throws IOException, InterruptedException {
        if (!isLoggedIn()) return;

        String roomName = session.getCurrentRoom();
        if (roomName == null) {
            sendResponse(false, "Join a room first.");
            return;
        }

        String content = req.getContent();
        if (content == null || content.trim().isEmpty()) {
            sendResponse(false, "Message is empty.");
            return;
        }

        ChatRoom room = server.getRoom(roomName);
        if (room == null) {
            sendResponse(false, "Room not found.");
            return;
        }

        Message msg = new Message(session.getUsername(), roomName, content, LocalDateTime.now());
        room.enqueue(msg);
        sendResponse(true, "Message sent.");
    }

    private boolean isLoggedIn() throws IOException {
        if (session.getUsername() == null) {
            sendResponse(false, "Login first.");
            return false;
        }
        return true;
    }

    private void leaveCurrentRoomIfAny() {
        String oldRoom = session.getCurrentRoom();
        if (oldRoom == null) return;

        ChatRoom old = server.getRoom(oldRoom);
        if (old != null) {
            old.removeMember(session);
            old.broadcast(Event.system(session.getUsername() + " switched room."));
        }
        session.setCurrentRoom(null);
        server.removeRoomIfEmpty(oldRoom);
    }


    private void sendResponse(boolean success, String message) throws IOException {
        session.sendResponse(new Response(success, message));
    }

    private void cleanup() {
        try {
            if (session != null) {
                leaveCurrentRoomIfAny();
                server.unregisterUsername(session.getUsername());
                session.close();
            } else {
                socket.close();
            }
        } catch (Exception ignored) {}
    }
}