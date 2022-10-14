package client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;


public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;
    public ListView<String> usersView;
    private String currentDirectory;
    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;
    private Socket socket;
    private boolean needReadMessages = true;

    private DaemonThreadFactory factory;
    FileSplit fileSplit = new FileSplit();
    @FXML
    TextField LoginField = new TextField("sergey");
    @FXML
    TextField PasswordField = new TextField("123");
    @FXML
    TextField IPField = new TextField("127.0.0.1");
    @FXML
    TextField PortField = new TextField("8189");
    @FXML
     Button mail = new Button("Mail");


    //Инициализация
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        needReadMessages = true;
        factory = new DaemonThreadFactory();
        setCurrentDirectory(System.getProperty("user.home"));
        Path cd = Path.of(currentDirectory);

        //Создаем серверную папку при старте
        File cdf = new File(cd.toFile() + String.valueOf("/server_files"));
        if(!cdf.exists()){
            try {
                Files.createDirectory(Path.of(currentDirectory + "/server_files"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //Обновляем список на клиенте
        fillView(clientView, getFiles(currentDirectory));

        //Событие на мышку
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(currentDirectory + "/" + selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(currentDirectory + "/" + selected);
                }
            }
        });
        //------------------------------------

        //Событие на мышку
        serverView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                doubleClickOnServerView();
            }
        });
        //------------------------------------
    }




    private void initNetwork() {
        try {
            socket = new Socket(IPField.getText(), Integer.parseInt(PortField.getText()));
            network = new Network<>(
                    new ObjectDecoderInputStream(socket.getInputStream()),
                    new ObjectEncoderOutputStream(socket.getOutputStream())
            );
            factory.getThread(this::readMessages, "cloud-client-read-thread")
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mail.setVisible(true);
    }



    //Авторизация
    private void auth() throws IOException {
        String login = LoginField.getText();
        String pass = PasswordField.getText();
        network.getOutputStream().writeObject(new FileAuth(login,pass));
    }



    private void readMessages() {
        try {
            while (needReadMessages) {
                CloudMessage message = (CloudMessage) network.getInputStream().readObject();

                if (message instanceof FileMessage fileMessage) {
                    Files.write(Path.of(currentDirectory+"/server_files").resolve(fileMessage.getFileName()), fileMessage.getBytes());
                    Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));

                } else if (message instanceof FileLastChank fileLastChank) {
                    String path = String.valueOf(Path.of(currentDirectory).resolve(fileLastChank.getFileName()));
                    List<File> listOfFilesToMerge = fileSplit.listOfFilesToMerge(new File(path+".001"));
                    fileSplit.mergeFiles(listOfFilesToMerge, new File(path));
                    Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));

                } else if (message instanceof ListMessage listMessage) {
                    Platform.runLater(() -> fillView(serverView, listMessage.getFiles()));

                } else if (message instanceof FileUsers fileUsers) {
                    Platform.runLater(() -> fillView(usersView, fileUsers.getUsers()));
                }
            }
        } catch (Exception e) {
            System.err.println("Server off");
            e.printStackTrace();
        }
    }




    //---------------------------------------------------------------
    //-----------Вспомогательные методы------------------------------
    //---------------------------------------------------------------
    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, getFiles(currentDirectory));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }

    private List<String> getFiles(String directory) {
        // file.txt 125 b
        // dir [DIR]
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>(Arrays.asList(list));
                files.add(0, "..");
                return files;
            }
        }
        return List.of();
    }





    //---------------------------------------------------------------
    //-----------Обработка кодов onAction из файла fxml--------------
    //---------------------------------------------------------------

    public void connect(ActionEvent actionEvent) throws IOException {
        initNetwork();
        auth();
    }

    public void downloadFile(ActionEvent actionEvent) throws IOException {
        String login = LoginField.getText();
        String pass = PasswordField.getText();
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new FileRequest(fileName,login,pass));
    }

    public void sendToServer(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String path = String.valueOf(Path.of(currentDirectory).resolve(fileName));

        fileSplit.splitFile(new File(path));
        List<File> listOfFilesToMerge = fileSplit.listOfFilesToMerge(new File(path+".001"));
        for (File f : listOfFilesToMerge) {
            network.getOutputStream().writeObject(new FileMessage(Path.of(String.valueOf(f))));
        }
        fileSplit.mergeFiles(listOfFilesToMerge, new File(path));
        network.getOutputStream().writeObject(new FileLastChank(fileName));
    }

    //Была кнопка для входа в папку на серваке - передалано на двой клик
    public void enterToDir(ActionEvent actionEvent) throws IOException {
        String login = LoginField.getText();
        String pass = PasswordField.getText();
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new EnterDir(fileName,login,pass));
    }

    //Двойной клик
    private void doubleClickOnServerView() {
        String login = LoginField.getText();
        String pass = PasswordField.getText();
        String fileName = serverView.getSelectionModel().getSelectedItem();
        try {
            network.getOutputStream().writeObject(new EnterDir(fileName,login,pass));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void disconnect(ActionEvent actionEvent) throws IOException {
        //не реализовано
    }

    public void delete(ActionEvent actionEvent) throws IOException {
        //не реализовано
    }

    public void rename(ActionEvent actionEvent) throws IOException {
        //не реализовано
    }

}
