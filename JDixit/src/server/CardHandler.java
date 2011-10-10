package server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
	
	public void resetDeck() {
		_deck.clear();
		_deck.addAll(_imageMap.keySet());
		Collections.shuffle(_deck);
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
				final String extension = name.substring(index + 1).toLowerCase();
				
				if(!isSupportedFormat(extension)) {
					System.err.println("Error: unsupported image format for " + f.getName());
					continue;
				}
				
				Card card = new Card(extension);
				
				BufferedImage image = ImageIO.read(f);
				_imageMap.put(card, image);
				
				_deck.add(card);
			} catch (IOException e) {
				System.err.println("Error: Could not load image at " + f.getName());
//				e.printStackTrace();
			}
			
		}
		
		Collections.shuffle(_deck);
		
		System.out.println(_deck.size() + " cards found");
	}
	
	private boolean isSupportedFormat(String format) {
		final String[] formatNames = ImageIO.getReaderFormatNames();
		
		for(String f : formatNames) {
			if(f.equals(format)) {
				return true;
			}
		}
		
		return false;
	}
	
	public BufferedImage getImage(Card card) {
		return _imageMap.get(card);
	}
	
	public int deckSize() {
		return _deck.size();
	}
	
	public Card deal() {
		if(_deck.size() > 0) {
			return _deck.remove(0);
		}
		return null;
	}
	
	public ArrayList<Card> dealHand(int handSize) {
		if(_deck.size() < handSize) {
			return null;
		}
		ArrayList<Card> hand = new ArrayList<Card>();
		for(int i = 0; i < handSize; i++) {		
			hand.add(_deck.remove(0));
		}
		
		return hand;
	}
}
