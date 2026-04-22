import model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;



public class Client extends Thread{


	Socket socketClient;

	ObjectOutputStream out;
	ObjectInputStream in;

	private Consumer<Serializable> callback;

	Client(Consumer<Serializable> call){

		callback = call;
	}

	public void run() {

		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
			System.err.println("Client connection error: " + e.getMessage());
			callback.accept("Error: Could not connect to server. Make sure the server is running.");
			return; // Stop the thread if we can't connect
		}

		while(true) {

			try {
				Message message = (Message) in.readObject();
				callback.accept(message);
			}
			catch(Exception e) {
				callback.accept("Lost connection to server");
				break;
			}
		}

	}

	public void send(Message message) {
		if (out == null) {
			System.err.println("Cannot send message: Not connected to server.");
			callback.accept("Error: Not connected to server.");
			return;
		}

		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException e) {
			System.err.println("Failed to send message: " + e.getMessage());
			callback.accept("Lost connection to server");
			try {
				socketClient.close();
			} catch (Exception ex) {}
		}
	}


}
