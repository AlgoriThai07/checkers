
import java.util.ArrayList;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.Message;

public class GuiClient extends Application{

	
	TextField c1;
	Button b1;
	HashMap<String, Scene> sceneMap;
	Client clientConnection;

	TextField usernameField;
	Button signInButton;
	boolean signedIn = false;

	TextField receiverField;

	TextField groupNameField;
	TextField groupMembersField;
	Button createGroupButton;

	ListView<String> listItems2;
	ListView<String> usersList;

	String username = "";
	
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems2 = new ListView<String>();
		usersList = new ListView<String>();

		usernameField = new TextField();
		receiverField = new TextField();
		signInButton = new Button("Sign In");
		groupNameField = new TextField();
		groupMembersField = new TextField();
		createGroupButton = new Button("Create Group");

		clientConnection = new Client(data->{
				Platform.runLater(()->{
					if (data instanceof Message) {
						Message message = (Message) data;

						if (message.getType().equals("SIGNIN")) {
							if (message.isSuccess()) {
								signedIn = true;
								listItems2.getItems().add("Signed in as " + username);
								signInButton.setDisable(true);
								usernameField.setDisable(true);
							} else {
								listItems2.getItems().add("Sign in failed: " + message.getContent() );
							}
						} else if (message.getType().equals("USER_LIST")) {
							usersList.getItems().clear();
							if (message.getMembers() != null) {
								usersList.getItems().addAll(message.getMembers());
							}
						} else if (message.getType().equals("ERROR")) {
							listItems2.getItems().add("Error: " + message.getContent());
						} else if (message.getType().equals("PRIVATE")) {
							listItems2.getItems().add("[PRIVATE] " + message.getSender() + " -> " + message.getReceiver() + ": " + message.getContent());
						} else if (message.getType().equals("GROUP_MESSAGE")){
							listItems2.getItems().add("[GROUP] " + message.getSender() + " -> " + message.getGroupName() + ": " + message.getContent());
						} else if (message.getType().equals("CREATE_GROUP")) {
							listItems2.getItems().add(message.getContent());
						} else {
							listItems2.getItems().add(message.getSender() + ": " + message.getContent());
						}
					} else {
						listItems2.getItems().add(data.toString());
					}
			});
		});
							
		clientConnection.start();

		signInButton.setOnAction(e-> {
			String requestedUsername = usernameField.getText().trim();
			if (!requestedUsername.isEmpty()) {
				username = requestedUsername;
				Message message = new Message("SIGNIN", username, null, null, null, null, false);
				clientConnection.send(message);
			}
		});

		createGroupButton.setOnAction(e-> {
			if (!signedIn) {
				listItems2.getItems().add("Please sign in first");
				return;
			}
			String groupName = groupNameField.getText().trim();
			String rawMembers = groupMembersField.getText().trim();

			if (groupName.isEmpty()) {
				listItems2.getItems().add("Group name cannot be empty");
				return;
			}

			ArrayList<String> members = new ArrayList<String>();
			if (!rawMembers.isEmpty()) {
				String[] words = rawMembers.split(",");
				for (String word : words) {
					String member = word.trim();
					if (!member.isEmpty()) {
						members.add(member);
					}
				}
			}

			Message message = new Message("CREATE_GROUP", username, null, groupName, null, members, false);
			clientConnection.send(message);
		});
		
		c1 = new TextField();
		b1 = new Button("Send");
		b1.setOnAction(e-> {
			if (!signedIn) {
				listItems2.getItems().add("Please sign in first");
				return;
			}
			String content = c1.getText().trim();
			String receiver = receiverField.getText().trim();
			String groupName = groupNameField.getText().trim();

			if (!content.isEmpty()) {
				Message message;
				if (!receiver.isEmpty()) {
					message = new Message("PRIVATE", username, receiver, null, content, null, true);
				} else if (!groupName.isEmpty()) {
					message = new Message("GROUP_MESSAGE", username, null, groupName, content, null, true);
				} else {
					message = new Message("PUBLIC", username, null, null, content, null, true);
				}
				clientConnection.send(message);
				c1.clear();
			}
		});
		sceneMap = new HashMap<String, Scene>();

		sceneMap.put("client",  createClientGui());
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });


		primaryStage.setScene(sceneMap.get("client"));
		primaryStage.setTitle("Client");
		primaryStage.show();
		
	}
	

	
	public Scene createClientGui() {

		Label signInLabel = new Label("Sign In");
		Label usersLabel = new Label("Connected Users");
		Label privateLabel = new Label("Private model.Message");
		Label groupLabel = new Label("Group");
		Label messageLabel = new Label("model.Message");

		HBox signInRow = new HBox(10, usernameField, signInButton);
		HBox privateRow = new HBox(10, receiverField);
		HBox groupRow1 = new HBox(10, groupNameField);
		HBox groupRow2 = new HBox(10, groupMembersField, createGroupButton);
		HBox messageRow = new HBox(10, c1, b1);

		VBox root = new VBox(12, signInLabel, signInRow, usersLabel, usersList, privateLabel, privateRow, groupLabel, groupRow1, groupRow2, messageLabel, messageRow, listItems2);

		root.setStyle("-fx-background-color: blue;" + "-fx-padding: 15;" + "-fx-font-family: 'Arial';");
		signInLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
		usersLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
		privateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
		groupLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
		messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

		usernameField.setPrefWidth(280);
		receiverField.setPrefWidth(280);
		groupNameField.setPrefWidth(280);
		groupMembersField.setPrefWidth(280);
		c1.setPrefWidth(280);

		signInButton.setPrefWidth(100);
		createGroupButton.setPrefWidth(120);
		b1.setPrefWidth(80);

		usersList.setPrefHeight(100);
		listItems2.setPrefHeight(180);

		usernameField.setPromptText("Enter username");
		receiverField.setPromptText("Receiver username (leave blank for public)");
		groupNameField.setPromptText("Group name");
		groupMembersField.setPromptText("Members separated by commas");
		c1.setPromptText("Type your message here");

		return new Scene(root, 520, 650);
	}

}
