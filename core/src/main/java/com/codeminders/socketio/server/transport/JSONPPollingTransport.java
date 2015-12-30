/**
 * The MIT License
 * Copyright (c) 2015
 *
 * Contributors: Tad Glines, Ovea.com, Mycila.com
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
 *
 * @author Alexander Sova <bird@codeminders.com>
 */
package com.codeminders.socketio.server.transport;

import com.codeminders.socketio.server.SocketIOFrame;
import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.server.SocketIOSession;
import com.codeminders.socketio.server.TransportType;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public abstract class JSONPPollingTransport extends AbstractHttpTransport
{
    private static final String EIO_PREFIX = "___eio";
    private static final String FRAME_ID   = JSONPPollingTransport.class.getName() + ".FRAME_ID";

    protected JSONPPollingTransport() { }

    @Override
    public TransportType getType()
    {
        return TransportType.JSONP_POLLING;
    }

    @Override
    public void startSend(SocketIOSession session, ServletResponse response) throws IOException
    {
        response.setContentType("text/javascript; charset=UTF-8");
    }

    @Override
    public void writeData(SocketIOSession session, ServletResponse response, String data) throws IOException
    {
        //TODO: encode data?
        response.getOutputStream().print(EIO_PREFIX);
        response.getOutputStream().print("[" + session.getAttribute(FRAME_ID) + "]('");
        response.getOutputStream().print(data);
        response.getOutputStream().print("');");
    }

    @Override
    public void finishSend(SocketIOSession session, ServletResponse response) throws IOException
    {
        response.flushBuffer();
    }

    @Override
    public void onConnect(SocketIOSession session, ServletRequest request, ServletResponse response)
            throws IOException, SocketIOProtocolException
    {
        try {
            //TODO: Use string constant for request parameter name "j"
            //TODO: Do we really need to enforce "j" to be an integer?
            session.setAttribute(FRAME_ID, Integer.parseInt(request.getParameter("j")));
        } catch (NullPointerException | NumberFormatException e) {
            throw new SocketIOProtocolException("Missing or invalid 'j' parameter. It suppose to be integer");
        }

        startSend(session, response);

        //TODO: check it!
        writeData(session, response, SocketIOFrame.encode(SocketIOFrame.FrameType.CONNECT, session.getSessionId()));

    }
}
