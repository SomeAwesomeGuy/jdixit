package client;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import message.Card;

public class ThumbnailLabel extends JLabel {
	private static final long serialVersionUID = -1487633696598109579L;
	
	private Card _card;

	public ThumbnailLabel(Card card, ImageIcon icon) {
		super(icon);
		_card = card;
	}
	
	public void setSelected(boolean selected) {
		if(selected) {
			setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));
		}
		else {
			setBorder(null);
		}
	}
	
	public Card getCard() {
		return _card;
	}
}
