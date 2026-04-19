package common;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private RequestType type;
    private String username;
    private String roomName;
    private String content;

    public Request(RequestType type, String username, String roomName, String content) {
        this.type = type;
        this.username = username;
        this.roomName = roomName;
        this.content = content;
    }

    public RequestType getType() { return type; }
    public String getUsername() { return username; }
    public String getRoomName() { return roomName; }
    public String getContent() { return content; }
}