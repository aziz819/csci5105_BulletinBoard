/**
 * The system default
 * 
 * @author fan
 *
 */
public class Config {
	// rank is used for election, also can be marked the number of servers
	public static int rank = 0;
	// set the first number of port as 6600, other servers' port just increase by 1 
	public static int port = 6600;
	// coordinator
	public static Server server = null;
	// ID for article, only be assigned by coordinator
	public static int id = 1;
	
	public static final int BUFFER_SIZE = 256;
	
	// The following stirng are default operation type
	public static final String LEAVE = "leave";
	public static final String JOIN = "join";
	public static final String READ = "read";
	public static final String LIST = "list";
	public static final String POST = "post";
	public static final String REPLY = "reply";
	public static final String PRIMARY = "true";
}
