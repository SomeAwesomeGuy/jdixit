package message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Message implements Serializable {
	private static final long serialVersionUID = -8006817488241702192L;

	public enum Type {
		FAIL,
		CHAT,
		STORY,
		SUBMIT,
		VOTE,
		REGISTER,
		PIC,
		UPDATE,
		EXIT
	}
	
	public enum Status {
		LOBBY ("Waiting for game to start"),
		AWAITING_STORY ("is thinking of a story"),
		CARD_SUBMISSION ("Pick a card from your hand"),
		CARD_VOTE("Choose the storyteller's card");
		
		private String _statusMessage;
		private Status(String message) {
			_statusMessage = message;
		}
		public String toString() {
			return _statusMessage;
		}
	}
	
	private long _messageID;
	
	private HashMap<String,Integer> _scores;
	
	private ArrayList<String> _players;
	
	private ChatLog _chatLog;
	
	private String _message, _player;
	
	private Type _type;
	
	private Status _status;
	
	private int _cardID;
	
	public Message(Type type) {
		clear();
		_type = type;
		_messageID = 0;
	}
	
	public void clear() {
		_scores = null;
		_message = null;
		_player = null;
		_type = null;
		_cardID = -1;
	}
	
	public void setChange() {
		_messageID++;
	}

	public HashMap<String, Integer> getScores() {
		return _scores;
	}

	public void setScores(HashMap<String, Integer> scores) {
		_scores = scores;
	}
	
	public ArrayList<String> getPlayers() {
		return _players;
	}
	
	public void setPlayers(ArrayList<String> players) {
		_players = players;
	}
	
	public ChatLog getChatLog() {
		return _chatLog;
	}
	
	public void setChatLog(ChatLog chatLog) {
		_chatLog = chatLog;
	}

	public String getMessage() {
		return _message;
	}

	public void setMessage(String message) {
		_message = message;
	}

	public String getPlayer() {
		return _player;
	}

	public void setPlayer(String player) {
		_player = player;
	}

	public Type getType() {
		return _type;
	}

	public void setType(Type type) {
		_type = type;
	}
	
	public Status getStatus() {
		return _status;
	}

	public void setStatus(Status status) {
		_status = status;
	}

	public int getCard() {
		return _cardID;
	}

	public void setCard(int card) {
		_cardID = card;
	}
	
	public void setMessageID(long id) {
		_messageID = id;
	}
	
	public long getMessageID() {
		return _messageID;
	}
}
