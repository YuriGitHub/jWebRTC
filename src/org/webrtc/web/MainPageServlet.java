package org.webrtc.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webrtc.common.Helper;
import org.webrtc.common.SignalingWebSocket;
import org.webrtc.model.Room;


/**The main UI page, renders the 'index.html' template.*/
public class MainPageServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(MainPageServlet.class.getName()); 
	
	/** Renders the main page. When this page is shown, we create a new channel to push asynchronous updates to the client.*/
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Map<String, String> params = Helper.get_query_map(req.getQueryString());
		String room_key    = Helper.sanitize(params.get("r"));
		String debug       = params.get("debug");
	    String stun_server = params.get("ss");
	    if(room_key==null || room_key.equals("")) {
	    	room_key = Helper.generate_random(8);
	        String redirect = "/?r=" + room_key;
	        if(debug!=null)
	        	redirect += ("&debug=" + debug);
	        if(stun_server!=null || !stun_server.equals(""))
	        	redirect += ("&ss=" + stun_server);	        	        
	        logger.info("Redirecting visitor to base URL to " + redirect);
	        resp.sendRedirect(redirect);	        
	        return;
	    }else {
	    	String user = null;
	        int initiator = 0;
	        Room room = Room.get_by_key_name(room_key);
	        if(room==null && debug != "full") { // New room.
	        	user = Helper.generate_random(8);
	        	room = new Room(room_key);
	        	room.add_user(user);
	        	if(!debug.equals("loopback"))
	        		initiator = 0;	          
	        	else {
	        		room.add_user(user);
	        		initiator = 1;	        		
	        	}
	        } else if(room!=null && room.get_occupancy() == 1 && !debug.equals("full")) {// 1 occupant.
	        	user = Helper.generate_random(8);
	        	room.add_user(user);
	        	initiator = 1;
	        } else { // 2 occupants (full).
	        	Map<String, String> template_values = new HashMap<String, String>(); 
		        template_values.put("room_key", room_key);
	        	File file = new File("full.jtpl");
		        resp.getWriter().print(Helper.generatePage(file, "main", template_values));
	        	logger.info("Room " + room_key + " is full");
	        	return;
	        }

	        String room_link = "http://localhost:8080/?r=" + room_key;
	        if(debug!=null)
	        	room_link += ("&debug=" + debug);
	        if(stun_server!=null)
	        	room_link += ("&ss=" + stun_server);

	        	        
	        String token = Helper.make_token(room_key, user);
	        String pc_config = Helper.make_pc_config(stun_server);
	        Map<String, String> template_values = new HashMap<String, String>(); 
	        template_values.put("token", token);
	        template_values.put("me", user);
	        template_values.put("room_key", room_key);
	        template_values.put("room_link", room_link);
	        template_values.put("initiator", ""+initiator);
	        template_values.put("pc_config", pc_config);
	        	        
	        File file = new File("index.jtpl");
	        resp.getWriter().print(Helper.generatePage(file, "main", template_values));
	        logger.info("User " + user + " added to room " + room_key);
	        logger.info("Room " + room_key + " has state " + room);
	    }	    
	}
	
	
}