import java.util.ArrayList;

/**
 * The system default
 * 
 * @author Fan Zhang, Zhiqi Chen
 *
 */
public class Config {
	// define the total number of servers
	public static int total;
	// rank is used for election, also can be marked the number of servers
	public static int rank = 0;
	// set the inital port 
	public static final int FIRST_PORT = 6600;
	// set the first number of port as 6600, other servers' port just increase by 1 
	public static int port = FIRST_PORT;
	// coordinator
	public static Server server = null;
	// NrNw is the intersection of Read quorum and Write quorum
	public static Server NrNw = null;
	// ID for article, only be assigned by coordinator
	public static int id = 1;
	
	// Set a limit for counter of synch operation. Once the Limit is reached, then perform synch operation
	// After total LIMITS number of posts or reply, then synch
	public static final int LIMIT = 20;
	// Set a Latest version of article list
	public static ArrayList<Article> latestArticles;
	
	// Set a number of total number of articles should show up in a page
	public static final int PAGE_LIMIT = 20;
	// Set the buffer size for the UDP socket
	public static final int BUFFER_SIZE = 1024;
	
	// The following stirng are default operation type
	public static final String LEAVE = "leave";
	public static final String JOIN = "join";
	public static final String READ = "read";
	public static final String LIST = "list";
	public static final String POST = "post";
	public static final String REPLY = "reply";
	public static final String PRIMARY = "true";
	public static final String SYNC = "sync";
}
