package model;

import lombok.Getter;

@Getter
public class FileLastChank implements CloudMessage{

    private final String fileName;


    public FileLastChank(String fileName) {
        this.fileName = fileName;
    }



    @Override
    public MessageType getType() {
        return MessageType.FILE_LAST_CHANK;
    }

}
