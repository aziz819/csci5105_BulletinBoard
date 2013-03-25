import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/** 
 * Client should first connect to a server, then perform client's four functions
 * 
 * post(): post new article
 * reply(): reply to an existing article (also post a new article)
 * list(): read a list of articles
 * read(): read an article
 * 
 * @author Fan Zhang, Zhiqi Chen
 *
 */

public class Client {
	private String ipAddress;
	private int portNumber;
	
	public ArrayList<Server> serverList;
	// If connectedServer is null, then NO more UDP communication allowed
	public Server connectedServer;

	public Client(){
		try {
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		serverList = new ArrayList<Server>();
		setServerList();
	}
//	// User need to specify a port number used for communication with server
//	public Client(int port){
//		try {
//			ipAddress = InetAddress.getLocalHost().getHostAddress();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
//		portNumber = port;
//		serverList = new ArrayList<Server>();
//		setServerList();
//	}
	// This constructor is mainly for Server to create client object
	public Client(String ip, int port){
		ipAddress = ip;
		portNumber = port;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getPortNumber() {
		return portNumber;
	}
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	
	// Get the server info from Config file
	public void setServerList(){
		JSONObject config = readfile();
		int numberOfServer = Integer.parseInt((String)config.get("rank"));
		int serverPort = Integer.parseInt((String)config.get("port"))-1;
//		System.out.println(numberOfServer);
//		System.out.println(serverPort);
		while(numberOfServer>0){
			Server s = new Server(serverPort--);
			numberOfServer--;
			serverList.add(s);
		}	
	}
	
	// Get the server info from config.json
	public JSONObject readfile(){
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
			Object obj = parser.parse(new FileReader("config.json"));
			jsonObject = (JSONObject) obj;
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return jsonObject;
	}
	
	public void request(String msg){
		try {
			DatagramSocket socket = new DatagramSocket();
			byte buffer[] = new byte[Config.BUFFER_SIZE];
			buffer = msg.getBytes();
			DatagramPacket packet;
			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(connectedServer.ip),
					connectedServer.port);
			long startTime = System.currentTimeMillis();
			socket.send(packet);
			System.out.println("Request send to server");
			// simulate the propagation delay : 25ms to 100ms
			Thread.sleep((long) (25+Math.random()*100));
			buffer = new byte[Config.BUFFER_SIZE];
	        packet = new DatagramPacket(buffer, buffer.length);
	        socket.receive(packet); // wait for response
	        long endTime = System.currentTimeMillis();
	        long cost = endTime-startTime;
	        String ack = new String(packet.getData());
	        System.out.println("Receive ack from server: " + ack);
	        System.out.println("Cost of operation: "+ cost + "ms");
	        socket.close();
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// print a list of servers in client side
	public void printServerList() {
		if(serverList.size()==0){
			System.out.println("No server current available");
			return;
		}
		int index = 1;
		for (Server s : serverList) {
			System.out.println(index++ + ". "+ s.toString());
		}
	}

	@SuppressWarnings("unchecked")
	public void connectServer(int index){
		JSONObject obj1 = new JSONObject();
		obj1.put("type",Config.JOIN);
		// set connectedServer, then perform UDP communication
		Server server = serverList.get(index-1);
		this.connectedServer = server;
		request(obj1.toString());
		System.out.println("Join server with port " + server.port + " success");
	}
	@SuppressWarnings("unchecked")
	public void leaveServer(){
		JSONObject obj2 = new JSONObject();
		obj2.put("type", Config.LEAVE);
		request(obj2.toString());
		System.out.println("Leave server with port "+ connectedServer.port + " success");
		this.connectedServer = null;
	}	
	@SuppressWarnings("unchecked")
	public void post(Article article) {
		if(connectedServer==null)
			System.out.println("You must connect to a server first");
		else{
			JSONObject obj3 = new JSONObject();
			obj3.put("type",Config.POST);
			obj3.put("article", article.toJSONString());
			System.out.println(obj3);
			request(obj3.toString());
		}
			
	}
	@SuppressWarnings("unchecked")
	public void list(){
		if(connectedServer==null)
			System.out.println("You must connect to a server first");
		else{
			JSONObject obj = new JSONObject();
			obj.put("type", Config.LIST);
			request(obj.toString());
		}
	}
	public String toString(){
		return getIpAddress()+";"+Integer.toString(getPortNumber())+";";
	}
	
	
}
