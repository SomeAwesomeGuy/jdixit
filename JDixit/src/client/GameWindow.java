package client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.omg.CORBA._PolicyStub;

import com.thebuzzmedia.imgscalr.Scalr;

import layout.TableLayout;
import message.Card;
import message.Chat;
import message.ChatLog;
import message.Message;
import message.Message.Status;
import message.Player;

public class GameWindow extends JFrame {
	private static final long serialVersionUID = -5499624552626787637L;
	
	private static final int CHAR_LIMIT = 5000;
	private static final int CHAR_REMOVED = 1000;
	
	private JTextArea _chatArea, _storyArea;
	private JLabel _statusLabel;
	
	private JPanel _handPanel, _tablePanel, _scorePanel, _cardPanel;
	private JButton _submitButton;
	private JTabbedPane _tabbedPane;
	
	private ThumbnailLabel _selectedThumbnail;
	private Card _selectedCard;
	
	private int _latestChatID;
	
	private HashMap<Card, BufferedImage> _cardImageMap;
	private HashMap<Card, JPanel> _cardPanelMap;
	private HashMap<Card, ThumbnailLabel> _cardLabelMap;
	private ArrayList<Card> _handCardList, _tableCardList;
	
	private Status _status;
	private boolean _isStoryTime, _sentCard;

	public GameWindow() {
		super();
		_latestChatID = -1;
		_cardImageMap = new HashMap<Card,BufferedImage>();
		_cardPanelMap = new HashMap<Card, JPanel>();
		_cardLabelMap = new HashMap<Card, ThumbnailLabel>();
		_handCardList = new ArrayList<Card>();
		_tableCardList = new ArrayList<Card>();
		
		buildWindow();
		
	}
	
	private void buildWindow() {		
		final JPanel picPanel = new JPanel();
		picPanel.setBorder(new LineBorder(Color.black));
		picPanel.add(new JLabel("pic"));
		
		final JPanel thumbPanel = new JPanel();
		thumbPanel.setBorder(new LineBorder(Color.black));
		thumbPanel.add(new JLabel("thumb"));
		
		double size[][] = {
				{350, 550},
				{200, 350, 150}
		};
		
		setLayout(new TableLayout(size));
		add(getStatusPanel(), "0, 0");
		add(getChatPanel(), "0, 1");
		add(getPicPanel(), "1, 0, 1, 1");
		add(getThumbPanel(), "0, 2, 1, 2");
		pack();
		
		setTitle("JDixit");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
	}
	
