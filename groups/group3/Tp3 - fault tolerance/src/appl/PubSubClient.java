package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import core.Message;
import core.MessageImpl;
import core.Server;
import core.client.Client;

public class PubSubClient {

	private Server observer;
	private ThreadWrapper clientThread;

	private String clientAddress;
	private int clientPort;

	private String backupAddress;
	private int backupPort;

	private String primaryAddress;
	private int primaryPort;

	private boolean primaryUp;

	public PubSubClient() {
		// this constructor must be called only when the method
		// startConsole is used
		// otherwise the other constructor must be called
	}

	public boolean isPrimaryUp() {
		return primaryUp;
	}

	public void setPrimaryUp(boolean primaryUp) {
		this.primaryUp = primaryUp;
	}

	public int getPrimaryPort() {
		return primaryPort;
	}

	public void setPrimaryPort(int primaryPort) {
		this.primaryPort = primaryPort;
	}

	public String getPrimaryAddress() {
		return primaryAddress;
	}

	public void setPrimaryAddress(String primaryAddress) {
		this.primaryAddress = primaryAddress;
	}

	public int getBackupPort() {
		return backupPort;
	}

	public void setBackupPort(int backupPort) {
		this.backupPort = backupPort;
	}

	public String getBackupAddress() {
		return backupAddress;
	}

	public void setBackupAddress(String backupAddress) {
		this.backupAddress = backupAddress;
	}

	public PubSubClient(String clientAddress, int clientPort) {
		this.clientAddress = clientAddress;
		this.clientPort = clientPort;
		observer = new Server(clientPort);
		clientThread = new ThreadWrapper(observer);
		clientThread.start();
	}

	public void subscribe(String brokerAddress, int brokerPort) {

		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("sub");
		msgBroker.setContent(clientAddress + ":" + clientPort);
		try {
			Client subscriber = new Client(brokerAddress, brokerPort);
			System.out.println("SUB " + subscriber.sendReceive(msgBroker).getContent());
		} catch (Exception error) {
			System.out.println("Error in subscribe");
		}
		try{
			Message msgAddrs = new MessageImpl();
			Client subscriber = new Client(brokerAddress, brokerPort);
			msgAddrs.setBrokerId(brokerPort);
			msgAddrs.setType("syncAddrs");
			msgAddrs.setContent("Give me address");
			//Envio da mensagem para receber o endereço do backup
			Message responseAddrs = subscriber.sendReceive(msgAddrs);
			setBackupAddress(responseAddrs.getContent());
			setBackupPort(responseAddrs.getBrokerId());
			setPrimaryAddress(brokerAddress);
			setPrimaryPort(brokerPort);
			setPrimaryUp(true);
		}
		catch (Exception error) {}

	}

	public void unsubscribe(String brokerAddress, int brokerPort) {

		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("unsub");
		msgBroker.setContent(clientAddress + ":" + clientPort);
		try {
			Client subscriber = new Client(brokerAddress, brokerPort);
			subscriber.sendReceive(msgBroker);
		} catch (Exception error) {
			System.out.println("Error in unsubscribe");
		}
	}

	public void publish(String message, String brokerAddress, int brokerPort) {
		Message msgPub = new MessageImpl();
		msgPub.setBrokerId(brokerPort);
		msgPub.setType("pub");
		msgPub.setContent(message);
		try {
			Client publisher = new Client(this.primaryAddress,  this.primaryPort);
			publisher.sendReceive(msgPub);
		}

		catch (Exception error) {
			try {
				setPrimaryAddress(this.backupAddress);
				setPrimaryPort(this.backupPort);
				System.out.println("\nPrimary lost connection\n");
				Message msgNewPrimary = new MessageImpl();
				msgNewPrimary.setBrokerId(backupPort);
				msgNewPrimary.setType("setPrimary");
				Client publisherBrokerLost = new Client(this.primaryAddress, this.primaryPort);
				publisherBrokerLost.sendReceive(msgNewPrimary);
				
				Client publisher = new Client(this.primaryAddress, this.primaryPort);
				msgPub.setBrokerId(this.primaryPort);
				msgPub = publisher.sendReceive(msgPub);


			} catch (Exception backupError) {
			}
		}
		// System.out.println(msgPub.getContent() + " - " + msgPub.getLogId());

	}

