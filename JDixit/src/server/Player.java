package server;

import java.util.HashSet;

public class Player {
	
	private String _name;
	private int _score, _submittedCard, _votedCard;
	private HashSet<Integer> _hand;
	
	public Player(String name) {
		_name = name;
		_score = 0;
		_hand = new HashSet<Integer>();
	}
	
	public void turnReset() {
		_submittedCard = -1;
		_votedCard = -1;
	}
	
	public String getName() {
		return _name;
	}
	
	public int getScore() {
		return _score;
	}
	
	public int getSubmittedCard() {
		return _submittedCard;
	}
	
	public void setSubmittedCard(int card) {
		_votedCard = card;
	}
	
	public int getVotedCard() {
		return _votedCard;
	}
	
	public void setVotedCard(int card) {
		_submittedCard = card;
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
