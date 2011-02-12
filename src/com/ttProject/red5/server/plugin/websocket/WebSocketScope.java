package com.ttProject.red5.server.plugin.websocket;

import java.util.Set;

public class WebSocketScope {
	private String application;
	private String path;
	private Set<WebSocketConnection> conns;
	public WebSocketScope(String application, String path) {
		this.application = application;
		this.path = path; // /room/name�̌`�ł���B
	}
	// �N�����ɂ킩��͂�������Ascope�ɓo�^����Ă��Ȃ��Bapplication�ɑ΂��Ă͉������Ȃ��悤�ɂ��Ă����B
	public String getPath() {
		return application + path;
	}
	public void addConnection(WebSocketConnection conn) {
		conns.add(conn);
	}
	public void removeConnection(WebSocketConnection conn) {
		conns.remove(conn);
		// �R�l�N�V�������������Ƃ��ɁA���܂܂�scope�ɐݒu���Ă���conn�������K�v������B
	}
}
