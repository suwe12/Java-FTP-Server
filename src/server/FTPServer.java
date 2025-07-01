package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FTP服务器主类，负责监听客户端连接并分发处理
 */
public class FTPServer {
	private static final int DEFAULT_PORT = 21;
	private final int port;
	private final ExecutorService threadPool;
	private boolean running;

	public FTPServer(int port) {
		this.port = port;
		this.threadPool = Executors.newCachedThreadPool();
		this.running = false;
	}

	public void start() {

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("FTP Server started on port " + port);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("New client.client connected: " + clientSocket.getInetAddress());

				// 为每个客户端创建一个处理器并提交到线程池
				ClientHandler clientHandler = new ClientHandler(clientSocket);
				threadPool.submit(clientHandler);
			}
		} catch (IOException e) {
			if (running) {
				System.err.println("Server error: " + e.getMessage());
			}
		} finally {
			threadPool.shutdown();
			System.out.println("Server stopped");
		}
	}

	public void stop() {
		running = false;
	}

	public static void main(String[] args) {
		FTPServer server = new FTPServer(DEFAULT_PORT);
		server.start();
	}
}