package server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import message.Card;
import message.ChatLog;
import message.Message;
import message.Player;
import message.Message.Status;
import message.Message.Type;

public class DixitServer {
	private static final int PORT = 34948;
	private static final int MIN_PLAYERS = 1;
//	private static final int MAX_PLAYERS = 8;
	
	private InfoWindow _infoWindow;
	
	private ClientHandler _clientHandler;
	private CardHandler _cardHandler;
	
	private ServerSocket _serverSocket;
	
	private HashMap<String, Player> _playerMap;
	private ArrayList<Player> _playerList;
	private int _playersLeft;
	
	private Message _gameState;
	
	private int _storyTeller;
	private Card _storyCard;
	private ArrayList<Card> _tableCards;
	
	public DixitServer() {
		_playerMap = new HashMap<String, Player>();
		_playerList = new ArrayList<Player>();
		_gameState = new Message(Type.UPDATE);
		_storyTeller = 0;
		_storyCard = null;
		_tableCards = new ArrayList<Card>();

		_gameState.setStatus(Status.LOBBY);
		_gameState.setChatLog(new ChatLog());
		
		try {
			_cardHandler = new CardHandler();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			_infoWindow = new InfoWindow(InetAddress.getLocalHost().getHostAddress());
			_serverSocket = new ServerSocket(PORT);
			
			System.out.println("Server started, listening on port " + PORT);
			_clientHandler = new ClientHandler();
			_clientHandler.start();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private synchronized boolean registerPlayer(String name, ClientConnection connection) {
		if(_playerMap.containsKey(name)) {
			return false;
		}
		final Player player = new Player(name);
		_playerMap.put(name, player);
		_playerList.add(player);
		_infoWindow.refreshPlayerPanel();
		System.out.println("Player " + name + " has joined the game");
		return true;
	}
	
	private void transferPlayersToGamestate() {
		ArrayList<Player> players = new ArrayList<Player>();
		for(Player p : _playerList) {
			players.add((Player)p.clone());
		}
		_gameState.setPlayers(players);
	}
	
	private void startGame() { //TODO: may not need this
		System.out.println("Game started");
		startTurn();
	}
	
	private void dealCards() {
		if(_gameState.getStatus() == Status.LOBBY) {
			for(Player p : _playerList) {
				p.setHand(_cardHandler.dealHand());
			}
		}
		else {
			for(Player p : _playerList) {
				p.addToHand(_cardHandler.deal());
			}
		}
	}
	
	private String getStoryTeller() {
		return _playerList.get(_storyTeller).getName();
	}
	
	private void startTurn() {
		dealCards();
		transferPlayersToGamestate();
		_gameState.setPlayer(getStoryTeller());
		_gameState.setMessage(null);
		_gameState.setCard(null);
		
		_playersLeft = _playerList.size();
		for(Player p : _playerList) {
			p.turnReset();
		}

		_gameState.setStatus(Status.AWAITING_STORY);
		_gameState.update();
		System.out.println("New turn, storyteller: " + getStoryTeller());
	}
	
	private void addChat(Message message) {
		_gameState.getChatLog().addChat(message.getPlayer(), message.getMessage());
		_gameState.update();
	}
	
	private void setStory(Message message) {
		final Player player = _playerMap.get(message.getPlayer());
		
		_gameState.setTableCards(null);
		_tableCards.clear();
		
		_gameState.setMessage(message.getMessage());
		_storyCard = message.getCard();
		_tableCards.add(_storyCard);
		player.setSubmittedCard(_storyCard);
		
		_gameState.setStatus(Status.CARD_SUBMISSION);
		_gameState.update();
		System.out.println("Waiting for cards");
	}
	
	private void acceptSubmission(Message message) {
		final Player player = _playerMap.get(message.getPlayer());
		if(player.getSubmittedCard() == null) {
			_playersLeft--;
		}
		player.setSubmittedCard(message.getCard());
		
		if(_playersLeft == 0) {
			
			_playersLeft = _playerMap.size();
			
			for(Player p : _playerList) {
				if(p.getName().equals(getStoryTeller())) {
					continue;
				}
				Card submitted = p.getSubmittedCard();
				_tableCards.add(submitted);
			}
			
			Collections.shuffle(_tableCards);
			final HashSet<Card> cards = new HashSet<Card>();
			
			for(int i = 0; i < _tableCards.size(); i++) {
				Card card = _tableCards.get(i);
				card.setTablePosition(i + 1);
				cards.add(card);
			}
			
			_gameState.setTableCards(cards);
			_gameState.setStatus(Status.CARD_VOTE);
			_gameState.update();
			System.out.println("Waiting for votes");
		}
	}
	
	private boolean acceptVote(Message message) {
		final Player player = _playerMap.get(message.getPlayer());
		if(message.getCard() == player.getSubmittedCard()) {
			return false;
		}
		if(player.getVotedCard() == null) {
			_playersLeft--;
		}
		player.setVotedCard(message.getCard());
		
		if(_playersLeft == 0) {
			
			for(Player p : _playerList) {
				if(p.getName().equals(getStoryTeller())) {
					continue;
				}
				Card voted = p.getVotedCard();
				voted.addVoter(p);
			}
			
			score();
			
			_storyTeller++;
			_storyTeller %= _playerMap.size(); //wrap around
			startTurn();
		}
		return true;
	}
	
	private void score() {
		int storyVoteCount = _playerList.get(_storyTeller).getSubmittedCard().getVoters().size();
		
		if(storyVoteCount == 0 || storyVoteCount == _playerList.size() - 1) {
			for(int i = 0; i < _playerList.size(); i++) {
				if(i == _storyTeller) {
					continue;
				}
				final Player p = _playerList.get(i);
				
				p.addScore(2);
			}
		}
		else {
			_playerList.get(_storyTeller).addScore(3);
			
			for(Card c : _tableCards) {
				if(c == _storyCard) {
					for(Player p : c.getVoters()) {
						p.addScore(3);
					}
				}
				
				int numVoters = c.getVoters().size();
				c.getOwner().addScore(numVoters);
			}
		}
	}
	
	private Message getUpdate(long latestMessageID) {
		return latestMessageID < _gameState.getMessageID() ? _gameState : null;
	}
	
	public class ClientConnection extends Thread {
		private Socket _socket;
		
		public ClientConnection(Socket socket) {
			super();
			_socket = socket;
		}
		
		@Override
		public void run() {
			try {
				final ObjectOutputStream objectOut = new ObjectOutputStream(_socket.getOutputStream());
				final ObjectInputStream objectIn = new ObjectInputStream(_socket.getInputStream());
				
				final Message message = (Message)objectIn.readObject();
				final Type type = message.getType();
				if(type == Type.REGISTER) {
					final Message returnMessage = new Message(Type.FAIL);
					if(_gameState.getStatus() != Status.LOBBY) {
						returnMessage.setMessage("The game has already started");
					}
					else {
						if(registerPlayer(message.getPlayer(), this)) {
							returnMessage.setType(Type.REGISTER);
						}
						else {
							returnMessage.setMessage("Name already taken");
						}
					}
					
					objectOut.writeObject(returnMessage);
					objectOut.flush();
				}
				else if(type == Type.CHAT) {
					addChat(message);
				}
				else if(type == Type.UPDATE) {
					objectOut.writeObject(getUpdate(message.getMessageID()));
					objectOut.flush();
				}
				else if(type == Type.STORY) {
					if(_gameState.getStatus() == Status.AWAITING_STORY && message.getPlayer().equals(getStoryTeller())) {
						setStory(message);
					}
				}
				else if(type == Type.SUBMIT) {
					if(_gameState.getStatus() == Status.CARD_SUBMISSION && !message.getPlayer().equals(getStoryTeller())) {
						acceptSubmission(message);
					}
				}
				else if(type == Type.VOTE) {
					if(_gameState.getStatus() == Status.CARD_VOTE && !message.getPlayer().equals(getStoryTeller())) {
						acceptVote(message);
					}
				}
				else if(type == Type.CARD) {
					final Card card = message.getCard();
					final BufferedImage image = _cardHandler.getImage(card);
					ImageIO.write(image, card.getFormat(), _socket.getOutputStream());
				}
				
				objectIn.close();
				objectOut.close();
				_socket.close();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Thread for listening for incoming socket connections
	 */
	private class ClientHandler extends Thread {
		@Override
		public void run() {
			while(true) {	//TODO: Change?
				try {
					final Socket socket = _serverSocket.accept();
//					System.out.println("Client connected from " + socket.getRemoteSocketAddress());
					new ClientConnection(socket).start();
				} 
				catch (SocketException e) {
					System.out.println("Socket closed");
				}
				catch (IOException e) {
					System.out.println("Error: socket problem");
//					e.printStackTrace();
				}
			}
		}
	}
	
	private class InfoWindow extends JFrame {
		private static final long serialVersionUID = -4848838839984637433L;
		
		private final JPanel _playerPanel;
		private final JButton _startButton;
		
		public InfoWindow(String ipAddress) {
			super();
			
			final JLabel ipLabel = new JLabel(ipAddress);
			final JPanel ipPanel = new JPanel();
			ipPanel.setBorder(BorderFactory.createTitledBorder("IP Address:"));
			ipPanel.add(ipLabel);
			
			final JLabel noneLabel = new JLabel("<none>");
			_playerPanel = new JPanel(new GridLayout(0, 1));
			_playerPanel.setBorder(BorderFactory.createTitledBorder("Players:"));
			_playerPanel.add(noneLabel);
			
			_startButton = new JButton("Start game");
			_startButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					startGame();
				}
			});
			_startButton.setEnabled(false);
			final JPanel buttonPanel = new JPanel();
			buttonPanel.add(_startButton);
			
			setLayout(new BorderLayout());
			add(ipPanel, BorderLayout.NORTH);
			add(_playerPanel, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.SOUTH);
			pack();
			
			setTitle("JDixit Server");
			setLocationRelativeTo(null);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setVisible(true);
		}
		
		public void refreshPlayerPanel() {
			_playerPanel.removeAll();
			for(Player p : _playerList) {
				_playerPanel.add(new JLabel(p.getName()));
			}
			_playerPanel.revalidate();
			pack();
			if(_playerMap.size() >= MIN_PLAYERS) {
				_startButton.setEnabled(true);
			}
		}
		
	}
	
	public static void main(String[] args) {
		new DixitServer();
	}

}

//TODO: Close socket on exit
