package shared;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Type {REQUEST_NAME, NAME_RESPONSE, INFO_ASSIGN, MOVE, BOARD_UPDATE, STATUS}
    public Type type;
    public Object payload;

    public Message(Type type, Object payload){
        this.type = type;
        this.payload = payload;
    }
}
