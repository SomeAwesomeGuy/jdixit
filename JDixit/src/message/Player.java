package message;

import java.io.Serializable;
import java.util.ArrayList;

import server.DixitServer;

public class Player implements Serializable {
	private static final long serialVersionUID = -7629369161724304390L;
	
	private String _name;
	private int _score, _timeLeft;
	private Card _submittedCard, _votedCard;
	private ArrayList<Card> _hand;
	
	public Player(String name) {
		_name = name;
		gameReset();
		checkIn();
	}
	
	public void checkIn() {
		_timeLeft = DixitServer.CHECKIN;
	}
	
	public boolean check() {
		if(--_timeLeft == 0) {
			return true;
		}
		return false;
	}
	
	public void gameReset() {
		_hand = null;
		_score = 0;
		_submittedCard = null;
		_votedCard = null;
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
		_submittedCard = card;
	}
	
	public Card getVotedCard() {
		return _votedCard;
	}
	
	public void setVotedCard(Card card) {
		_votedCard = card;
	}
	
	public void addScore(int points) {
		_score += points;
	}
	
	public void addToHand(Card card) {
		_hand.add(card);
		card.setOwner(this);
	}
	
	public void removeFromHand(Card card) {
		_hand.remove(card);
	}
	
	public void setHand(ArrayList<Card> hand) {
		_hand = hand;
		if(hand != null) {
			for(Card c : _hand) {
				c.setOwner(this);
			}
		}
	}
	
	public ArrayList<Card> getHand() {
		return _hand;
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
		clone.setHand(_hand);
		
		return clone;
	}
	
	
}
