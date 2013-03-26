import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * CoordinatorHelper will make a election for coordinator based on the highest rank
 * 
 * @author Fan Zhang, Zhiqi Chen
 *
 */
public class CoordinatorHelper extends Thread {

	ArrayList<Server> serverList = new ArrayList<Server>();
	Server coordinator;
	
	public CoordinatorHelper(){
		setServerList();
		coordinator = getHighestRank(serverList);
		start();
	}
	
	// Get the highest rank of server
	public Server getHighestRank(ArrayList<Server> serverList){
		int highRank = 0;
		Server primary = null;
		for(Server s : serverList){
			if(s.port>highRank){
				highRank = s.port;
				primary = s;
			}
		}
		return primary;
		
	}
	// Get the server info from config.json
	public JSONObject readfile() {
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
	
	// Get the server info from Config file
	public void setServerList() {
		JSONObject config = readfile();
		int numberOfServer = Integer.parseInt((String) config.get("rank"));
		int serverPort = Integer.parseInt((String) config.get("port")) - 1;
		while (numberOfServer > 0) {
			Server s = new Server(serverPort--);
			numberOfServer--;
			serverList.add(s);
		}
	}
	
	public void run() {
		while(true){
			ExecutorService service = Executors.newSingleThreadExecutor();
			try {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						setPrimaryServer();
					}
				};
				Future<?> f = service.submit(r);
				f.get(2, TimeUnit.SECONDS); // attempt the task for two seconds
			} catch (final InterruptedException e) {
				e.printStackTrace();
			} catch (final TimeoutException e) {
				// Once timeout, remove the coordinator from server list
				serverList.remove(coordinator);
				// election for the new coordinator
				coordinator = getHighestRank(serverList);
				System.out.println("New coordinator: "+ coordinator.toString());
				e.printStackTrace();
			} catch (final ExecutionException e) {
				e.printStackTrace();
			} finally {
				service.shutdown();
			}
			try {
				// set a delay 
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setPrimaryServer(){
		JSONObject obj = new JSONObject();
		obj.put("type", Config.PRIMARY);
		request(obj.toString());
	}
	
	public void request(String msg){
		try {
			DatagramSocket socket = new DatagramSocket();
			byte buffer[] = new byte[Config.BUFFER_SIZE];
			buffer = msg.getBytes();
			DatagramPacket packet;
			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(coordinator.ip),
					coordinator.port);
			socket.send(packet);
			System.out.println("Request send to server");
			// simulate the propagation delay : 25ms to 100ms
			Thread.sleep((long) (25+Math.random()*100));
			buffer = new byte[Config.BUFFER_SIZE];
	        packet = new DatagramPacket(buffer, buffer.length);
	        socket.receive(packet); // wait for response
	        
	        
	        String ack = new String(packet.getData());
	        System.out.println("Coordinator: "+ ack);
	        socket.close();
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		new CoordinatorHelper();
//	}

}
