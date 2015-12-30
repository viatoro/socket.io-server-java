/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 *
 * Contributors: Ovea.com, Mycila.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.server;

import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;

public interface SocketIOSession
{
    void setAttribute(String key, Object val);
    Object getAttribute(String key);

    //TODO: do we need it in Session interface
    interface SessionTask
    {
		/**
		 * @return True if task was or was already canceled, false if the task is executing or has executed.
		 */
		boolean cancel();
	}
    SessionTask scheduleTask(Runnable task, long delay);


	String getSessionId();

	ConnectionState getConnectionState();
	
	SocketIOInbound getInbound();
    TransportConnection getConnection(); //Outbound connection

	void setTimeout(long timeout);
	long getTimeout();

	void startTimeoutTimer();
	void clearTimeoutTimer();

	/**
	 * Initiate close.
	 */
	void startClose();

    void onPacket(EngineIOPacket packet);
    void onPacket(SocketIOPacket packet);

	void onPing(String data);

    void onClose(String data);
	void onEvent(String name, Object[] args);

	/**
	 * @param connection The connection or null if the connection failed.
	 */
	void onConnect(TransportConnection connection);
	void onDisconnect(DisconnectReason reason);

	/**
	 * Called by handler to report that it is done and the session can be cleaned up.
	 * If onDisconnect has not been called yet, then it will be called with DisconnectReason.ERROR.
	 */
	void onShutdown();
}
