package model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@NoArgsConstructor
@Getter
public class FileLarge implements CloudMessage{

    private String fileName;
    private long size;
    private byte[] bytes;

    public FileLarge(Path file) throws IOException {
        fileName = file.getFileName().toString();
        bytes = Files.readAllBytes(file);
        size = bytes.length;
    }



    @Override
    public MessageType getType() {
        return MessageType.FILE_LARGE;
    }
}
