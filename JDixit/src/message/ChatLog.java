package message;

import java.io.Serializable;
import java.util.ArrayList;

public class ChatLog implements Serializable {
	private static final long serialVersionUID = -67622461610166898L;
	private static final int LIMIT = 20;
	
	private ArrayList<Chat> _chats;
	
	public ChatLog() {
		_chats = new ArrayList<Chat>();
	}
	
	public int getLatestID() {
		return _chats.get(_chats.size() - 1).getId();
	}
	
	public synchronized void addChat(String name, String message) {
		_chats.add(new Chat(name, message));
		if(_chats.size() > LIMIT) {
			_chats.remove(0);
		}
	}

	public ArrayList<Chat> getLatestChats(int lastID) {
		if(_chats.size() == 0 || getLatestID() == lastID) {
			return null;
		}
		
		int firstChatID = _chats.get(0).getId();
		
		if(lastID < firstChatID) {
			return _chats;
		}
		
		final ArrayList<Chat> latestChats = new ArrayList<Chat>();
		int nextIndex = lastID - firstChatID + 1;
		
		for(int i = nextIndex; i < _chats.size(); i++) {
			latestChats.add(_chats.get(i));
		}
		
		return latestChats;
	}
}
