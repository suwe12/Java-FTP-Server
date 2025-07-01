package utils;

/**
 * 封装FTP响应码和消息的工具类
 */
class FTPResponse {
	public static final String READY = "220 Welcome to Java FTP Server";
	public static final String LOGGED_IN = "230 User logged in, proceed.";
	public static final String NEED_PASSWORD = "331 User name okay, need password.";
	public static final String NOT_LOGGED_IN = "530 Not logged in.";
	public static final String COMMAND_OK = "200 Command okay.";
	public static final String PORT_SUCCESS = "200 PORT command successful.";
	public static final String DATA_CONNECTION_OPEN = "150 File status okay; about to open data connection.";
	public static final String TRANSFER_COMPLETE = "226 Closing data connection. Requested file action successful.";
	public static final String CANT_OPEN_DATA_CONNECTION = "425 Can't open data connection.";
	public static final String FILE_NOT_FOUND = "550 File not found or access denied.";
	public static final String SYNTAX_ERROR = "500 Syntax error, command unrecognized.";
	public static final String COMMAND_NOT_IMPLEMENTED = "502 Command not implemented.";

	// 可以添加更多响应码...
}