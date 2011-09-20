package message;

import java.io.File;
import java.io.Serializable;

public class Card implements Serializable {
	private static final long serialVersionUID = -4879978242699827818L;
	
	private static int _nextId = 0;

	private File _file;
	private int _id;
	private int _tablePosition;
	
	public Card(File file) {
		_id = _nextId++;
		_file = file;
	}
	
	
	public int getId() {
		return _id;
	}
	
	public void setTablePosition(int position) {
		_tablePosition = position;
	}
	
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Card) {
			return _id == ((Card)obj).getId();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return _id;
	}
}
