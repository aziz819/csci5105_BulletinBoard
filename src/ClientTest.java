import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Provide a command interface for user to perform functions in client.
 * 
 * @author Fan Zhang, Zhiqi Chen
 */

public class ClientTest {
	public static Client client;
	
	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		if (str.charAt(0) == '-') {
			return false;
		}
		for (int i=0; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}
		return true;
	}
	/*
	 * testInput try to decide whether the input is legal or not
	 * based on two input situate: yes or no; 1,2,3,4,5,6...
	 */
	public static boolean testInput(String input, String testCase) {
		if (input == null || input.isEmpty())
			return false;
		String in = input.trim().toLowerCase();
		if (testCase.equals("yesno")) {
			if (in.equals("yes") || in.equals("no"))
				return true;
			return false;
		}
		if (testCase.equals("number")) {
			if (isInteger(in)) {
				return true;
			}
			return false;
		}
		return false;

	}
	/*
	 * createClient() enable user to create client
	 */
	public static void createClient(){
//		Scanner input = new Scanner(System.in);
//		
//		System.out.print("Please enter client port: ");
//		String port = input.nextLine().trim();
//		while(!testInput(port,"number")){
//			System.out.print("Be smart, enter a port number: ");
//			port = input.nextLine().trim();
//		}
//		client = new Client(Integer.parseInt(port));
		client = new Client();
		System.out.println("Create client success!");

	}
	
	// Print a menu for user to choose
	public static void clientMenu() throws RemoteException{
		System.out.println("\nPlease select following options");
		System.out.println("1. List all servers");
		System.out.println("2. Join Server");
		System.out.println("3. Leave Server");
		System.out.println("4. Post a new article");
		System.out.println("5. List all articles");
		System.out.println("6. Read an article");
		System.out.println("7. Reply an article");
		System.out.println("8. Exit");
		System.out.print("Command: ");
		Scanner option = new Scanner(System.in);
		String selection = option.nextLine().trim();
		if(!testInput(selection,"number")||Integer.parseInt(selection)>8||Integer.parseInt(selection)<1){
			System.out.println("\n Invalid input!Enter a number bewteen 1 to 8");
			return;
		}
			
		switch(Integer.parseInt(selection)){
		case 1:client.printServerList();
			break;
		case 2: {
			System.out.println("Which server would you like to connect:");
			client.printServerList();
			JSONObject config = readfile();
			int numberOfServer = Integer.parseInt((String)config.get("rank"));
			String index = option.nextLine().trim();
			if (!testInput(index, "number")
					|| Integer.parseInt(index) > numberOfServer
					|| Integer.parseInt(index) < 1) {
				System.out
						.println("\n Invalid input!Enter a number bewteen 1 to "
								+ numberOfServer);
				return;
			}
			client.connectServer(Integer.parseInt(index));
		}
			break;
		case 3: client.leaveServer();
			break;
		case 4: {
			System.out.println("Please enter a title: ");
			String title = option.nextLine().trim();
			if(title.isEmpty()||title==null)
				return;
			System.out.println("Please enter the content: ");
			String content = option.nextLine().trim();
			if(content.isEmpty()||content==null)
				return;
			Article article = new Article(title,content);
			client.post(article);
		}
			break; 
		case 5: client.list();
			break;
		case 6: break;
		case 7: break;
		case 8:System.exit(0);
			break;
		default: break;
		}
		
	}
	// Get the server info from config.json
		public static JSONObject readfile(){
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
	/**
	 * @param args
	 */
	public static void main(String[] args) throws RemoteException{
		System.out.println("\n-----------Welcome to the Client Console-----------");
		System.out.println("Author: Fan Zhang, Zhiqi Chen\n");
		System.out.println("Do you want to create a client now? Yes/No:");
		Scanner user_input = new Scanner(System.in);
		String input = user_input.nextLine().trim().toLowerCase();
		while(!testInput(input,"yesno")){
			System.out.println("\nPlease follow instruction: Just type Yes/No");
			System.out.println("Do you want to create a client now? Yes/No:");
			input = user_input.nextLine().trim().toLowerCase();
		}
		if(input.equals("yes")){
			createClient();
			while(true){
				clientMenu();
			}
		}else{
			System.out.println("Thanks for using Client Console!");
			System.exit(0);
		}
			
	}
}
