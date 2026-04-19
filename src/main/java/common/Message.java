package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sender;
    private String roomName;
    private String content;
    private LocalDateTime timestamp;

    public Message(String sender, String roomName, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.roomName = roomName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSender() { return sender; }
    public String getRoomName() { return roomName; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String format() {
        return "[" + timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                + sender + ": " + content;
    }
}