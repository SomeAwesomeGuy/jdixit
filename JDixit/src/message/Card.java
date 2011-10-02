package message;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class Card implements Serializable {
	private static final long serialVersionUID = -4879978242699827818L;
	
	private static int _nextId = 0;

	private String _format;
	private int _id;
	private int _tablePosition;
	private Player _owner;
	private ArrayList<Player> _voters;
	
	public Card(String format) throws IOException {
		_id = _nextId++;
		_voters = new ArrayList<Player>();
		_format = format;
	}
	
	
	public int getId() {
		return _id;
	}
	
	public void setTablePosition(int position) {
		_tablePosition = position;
	}
	
	public int getTablePosition() {
		return _tablePosition;
	}
	
	public void setOwner(Player owner) {
		_owner = owner;
	}
	
	public Player getOwner() {
		return _owner;
	}
	
	public void addVoter(Player voter) {
		_voters.add(voter);
	}
	
	public ArrayList<Player> getVoters() {
		return _voters;
	}
	
	public String getFormat() {
		return _format;
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
