package message;

import java.util.HashSet;

public class Player {
	
	private String _name;
	private int _score;
	private Card _submittedCard, _votedCard, _mostRecent;
	private HashSet<Card> _hand;
	
	public Player(String name) {
		_name = name;
		_score = 0;
		_hand = null;
		_mostRecent = null;
	}
	
	public void turnReset() {
		_hand.remove(_submittedCard);
		_submittedCard = null;
		_votedCard = null;
	}
	
	public String getName() {
		return _name;
	}
	
	public int getScore() {
		return _score;
	}
	
	public Card getSubmittedCard() {
		return _submittedCard;
	}
	
	public void setSubmittedCard(Card card) {
		_votedCard = card;
	}
	
	public Card getVotedCard() {
		return _votedCard;
	}
	
	public void setVotedCard(Card card) {
		_submittedCard = card;
	}
	
	public boolean isInHand(Card card) {
		return _hand.contains(card);
	}
	
	public void addScore(int points) {
		_score += points;
	}
	
	public void addToHand(Card card) {
		_hand.add(card);
		_mostRecent = card;
	}
	
	public void removeFromHand(Card card) {
		_hand.remove(card);
	}
	
	public void setHand(HashSet<Card> hand) {
		_hand = hand;
	}
	
	public Card getMostRecentCard() {
		return _mostRecent;
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
	
	@Override
	public Object clone() {
		final Player clone = new Player(_name);
		clone.addScore(_score);
		clone.setSubmittedCard(_submittedCard);
		clone.setVotedCard(_votedCard);
		for(Card i : _hand) {
			clone.addToHand(i);
		}
		
		return clone;
	}
	
	
}
