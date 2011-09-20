package message;

import java.io.Serializable;
import java.util.ArrayList;

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
	
	private ArrayList<Player> _players;
	
	private ChatLog _chatLog;
	
	private String _message, _player;
	
	private Type _type;
	
	private Status _status;
	
	private Card _card;
	
	public Message(Type type) {
		clear();
		_type = type;
		_messageID = 0;
	}
	
	public void clear() {
		_message = null;
		_player = null;
		_type = null;
		_card = null;
	}
	
	public void setChange() {
		_messageID++;
	}
	
	public ArrayList<Player> getPlayers() {
		return _players;
	}
	
	public void setPlayers(ArrayList<Player> players) {
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

	public Card getCard() {
		return _card;
	}

	public void setCard(Card card) {
		_card = card;
	}
	
	public void setMessageID(long id) {
		_messageID = id;
	}
	
	public long getMessageID() {
		return _messageID;
	}
}
