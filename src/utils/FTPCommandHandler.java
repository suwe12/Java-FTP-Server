package utils;

import server.ClientHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * 处理FTP命令的逻辑类
 * 解析命令
 */
public class FTPCommandHandler {
	private final ClientHandler clientHandler;
	private String currentUser;
	private boolean authenticated;
	private Path currentDirectory;
	private InetSocketAddress dataAddress;
	private final FTPDataConnection dataConnection;

	public FTPCommandHandler(ClientHandler clientHandler) {
		this.clientHandler = clientHandler;
		this.currentUser = null;
		this.authenticated = false;
		this.currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		this.dataConnection = new FTPDataConnection(this);
	}

	public void handleCommand(String command, String argument) {
		// 将GET命令映射到RETR处理逻辑
		if ( "GET".equalsIgnoreCase(command) ) {
			command = "RETR";
		}

		switch ( command ) {
			case "USER":
				handleUser(argument);
				break;
			case "PASS":
				handlePass(argument);
				break;
			case "SYST":
				clientHandler.sendResponse("215 UNIX Type: L8");
				break;
			case "TYPE":
				handleType(argument);
				break;
			case "CWD":
				handleCwd(argument);
				break;
			case "PWD":
				clientHandler.sendResponse("257 \"" + currentDirectory + "\" is current directory.");
				break;
			case "PORT":
				handlePort(argument);
				break;
			case "LIST":
				handleList(argument);
				break;
			case "RETR":
				handleRetr(argument);
				break;
			case "QUIT":
				clientHandler.sendResponse("221 Goodbye");
				break;
			default:
				clientHandler.sendResponse("502 Command not implemented");
		}
	}

	private void handleUser(String username) {
		this.currentUser = username;
		clientHandler.sendResponse("331 User name okay, need password.");
	}

	private void handlePass(String password) {
		if ( currentUser != null && "password".equals(password) ) {
			authenticated = true;
			clientHandler.sendResponse("230 User logged in, proceed.");
		} else {
			clientHandler.sendResponse("530 Not logged in.");
		}
	}

	private void handleType(String type) {
		if ( "A".equalsIgnoreCase(type) || "I".equalsIgnoreCase(type) ) {
			clientHandler.sendResponse("200 Type set to " + type);
		} else {
			clientHandler.sendResponse("504 Command not implemented for that parameter");
		}
	}

	private void handleCwd(String directory) {
		if ( !authenticated ) {
			clientHandler.sendResponse("530 Not logged in.");
			return;
		}

		Path newDirectory;
		if ( directory.startsWith("/") ) {
			newDirectory = Paths.get(directory);
		} else {
			newDirectory = currentDirectory.resolve(directory);
		}

		if ( Files.exists(newDirectory) && Files.isDirectory(newDirectory) ) {
			try {
				currentDirectory = newDirectory.toRealPath();
				clientHandler.sendResponse("250 Directory changed to " + currentDirectory);
			} catch ( IOException e ) {
				clientHandler.sendResponse("550 Failed to change directory.");
			}
		} else {
			clientHandler.sendResponse("550 Directory not found.");
		}
	}

	private void handlePort(String argument) {
		if ( !authenticated ) {
			clientHandler.sendResponse("530 Not logged in.");
			return;
		}

		try {
			// 解析PORT命令参数 (h1,h2,h3,h4,p1,p2)
			String[] parts = argument.split(",");
			if ( parts.length != 6 ) {
				clientHandler.sendResponse("501 Syntax error in parameters or arguments.");
				return;
			}

			// 构建IP地址
			String ip = String.join(".", Arrays.copyOfRange(parts, 0, 4));

			// 计算端口号
			int p1 = Integer.parseInt(parts[4]);
			int p2 = Integer.parseInt(parts[5]);
			int port = (p1 * 256) + p2;

			// 记录客户端数据地址
			this.dataAddress = new InetSocketAddress(ip, port);
			System.out.println("PORT命令解析成功 - IP: " + ip + ", 端口: " + port);
			clientHandler.sendResponse("200 PORT command successful.");
		} catch ( NumberFormatException e ) {
			clientHandler.sendResponse("501 Syntax error in parameters or arguments.");
		}
	}

	private void handleList(String argument) {
		if ( !authenticated ) {
			clientHandler.sendResponse("530 Not logged in.");
			return;
		}

		if ( dataAddress == null ) {
			clientHandler.sendResponse("425 Use PORT first.");
			return;
		}

		try {
			clientHandler.sendResponse("150 Here comes the directory listing.");
			//发送文件列表
			dataConnection.sendDirectoryListing(currentDirectory);
			clientHandler.sendResponse("226 Directory send OK.");
		} catch ( IOException e ) {
			clientHandler.sendResponse("425 Can't open data connection.");
			System.err.println("Data connection error: " + e.getMessage());
		} finally {
			dataAddress = null; // 重置数据地址
		}
	}

	private void handleRetr(String filename) {
		if ( !authenticated ) {
			clientHandler.sendResponse("530 Not logged in.");
			return;
		}

		if ( dataAddress == null ) {
			clientHandler.sendResponse("425 Use PORT first.");
			return;
		}

		Path filePath = currentDirectory.resolve(filename);

		if ( !Files.exists(filePath) || Files.isDirectory(filePath) ) {
			clientHandler.sendResponse("550 File not found or access denied.");
			return;
		}

		try {
			clientHandler.sendResponse("150 Opening ASCII mode data connection for " + filename);
			//发送文件
			dataConnection.sendFile(filePath);
			clientHandler.sendResponse("226 Transfer complete.");
		} catch ( IOException e ) {
			clientHandler.sendResponse("425 Can't open data connection.");
			System.err.println("Data connection error: " + e.getMessage());
		} finally {
			dataAddress = null; // 重置数据地址
		}
	}

	public InetSocketAddress getDataAddress() {
		return dataAddress;
	}

	public ClientHandler getClientHandler() {
		return clientHandler;
	}
}