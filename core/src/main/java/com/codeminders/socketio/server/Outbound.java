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

import com.codeminders.socketio.common.SocketIOException;

public interface Outbound
{
    /**
     * Terminate the connection. This method may return before the connection disconnect
     * completes. The onDisconnect() method of the associated SocketInbound will be called
     * when the disconnect is completed.
     * This method will try to notify the remote end.
     */
    void disconnect();

    //TODO: do we need an abort() or kill() method for user to call in case of IO error?
    //TODO: should we try to send anything (like DISCONNECT packet) in this case?
    //TODO: no onDisconnect to be called if such method is used by the user.

    /**
     * Emits an event to the socket identified by the string name.
     *
     * @param name event name
     * @param args list of arguments. Arguments can contain any type of field that can result of JSON decoding,
     *             including objects and arrays of arbitrary size.
     * @throws IllegalStateException if the socket is not CONNECTED.
     * @throws SocketIOException
     */

    void emit(String name, Object... args) throws SocketIOException;

}
