package model;

import lombok.Getter;

import java.io.IOException;
import java.util.List;

@Getter
public class FileUsers implements CloudMessage{

    private final List<String> users;

    public FileUsers(List<String> users) throws IOException {
        this.users = users;

    }

    @Override
    public MessageType getType() {
        return MessageType.FILE_USERS;
    }


}
