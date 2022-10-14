package model;

import lombok.Getter;

@Getter
public class FileRequest implements CloudMessage {

    private final String fileName;
    private final String login;
    private final String pass;


    public FileRequest(String fileName,String login,String pass) {
        this.fileName = fileName;
        this.login = login;
        this.pass = pass;
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE_REQUEST;
    }
}
