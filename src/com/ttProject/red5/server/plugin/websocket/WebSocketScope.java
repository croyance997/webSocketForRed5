package com.ttProject.red5.server.plugin.websocket;

import java.util.HashSet;
import java.util.Set;

public class WebSocketScope {
	private String path;
	private Set<WebSocketConnection> conns = new HashSet<WebSocketConnection>();
	private Set<IWebSocketDataListener> listeners = new HashSet<IWebSocketDataListener>();
	public WebSocketScope(String path) {
		this.path = path; // /room/name�̌`�ł���B
	}
	// �N�����ɂ킩��͂�������Ascope�ɓo�^����Ă��Ȃ��Bapplication�ɑ΂��Ă͉������Ȃ��悤�ɂ��Ă����B
	public String getPath() {
		return path;
	}
	public void addConnection(WebSocketConnection conn) {
		conns.add(conn);
	}
	public void removeConnection(WebSocketConnection conn) {
		conns.remove(conn);
		// �R�l�N�V�������������Ƃ��ɁA���܂܂�scope�ɐݒu���Ă���conn�������K�v������B
	}
	public void addListener(IWebSocketDataListener listener) {
		listeners.add(listener);
	}
	public void removeListener(IWebSocketDataListener listener) {
		listeners.remove(listener);
	}
	public boolean isValid() {
		return (conns.size() + listeners.size()) > 0;
	}
}
