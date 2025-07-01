package server;

import utils.FTPCommandHandler;

import java.io.*;
import java.net.Socket;

/**
 * 处理单个客户端连接的处理器
 */
public class ClientHandler implements Runnable {
	private final Socket clientSocket;
	private PrintWriter controlOut;
	private BufferedReader controlIn;
	private final FTPCommandHandler commandHandler;

	public ClientHandler(Socket socket) {
		this.clientSocket = socket;
		this.commandHandler = new FTPCommandHandler(this);
	}

	@Override
	public void run() {
		try (
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
		) {
			this.controlOut = out;
			this.controlIn = in;

			sendResponse("220 Welcome to Java FTP Server");

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println("Client: " + inputLine);
				if (inputLine.isEmpty()) {
					continue;
				}

				String[] tokens = inputLine.split(" ", 2);
				String command = tokens[0].toUpperCase();
				String argument = tokens.length > 1 ? tokens[1] : "";
				System.out.println("-----"+clientSocket.getInetAddress().toString()+clientSocket.getLocalPort());

				try {
					commandHandler.handleCommand(command, argument);
				} catch (Exception e) {
					sendResponse("500 Syntax error, command unrecognized");
					System.err.println("Error handling command: " + e.getMessage());
					e.printStackTrace();
				}

				if ("QUIT".equals(command)) {
					break;
				}
			}
		} catch (IOException e) {
			System.err.println("Error handling client.client: " + e.getMessage());
		} finally {
			try {
				clientSocket.close();
				System.out.println("Client disconnected: " + clientSocket.getInetAddress());
			} catch (IOException e) {
				System.err.println("Error closing client.client socket: " + e.getMessage());
			}
		}
	}

	public void sendResponse(String response) {
		System.out.println("Server: " + response);
		controlOut.println(response);
	}

	public Socket getClientSocket() {
		return clientSocket;
	}
}