	public List<Message> getLogMessages() {
		return observer.getLogMessages();
	}

	public void stopPubSubClient() {
		System.out.println("Client stopped...");
		observer.stop();
		clientThread.interrupt();
	}

	public void startConsole() {
		/*
		 * Scanner reader = new Scanner(System.in); // Reading from System.in
		 * System.out.print("Enter the client address (ex. localhost): "); String
		 * clientAddress = reader.next();
		 * System.out.print("Enter the client port (ex.8080): "); int clientPort =
		 * reader.nextInt();
		 * System.out.println("Now you need to inform the broker credentials...");
		 * System.out.print("Enter the broker address (ex. localhost): "); String
		 * brokerAddress = reader.next();
		 * System.out.print("Enter the broker port (ex.8080): "); int brokerPort =
		 * reader.nextInt();
		 * 
		 * observer = new Server(clientPort); clientThread = new
		 * ThreadWrapper(observer); clientThread.start();
		 * 
		 * Message msgBroker = new MessageImpl(); msgBroker.setType("sub");
		 * msgBroker.setBrokerId(brokerPort);
		 * msgBroker.setContent(clientAddress+":"+clientPort); Client subscriber = new
		 * Client(brokerAddress, brokerPort); subscriber.sendReceive(msgBroker);
		 * 
		 * System.out.println("Do you want to subscribe for more brokers? (Y|N)");
		 * String resp = reader.next();
		 * 
		 * if(resp.equals("Y")||resp.equals("y")){ String message = ""; Message msgSub =
		 * new MessageImpl(); msgSub.setType("sub");
		 * msgSub.setContent(clientAddress+":"+clientPort);
		 * while(!message.equals("exit")){
		 * System.out.println("You must inform the broker credentials...");
		 * System.out.print("Enter the broker address (ex. localhost): "); brokerAddress
		 * = reader.next(); System.out.print("Enter the broker port (ex.8080): ");
		 * brokerPort = reader.nextInt(); subscriber = new Client(brokerAddress,
		 * brokerPort); msgSub.setBrokerId(brokerPort); subscriber.sendReceive(msgSub);
		 * System.out.println(" Write exit to finish..."); message = reader.next(); } }
		 * 
		 * System.out.println("Do you want to publish messages? (Y|N)"); resp =
		 * reader.next(); if(resp.equals("Y")||resp.equals("y")){ String message = "";
		 * Message msgPub = new MessageImpl(); msgPub.setType("pub");
		 * while(!message.equals("exit")){
		 * System.out.println("Enter a message (exit to finish submissions): "); message
		 * = reader.next(); msgPub.setContent(message);
		 * 
		 * System.out.println("You must inform the broker credentials...");
		 * System.out.print("Enter the broker address (ex. localhost): "); brokerAddress
		 * = reader.next(); System.out.print("Enter the broker port (ex.8080): ");
		 * brokerPort = reader.nextInt();
		 * 
		 * msgPub.setBrokerId(brokerPort); Client publisher = new Client(brokerAddress,
		 * brokerPort); publisher.sendReceive(msgPub);
		 * 
		 * List<Message> log = observer.getLogMessages();
		 * 
		 * Iterator<Message> it = log.iterator(); System.out.print("Log itens: ");
		 * while(it.hasNext()){ Message aux = it.next();
		 * System.out.print(aux.getContent() + aux.getLogId() + " | "); }
		 * System.out.println();
		 * 
		 * } }
		 * 
		 * System.out.print("Shutdown the client (Y|N)?: "); resp = reader.next(); if
		 * (resp.equals("Y") || resp.equals("y")){
		 * System.out.println("Client stopped..."); observer.stop();
		 * clientThread.interrupt();
		 * 
		 * }
		 * 
		 * //once finished reader.close();
		 */
	}

	class ThreadWrapper extends Thread {
		Server s;

		public ThreadWrapper(Server s) {
			this.s = s;
		}

		public void run() {
			s.begin();
		}
	}

}
