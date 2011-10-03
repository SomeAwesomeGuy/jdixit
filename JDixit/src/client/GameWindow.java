package client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.omg.CORBA._PolicyStub;

import com.thebuzzmedia.imgscalr.Scalr;

import layout.TableLayout;
import message.Card;
import message.Chat;
import message.ChatLog;
import message.Message;
import message.Message.Status;

public class GameWindow extends JFrame {
	private static final long serialVersionUID = -5499624552626787637L;
	
	private static final int CHAR_LIMIT = 5000;
	private static final int CHAR_REMOVED = 1000;
	
	private JTextArea _chatArea, _storyArea;
	private JLabel _statusLabel;
	
	private JPanel _handPanel, _tablePanel, _scorePanel, _cardPanel;
	private JButton _submitButton;
	
	private ThumbnailLabel _selectedThumbnail;
	private Card _selectedCard;
	
	private int _latestChatID;
	
	private HashMap<Card,BufferedImage> _cardMap;
	private ArrayList<Card> _cardList;

	private ArrayList<BufferedImage> _tableCards;
	
	private Status _status;
	private boolean _isStoryTime;

	public GameWindow() {
		super();
		_latestChatID = -1;
		_cardMap = new HashMap<Card,BufferedImage>();
		_cardList = new ArrayList<Card>();
		
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
				Card selected = _selectedThumbnail.getCard();
				if(_isStoryTime) {
					storyInput(selected);
				}
				else {
					
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
		FlowLayout layout = new FlowLayout();
		layout.setHgap(20);
		_handPanel = new JPanel(layout);
		JScrollPane handPane = new JScrollPane(_handPanel);
		
		_tablePanel = new JPanel();
		
		
		_scorePanel = new JPanel();
		
		
		JTabbedPane pane = new JTabbedPane(JTabbedPane.LEFT);
		pane.addTab("Hand", handPane);
		pane.addTab("Table", _tablePanel);
		pane.addTab("Score", _scorePanel);
		return pane;
	}
	
	private JPanel getStatusPanel() {
		JLabel titleLabel = new JLabel("JDixit");
		titleLabel.setFont(new Font("Harrington", Font.BOLD, 52));
		titleLabel.setToolTipText("Made by Sean W., dedicated to the Mechabananas");
		
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
	
	private void storyInput(Card card) {
		String story = (String)JOptionPane.showInputDialog(this, "Tell us your story...", "Story Time!", JOptionPane.PLAIN_MESSAGE, null, null, "");
		DixitClient.getInstance().sendSubmission(card, story);
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
		switch(_status) {
		case LOBBY:
			_statusLabel.setText(message.getStatus().toString());
			break;
		case AWAITING_STORY:
			_isStoryTime = false;
			_submitButton.setEnabled(false);
			_statusLabel.setText(message.getPlayer() + message.getStatus().toString());
			break;
		case CARD_SUBMISSION:
			_submitButton.setEnabled(!_isStoryTime);
			if(_isStoryTime) {
				_statusLabel.setText("Waiting for other players to submit");
				break;
			}
			_storyArea.setText(message.getMessage());
		case CARD_VOTE:
			if(_isStoryTime) {
				_statusLabel.setText("Waiting for other players to vote");
				break;
			}
			_statusLabel.setText(message.getStatus().toString());
			break;
		}
	}
	
	public void promptForStory() {
		_isStoryTime = true;
		JOptionPane.showMessageDialog(this, "You're the storyteller!\nSubmit a card and write your story.");
		_submitButton.setEnabled(true);
	}
	
	public void addCard(Card card, BufferedImage image) {
		_cardList.add(card);
		_cardMap.put(card, image);
		_selectedCard = card;
	}
	
	public boolean hasCard(Card card) {
		return _cardMap.containsKey(card);
	}
	
	public void removeCard(Card card) {
		_cardList.remove(card);
		_cardMap.remove(card);
		CardLayout layout = (CardLayout)_cardPanel.getLayout();
		Component[] components = _cardPanel.getComponents();
		
		for(Component c : components) {
			if(c.getName().equals("" + card.getId())) {
				layout.removeLayoutComponent(c);
				System.out.println("Component removed");	//TODO: Remove this
				break;
			}
		}
		
		redrawHand();
	}
	
	public void setSelected(ThumbnailLabel label) {
		if(_selectedThumbnail != null) {
			_selectedThumbnail.setSelected(false);
		}
		_selectedThumbnail = label;
		_selectedThumbnail.setSelected(true);
		CardLayout layout = (CardLayout)_cardPanel.getLayout();
		layout.show(_cardPanel, "" + label.getCard().getId());
	}
	
	public int getHandSize() {
		return _cardMap.size();
	}
	
	public void redrawHand() {
		_handPanel.removeAll();

		for(Card c : _cardList) {
			final BufferedImage image = _cardMap.get(c);
			JPanel imagePanel = new JPanel();
			imagePanel.add(new JLabel(new ImageIcon(image)));
			_cardPanel.add(imagePanel, "" + c.getId());
			
			final BufferedImage thumb = Scalr.resize(image, 100); 
			final ThumbnailLabel label = new ThumbnailLabel(c, new ImageIcon(thumb));
			label.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					setSelected(label);
				}
			});
			
			_handPanel.add(label);
			
			if(c == _selectedCard) {
				_selectedThumbnail = label;
			}
		}
		setSelected(_selectedThumbnail);
		_handPanel.revalidate();
	}
}
