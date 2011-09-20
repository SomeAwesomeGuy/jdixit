package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import message.Card;

public class CardHandler {
	private static final String _cardsPath = "cards";
	
	private HashSet<Card> _cards;
	private ArrayList<Card> _deck;
	
	public CardHandler() throws FileNotFoundException {
		
		_cards = new HashSet<Card>();
		
		loadCards();
		
		
	}
	
	private void loadCards() throws FileNotFoundException {
		File cardDirectory = new File(_cardsPath);
		if(!cardDirectory.exists() || !cardDirectory.isDirectory()) {
			throw new FileNotFoundException("Can't find the cards directory");
		}
		
		File[] cardFiles = cardDirectory.listFiles();
		for(File f : cardFiles) {
			Card card = new Card(f);
			_cards.add(card);
			_deck.add(card);
		}
		
		Collections.shuffle(_deck);
		
		System.out.println(_cards.size() + " cards found");
	}
	
	
	
	public Card deal() {
		if(_deck.size() > 0) {
			return _deck.remove(0);
		}
		return null;
	}
	
	public HashSet<Card> dealHand() {
		if(_deck.size() < 6) {		// TODO: Parameterize hand size?
			return null;
		}
		HashSet<Card> hand = new HashSet<Card>();
		for(int i = 0; i < 6; i++) {		
			hand.add(_deck.remove(0));
		}
		
		return hand;
	}
}
