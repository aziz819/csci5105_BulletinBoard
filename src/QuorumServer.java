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
 * QuorumServer is a subclass of Server. Some methods are overrides in order to 
 * implement quorum consistency.
 * 
 * @author Fan Zhang, Zhiqi Chen
 *
 */
public class QuorumServer extends Server{
	
	public ArrayList<Server> Nr;
	public ArrayList<Server> Nw;
	public Server NrNw; // which is one server within the intersection of the Nr and Nw.
	public int counter;
	public int NrSize, NwSize;
	
	public QuorumServer(String NrSize, String NwSize){
		
		this.NrSize = Integer.parseInt(NrSize);
		this.NwSize = Integer.parseInt(NwSize);
		
		if(!checkConstrain(this.NrSize, this.NwSize)){
			System.out.println("Nr and Nw violate the constraints");
			System.exit(0);
		}
		counter = 0;
	}
	public QuorumServer(int NrSize, int NwSize){
		this.NrSize = NrSize;
		this.NwSize = NwSize;
		if(!checkConstrain(this.NrSize, this.NwSize)){
			System.out.println("Nr and Nw violate the constraints!");
			System.exit(0);
		}
		counter = 0;
	}
	// Check whether Nr and Nw satisfy the constraints
	public boolean checkConstrain(int rsize, int wsize){
		if(rsize + wsize>this.serverList.size() && wsize>this.serverList.size()/2)
			return true;
		else
			return false;
	}
	
	// ONLY COORDINATOR can create Read Quorum and Write Quorum
	public void createQuorum(){
		ArrayList<Server> tempList = new ArrayList<Server>(this.serverList);
		setNrNw(tempList);
	}
	
	// Randomly choose Nr number of server from server list, then set the Write Quorum
	public void setNrNw(ArrayList<Server> slist){
		Nr = new ArrayList<Server>();
		Nw = new ArrayList<Server>();
		int count = NrSize;
		while(count>0){
			int length = slist.size();
			int index = (int) (Math.random()*length);
			Server s = slist.get(index);
			Nr.add(s);
			slist.remove(s);
			count--;
		}
		// Condition Nr + Nw > N holds the rest elements in slist must be the elements in Nw. 
		// Also, there are some elements in Nr also belong to Nw. 
		for(Server s : slist){
			Nw.add(s);
		}
		count = NwSize - Nw.size();
		ArrayList<Server> tempList = new ArrayList<Server>(Nr);
		while(count>0){
			int length = tempList.size();
			int index = (int) (Math.random()*length);
			Server s = tempList.get(index);
			Nw.add(s);
			// Random choose the server within the intersection of Nr and Nw
			Config.NrNw = s;
			tempList.remove(s);
			count--;
		}
	}
	
	/**
	 * check the incoming message type
	 * Process the requests and send out the confirmation message
	 * 
	 * This method offers Quorum Consistency 
	 */
	@SuppressWarnings("unchecked")
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
						// Quorum can only be created by coordinator
						createQuorum();
						System.out.println("Nr = "+Nr.size()+"\nNw = "+Nw.size());
						String newArticle = setId(article).toJSONString();
						insertArticle(articleFactory(newArticle));
						// If current server is coordinator, set id for the article, and send message to Write Quorum servers
						sendAllServer(addType(newArticle, Config.POST).toString());
						if(++counter>=Config.LIMIT){
							counter = 0;
							Config.latestArticles = new ArrayList<Article>(articleList);
							JSONObject sync = new JSONObject();
							// Send a synchronize message
							sync.put("type",Config.SYNC);
							sendServers(sync.toString());
						}
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
				NrNw = Config.NrNw;
				// Return the article which has the highest version number from the server NrNw in READ Quorum
				System.out.println("Current Server"+this.port+"\nNrNw Server"+NrNw.port);
				if (this.port == NrNw.port) {
					ack = printArticleList();
				}else{
					ack = readRequest(inMsg,NrNw);
				}
				
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			} else if(((String) jsonObject.get("type")).equals(Config.READ)){
				NrNw = Config.NrNw;
				// Return the article which has the highest version number
				if (this.port == NrNw.port) {
					int index = Integer.parseInt((String.valueOf(jsonObject
							.get("index"))));
					for (Article article : articleList) {
						if (article.id == index) {
							ack = article.toString();
						}
					}
				} else {
					ack = readRequest(inMsg,NrNw);
				}
				buffer = ack.getBytes();
				packet = new DatagramPacket(
						buffer,
						buffer.length,
						InetAddress.getByName(client.getIpAddress()),
						client.getPortNumber());
				socket.send(packet);
			} else if(((String) jsonObject.get("type")).equals(Config.PRIMARY)){
				setCoordinator(true);
				ack = this.toString();
				buffer = ack.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(client.getIpAddress()), client.getPortNumber());
				socket.send(packet);
			} else if(((String) jsonObject.get("type")).equals(Config.SYNC)){
				// When receive SYNC request, perform synchronization
				articleList = new ArrayList<Article>(union(Config.latestArticles,articleList));
				ack = "Synchronize completed";
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
	
	// Send message to server.
	public String readRequest(String msg, Server server) {
		String ack = "";
		try {
			DatagramSocket socket = new DatagramSocket();
			byte buffer[] = new byte[Config.BUFFER_SIZE];
			buffer = msg.getBytes();
			DatagramPacket packet;

			packet = new DatagramPacket(buffer, buffer.length,
					InetAddress.getByName(server.ip), server.port);
			socket.send(packet);
			System.out.println("Request send to NrNw server");

			// simulate the propagation delay : 25ms to 100ms
			Thread.sleep((long) (25 + Math.random() * 100));

			buffer = new byte[Config.BUFFER_SIZE];
			packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet); // wait for response
			ack = new String(packet.getData());

			socket.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ack;
	}

	// send message to Write Quorum servers. This method is only be called by coordinator
	public void sendAllServer(String msg) {
		// server list set to Write Quorum
		for (Server server : Nw) {
			if (this.port != server.port)
				request(msg, server);
		}
	}
	
	// send message to all other servers. This method is only be called by coordinator
	public void sendServers(String msg) {
		// server list set to all other servers
		for (Server server : serverList) {
			if (this.port != server.port)
				request(msg, server);
		}
	}
		
	// SYNCHRONIZE Get the union of two arraylists
	public ArrayList<Article> union(ArrayList<Article> list1, ArrayList<Article> list2) {
		if(list1.size()==list2.size())
			return new ArrayList<Article>(list1);
		HashMap<Integer, Article> hm = new HashMap<Integer, Article>();
		for(Article a : list1){
			hm.put(a.id, a);
		}
		for(Article b : list2){
			if(!hm.containsKey(b.id)){
				insertArticle(b,list1);
			}
		}
        return list1;
    }
	
	// Format insert article to articleList
		public void insertArticle(Article article, ArrayList<Article> articleList){
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

//	public static void main(String[] args) {
//		System.out.println(args[0]+";"+args[1]);
//		// Create server
//		 new QuorumServer(args[0],args[1]);
//		 new QuorumServer(args[0],args[1]);
//		 new QuorumServer(args[0],args[1]);
//		 new QuorumServer(args[0],args[1]);
//		 new QuorumServer(args[0],args[1]);
//		
//		new CoordinatorHelper();
//	}

}
