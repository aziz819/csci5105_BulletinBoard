import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

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
		Config.server = this;
//		Config.server = new Server(this.port);
	}
	
	public boolean checkCoordinator(){
		return isCoordinator;
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
		setServerList();
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

//		System.out.print(obj);
	}

	public boolean election() {
		return false;
	}

	public void run() {
		listenClient();
	}

	/**
	 *  Listen incoming client's request
	 *  ONLY Receive the incoming message, the reply message will be send out by checkMsg()
	 */
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
				
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				client = new Client(address.getHostAddress(), port);		
				
			} catch (Exception e) {
				System.out.println("Socket communication failed");
				e.printStackTrace();
			}	
			checkMsg();
		}
	}

	/**
	 * check the incoming message type
	 * Process the requests and send out the confirmation message
	 * 
	 * POST request offers Sequential Consistency 
	 */
	public void checkMsg() {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(inMsg);
			JSONObject jsonObject = (JSONObject) obj;
			
			String ack = "";
			byte buffer[];
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet;
			
			if (((String) jsonObject.get("type")).equals(Config.JOIN)) {
				clientList.add(client);
				System.out.println("client with PORT "+client.getPortNumber()+" JOINED server");
				ack = "Join Server Success";
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			} else if (((String) jsonObject.get("type")).equals(Config.LEAVE)) {
				clientList.remove(client);
				System.out.println("client with PORT "+client.getPortNumber()+" LEAVED server");
				ack = "Leave Server Success";
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			} else if (((String) jsonObject.get("type")).equals(Config.POST)) {
				String article = (String) jsonObject.get("article");
				System.out.println(article);
				if(article.contains("-1")){
					if(this.checkCoordinator()){
						String newArticle = setId(article).toJSONString();
						insertArticle(articleFactory(newArticle));
						// If current server is coordinator, set id for the article, and send to all servers except coordinator itself
						sendAllServer(addType(newArticle, Config.POST).toString());
						
					}else{
						// If current server is not coordinator, send article to coordinator
						request(addType(article, Config.POST).toString(), Config.server);
					}
				}else{
					insertArticle(articleFactory(article));
				}
				ack = "Post new article to Server Success";
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			} else if(((String) jsonObject.get("type")).equals(Config.LIST)){
				ack = printArticleList();
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			} else if(((String) jsonObject.get("type")).equals(Config.READ)){
				int index = Integer.parseInt((String.valueOf(jsonObject.get("index"))));
				for(Article article : articleList){
					if(article.id == index){
						ack = article.toString();
						buffer = ack.getBytes();
						packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
						socket.send(packet);
					}
				}
			} else if(((String) jsonObject.get("type")).equals(Config.PRIMARY)){
				setCoordinator(true);
				ack = this.toString();
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			}
		} catch (ParseException e) {
			System.out.println("position: " + e.getPosition());
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Format insert article to articleList
	public void insertArticle(Article article){
		if(articleList.size()<2||article.replyId==0)
			articleList.add(article);
		else{
			for(int i=0;i<articleList.size();i++){
				Article a = articleList.get(i);
				if(article.replyId==a.id){
					if(i==articleList.size()-1){
						articleList.add(article);
						return;
					}
					for(int j=i+1;j<articleList.size();j++){
						Article b = articleList.get(j);
						if(article.replyId!=b.replyId){
							articleList.add(j,article);
							return;
						}
					}
				}
			}
		}
	}
	
	// return all the articles
	public String printArticleList(){
		StringBuffer out = new StringBuffer("\n");
		if(articleList.size()==0)
			return "";
		else{
			// Construct indents
			HashMap<Integer, String> map = new HashMap<Integer, String>();
			for(Article a : articleList){
				String indent = "";
				if(a.replyId==0)
					map.put(a.id, indent);
				else{
					if(map.containsKey(a.replyId)){
						indent = "\t"+map.get(a.replyId);
						map.put(a.id, indent);
					}	
				}
			}
			// Format output
			for(Article a : articleList){
				out.append(map.get(a.id));
				out.append(a.id);
				out.append(" ");
				out.append(a.title);
				out.append("\n");
			}
		}
		return out.toString();
	}

	// Add a message type
	@SuppressWarnings("unchecked")
	public JSONObject addType(String msg, String type){
		JSONObject obj = new JSONObject();
		obj.put("type",type);
		obj.put("article", msg);
		return obj;
	}
	// Set the id for article
	public Article setId(String article){
		JSONObject obj = new JSONObject();
		JSONParser parser = new JSONParser();
		Article newArticle = new Article();
		try {
			Object obje = parser.parse(article);
			obj = (JSONObject) obje;
			newArticle.id = Config.id++;
			newArticle.title = (String) obj.get("title");
			newArticle.content = (String) obj.get("content");
			newArticle.replyId = Integer.parseInt(String.valueOf(obj.get("replyId")));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newArticle;
	}
	
	public Article articleFactory(String article){
		JSONObject obj = new JSONObject();
		JSONParser parser = new JSONParser();
		Article newArticle = new Article();
		try {
			Object obje = parser.parse(article);
			obj = (JSONObject) obje;
			newArticle.id = Integer.parseInt(String.valueOf(obj.get("id")));
			newArticle.title = (String) obj.get("title");
			newArticle.content = (String) obj.get("content");
			newArticle.replyId = Integer.parseInt(String.valueOf(obj.get("replyId")));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newArticle;
	}
	// Get the server info from Config class
	public void setServerList() {
		int numberOfServer = Config.total;
		int serverPort = Config.FIRST_PORT+numberOfServer-1;
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
	
	// send message to all servers except itself. This method is only be called by coordinator
	public void sendAllServer(String msg){
		for(Server server: serverList){
			if(this.port != server.port)
				request(msg, server);
		}
	}
	// Send message to server.
	public void request(String msg, Server server){
		try {
			DatagramSocket socket = new DatagramSocket();
			byte buffer[] = new byte[Config.BUFFER_SIZE];
			buffer = msg.getBytes();
			DatagramPacket packet;
		
			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(server.ip),
					server.port);
			socket.send(packet);
			System.out.println("Request send to server");
			
			// simulate the propagation delay : 25ms to 100ms
			Thread.sleep((long) (25+Math.random()*100));
			
			// Don't need the ack from coordinator, since the coordinator will send the updated article back
			if(server.port != Config.server.port){
				buffer = new byte[Config.BUFFER_SIZE];
		        packet = new DatagramPacket(buffer, buffer.length);
		        socket.receive(packet); // wait for response
		        String ack = new String(packet.getData());
		        System.out.println("Receive ack from server " + ack);
			}
			
	        socket.close();
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// Create server
		Server s1 = new Server();
		Server s2 = new Server();
		Server s3 = new Server();

		new CoordinatorHelper();
	}

}
