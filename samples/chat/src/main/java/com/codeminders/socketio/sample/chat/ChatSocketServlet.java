/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.sample.chat;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.server.*;
import com.codeminders.socketio.server.transport.websocket.WebsocketIOServlet;
import com.google.common.io.ByteStreams;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatSocketServlet extends WebsocketIOServlet {
	private static final String ANNOUNCEMENT = "announcement"; // server to all
																// connected
																// clients
	private static final String CHAT_MESSAGE = "chat message"; // broadcast to
																// room
	private static final String WELCOME = "welcome"; // single event sent by
														// server to specific
														// client
	private static final String FORCE_DISCONNECT = "force disconnect"; // client
																		// requests
																		// server
																		// to
																		// disconnect
	private static final String SERVER_BINARY = "server binary"; // client
																	// requests
																	// server to
																	// send a
																	// binary
	private static final String CLIENT_BINARY = "client binary"; // client sends
																	// binary

	private static final Logger LOGGER = Logger.getLogger(ChatSocketServlet.class.getName());

	private static final long serialVersionUID = 1L;

	 private Map<String, Map<String, Map<String, Object>>> states = new HashMap<>();
	
	 private Map<String, LinkedList<Map<String, Object>>> history = new HashMap<>();

	private Map<String, Object> saveState(String channel,String  uuid,Map<String, Object> state) {
		// since this is per channel, create channel object if doesn't exist
		if (!states.containsKey(channel)) {
			states.put(channel, new HashMap<String, Map<String, Object>>());
		}

		// save state to the channel based on user uuid
		states.get(channel).put(uuid, state);

		// return given state
		return state;
	}

	private LinkedList<Map<String, Object>> saveHistory(String channel,String uuid,Map<String, Object> data){
		
		  // create an array for this channel if it doesn't exist
		  if(!history.containsKey(channel)) {
			  LinkedList<Map<String, Object>> list = new LinkedList<>();
			  history.put(channel,list);
		  }

		  // push the newest uuid and data to the front of the array
		  history.get(channel).addLast(data);

		  // if we have more than 100 messages for this channel, remove the first
		  if(history.get(channel).size() > 100) {
			  history.get(channel).pop();
		  }

		  // return the entire history
		  return history.get(channel);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		of("/").on(new ConnectionListener() {
			@Override
			public void onConnect(final Socket socket) {
				// try {
				// socket.emit(WELCOME, "Welcome to Socket.IO Chat, " +
				// socket.getId() + "!");
				//
				//// socket.join("room");
				// } catch (SocketIOException e) {
				// e.printStackTrace();
				// socket.disconnect(true);
				// }

				socket.on(new DisconnectListener() {

					@Override
					public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage) {
						of("/").emit(ANNOUNCEMENT, socket.getSession().getSessionId() + " disconnected");
					}
				});

				socket.on(CHAT_MESSAGE, new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);

						try {
							socket.broadcast("room", CHAT_MESSAGE, socket.getId(), args[0]);
						} catch (SocketIOException e) {
							e.printStackTrace();
						}

						return "OK"; // this object will be sent back to the
										// client in ACK packet
					}
				});

				socket.on(FORCE_DISCONNECT, new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						socket.disconnect(false);
						return null;
					}
				});

				socket.on(CLIENT_BINARY, new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						Map map = (Map<Object, Object>) args[0];
						InputStream is = (InputStream) map.get("buffer");
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						try {
							ByteStreams.copy(is, os);
							byte[] array = os.toByteArray();
							String s = "[";
							for (byte b : array)
								s += " " + b;
							s += " ]";
							LOGGER.log(Level.FINE, "Binary received: " + s);
						} catch (IOException e) {
							e.printStackTrace();
						}

						return "OK";
					}
				});

				socket.on(SERVER_BINARY, new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						try {
							socket.emit(SERVER_BINARY, new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }),
									new ACKListener() {
										@Override
										public void onACK(Object[] args) {
											System.out.println("ACK received: " + args[0]);
										}
									});
						} catch (SocketIOException e) {
							socket.disconnect(true);
						}

						return null;
					}
				});

				socket.on("channel", new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);

						try {
							String channel = (String) args[0];
							String uuid = (String) args[1];
							Map<String, Object> data = (LinkedHashMap<String, Object>) args[2];
							socket.join(channel);
							saveState(channel, uuid, data);
							socket.broadcast(channel, "join",channel, uuid, data);
							socket.emit("join",channel, uuid, data);
						} catch (SocketIOException e) {
							e.printStackTrace();
						}

						return "OK"; // this object will be sent back to the
										// client in ACK packet
					}
				});

				socket.on("setState", new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);
						try {
							String channel = (String) args[0];
							String uuid = (String) args[1];
							Map<String, Object> data = (LinkedHashMap<String, Object>) args[2];
							saveState(channel, uuid, data);
							
							socket.broadcast(channel, "state",channel, uuid, data);
						 } catch (SocketIOException e) {
							 e.printStackTrace();
						 }

						return "OK"; // this object will be sent back to the
										// client in ACK packet
					}
				});

				socket.on("publish", new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);
						String channel = (String) args[0];
						String uuid = (String) args[1];
						Map<String, Object> data = (LinkedHashMap<String, Object>) args[2];
						data.put("time", new Date());
						saveHistory(channel, uuid, data );
						
						try {
							socket.broadcast(channel, "message",channel, uuid, data);
						} catch (SocketIOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return "OK"; // this object will be sent back to the
										// client in ACK packet
					}
				});

				socket.on("whosonline", new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);
						String channel = (String) args[0];
						String uuid = (String) args[1];
						Map<String, String> data = (LinkedHashMap<String, String>) args[2];
						if(!states.containsKey(channel)) {
							return null;
					    } else {
					    	return states.get(channel);
					    }
					}
				});

				socket.on("history", new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);
						String channel = (String) args[0];
						String uuid = (String) args[1];
						Map<String, String> data = (LinkedHashMap<String, String>) args[2];
						if(!history.containsKey(channel)) {
							return null;
					    } else {
					    	return history.get(channel);
					    }
										// client in ACK packet
					}
				});

				socket.on("leave", new EventListener() {
					@Override
					public Object onEvent(String name, Object[] args, boolean ackRequested) {
						LOGGER.log(Level.FINE, "Received chat message: " + args[0]);
						String channel = (String) args[0];
						String uuid = (String) args[1];
						Map<String, String> data = (LinkedHashMap<String, String>) args[2];
						socket.leave(channel);

						return "OK"; // this object will be sent back to the
										// client in ACK packet
					}
				});

			}
		});

		// Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new
		// Runnable()
		// {
		// @Override
		// public void run()
		// {
		// try
		// {
		// of("/chat").in("room").emit("time", new Date().toString());
		// }
		// catch (SocketIOException e)
		// {
		// e.printStackTrace();
		// }
		// }
		// }, 0, 20, TimeUnit.SECONDS);

		// of("/news").on(new ConnectionListener()
		// {
		// @Override
		// public void onConnect(Socket socket)
		// {
		// socket.on();
		// }
		// });
	}
}
