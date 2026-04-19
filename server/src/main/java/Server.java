import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import Model.Message;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	HashMap<String, ArrayList<String>> groups = new HashMap<>();
	TheServer server;
	private Consumer<Serializable> callback;
	
	
	Server(Consumer<Serializable> call){
	
		callback = call;
		server = new TheServer();
		server.start();
	}

	public boolean usernameTaken(String username) {
		for (ClientThread c : clients) {
			if (c.username != null && c.username.equals(username)) {
				return true;
			}
		}
		return false;
	}

	public void sendToClient(ClientThread client, Message message) {
		try {
			client.out.writeObject(message);
			client.out.flush();
		} catch (Exception e) {
			callback.accept("OOOOPPs...Something wrong with the socket from client: " + client.count + "....closing down!");
		}
	}

	public ArrayList<String> getSignedInUsers() {
		ArrayList<String> users = new ArrayList<>();
		for (ClientThread c : clients) {
			if (c.signedIn && c.username != null) {
				users.add(c.username);
			}
		}
		return users;
	}

	public ClientThread getClientByUsername(String username) {
		for (ClientThread c : clients) {
			if (c.username != null && c.username.equals(username)) {
				return c;
			}
		}
		return null;
	}

	public void sendUserListToAll() {
		Message userListMsg = new Message("USER_LIST", "SERVER", null, null, null, getSignedInUsers(), true);
		for (ClientThread c : clients) {
			if (c.signedIn) {
				sendToClient(c, userListMsg);
			}
		}
	}

	public boolean groupExists(String groupName) {
		return groups.containsKey(groupName);
	}

	public void createGroup(String groupName, ArrayList<String> members) {
		groups.put(groupName, members);
	}

	public ArrayList<String> getGroupMembers(String groupName) {
		return groups.get(groupName);
	}

	public boolean allUsersExist(ArrayList<String> members) {
		for (String member : members) {
			if (getClientByUsername(member) == null) {
				return false;
			}
		}
		return true;
	}
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				callback.accept("client has connected to server: " + "client #" + count);
				clients.add(c);
				c.start();
				
				count++;
				
			    }
			}//end of try
				catch(Exception e) {
					callback.accept("Server socket did not launch");
				}
			}//end of while
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			String username;
			boolean signedIn = false;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
			}
			
			public void updateClients(Message message) {
				for(int i = 0; i < clients.size(); i++) {
					ClientThread t = clients.get(i);
					try {
					 t.out.writeObject(message);
					 t.out.flush();
					}
					catch(Exception e) {}
				}
			}
			
			public void run(){
					
				try {
					out = new ObjectOutputStream(connection.getOutputStream());
					in = new ObjectInputStream(connection.getInputStream());
					connection.setTcpNoDelay(true);
				}
				catch(Exception e) {
					System.out.println("Streams not open");
					return;
				}

				Message m = new Message("PUBLIC", "SERVER", null, null, "new client on server: client #"+count,  null, true);
				updateClients(m);
					
				 while(true) {
					    try {
					    	Message message = (Message) in.readObject();
							callback.accept("Received " + message.getType() + " from client #" + count);
					    	if (message.getType().equals("SIGNIN")) {
								String requestedUsername = message.getSender();
								if (signedIn) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "You are already signed in", null, false);
									sendToClient(this, errorMsg);
                                }
								else if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Username cannot be empty", null, false);
									sendToClient(this, errorMsg);
								} else if (usernameTaken(requestedUsername)) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Username already taken", null, false);
									sendToClient(this, errorMsg);
								} else {
									this.username = requestedUsername;
									this.signedIn = true;

									Message successMsg = new Message("SIGNIN", "SERVER", null, null, "Signed in as: " + requestedUsername, null, true);
									sendToClient(this, successMsg);

									Message joinedMsg = new Message("PUBLIC", "SERVER", null, null, this.username + " has joined the server", null, true);
									updateClients(joinedMsg);
									sendUserListToAll();
								}
							} else if (message.getType().equals("PUBLIC")) {
								if (!signedIn) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "You must sign in first", null, false);
									sendToClient(this, errorMsg);
								} else {
									updateClients(message);
								}
							} else if (message.getType().equals("PRIVATE")) {
								if (!signedIn) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "You must sign in first", null, false);
									sendToClient(this, errorMsg);
								} else {
									String receiver = message.getReceiver();
									ClientThread target = getClientByUsername(receiver);
									if (receiver == null || receiver.trim().isEmpty()) {
										Message errorMsg = new Message("ERROR", "SERVER", null, null, "Private message requires a recipient", null, false);
										sendToClient(this, errorMsg);
									} else if (target == null) {
										Message errorMsg = new Message("ERROR", "SERVER", null, null, "User not found", null, false);
										sendToClient(this, errorMsg);
									} else {
										sendToClient(target, message);
										sendToClient(this, message);
									}
								}
							} else if (message.getType().equals("CREATE_GROUP")) {
								if (!signedIn) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "You must sign in first", null, false);
									sendToClient(this, errorMsg);
									continue;
								}
								String groupName = message.getGroupName();
								ArrayList<String> members = message.getMembers();
								if (groupName == null || groupName.trim().isEmpty()) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Group name cannot be empty", null, false);
									sendToClient(this, errorMsg);
								} else if (groupExists(groupName)) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Group name already taken", null, false);
									sendToClient(this, errorMsg);
								} else if (members == null || members.isEmpty()) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Group must have at least one member", null, false);
									sendToClient(this, errorMsg);
								} else {
									if (!members.contains(this.username)) {
										members.add(this.username);
									}
									if (!allUsersExist(members)) {
										Message errorMsg = new Message("ERROR", "SERVER", null, null, "One or more members not found", null, false);
										sendToClient(this, errorMsg);
									} else {
										createGroup(groupName, members);
										Message successMsg = new Message("CREATE_GROUP", "SERVER", null, groupName, "Group created: " + groupName, members, true);
										sendToClient(this, successMsg);
									}
								}
							} else if (message.getType().equals("GROUP_MESSAGE")) {
								if (!signedIn) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "You must sign in first", null, false);
										sendToClient(this, errorMsg);
										continue;
								}
								String groupName = message.getGroupName();
								if (groupName == null || groupName.trim().isEmpty()) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Group name cannot be empty", null, false);
									sendToClient(this, errorMsg);
								} else if (!groupExists(groupName)) {
									Message errorMsg = new Message("ERROR", "SERVER", null, null, "Group not found", null, false);
									sendToClient(this, errorMsg);
								} else {
									ArrayList<String> members = getGroupMembers(groupName);
									if (!members.contains(this.username)) {
										Message errorMsg = new Message("ERROR", "SERVER", null, null, "You are not a member of this group", null, false);
										sendToClient(this, errorMsg);
									} else {
										for (String member : members) {
											ClientThread target = getClientByUsername(member);
											if (target != null) {
												sendToClient(target, message);
											}
										}
									}
								}
							}
						}
					    catch(Exception e) {
					    	callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					    	String name = (username != null) ? username : "client #"+count;
							Message errored = new Message("PUBLIC", "SERVER", null, null, name +    " has disconnected", null, true);
					    	updateClients(errored);
							clients.remove(this);
							sendUserListToAll();
					    	break;
					    }
					}
				}//end of run
		}//end of client thread
}


	
	

	
