import org.json.simple.JSONAware;
import org.json.simple.JSONObject;


public class Article implements JSONAware{
	public int id;
	public String title;
	public String content;
	public int replyId;
	
	public Article(){
		
	}
	
	// Construct a POST message
	public Article(String title, String content){
		this.title = title;
		this.content = content;
		// -1 means the article has not been assign an id by coordinator
		id = -1;
		// 0 means that the article is not a reply, otherwise, it just like a foreign key
		replyId = 0;
	}
	
	// coordinator assign the id
	public Article(int id, String title, String content){
		this.id = id;
		this.title = title;
		this.content = content;
	}
	
	// Construct a REPLY message 
	public Article(String title, String content, int replyId) {
		this.replyId = replyId;
		this.title = title;
		this.content = content;
		// -1 means the article has not been assign an id by coordinator
		id = -1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toJSONString(){
	    JSONObject obj = new JSONObject();
	    obj.put("id", new Integer(id));
	    obj.put("title", title);
	    obj.put("content", content);
	    obj.put("replyId", new Integer(replyId));
	    return obj.toString();
	  }
	
	public String toString(){
		return "\nID: " + id + "\nTitle: " + title + "\nContent: " + content;
	}
//	@Override
//	public String toJSONString() {
//		StringBuffer sb = new StringBuffer();
//        
//        sb.append("{");
//        
//        sb.append(JSONObject.escape("id"));
//        sb.append(":");
//        sb.append(id);
//        sb.append(",");
//
//        sb.append(JSONObject.escape("title"));
//        sb.append(":");
//        sb.append("\"" + JSONObject.escape(title) + "\"");
//        
//        sb.append(",");
//        
//        sb.append(JSONObject.escape("content"));
//        sb.append(":");
//        sb.append("\"" + JSONObject.escape(content) + "\"");
//        
//        sb.append("}");
//        
//        return sb.toString();
//	}
	
}
