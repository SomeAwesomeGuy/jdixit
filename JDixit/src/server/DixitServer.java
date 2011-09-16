package server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import message.ChatLog;
import message.Message;
import message.Player;
import message.Message.Status;
import message.Message.Type;

public class DixitServer {
	private static final int PORT = 34948;
	private static final int MIN_PLAYERS = 3;
//	private static final int MAX_PLAYERS = 8;
	
	private InfoWindow _infoWindow;
	
	private ClientHandler _clientHandler;
	
	private ServerSocket _serverSocket;
	
	private HashMap<String, Player> _playerMap;
	private ArrayList<Player> _playerList;
	private int _playersLeft;
	
	private Message _gameState;
	
	private int _storyTeller;
	private int _storyCard;
	
	public DixitServer() {
		_playerMap = new HashMap<String, Player>();
		_playerList = new ArrayList<Player>();
		_gameState = new Message(Type.UPDATE);
		_storyTeller = 0;
		_storyCard = -1;

		_gameState.setStatus(Status.LOBBY);
		_gameState.setChatLog(new ChatLog());
		
		try {
			_infoWindow = new InfoWindow(InetAddress.getLocalHost().getHostAddress());
			_serverSocket = new ServerSocket(PORT);
			
			System.out.println("Server started, listening on port " + PORT);
			_clientHandler = new ClientHandler();
			_clientHandler.start();
		} 
		catch (IOException e) {
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
	
	private void transferPlayers() {
		ArrayList<Player> players = new ArrayList<Player>();
		for(Player p : _playerList) {
			players.add((Player)p.clone());
		}
		_gameState.setPlayers(players);
	}
	
	private void startGame() { //TODO: may not need this
		
		startTurn();
	}
	
	private void startTurn() {
		transferPlayers();
		_gameState.setStatus(Status.AWAITING_STORY);
		_gameState.setPlayer(_playerList.get(_storyTeller).getName());
		_gameState.setMessage(null);
		_gameState.setCard(-1);
		_gameState.setChange();
		_playersLeft = _playerList.size();
		for(Player p : _playerList) {
			p.turnReset();
		}
	}
	
	private void addChat(Message message) {
		_gameState.getChatLog().addChat(message.getPlayer(), message.getMessage());
		_gameState.setChange();
	}
	
	private void setStory(Message message) {
		_gameState.setMessage(message.getMessage());
		_gameState.setStatus(Status.CARD_SUBMISSION);
		_storyCard = message.getCard();
		_gameState.setChange();
	}
	
	private void acceptSubmission(Message message) {
		final Player player = _playerMap.get(message.getPlayer());
		if(player.getSubmittedCard() == -1) {
			_playersLeft--;
		}
		player.setSubmittedCard(message.getCard());
		
		if(_playersLeft == 0) {
			_gameState.setStatus(Status.CARD_VOTE);
			_gameState.setChange();
			_playersLeft = _playerMap.size();
			//TODO: gather submitted cards
		}
	}
	
	private boolean acceptVote(Message message) {
		final Player player = _playerMap.get(message.getPlayer());
		if(message.getCard() == player.getSubmittedCard()) {
			return false;
		}
		if(player.getVotedCard() == -1) {
			_playersLeft--;
		}
		player.setVotedCard(message.getCard());
		
		if(_playersLeft == 0) {
			_storyTeller++;
			_storyTeller %= _playerMap.size(); //wrap around
			score();
			startTurn();
		}
		return true;
	}
	
	private void score() {
		HashMap<Integer, Player> cardMap = new HashMap<Integer, Player>();
		for(Player p : _playerList) {
			cardMap.put(p.getSubmittedCard(), p);
		}
		
		int storyVoteCount = 0;
		for(int i = 0; i < _playerList.size(); i++) {
			if(i == _storyTeller) {
				continue;
			}
			final Player p = _playerList.get(i);
			
			int votedCard = p.getVotedCard();
			if(votedCard == _storyCard) {
				storyVoteCount++;
			}
		}
		
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
			for(int i = 0; i < _playerList.size(); i++) {
				if(i == _storyTeller) {
					continue;
				}
				final Player p = _playerList.get(i);
				
				int votedCard = p.getVotedCard();
				if(votedCard == _storyCard) {
					p.addScore(3);
					storyVoteCount++;
				}
				else {
					cardMap.get(p.getVotedCard()).addScore(1);
				}
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
					if(_gameState.getStatus() == Status.AWAITING_STORY && message.getPlayer().equals(_playerList.get(_storyTeller))) {
						setStory(message);
					}
				}
				else if(type == Type.SUBMIT) {
					if(_gameState.getStatus() == Status.CARD_SUBMISSION && !message.getPlayer().equals(_playerList.get(_storyTeller))) {
						acceptSubmission(message);
					}
				}
				else if(type == Type.VOTE) {
					if(_gameState.getStatus() == Status.CARD_VOTE && !message.getPlayer().equals(_playerList.get(_storyTeller))) {
						acceptVote(message);
					}
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
