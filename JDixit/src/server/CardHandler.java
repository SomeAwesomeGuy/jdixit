package server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javax.imageio.ImageIO;

import message.Card;

public class CardHandler {
	private static final String _cardsPath = "cards";
	
	private ArrayList<Card> _deck;
	private HashMap<Card,BufferedImage> _imageMap;
	
	public CardHandler() throws FileNotFoundException {
		_deck = new ArrayList<Card>();
		_imageMap = new HashMap<Card, BufferedImage>();
		
		loadCards();
	}
	
	private void loadCards() throws FileNotFoundException {
		File cardDirectory = new File(_cardsPath);
		if(!cardDirectory.exists() || !cardDirectory.isDirectory()) {
			throw new FileNotFoundException("Can't find the cards directory");
		}
		
		File[] cardFiles = cardDirectory.listFiles();
		for(File f : cardFiles) {
			try {
				final String name = f.getName();
				final int index = name.lastIndexOf(".");
				
				
				Card card = new Card(name.substring(index + 1).toUpperCase());
				
				BufferedImage image = ImageIO.read(f);
				_imageMap.put(card, image);
				
				_deck.add(card);
			} catch (IOException e) {
				System.err.println("Error: Could not load image at " + f.getPath());
//				e.printStackTrace();
			}
			
		}
		
		Collections.shuffle(_deck);
		
		System.out.println(_deck.size() + " cards found");
	}
	
	public BufferedImage getImage(Card card) {
		return _imageMap.get(card);
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
