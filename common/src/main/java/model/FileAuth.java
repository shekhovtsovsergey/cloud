package model;

import lombok.Getter;

@Getter
public class FileAuth implements CloudMessage {

    private final String login;
    private final String pass;

    public FileAuth(String login,String pass) {
        this.login = login;
        this.pass = pass;
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE_AUTH;
    }


}
