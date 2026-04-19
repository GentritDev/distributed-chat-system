package server;

import common.Event;
import common.Message;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ChatRoom {
    private final String name;
    private final Set<ClientSession> members = new HashSet<>();
    private final ReentrantLock membersLock = new ReentrantLock();

    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private final Thread dispatcherThread;

    public ChatRoom(String name) {
        this.name = name;
        this.dispatcherThread = new Thread(this::dispatchLoop, "RoomDispatcher-" + name);
        this.dispatcherThread.start();
    }

    public String getName() { return name; }

    public void addMember(ClientSession session) {
        membersLock.lock();
        try {
            members.add(session);
        } finally {
            membersLock.unlock();
        }
    }

    public void removeMember(ClientSession session) {
        membersLock.lock();
        try {
            members.remove(session);
        } finally {
            membersLock.unlock();
        }
    }

    public int memberCount() {
        membersLock.lock();
        try {
            return members.size();
        } finally {
            membersLock.unlock();
        }
    }

    public void enqueue(Message msg) throws InterruptedException {
        queue.put(msg);
    }

    private void dispatchLoop() {
        while (running) {
            try {
                Message msg = queue.take();
                broadcast(Event.chat(msg));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void broadcast(Event event) {
        membersLock.lock();
        try {
            Set<ClientSession> dead = new HashSet<>();
            for (ClientSession member : members) {
                try {
                    member.sendEvent(event);
                } catch (IOException e) {
                    dead.add(member);
                }
            }
            members.removeAll(dead);
        } finally {
            membersLock.unlock();
        }
    }

    public void shutdown() {
        running = false;
        dispatcherThread.interrupt();
    }
}