	private JPanel getPicPanel() {
		_cardPanel = new JPanel(new CardLayout());
		
		_submitButton = new JButton("Submit");
		_submitButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(_status == Status.AWAITING_STORY) {
					if(_isStoryTime) {
						storyInput();
					}
				}
				else if(_status == Status.CARD_SUBMISSION){
					submitCard();
				}
				else if(_status == Status.CARD_VOTE) {
					voteCard();
				}
			}
		});
		_submitButton.setEnabled(false);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(_submitButton);
		
		JPanel picPanel = new JPanel(new BorderLayout());
		picPanel.add(_cardPanel, BorderLayout.CENTER);
		picPanel.add(buttonPanel, BorderLayout.SOUTH);
		return picPanel;
	}
	
	private JTabbedPane getThumbPanel() {
		FlowLayout handLayout = new FlowLayout();
		handLayout.setHgap(20);
		_handPanel = new JPanel(handLayout);
		JScrollPane handPane = new JScrollPane(_handPanel);
		
		FlowLayout tableLayout = new FlowLayout();
		tableLayout.setHgap(20);
		_tablePanel = new JPanel(tableLayout);
		JScrollPane tablePane = new JScrollPane(_tablePanel);
		
		FlowLayout scoreLayout = new FlowLayout();
		scoreLayout.setHgap(20);
		_scorePanel = new JPanel(scoreLayout);
		JScrollPane scorePane = new JScrollPane(_scorePanel);
		
		_tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		_tabbedPane.addTab("Hand", handPane);
		_tabbedPane.addTab("Table", tablePane);
		_tabbedPane.addTab("Score", scorePane);
		
		return _tabbedPane;
	}
	
	private JPanel getStatusPanel() {
		JLabel titleLabel = new JLabel("JDixit");
		titleLabel.setFont(new Font("Harrington", Font.BOLD, 52));
		titleLabel.setToolTipText("<html>Made by Sean W.<br>Dedicated to the Mechabananas</html>");
		
		JPanel titlePanel = new JPanel();
		titlePanel.add(titleLabel);
		
		_statusLabel = new JLabel();
		JPanel labelPanel = new JPanel();
		labelPanel.setBorder(new TitledBorder("Status"));
		labelPanel.add(_statusLabel);
		
		_storyArea = new JTextArea();
		_storyArea.setEditable(false);
		_storyArea.setText("Nothing yet...");
		_storyArea.setLineWrap(true);
		_storyArea.setWrapStyleWord(true);
		JScrollPane storyScrollPane = new JScrollPane(_storyArea);
		JPanel storyPanel = new JPanel(new BorderLayout());
		storyPanel.setBorder(new TitledBorder("Story"));
		storyPanel.add(storyScrollPane, BorderLayout.CENTER);
		
		JPanel infoPanel = new JPanel(new BorderLayout());
		infoPanel.add(labelPanel, BorderLayout.NORTH);
		infoPanel.add(storyPanel, BorderLayout.CENTER);
		
		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.add(titlePanel, BorderLayout.NORTH);
		statusPanel.add(infoPanel, BorderLayout.CENTER);
		return statusPanel;
	}
	
	private JPanel getChatPanel() {
		_chatArea = new JTextArea();
		_chatArea.setLineWrap(true);
		_chatArea.setWrapStyleWord(true);
		_chatArea.setEditable(false);
		final JScrollPane displayPane = new JScrollPane(_chatArea);
		
		final JTextArea typeArea = new JTextArea(3, 0);
		typeArea.setLineWrap(true);
		typeArea.setWrapStyleWord(true);
		final JScrollPane typePane = new JScrollPane(typeArea);
		
		final InputMap input = typeArea.getInputMap();
	    input.put(KeyStroke.getKeyStroke("shift ENTER"), "insert-break");
	    input.put(KeyStroke.getKeyStroke("ENTER"), "text-submit");
	    typeArea.getActionMap().put("text-submit", new AbstractAction() {
			private static final long serialVersionUID = -5511385005600585758L;
			@Override
	        public void actionPerformed(ActionEvent e) {
	        	final String text = typeArea.getText();
				if(!text.equals("")) {
					DixitClient.getInstance().sendChat(text);
					typeArea.setText("");
					typeArea.requestFocusInWindow();
					if(_chatArea.getText().length() > CHAR_LIMIT) {
						_chatArea.replaceRange("", 0, CHAR_REMOVED);
					}
				}
	        }
	    });
		
		final JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
		chatPanel.add(displayPane, BorderLayout.CENTER);
		chatPanel.add(typePane, BorderLayout.SOUTH);
		
		chatPanel.setBorder(new TitledBorder("Chatter box"));
		return chatPanel;
	}
	
	private void storyInput() {
		_sentCard = true;
		String story = (String)JOptionPane.showInputDialog(this, "Tell us your story...", "Story Time!", JOptionPane.PLAIN_MESSAGE, null, null, "");
		if(story != null && !story.equals("")) {
			_selectedCard.setAsStoryCard();
			DixitClient.getInstance().sendSubmission(_selectedCard, story);
			removeCard(_selectedCard);
			redrawHand();
		}
	}
	
	private void submitCard() {
		_sentCard = true;
		_submitButton.setEnabled(false);
		DixitClient.getInstance().sendSubmission(_selectedCard, null);
		removeCard(_selectedCard);
		redrawHand();
	}
	
	private void voteCard() {
		_sentCard = true;
		_submitButton.setEnabled(false);
		DixitClient.getInstance().sendSubmission(_selectedCard, null);
	}
	
	public void updateChat(ChatLog log) {
		final ArrayList<Chat> chatList = log.getLatestChats(_latestChatID);
		if(chatList != null) {
			for(Chat c : chatList) {
				_chatArea.append(c.toString() + "\n");
			}
			_chatArea.setCaretPosition(_chatArea.getText().length() - 1);
			_latestChatID = chatList.get(chatList.size() - 1).getId();
		}
	}
	
	public void setStatus(Message message) {
		_status = message.getStatus();
		_sentCard = false;
		_submitButton.setEnabled(false);
		switch(_status) {
		case LOBBY:
			_statusLabel.setText(_status.toString());
			break;
		case AWAITING_STORY:
			_isStoryTime = false;
			_statusLabel.setText(message.getPlayer() + _status.toString());
			break;
		case CARD_SUBMISSION:
			_storyArea.setText(message.getMessage());
			if(_isStoryTime) {
				_statusLabel.setText("Waiting for other players to submit");
				break;
			}
		case CARD_VOTE:
			if(_isStoryTime) {
				_statusLabel.setText("Waiting for other players to vote");
				break;
			}
			_statusLabel.setText(_status.toString());
			break;
		case GAME_END:
			_statusLabel.setText(_status.toString() + message.getPlayer());
		}
		setSelected(_selectedThumbnail);
	}
	
	public void promptForStory() {
		_isStoryTime = true;
		JOptionPane.showMessageDialog(this, "You're the storyteller!\nSubmit a card and write your story.");
	}
	
	public void updateTableCards(ArrayList<Card> cards) {
		showTable();
		for(Card c : cards) {
			ThumbnailLabel label = _cardLabelMap.get(c);
			label.setInfo(c);
			if(c.isStoryCard()) {
				label.setStoryCard();
				setSelected(label);
			}
		}
	}
	
	public void clearCards() {
		ArrayList<Card> cardList = new ArrayList<Card>();
		cardList.addAll(_handCardList);
		
		for(Card c : cardList) {
			removeCard(c);
		}
		
		cardList.clear();
		cardList.addAll(_tableCardList);
		
		for(Card c : cardList) {
			removeCard(c);
		}
		_cardPanel.removeAll();
		_cardPanel.repaint();
		_cardPanel.revalidate();
	}
	
	public void addCard(Card card, BufferedImage image) {
		_cardImageMap.put(card, image);
		
		if(_status == Status.AWAITING_STORY) {
			_handCardList.add(card);
		}
		else if(_status == Status.CARD_VOTE) {
			_tableCardList.add(card);
		}
	}
	
	public boolean hasCard(Card card) {
		return _cardImageMap.containsKey(card);
	}
	
	public void clearTable() {
		ArrayList<Card> list = new ArrayList<Card>();
		list.addAll(_tableCardList);
		for(Card c : list) {
			removeCard(c);
		}
	}
	
	public void showTable() {
		_tabbedPane.setSelectedIndex(1);
	}
	
	public void showHand() {
		_tabbedPane.setSelectedIndex(0);
	}
	
	public void removeCard(Card card) {
		_handCardList.remove(card);
		_tableCardList.remove(card);
		_cardImageMap.remove(card);
		_cardLabelMap.remove(card);
		if(card == _selectedCard) {
			_selectedCard = null;
		}
		CardLayout layout = (CardLayout)_cardPanel.getLayout();
		
		layout.removeLayoutComponent(_cardPanelMap.get(card));
		_cardPanelMap.remove(card);
	}
	
	public void setSelected(ThumbnailLabel label) {
		if(label == null) {
			return;
		}
		
		if(_selectedThumbnail != null) {
			_selectedThumbnail.setSelected(false);
		}
		_selectedThumbnail = label;
		_selectedThumbnail.setSelected(true);
		_selectedCard = _selectedThumbnail.getCard();
		CardLayout layout = (CardLayout)_cardPanel.getLayout();
		layout.show(_cardPanel, "" + label.getCard().getId());
		
		switch(_status) {
		case AWAITING_STORY:
			_submitButton.setEnabled(!_sentCard && _isStoryTime && _selectedThumbnail.isHandCard());
			break;
		case CARD_SUBMISSION:
			_submitButton.setEnabled(!_sentCard && !_isStoryTime && _selectedThumbnail.isHandCard());
			break;
		case CARD_VOTE:
			_submitButton.setEnabled(!_sentCard && !_isStoryTime && !_selectedThumbnail.isHandCard());
			break;
		case GAME_END:
			_submitButton.setEnabled(false);
			break;
		}
	}
	
	public int getHandSize() {
		return _handCardList.size();
	}
	
	public int getTableSize() {
		return _tableCardList.size();
	}
	
	public void redrawTable() {
		_tablePanel.removeAll();
		_tablePanel.repaint();
		
		Card[] order = new Card[_tableCardList.size()];
		
		for(Card c : _tableCardList) {
			order[c.getTablePosition()] = c;
		}

		for(Card c : order) {
			final BufferedImage image = _cardImageMap.get(c);
			final JPanel imagePanel = new JPanel(new GridBagLayout());
			imagePanel.add(new JLabel(new ImageIcon(image)), new GridBagConstraints());
			_cardPanel.add(imagePanel, "" + c.getId());
			_cardPanelMap.put(c, imagePanel);
			
			final ThumbnailLabel label = new ThumbnailLabel(c, new ImageIcon(Scalr.resize(image, 100)), false);
			label.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					setSelected(label);
				}
			});
			
			_tablePanel.add(label);
			_cardLabelMap.put(c, label);
		}
		
		_handPanel.revalidate();
	}
	
	public void redrawHand() {
		_handPanel.removeAll();
		_handPanel.repaint();
		
		if(_selectedCard == null && _handCardList.size() > 0) {
			_selectedCard = _handCardList.get(_handCardList.size() - 1);
		}

		for(Card c : _handCardList) {
			final BufferedImage image = _cardImageMap.get(c);
			final JPanel imagePanel = new JPanel(new GridBagLayout());
			imagePanel.add(new JLabel(new ImageIcon(image)), new GridBagConstraints());
			_cardPanel.add(imagePanel, "" + c.getId());
			_cardPanelMap.put(c, imagePanel);
			
			final ThumbnailLabel label = new ThumbnailLabel(c, new ImageIcon(Scalr.resize(image, 100)), true);
			label.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					setSelected(label);
				}
			});
			
			_handPanel.add(label);
			_cardLabelMap.put(c, label);
			
			if(c == _selectedCard) {
				_selectedThumbnail = label;
			}
		}
		setSelected(_selectedThumbnail);
		_handPanel.revalidate();
	}
	
	public void redrawScore(ArrayList<Player> players) {
		_scorePanel.removeAll();
		_scorePanel.repaint();
		
		if(players != null) {
			Collections.sort(players, new Comparator<Player>() {
				@Override
				public int compare(Player p1, Player p2) {
					return p2.getScore() - p1.getScore();
				}
			});
			
			for(Player p : players) {
				JLabel label = new JLabel("<html>" + p.getName() + "<br><br>" + p.getScore() + "</html>");
				label.setFont(new Font("Harrington", Font.BOLD, 20));
				_scorePanel.add(label);
			}
		}
		
		_scorePanel.revalidate();
	}
}
