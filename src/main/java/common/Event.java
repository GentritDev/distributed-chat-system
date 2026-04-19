package common;

import java.io.Serializable;
import java.util.List;

public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    private EventType type;
    private String text;
    private Message message;
    private List<String> rooms;

    public static Event system(String text) {
        Event e = new Event();
        e.type = EventType.SYSTEM_MESSAGE;
        e.text = text;
        return e;
    }

    public static Event chat(Message m) {
        Event e = new Event();
        e.type = EventType.CHAT_MESSAGE;
        e.message = m;
        return e;
    }

    public static Event roomList(List<String> rooms) {
        Event e = new Event();
        e.type = EventType.ROOM_LIST;
        e.rooms = rooms;
        return e;
    }

    public EventType getType() { return type; }
    public String getText() { return text; }
    public Message getMessage() { return message; }
    public List<String> getRooms() { return rooms; }
}