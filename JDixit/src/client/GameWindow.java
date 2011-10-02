package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import layout.TableLayout;
import message.Card;
import message.Chat;
import message.ChatLog;
import message.Message;

public class GameWindow extends JFrame {
	private static final long serialVersionUID = -5499624552626787637L;
	
	private static final int CHAR_LIMIT = 5000;
	private static final int CHAR_REMOVED = 1000;
	
	private JTextArea _textArea;
	
	private int _latestChatID;
	
	private HashMap<Card,BufferedImage> _hand;

	private ArrayList<BufferedImage> _tableCards;

	public GameWindow() {
		super();
		_latestChatID = -1;
		_hand = new HashMap<Card,BufferedImage>();
		
		buildWindow();
		
	}
	
	private void buildWindow() {
		final JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.setBorder(new LineBorder(Color.black));
		statusPanel.add(new JLabel("status"));
		
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
		add(statusPanel, "0, 0");
		add(getChatPanel(), "0, 1");
		add(picPanel, "1, 0, 1, 1");
		add(thumbPanel, "0, 2, 1, 2");
		pack();
		
		setTitle("JDixit");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
	}
	
	private JPanel getChatPanel() {
		_textArea = new JTextArea();
		_textArea.setLineWrap(true);
		_textArea.setEditable(false);
		final JScrollPane displayPane = new JScrollPane(_textArea);
		
		final JTextArea typeArea = new JTextArea(3, 0);
		typeArea.setLineWrap(true);
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
					if(_textArea.getText().length() > CHAR_LIMIT) {
						_textArea.replaceRange("", 0, CHAR_REMOVED);
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
	
	public void updateChat(ChatLog log) {
		final ArrayList<Chat> chatList = log.getLatestChats(_latestChatID);
		if(chatList != null) {
			for(Chat c : chatList) {
				_textArea.append(c.toString() + "\n");
			}
			_textArea.setCaretPosition(_textArea.getText().length() - 1);
			_latestChatID = chatList.get(chatList.size() - 1).getId();
		}
	}
	
	public void setStatusText(Message message) {
		
	}
	
	public void addCard(Card card, BufferedImage image) {
		_hand.put(card, image);
	}
	
	public boolean hasCard(Card card) {
		return _hand.containsKey(card);
	}
}
