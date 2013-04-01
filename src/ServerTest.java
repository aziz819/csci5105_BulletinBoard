import java.util.Scanner;

/**
 * UI for starting up servers. Allow user to choose different types of consistency
 * 
 * @author Fan Zhang, Zhiqi Chen
 *
 */
public class ServerTest {
	
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
	
	// Check whether Nr and Nw satisfy the constraints
	public static boolean checkConstrain(int rsize, int wsize, int nsize) {
		if (rsize + wsize > nsize
				&& wsize > nsize / 2)
			return true;
		else
			return false;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("\n-----------Welcome to the Server Console-----------");
		System.out.println("Author: Fan Zhang, Zhiqi Chen\n");
		System.out.println("1. sequential consistency \n2. Quorum consistency \nWhich type of consistency you want to run. Please enter 1 or 2:");
		Scanner user_input = new Scanner(System.in);
		String input = user_input.nextLine().trim().toLowerCase();
		while(!testInput(input,"number")||Integer.parseInt(input)<1||Integer.parseInt(input)>2){
			System.out.println("\nPlease follow instruction: Just type 1 or 2");
			input = user_input.nextLine().trim().toLowerCase();
		}
		if(input.equals("1")){
			System.out.println("How many servers you want to run:");
			input = user_input.nextLine().trim().toLowerCase();
			while(!testInput(input,"number")){
				System.out.println("\nPlease follow instruction: Just type a number");
				input = user_input.nextLine().trim().toLowerCase();
			}
			int i=0;
			Config.total = Integer.parseInt(input);
			while(i<Integer.parseInt(input)){
				new Server();
				i++;
			}
			//hold a leader election to determine a new server.
			new CoordinatorHelper();
		}else{
			System.out.println("How many servers you want to run:");
			input = user_input.nextLine().trim().toLowerCase();
			while(!testInput(input,"number")){
				System.out.println("\nPlease follow instruction: Just type a number");
				input = user_input.nextLine().trim().toLowerCase();
			}
			int nsize = Integer.parseInt(input);
			System.out.println("Please assign the size of Read Quorum:");
			input = user_input.nextLine().trim().toLowerCase();
			while(!testInput(input,"number")||Integer.parseInt(input)>nsize){
				System.out.println("\nPlease check your input. Input value should be follow Nr + Nw > N");
				input = user_input.nextLine().trim().toLowerCase();
			}
			int rsize = Integer.parseInt(input);
			System.out.println("Please assign the size of Write Quorum:");
			input = user_input.nextLine().trim().toLowerCase();
			while(!testInput(input,"number")||Integer.parseInt(input)>nsize||!checkConstrain(rsize, Integer.parseInt(input), nsize)){
				System.out.println("\nPlease check your input. Input value should be follow Nw > N/2");
				input = user_input.nextLine().trim().toLowerCase();
			}
			int wsize = Integer.parseInt(input);
			int i=0;
			Config.total = nsize;
			while(i<nsize){
				new QuorumServer(rsize,wsize);
				i++;
			}
			//hold a leader election to determine a new coordinator
			new CoordinatorHelper();
		}
	}

}
