package client;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import message.Card;
import message.Player;

public class ThumbnailLabel extends JLabel {
	private static final long serialVersionUID = -1487633696598109579L;
	
	private Card _card;
	private boolean _isHandCard, _isStoryCard;

	public ThumbnailLabel(Card card, ImageIcon icon, boolean isHandCard) {
		super(icon);
		_card = card;
		_isHandCard = isHandCard;
		_isStoryCard = false;
	}
	
	public void setSelected(boolean selected) {
		if(selected) {
			Color color = _isStoryCard ? Color.RED : Color.BLACK;
			setBorder(BorderFactory.createLineBorder(color, 5));
		}
		else {
			if(_isStoryCard) {
				setBorder(BorderFactory.createLineBorder(Color.ORANGE, 5));
			}
			else {
				setBorder(null);
			}
		}
	}
	
	public Card getCard() {
		return _card;
	}
	
	public boolean isHandCard() {
		return _isHandCard;
	}
	
	public void setStoryCard() {
		_isStoryCard = true;
	}
	
	public void setInfo(Card card) {
		StringBuilder tooltip = new StringBuilder();
		tooltip.append("<html>Owner:<br>" + card.getOwner().getName() + "<br><br>Voters:");
		
		ArrayList<Player> voters = card.getVoters();
		if(voters.size() == 0) {
			tooltip.append("<br>None :(");
		}
		else {
			for(Player v : voters) {
				tooltip.append("<br>" + v.getName());
			}
		}
		tooltip.append("</html>");
		setToolTipText(tooltip.toString());
	}
}
