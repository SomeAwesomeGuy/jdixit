package server;

import java.awt.BorderLayout;
import java.awt.Font;
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
import java.util.Comparator;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import message.Card;
import message.ChatLog;
import message.Message;
import message.Player;
import message.Message.Status;
import message.Message.Type;

public class DixitServer {
	private static final int PORT = 34948;
	private static final int MIN_PLAYERS = 3;
	private static final int MAX_PLAYERS = 8;
	private static final int HAND_SIZE = 6;
	private static final String SYSTEM = "System";
	
	private InfoWindow _infoWindow;
	
	private ClientHandler _clientHandler;
	private CardHandler _cardHandler;
	
	private ServerSocket _serverSocket;
	
	private HashMap<String, Player> _playerMap;
	private ArrayList<Player> _playerList;
	private int _playersLeft;
	
	private Message _gameState;
	
	private int _storyTeller, _scoreLimit;
	private Card _storyCard;
	private ArrayList<Card> _tableCards;
	
	public DixitServer() {
		_playerMap = new HashMap<String, Player>();
		_playerList = new ArrayList<Player>();
		_gameState = new Message(Type.UPDATE);
		_storyTeller = 0;
		_storyCard = null;
		_tableCards = new ArrayList<Card>();
		_scoreLimit = 0;

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
	
	private synchronized String registerPlayer(String name, ClientConnection connection) {
		if(_playerMap.containsKey(name) || name.equals(SYSTEM)) {
			return "This name is already taken.";
		}
		if(_playerList.size() == MAX_PLAYERS) {
			return "Cannot connect. This game is full.";
		}
		final Player player = new Player(name);
		_playerMap.put(name, player);
		_playerList.add(player);
		_infoWindow.refreshPlayerPanel();
		_gameState.getChatLog().addChat(SYSTEM, "Player \"" + name + "\" has joined the game");
		_gameState.update();
		return null;
	}
	
	private void transferPlayersToGamestate() {
		ArrayList<Player> players = new ArrayList<Player>();
		for(Player p : _playerList) {
			players.add((Player)p.clone());
		}
		_gameState.setPlayers(players);
	}
	
	private void startGame() {
		System.out.println("Game started");
		Collections.shuffle(_playerList);
		dealCards();
		_gameState.getChatLog().addChat(SYSTEM, "Let the games begin!");
		startTurn();
	}
	
	private void resetGame() {
		_cardHandler.resetDeck();
		
		for(Player p : _playerList) {
			p.gameReset();
		}
		
		_gameState.setPlayers(null);
		_gameState.getChatLog().addChat(SYSTEM, "The game has been reset.");
		_gameState.setTableCards(null);
		_gameState.setPlayer(null);
		_gameState.setCard(null);
		_gameState.setStatus(Status.LOBBY);
		_gameState.update();
	}
	
	private boolean dealCards() {
		if(_gameState.getStatus() == Status.LOBBY) {
			for(Player p : _playerList) {
				p.setHand(_cardHandler.dealHand(HAND_SIZE));
			}
		}
		else {
			if(_cardHandler.deckSize() < _playerList.size()) {
				return false;
			}
			for(Player p : _playerList) {
				if(p.getHand().size() < HAND_SIZE) {
					p.addToHand(_cardHandler.deal());
				}
			}
		}
		return true;
	}
	
	private String getStoryTeller() {
		return _playerList.get(_storyTeller).getName();
	}
	
	private void endGame() {
		System.out.println("Game over");
		Collections.sort(_playerList, new Comparator<Player>() {
			@Override
			public int compare(Player p1, Player p2) {
				return p2.getScore() - p1.getScore();
			}
		});
		int highscore = _playerList.get(0).getScore();
		String winner = _playerList.get(0).getName();
		for(int i = 1; i < _playerList.size(); i++) {
			Player p = _playerList.get(i);
			if(p.getScore() == highscore) {
				winner += " and " + p.getName();
			}
		}
		
		transferPlayersToGamestate();
		_gameState.getChatLog().addChat(SYSTEM, "Game over!");
		_gameState.setPlayer(winner);
		_gameState.setStatus(Status.GAME_END);
		_gameState.update();
	}
	
	private void startTurn() {
		transferPlayersToGamestate();
		_gameState.setPlayer(getStoryTeller());
		_gameState.setMessage(null);
		_gameState.setCard(null);
		
		_playersLeft = _playerList.size() - 1;
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
		System.out.println("Accepted story from " + message.getPlayer());
		final Player player = _playerMap.get(message.getPlayer());
		
		_gameState.setTableCards(null);
		_tableCards.clear();
		
		_gameState.setMessage(message.getMessage());
		_storyCard = message.getCard();
		player.setSubmittedCard(_storyCard);
		player.removeFromHand(_storyCard);
		
		_gameState.setStatus(Status.CARD_SUBMISSION);
		_gameState.update();
		System.out.println("Waiting for cards");
	}
	
	private void acceptSubmission(Message message) {
		System.out.println("Accepted submission from " + message.getPlayer());
		final Player player = _playerMap.get(message.getPlayer());
		if(player.getSubmittedCard() == null) {
			_playersLeft--;
		}
		player.setSubmittedCard(message.getCard());
		player.removeFromHand(message.getCard());
		if(_playersLeft == 0) {
			advanceToVote();
		}
	}
	
	private void advanceToVote() {
		_playersLeft = _playerMap.size() - 1;
		
		for(Player p : _playerList) {
			Card submitted = p.getSubmittedCard();
			_tableCards.add(submitted);
		}
		
		Collections.shuffle(_tableCards);
		
		for(int i = 0; i < _tableCards.size(); i++) {
			_tableCards.get(i).setTablePosition(i);
		}
		
		_gameState.setTableCards(_tableCards);
		_gameState.setStatus(Status.CARD_VOTE);
		_gameState.update();
		System.out.println("Waiting for votes");
	}
	
	private boolean acceptVote(Message message) {
		System.out.println("Accepted vote from " + message.getPlayer());
		final Player player = _playerMap.get(message.getPlayer());
		if(message.getCard() == player.getSubmittedCard()) {
			return false;
		}
		if(player.getVotedCard() == null) {
			_playersLeft--;
		}
		for(Card c : _tableCards) {
			if(c.equals(message.getCard())) {
				player.setVotedCard(c);
				break;
			}
		}
		
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
			
			if(_scoreLimit > 0) {
				for(Player p : _playerList) {
					if(p.getScore() >= _scoreLimit) {
						endGame();
						return true;
					}
				}
			}
			
			if(dealCards()) {
				startTurn();
			}
			else {
				endGame();
			}
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
	
	private void playerExit(String player) {
		System.out.println(player + " exiting");
		final Status status = _gameState.getStatus();
		if(status == Status.LOBBY) {
			removePlayer(player, false);
		}
		else if(status == Status.AWAITING_STORY) {
			removePlayer(player, player.equals(getStoryTeller()));
		}
		else if(status == Status.CARD_SUBMISSION) {
			if(player.equals(getStoryTeller())) {
				removePlayer(player, true);
			}
			else {
				if(_playerMap.get(player).getSubmittedCard() == null) {
					removePlayer(player, false);
					_playersLeft--;
					if(_playersLeft == 0) {
						advanceToVote();
					}
				}
				else {
					removePlayer(player, false);
				}
			}
		}
		else if(status == Status.CARD_VOTE) {
			removePlayer(player, true);
		}
		else if(status == Status.GAME_END) {
			removePlayer(player, false);
		}
		if(status != Status.LOBBY && _playerList.size() < MIN_PLAYERS) {
			_gameState.getChatLog().addChat("System", "Not enough players to continue the game");
			resetGame();
		}
	}
	
	private void removePlayer(String player, boolean resetRound) {
		String storyTeller = _gameState.getStatus() == Status.LOBBY ? "" : getStoryTeller();
		_playerList.remove(_playerMap.get(player));
		_playerMap.remove(player);
		_infoWindow.refreshPlayerPanel();
		_gameState.getChatLog().addChat("System", "Player \"" + player + "\" has left the game");
		if(resetRound) {
			for(Player p : _playerList) {
				p.turnReset();
			}
			_gameState.getChatLog().addChat("System", "The round has been reset");
			dealCards();
			_gameState.setStatus(Status.AWAITING_STORY);
		}
		if(_playerList.size() >= MIN_PLAYERS && storyTeller.equals(player)) {
			_storyTeller %= _playerMap.size(); //wrap around
		}
		else {
			for(int i = 0; i < _playerList.size(); i++) {
				if(_playerList.get(i).getName().equals(storyTeller)) {
					_storyTeller = i;
				}
			}
		}
		transferPlayersToGamestate();
		_gameState.update();
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
						String reason = registerPlayer(message.getPlayer(), this);
						if(reason == null) {
							returnMessage.setType(Type.REGISTER);
						}
						else {
							returnMessage.setMessage(reason);
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
				else if(type == Type.EXIT) {
					playerExit(message.getPlayer());
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
		private final JRadioButton _cardEnd, _pointLimit;
		
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
			
			
			_cardEnd = new JRadioButton("Cards run out", true);
			_cardEnd.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					_scoreLimit = 0;
					_pointLimit.setText("Point limit");
				}
			});
			_pointLimit = new JRadioButton("Point limit", false);
			_pointLimit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getPointLimit();
				}
			});
			
			final JPanel settingsPanel = new JPanel(new GridLayout(2, 1));
			settingsPanel.setBorder(BorderFactory.createTitledBorder("End Condition:"));
			settingsPanel.add(_cardEnd);
			settingsPanel.add(_pointLimit);
			
			_startButton = new JButton("Start Game");
			_startButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					startGame();
					_startButton.setEnabled(false);
				}
			});
			_startButton.setEnabled(false);
			JButton resetButton = new JButton("Reset Game");
			resetButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					resetGame();
					if(_playerList.size() >= MIN_PLAYERS) {
						_startButton.setEnabled(true);
					}
				}
			});
			
			ButtonGroup endCondition = new ButtonGroup();
			endCondition.add(_cardEnd);
			endCondition.add(_pointLimit);
			
			final JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
			buttonPanel.add(_startButton);
			buttonPanel.add(resetButton);
			
			final JPanel optionsPanel = new JPanel(new BorderLayout());
			optionsPanel.add(settingsPanel, BorderLayout.CENTER);
			optionsPanel.add(buttonPanel, BorderLayout.SOUTH);
			
			
			JPanel serverPanel = new JPanel(new BorderLayout());
			serverPanel.add(ipPanel, BorderLayout.NORTH);
			serverPanel.add(_playerPanel, BorderLayout.CENTER);
			serverPanel.add(optionsPanel, BorderLayout.SOUTH);
			
			JLabel titleLabel = new JLabel(" JDixit ");
			titleLabel.setFont(new Font("Harrington", Font.BOLD, 48));
			
			setLayout(new BorderLayout());
			add(titleLabel, BorderLayout.NORTH);
			add(serverPanel, BorderLayout.CENTER);
			pack();
			
			setTitle("JDixit Server");
			setLocationRelativeTo(null);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setResizable(false);
			setVisible(true);
		}
		
		private void getPointLimit() {
			String limitString = (String)JOptionPane.showInputDialog(this, "Input point limit:", "Point Limit", JOptionPane.PLAIN_MESSAGE, null, null, "");
			if(limitString == null) {
				_cardEnd.setSelected(true);
			}
			else {
				try {
					_scoreLimit = Integer.parseInt(limitString);
					_pointLimit.setText("Point limit: " + _scoreLimit);
				}
				catch(NumberFormatException e) {
					JOptionPane.showMessageDialog(this, "Unrecognized input\nPlease input a positive integer");
					_cardEnd.setSelected(true);
				}
			}
		}
		
		public void refreshPlayerPanel() {
			_playerPanel.removeAll();
			for(Player p : _playerList) {
				_playerPanel.add(new JLabel(p.getName()));
			}
			
			_startButton.setEnabled(_playerList.size() >= MIN_PLAYERS);
			
			if(_playerList.size() == 0) {
				_playerPanel.add(new JLabel("<None>"));
			}
			
			_playerPanel.revalidate();
			pack();
		}
		
	}
	
	public static void main(String[] args) {
		new DixitServer();
	}

}

//TODO: Close socket on exit
