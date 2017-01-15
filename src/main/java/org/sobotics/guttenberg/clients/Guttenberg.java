package org.sobotics.guttenberg.clients;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.sobotics.guttenberg.finders.NewAnswersFinder;
import org.sobotics.guttenberg.finders.PlagFinder;
import org.sobotics.guttenberg.roomdata.BotRoom;
import org.sobotics.guttenberg.utils.FilePathUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.tunaki.stackoverflow.chat.ChatHost;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.event.EventType;

/**
 * Fetches and analyzes the data from the API
 * */
public class Guttenberg {	
	private StackExchangeClient client;
    private List<BotRoom> rooms;
    private List<Room> chatRooms;
    private ScheduledExecutorService executorService;
	
	public Guttenberg(StackExchangeClient client, List<BotRoom> rooms) {
		this.client = client;
		this.rooms = rooms;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		chatRooms = new ArrayList<>();
	}
	
	public void start() {
		for(BotRoom room:rooms){
            Room chatroom = client.joinRoom(ChatHost.STACK_OVERFLOW ,room.getRoomId());

            if(room.getRoomId()==111347){
            	//check if Guttenberg is running on the server
            	Properties prop = new Properties();

                try{
                    prop.load(new FileInputStream(FilePathUtils.loginPropertiesFile));
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            	
                if (prop.getProperty("location").equals("server")) {
                	chatroom.send("Grias di o/ (SERVER VERSION)" );
                } else {
                	//chatroom.send("Grias di o/ (DEVELOPMENT VERSION; "+prop.getProperty("location")+")" );
                }
            }

            chatRooms.add(chatroom);
            if(room.getMention(chatroom)!=null)
                chatroom.addEventListener(EventType.USER_MENTIONED, room.getMention(chatroom));
            /*if(room.getReply(chatroom)!=null)
                chatroom.addEventListener(EventType.MESSAGE_REPLY, room.getReply(chatroom));*/
        }
		
		
		executorService.scheduleAtFixedRate(()->execute(), 0, 59, TimeUnit.SECONDS);
	}
	
	private void execute() {
		System.out.println("Executing at - " + Instant.now());
		//NewAnswersFinder answersFinder = new NewAnswersFinder();
		
		//Fetch recent answers and set them as targets in a PlagFinder
		
		JsonArray recentAnswers = NewAnswersFinder.findRecentAnswers();
		List<PlagFinder> plagFinders = new ArrayList<PlagFinder>();
		
		for (JsonElement answer : recentAnswers) {
			PlagFinder plagFinder = new PlagFinder(answer.getAsJsonObject());
			plagFinders.add(plagFinder);
		}
		
		
		//Let PlagFinders collect data
		for (PlagFinder finder : plagFinders) {
			finder.collectData();
		}
		
	}
}
