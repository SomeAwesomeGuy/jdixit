package server;

import java.util.HashSet;

public class Player {
	
	private String _name;
	private int _score;
	private HashSet<Integer> _hand;
	
	public Player(String name) {
		_name = name;
		_score = 0;
		_hand = new HashSet<Integer>();
	}
	
	public void sendMessage(String message) {
		
	}
	
	public String getName() {
		return _name;
	}
	
	public int getScore() {
		return _score;
	}
	
	public boolean isInHand(int card) {
		return _hand.contains(card);
	}
	
	public void addScore(int points) {
		_score += points;
	}
	
	public void addToHand(int card) {
		_hand.add(card);
	}
	
	public void removeFromHand(int card) {
		_hand.remove(card);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Player) {
			return _name.equals(((Player)obj).getName());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return _name.hashCode();
	}
}
