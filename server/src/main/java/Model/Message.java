package Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    private String type;
    private String sender;
    private String receiver;
    private String groupName;
    private String content;
    private ArrayList<String> members;
    private boolean success;

    public Message(String type, String sender, String receiver, String groupName, String content, ArrayList<String> members, boolean success) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.groupName = groupName;
        this.content = content;
        this.members = members;
        this.success = success;
    }

    public String getType() {
        return type;
    }
    public String getSender() {
        return sender;
    }
    public String getReceiver() {
        return receiver;
    }
    public String getGroupName() {
        return groupName;
    }
    public String getContent() {
        return content;
    }
    public ArrayList<String> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "Model.Message [type=" + type + ", sender=" + sender + ", receiver=" + receiver + ", groupName=" + groupName + ", content=" + content + ", members=" + members + ", success=" + success + "]";
    }
}

