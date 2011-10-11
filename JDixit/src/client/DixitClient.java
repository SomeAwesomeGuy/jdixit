package client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import message.Card;
import message.Message;
import message.Message.Status;
import message.Message.Type;

public class DixitClient {	
	private static final int PORT = 34948;
	private static final int FRQ = 1000;
	
	private static DixitClient _instance;
	
	private CountDownLatch _latch;
	
	private JFrame _connectDialog;
	private JLabel _connectStatus;
	private JButton _connectButton;
	
	private String _ipAddress, _name;
	
	private GameWindow _game;
	
	private long _latestUpdateMessageID;
	private Status _latestStatus;
	
	private int _handSize, _tableSize;
	
	private DixitClient() {
		_latestUpdateMessageID = -1;
		_latestStatus = Status.LOBBY;
		
		launchConnectionDialog();
		
		_latch = new CountDownLatch(1);
		try {
			_latch.await();
			_game = new GameWindow();
			new Timer().scheduleAtFixedRate(new Updater(), 0, FRQ);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Runtime.getRuntime().addShutdownHook(new ShutDownThread());
	}
	
	
	private void launchConnectionDialog() {
		_connectStatus = new JLabel("Connect to the server", JLabel.CENTER);
		
		final JLabel ipLabel = new JLabel(" Server IP: ", JLabel.RIGHT);
		final JLabel nameLabel = new JLabel(" Name: ", JLabel.RIGHT);
		final JPanel labelPanel = new JPanel(new GridLayout(0, 1));
		labelPanel.add(ipLabel);
		labelPanel.add(nameLabel);
		
		final JTextField ipField = new JTextField("localhost", 15); //TODO: fix this for release
		final JTextField nameField = new JTextField("Player " + (int)(Math.random() * 1000), 15);	//TODO: fix this for release
		final JPanel fieldPanel = new JPanel(new GridLayout(0, 1));
		fieldPanel.add(ipField);
		fieldPanel.add(nameField);
		
		final JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.add(labelPanel, BorderLayout.WEST);
		inputPanel.add(fieldPanel, BorderLayout.CENTER);
		
		_connectButton = new JButton("Connect");
		_connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_ipAddress = ipField.getText();
				_name = nameField.getText().trim();
				registerPlayer();
			}
		});
		final JPanel buttonPanel = new JPanel();
		buttonPanel.add(_connectButton);
		
		_connectDialog = new JFrame();
		_connectDialog.setLayout(new BorderLayout());
		_connectDialog.add(_connectStatus, BorderLayout.NORTH);
		_connectDialog.add(inputPanel, BorderLayout.CENTER);
		_connectDialog.add(buttonPanel, BorderLayout.SOUTH);
		_connectDialog.pack();
		
		_connectDialog.setTitle("JDixit");
		_connectDialog.setLocationRelativeTo(null);
		_connectDialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_connectDialog.setVisible(true);
		
		final KeyAdapter enterListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					_connectButton.getActionListeners()[0].actionPerformed(null);
				}
			}
		};
		ipField.addKeyListener(enterListener);
		nameField.addKeyListener(enterListener);
	}
	
	private void registerPlayer() {
		if(_name.equals("")) {
			JOptionPane.showMessageDialog(_connectDialog, "Please enter a name.");
			return;
		}
		if(_connectButton.isEnabled()) {
			_connectButton.setEnabled(false);
			new ConnectionHandler(Type.REGISTER).start();
		}
	}
	
	public void sendChat(String message) {
		new ConnectionHandler(message).start();
	}
	
	public void submitCard(Card card) {
		new ConnectionHandler(Type.SUBMIT, card).start();
	}
	
	private void launchGame() {
		_latch.countDown();
	}
	
	private synchronized void handleUpdate(Message message) {
		_game.updateChat(message.getChatLog());
		_latestUpdateMessageID = message.getMessageID();
		Status status = message.getStatus();
		
		if(status != Status.LOBBY) {
			_game.redrawScore(message.getPlayers());
		}
		
		if(status != _latestStatus) {			
			_game.setStatus(message);
			_latestStatus = status;
			_game.toFront();
			
			if(status == Status.LOBBY) {
				_game.clearCards();
				_game.redrawHand();
				_game.redrawTable();
				_game.redrawScore(message.getPlayers());
			}
			else if(status == Status.AWAITING_STORY) {
				getCards(message);
				
				ArrayList<Card> cards = message.getTableCards();
				if(cards != null) {
					_game.updateTableCards(cards);
				}
				
				if(message.getPlayer().equals(_name)) {
					_game.promptForStory();
				}
			}
			else if(status == Status.CARD_SUBMISSION) {
				_game.showHand();
			}
			else if(status == Status.CARD_VOTE) {
				_game.clearTable();
				getCards(message);
			}
			else if(status == Status.GAME_END) {
				ArrayList<Card> cards = message.getTableCards();
				if(cards != null) {
					_game.updateTableCards(cards);
				}
				JOptionPane.showMessageDialog(_game, "Game Over!");
			}
		}
	}
	
	private void getCards(Message message) {
		ArrayList<Card> cards = message.getStatus() == Status.AWAITING_STORY ? message.getPlayer(_name).getHand() : message.getTableCards();
		_handSize = cards.size();
		_tableSize = message.getTableCards() == null ? 0 : message.getTableCards().size();
		for(Card c : cards) {
			if(!_game.hasCard(c)) {
				new ConnectionHandler(Type.CARD, c).start();
			}
		}
	}
	
	private void receiveCard(Card card, BufferedImage image) {
		_game.addCard(card, image);
		if(_latestStatus == Status.AWAITING_STORY) {
			if(_game.getHandSize() == _handSize) {
				_game.redrawHand();
			}
		}
		else if(_latestStatus == Status.CARD_VOTE) {
			if(_game.getTableSize() == _tableSize) {
				_game.redrawTable();
				_game.showTable();
			}
		}
	}
	
	public void sendSubmission(Card card, String story) {
		if(story == null) {
			if(_latestStatus == Status.CARD_SUBMISSION) {
				new ConnectionHandler(Type.SUBMIT, card).start();
			}
			else if(_latestStatus == Status.CARD_VOTE) {
				new ConnectionHandler(Type.VOTE, card).start();
			}
		}
		else {
			new ConnectionHandler(card, story).start();
		}
	}
	
	public String getName() {
		return _name;
	}
	
	private void disconnected() {
		JOptionPane.showMessageDialog(_game, "Unable to connect with the server. This client will now close.");
		System.exit(0);
	}
	
	private class Updater extends TimerTask {
		@Override
		public void run() {
			new ConnectionHandler(Type.UPDATE).start();
		}
	}
	
	private class ConnectionHandler extends Thread {		
		private Message.Type _mode;
		
		private Socket _socket;
		private ObjectInputStream _objectIn;
		private ObjectOutputStream _objectOut;
		private InetSocketAddress _address;
		
		private Message _message;
		
		public ConnectionHandler(Message.Type mode) {
			super();
			_mode = mode;
			_socket = new Socket();
			_address = new InetSocketAddress(_ipAddress, PORT);
			_message = new Message(mode);
			_message.setPlayer(_name);
		}
		
		public ConnectionHandler(String message) {
			this(Type.CHAT);
			_message.setMessage(message);
		}
		
		public ConnectionHandler(Message.Type mode, Card card) {
			this(mode);
			_message.setCard(card);
		}
		
		public ConnectionHandler(Card card, String message) {
			this(Type.STORY);
			_message.setCard(card);
			_message.setMessage(message);
		}
		
		private void connect(int timeout) throws IOException {
			_socket.connect(_address, timeout);
			
			_objectOut = new ObjectOutputStream(_socket.getOutputStream());
			_objectIn = new ObjectInputStream(_socket.getInputStream());
		}
		
		private void close() throws IOException {
			_objectIn.close();
			_objectOut.close();
			_socket.close();
		}
		
		private void initiateConnection() {
			try {
				_connectStatus.setText("Connecting...");
				
				connect(2000);
				System.out.println("Connected to server");
				_objectOut.writeObject(_message);
				_objectOut.flush();
				
				final Message returnMessage = (Message)_objectIn.readObject();
				
				if(returnMessage.getType() == Type.REGISTER) {
					_connectDialog.setVisible(false);
					launchGame();
				}
				else {
					JOptionPane.showMessageDialog(_connectDialog, returnMessage.getMessage());
					_connectButton.setEnabled(true);
					_connectStatus.setText("Connection failed");
				}
				
				close();
			}
			catch (SocketTimeoutException e) {
				unknownServer();
			}
			catch (UnknownHostException e) {
				unknownServer();
			} 
			catch (ConnectException e) {
				unknownServer();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void unknownServer() {
			System.out.println("Error: unknown server address");
			JOptionPane.showMessageDialog(_connectDialog, "Server not found\nCheck the address and try again");
			_connectStatus.setText("Connect to the server");
			_connectButton.setEnabled(true);
		}
		
		private void sendMessage() {
			try {
				connect(500);
				_objectOut.writeObject(_message);
				_objectOut.flush();
				
				close();
			} 
			catch (SocketTimeoutException e) {
				System.err.println("Error: Could not reach server");
			}
			catch (SocketException e) {
				System.err.println("Error: Could not reach server");
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void getCard() {
			try {
				connect(500);
				_objectOut.writeObject(_message);
				_objectOut.flush();
				BufferedImage image = ImageIO.read(_socket.getInputStream());
				receiveCard(_message.getCard(), image);
				
				close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void getUpdate() {
			try {
				_message.setMessageID(_latestUpdateMessageID);
				
				connect(500);
				_objectOut.writeObject(_message);
				_objectOut.flush();
				
				final Message returnMessage = (Message)_objectIn.readObject();
				if(returnMessage != null) {
					if(returnMessage.getType() == Type.UPDATE) {
						handleUpdate(returnMessage);
					}
					else {
						System.out.println("Error: received unexpected Message type.");
					}
				}
				
				close();
			} 
			catch (SocketTimeoutException e) {
				disconnected();
			}
			catch (SocketException e) {
				disconnected();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			switch(_mode) {
				case REGISTER:
					initiateConnection(); break;
				case CHAT:
				case SUBMIT:
				case VOTE:
				case STORY:
				case EXIT:
					sendMessage(); break;
				case UPDATE:
					getUpdate(); break;
				case CARD:
					getCard(); break;
			}
		}
	}
	
	private class ShutDownThread extends Thread {
		@Override
		public void run() {
			new ConnectionHandler(Type.EXIT).start();
		}
	}
	
	public static DixitClient getInstance() {
		if(_instance == null) {
			_instance = new DixitClient();
		}
		return _instance;
	}

	
	public static void main(String[] args) {
		getInstance();
	}

}
