package com.ttProject.red5.server.plugin.websocket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * WebSocketHandshake
 * <pre>
 * this class for handshake process.
 * </pre>
 */
public class WebSocketHandshake {
	private WebSocketConnection conn;
	private String key1;
	private String key2;
	private String origin;
	private String host;
	private String path;
	private byte[] key3;
	/**
	 * constructor with connection object.
	 * @param conn connection object.
	 */
	public WebSocketHandshake(WebSocketConnection conn) {
		this.conn = conn;
	}
	/**
	 * handShake
	 * <pre>
	 * analyze handshake input from client.
	 * </pre>
	 * @param buffer ioBuffer
	 */
	public void handShake(IoBuffer buffer) {
		byte[] b = new byte[buffer.capacity()];
		String data;
		int i = 0;
		for(byte bi:buffer.array()) {
			if(bi == 0x0D || bi == 0x0A) {
				if(b.length != 0) {
					data = (new String(b)).trim();
					if(data.contains("GET ")) {
						// get the path data for handShake
						String[] ary = data.split("GET ");
						ary = ary[1].split(" HTTP/1.1");
						path = ary[0];
						conn.setPath(ary[0]);
						ary = ary[0].split("/");
						WebSocketScopeManager manager = new WebSocketScopeManager();
						if(ary.length <= 1 || !manager.isPluginedApplication(ary[1])) {
							// scope is not application. handshake will be false;
							// send disconnect message.
							IoBuffer buf = IoBuffer.allocate(4);
							buf.put(new byte[]{(byte)0xFF, (byte)0x00});
							buf.flip();
							conn.send(buf);
							// close connection.
							conn.close();
							return;
						}
					}
					else if(data.contains("Sec-WebSocket-Key1")) {
						// get the key1 data
						key1 = data;
					}
					else if(data.contains("Sec-WebSocket-Key2")) {
						// get the key2 data
						key2 = data;
					}
					else if(data.contains("Host")) {
						// get the host data
						String[] ary = data.split("Host: ");
						host = ary[1];
						conn.setHost(ary[1]);
					}
					else if(data.contains("Origin")) {
						// get the origin data
						String[] ary = data.split("Origin: ");
						origin = ary[1];
						conn.setOrigin(ary[1]);
					}
					// for the information print out the string data.
					if(data.length() > 4) {
						System.out.println(data);
					}
				}
				i = 0;
				b = new byte[buffer.capacity()];
			}
			else {
				b[i] = bi;
				i ++;
			}
		}
		key3 = b;
		// start the handshake reply
		doHandShake();
	}
	/**
	 * start the handshake reply
	 * @param key3
	 */
	private void doHandShake() {
		if(key3 == null) {
			System.out.println("last byte is incollect");
			return;
		}
		if(key1 == null || key2 == null) {
			System.out.println("key data is missing");
			return;
		}
		// calicurate first 16 byte of integer data;
		byte[] b = new byte[16];
		int buf1 = getKeyInteger(key1);
		int buf2 = getKeyInteger(key2);
		byte[] result;
		try {
			b[0] = (byte)((buf1 & 0xFF000000) >> 24);
			b[1] = (byte)((buf1 & 0x00FF0000) >> 16);
			b[2] = (byte)((buf1 & 0x0000FF00) >> 8);
			b[3] = (byte)((buf1 & 0x000000FF));
			b[4] = (byte)((buf2 & 0xFF000000) >> 24);
			b[5] = (byte)((buf2 & 0x00FF0000) >> 16);
			b[6] = (byte)((buf2 & 0x0000FF00) >> 8);
			b[7] = (byte)((buf2 & 0x000000FF));
			b[8]  = key3[0];
			b[9]  = key3[1];
			b[10] = key3[2];
			b[11] = key3[3];
			b[12] = key3[4];
			b[13] = key3[5];
			b[14] = key3[6];
			b[15] = key3[7];
			// make MD5 byte data.
			result = crypt(b);
		}
		catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}
		catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return;
		}
		// make up reply data...
		IoBuffer buf = IoBuffer.allocate(2048);
		byte[] bb = {0x0D, 0x0A};
		buf.put("HTTP/1.1 101 WebSocket Protocol Handshake".getBytes());
		buf.put(bb);
		buf.put("Upgrade: WebSocket".getBytes());
		buf.put(bb);
		buf.put(("Sec-WebSocket-Origin: " + origin).getBytes());
		buf.put(bb);
		buf.put(("Sec-WebSocket-Location: " + "ws://" + host + path).getBytes());
		buf.put(bb);
		buf.put("Sec-WebSocket-Protocol: sample".getBytes());
		buf.put(bb);
		buf.put(bb);
		buf.put(result);
		buf.flip();
		// write the data on session
		conn.getSession().write(buf);
		// handshake is finished.
		conn.setConnected();
		// scope�������Ă����B
		WebSocketScopeManager manager = new WebSocketScopeManager();
		manager.addConnection(conn);
		System.out.println("HandShake complete" + conn.getSession().getId());
	}
	/**
	 * calicurate integer data.
	 * @param key input key string data.
	 * @return integer data after calicurate
	 */
	private Integer getKeyInteger(String key) {
		StringBuffer numList = new StringBuffer();
		int spaceCount = 0;
		for(int i=20;i < key.length(); i++) {
			char c = key.charAt(i);
			if(c >= 0x30 && c < 0x3A) {
				// this is number data.
				numList.append(c);
			}
			else if(c == ' ') {
				// this is space data.
				spaceCount ++;
			}
		}
		return (int)(new Long(numList.toString()) / spaceCount);
	}
	/**
	 * make md5 data.
	 * @param bytes input bytes
	 * @return crypted bytes data
	 * @throws NoSuchAlgorithmException
	 */
	private byte[] crypt(byte[] bytes) throws NoSuchAlgorithmException {
		if(bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("bytes for encrypt must have body");
		}
		MessageDigest md = MessageDigest.getInstance("MD5");
		return md.digest(bytes);
	}
}
