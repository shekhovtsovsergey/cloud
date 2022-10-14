package model;

import lombok.Getter;


@Getter
public class EnterDir implements CloudMessage {

    private final String fileName;
    private final String login;
    private final String pass;

    public EnterDir(String fileName,String login,String pass) {
        this.fileName = fileName;
        this.login = login;
        this.pass = pass;
    }

    @Override
    public MessageType getType() {
        return MessageType.ENTER_DIR;
    }

}


