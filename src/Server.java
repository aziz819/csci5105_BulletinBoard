import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * 
 * 
 * @author Fan Zhang, Zhiqi Chen
 */
public class Server extends Thread {
	// server's ip and port used for communication
	public String ip;
	public int port;
	// rank used for election the coordinator
	public int rank;
	public boolean isCoordinator;
	// List of clients who joined this server
	public ArrayList<Client> clientList = new ArrayList<Client>();
	// List of articles
	public ArrayList<Article> articleList = new ArrayList<Article>();
	// List of servers
	public ArrayList<Server> serverList = new ArrayList<Server>();
	// incoming message from client
	public String inMsg;
	public Client client;

	public void setCoordinator(boolean isCoordinator) {
		this.isCoordinator = isCoordinator;
		Config.server = new Server(this.port);
	}

	public Server() {
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.port = Config.port++;
		rank = Config.rank++;
		// set the default value as false
		isCoordinator = false;
		saveConfig();
		System.out.println("Server with Port " + port
				+ " waiting for clients request ");
		start();
	}

	// This constructor is mainly for client to save servers' information
	public Server(int port) {
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.port = port;
	}

	public Server(int port, boolean isCoordinator) {
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.port = port;
	}

	// save config data to file using JSON
	@SuppressWarnings("unchecked")
	public void saveConfig() {
		JSONObject obj = new JSONObject();
		obj.put("port", Integer.toString(Config.port));
		obj.put("rank", Integer.toString(Config.rank));
		try {
			FileWriter file = new FileWriter("config.json");
			file.write(obj.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.print(obj);
	}

	public boolean election() {
		return false;
	}

	/**
	 * Three functions included
	 * 
	 * 1. Listen for the client's request 2. check the incoming message 3. Ping
	 * coordinator periodically to make sure coordinator is running If the
	 * coordinator is down, call election() to elect new coordinator
	 */
	public void run() {
		listenClient();
	}

	// Listen incoming client's request
	public void listenClient() {

		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		byte buffer[];
		DatagramPacket packet;

		while (true) {
			try {
				buffer = new byte[Config.BUFFER_SIZE];
				packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				System.out.println("Server receive client request");
				inMsg = new String(packet.getData()).trim();
				
				checkMsg();
				
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				client = new Client(address.getHostAddress(), port);
				buffer = null;
				String ack = "CONFIRM";
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, address,
						port);
				socket.send(packet);
			} catch (Exception e) {
				System.out.println("Socket communication failed");
				e.printStackTrace();
			}		
		}
	}

	public void checkMsg() {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(inMsg);
			JSONObject jsonObject = (JSONObject) obj;

			if (((String) jsonObject.get("type")).equals(Config.JOIN)) {
				clientList.add(client);
				System.out.println("client with PORT "+client.getPortNumber()+" JOINED server");
			} else if (((String) jsonObject.get("type")).equals(Config.LEAVE)) {
				clientList.remove(client);
				System.out.println("client with PORT "+client.getPortNumber()+" LEAVED server");
			} else if (((String) jsonObject.get("type")).equals(Config.POST)) {
				String article = (String) jsonObject.get("article");
				System.out.println(article);
			}
		} catch (ParseException e) {
			System.out.println("position: " + e.getPosition());
			e.printStackTrace();
		}
	}

	// Get the server info from Config class
	public void setServerList() {
		int numberOfServer = Config.rank;
		int serverPort = Config.port - 1;
		// System.out.println(numberOfServer);
		// System.out.println(serverPort);
		while (numberOfServer > 0) {
			Server s = new Server(serverPort--);
			numberOfServer--;
			serverList.add(s);
		}
	}

	public String toString() {
		return this.ip + ": " + this.port;
	}

	public static void main(String[] args) {
		// Test a single server
		Server s1 = new Server();
		Server s2 = new Server();
		Server s3 = new Server();
		s3.setCoordinator(true);
		s1.setServerList();
		s2.setServerList();
		s3.setServerList();
	}

}
