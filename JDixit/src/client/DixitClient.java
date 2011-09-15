package client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import message.Message;
import message.Message.Type;

public class DixitClient implements ChatSender {	
	private static final int PORT = 34948;
	private static final int FRQ = 1000;
	
	private CountDownLatch _latch;
	
	private JFrame _connectDialog;
	private JLabel _connectStatus;
	private JButton _connectButton;
	
	private String _ipAddress, _name;
	
	private GameWindow _game;
	
	private long _latestUpdateMessageID;
	
	public DixitClient() {
		_latestUpdateMessageID = -1;
		
		launchConnectionDialog();
		
		_latch = new CountDownLatch(1);
		try {
			_latch.await();
			_game = new GameWindow(this);
			new Timer().scheduleAtFixedRate(new Updater(), 0, FRQ);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void launchConnectionDialog() {
		_connectStatus = new JLabel("Connect to the server", JLabel.CENTER);
		
		final JLabel ipLabel = new JLabel(" Server IP: ", JLabel.RIGHT);
		final JLabel nameLabel = new JLabel(" Name: ", JLabel.RIGHT);
		final JPanel labelPanel = new JPanel(new GridLayout(0, 1));
		labelPanel.add(ipLabel);
		labelPanel.add(nameLabel);
		
		final JTextField ipField = new JTextField("localhost", 15); //TODO: fix this for release
		final JTextField nameField = new JTextField(15);
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
				_name = nameField.getText();
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
	
	private void launchGame() {
		_latch.countDown();
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
				}
				
				close();
			}
			catch (SocketTimeoutException e) {
				unknownServer();
			}
			catch (UnknownHostException e) {
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
		
		private void sendChat() {
			try {
				connect(500);
				_objectOut.writeObject(_message);
				_objectOut.flush();
				
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
						_latestUpdateMessageID = returnMessage.getMessageID();
						//TODO: do other stuff with Message contents
						_game.updateChat(returnMessage.getChatLog());
					}
					else {
						System.out.println("Error: received unexpected Message type.");
					}
				}
				
				close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
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
					sendChat(); break;
				case UPDATE:
					getUpdate(); break;
			}
		}
	}

	
	public static void main(String[] args) {
		new DixitClient();
	}

}
