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
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import message.ChatLog;
import message.Message;
import message.Message.Status;
import message.Message.Type;

public class DixitServer {
	private static final int PORT = 34948;
	private static final int MIN_PLAYERS = 3;
//	private static final int MAX_PLAYERS = 8;
	
	private InfoWindow _infoWindow;
	
	private ClientHandler _clientHandler;
	
	private ServerSocket _serverSocket;
	
	private HashSet<Player> _playerSet;
	private ArrayList<Player> _playerList;
	
	private ChatLog _chatLog;
	
	private Message.Status _status;
	private Message _gameState;
	
	public DixitServer() {
		_playerSet = new HashSet<Player>();
		_playerList = new ArrayList<Player>();
		_chatLog = new ChatLog();
		_gameState = new Message(Type.UPDATE);

		setStatus(Status.LOBBY);
		_gameState.setChatLog(_chatLog);
		
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
	
	private void setStatus(Status status) {
		_status = status;
		_gameState.setStatus(status);
		_gameState.setChange();
	}
	
	private synchronized boolean registerPlayer(String name, ClientConnection connection) {
		final Player player = new Player(name);
		if(_playerSet.contains(player)) {
			return false;
		}
		_playerSet.add(player);
		_playerList.add(player);
		_infoWindow.refreshPlayerPanel();
		System.out.println("Player " + name + " has joined the game");
		return true;
	}
	
	private void startGame() {
		setStatus(Status.AWAITING_STORY);
		System.out.println("Starting game");
	}
	
	private void addChat(Message message) {
		_chatLog.addChat(message.getPlayer(), message.getMessage());
		_gameState.setChange();
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
					if(_status != Status.LOBBY) {
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
			if(_playerSet.size() >= MIN_PLAYERS) {
				_startButton.setEnabled(true);
			}
		}
		
	}
	
	public static void main(String[] args) {
		new DixitServer();
	}

}

//TODO: Close socket on exit
