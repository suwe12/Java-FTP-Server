package utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 管理FTP数据连接的类
 */
public class FTPDataConnection {
	private final FTPCommandHandler commandHandler;

	public FTPDataConnection(FTPCommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	public void sendDirectoryListing(Path directory) throws IOException {
		try ( Socket dataSocket = new Socket();
			  OutputStream dataOut = connectDataSocket(dataSocket) ) {

			try ( PrintWriter dataWriter = new PrintWriter(dataOut, true) ) {
				Files.list(directory)
						.map(path -> formatFileEntry(path))
						.forEach(dataWriter::println);
			}
		}
	}

	public void sendFile(Path filePath) throws IOException {
		try ( Socket dataSocket = new Socket() ) {
			InetSocketAddress dataAddr = commandHandler.getDataAddress();
			System.out.println("尝试连接客户端数据端口: " + dataAddr);

			// 设置更短的超时时间，便于调试

			dataSocket.connect(dataAddr, 30000); // 3秒超时
			System.out.println("数据连接已建立，开始传输文件: " + filePath.getFileName());

			try ( OutputStream dataOut = dataSocket.getOutputStream() ) {
				Files.copy(filePath, dataOut);
			}
			System.out.println("文件传输完成");
		} catch ( SocketTimeoutException e ) {
			System.err.println("连接超时: " + e.getMessage());
			throw new IOException("425 Can't open data connection - 连接超时");
		} catch ( IOException e ) {
			System.err.println("数据连接错误: " + e.getMessage());
			throw new IOException("425 Can't open data connection - " + e.getMessage());
		}
	}

	private OutputStream connectDataSocket(Socket socket) throws IOException {
		socket.connect(commandHandler.getDataAddress(), 5000); // 5秒超时
		return socket.getOutputStream();
	}

	//格式化路径
	private String formatFileEntry(Path path) {
		try {
			String permissions = Files.isDirectory(path) ? "d" : "-";
			permissions += Files.isReadable(path) ? "r" : "-";
			permissions += Files.isWritable(path) ? "w" : "-";
			permissions += Files.isExecutable(path) ? "x" : "-";
			return String.format("%s 1 owner group %10d %s %s",
					permissions,
					Files.size(path),
					java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(Files.getLastModifiedTime(path).toInstant()),
					path.getFileName());
		} catch ( IOException e ) {
			return path.getFileName() + " (error getting details)";
		}
	}
}