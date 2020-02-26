package core;

/**
 * 消息包定义
 * @author gaopan
 */
public class MessagePack {
    /** Ping */
    public static final int TYPE_PING = 0;
    /** 请求消息 */
    public static final int TYPE_MESSAGE = 1;

    /** 消息类型 */
    private int type;
    /** 发送者节点id */
    private String sender;
    /** 消息内容 */
    private String context;

    private MessagePack(int type, String sender, String context) {
        this.type = type;
        this.sender = sender;
        this.context = context;
    }

    public static MessagePack buildPing(String sender, int port) {
        return new MessagePack(TYPE_PING, sender, String.valueOf(port));
    }

    public static MessagePack buildMessage(String sender, String message) {
        return new MessagePack(TYPE_MESSAGE, sender, message);
    }

    public String encode() {
        return String.format("%d:%s:%s", type, sender, context);
    }

    public static MessagePack decode(String data) {
        String[] ss = data.split(":");
        return new MessagePack(Integer.parseInt(ss[0]), ss[1], ss[2]);
    }

    public int getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContext() {
        return context;
    }
}
