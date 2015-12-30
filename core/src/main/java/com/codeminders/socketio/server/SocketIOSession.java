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

//TODO: why we need Session as an interface at all?
//TODO: convert it to a class
public interface SocketIOSession
{
    void setAttribute(String key, Object val);
    Object getAttribute(String key);

    //TODO: do we need it in Session interface?
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

    /**
     *
     * @return outbound connection
     */
    TransportConnection getConnection();

	void setTimeout(long timeout);
	long getTimeout();

	void startTimeoutTimer();
	void clearTimeoutTimer();

	/**
	 * Initiate close.
	 */
    //TODO: rename to close()
	void startClose();


    void onPacket(EngineIOPacket packet);
    void onPacket(SocketIOPacket packet);

	void onPing(String data);

	void onEvent(String name, Object[] args);

	/**
     * Callback to be called by TransportConnection implementation when connection is established
     * TODO: have separate callback for failed connection
     *
	 * @param connection The connection or null if the connection failed.
	 */
	void onConnect(TransportConnection connection);

    /**
     * Callback to be called by TransportConnection implementation in case of disconnect
     *
     * @param reason disconnect reason
     */
	void onDisconnect(DisconnectReason reason);

	/**
	 * Called by TransportConnection to report that it is done and the session can be cleaned up.
	 * If onDisconnect has not been called yet, then it will be called with DisconnectReason.ERROR.
	 */
    //TODO: remove. use onDisconnect instead
	void onShutdown();
}
