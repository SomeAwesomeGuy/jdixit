package message;

import java.io.Serializable;

public class Chat implements Serializable {
	private static final long serialVersionUID = -6107439022842413257L;

	private static int _nextId = 0;
	
	private String _message, _name;
	private int _id;
	
	public Chat(String name, String message) {
		_message = message;
		_name = name;
		_id = _nextId++;
	}

	public String getMessage() {
		return _message;
	}

	public String getName() {
		return _name;
	}

	public int getId() {
		return _id;
	}
	
	@Override
	public String toString() {
		return _name + ": " + _message;
	}
}
