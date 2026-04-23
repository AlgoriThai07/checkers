import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.io.PrintStream;

public class GuiServer extends Application {

    ListView<String> listItems;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        listItems = new ListView<>();

        // Print system out logs to the server UI console
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    final String line = buffer.toString();
                    buffer = new StringBuilder();
                    Platform.runLater(() -> {
                        listItems.getItems().add(line);
                        // Auto scroll to bottom
                        listItems.scrollTo(listItems.getItems().size() - 1);
                    });
                    originalOut.println(line);
                } else if (b != '\r') {
                    buffer.append((char) b);
                }
            }
        }, true));

        // Start the server in a background thread
        Thread serverThread = new Thread(() -> {
            Server server = new Server(5555);
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        primaryStage.setScene(createServerGui());
        primaryStage.setTitle("Checkers Server");
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    private Scene createServerGui() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: #2b2b2b;");

        listItems.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13; " +
                           "-fx-control-inner-background: #1e1e1e; -fx-text-fill: #33f3ff;");
        pane.setCenter(listItems);

        return new Scene(pane, 600, 450);
    }
}
