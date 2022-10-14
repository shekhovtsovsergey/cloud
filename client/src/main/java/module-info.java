module client {


    requires javafx.controls;
    requires javafx.fxml;
    requires common;
    requires io.netty.codec;

    opens client to javafx.fxml;
    exports client;

}