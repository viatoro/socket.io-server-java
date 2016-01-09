/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
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

import com.codeminders.socketio.common.DisconnectReason;

/**
 * Interface that represents a listener for inbound events
 * A library user is expected to implement this interface in order to receive such events
 */
public interface Inbound
{

    /**
     * Called when the connection is established. This will only ever be called once.
     *
     * @param outbound The SocketOutbound associated with the connection
     */
    void onConnect(Outbound outbound);

    /**
     * Called when the socket connection is closed. This will only ever be called once.
     * This method may be called instead of onConnect() if the connection handshake isn't
     * completed successfully.
     *
     * @param reason       The reason for the disconnect.
     * @param errorMessage Possibly non null error message associated with the reason for disconnect.
     */
    void onDisconnect(DisconnectReason reason, String errorMessage);

    //TODO: add boolean param to indicate that ACK is requested by the client
    /**
     * Called one per arriving event.
     *
     * @param name event name
     * @param args event payload
     * @return JSON object to send as ACK if client requested ACK
     */
    Object onEvent(String name, Object[] args);

}
