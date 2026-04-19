module com.example.projekt1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.projekt1 to javafx.fxml;
    //exports com.example.projekt1;
    exports client;
    opens client to javafx.graphics, javafx.fxml;
}