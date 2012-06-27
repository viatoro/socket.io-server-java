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
package com.glines.socketio.server;

import java.util.ArrayList;
import java.util.List;

public class SocketIOFrame {
	public static final char SEPERATOR_CHAR = ':';
	public enum FrameType {
		UNKNOWN(-1),
		CLOSE(0),
		CONNECT(1),
		HEARTBEAT_INTERVAL(2),
		MESSAGE(3),
		JSON_MESSAGE(4),
        EVENT(5),
		ACK(6),
		ERROR(7),
        NOOP(8);

		private int value;
		
		FrameType(int value) {
			this.value = value;
		}
		
		public int value() {
			return value;
		}
		
		public static FrameType fromInt(int val) {
			switch (val) {
			case 0:
				return CLOSE;
			case 1:
				return CONNECT;
			case 2:
				return HEARTBEAT_INTERVAL;
			case 3:
				return MESSAGE;
			case 4:
				return JSON_MESSAGE;
            case 5:
				return EVENT;
			case 6:
				return ACK;
			case 7:
				return ERROR;
            case 8:
				return NOOP;
			default:
				return UNKNOWN;
			}
		}
	}

	public static final int TEXT_MESSAGE_TYPE = 0;
	public static final int JSON_MESSAGE_TYPE = 1;
	
	private static boolean isHexDigit(String str, int start, int end) {
		for (int i = start; i < end; i++) {
			char c = str.charAt(i);
			if (!Character.isDigit(c) &&
					c < 'A' && c > 'F' && c < 'a' && c > 'f') {
				return false;
			}
		}
		return true;
	}
	
	public static List<SocketIOFrame> parse(String data) {
		List<SocketIOFrame> messages = new ArrayList<SocketIOFrame>();
		int idx = 0;

		// Parse the data and silently ignore any part that fails to parse properly.
        while (data.length() > idx) {
			int start = idx;
			int end = data.indexOf(SEPERATOR_CHAR, start);

			if (-1 == end || start == end || !isHexDigit(data, start, end)) {
				break;
			}

			int ftype = Integer.parseInt(data.substring(start, start + 1));

			FrameType frameType = FrameType.fromInt(ftype);
			if (frameType == FrameType.UNKNOWN) {
				break;
			}

            start = end + 1;
            end = data.indexOf(SEPERATOR_CHAR, start);

            int messageId = 0;
            if (end - start > 1) {
				messageId = Integer.parseInt(data.substring(start + 1, end));
			}
			
			start = end + 1;
			end = data.indexOf(SEPERATOR_CHAR, start);

			String endpoint = "";
            if (end - start > 1) {
				endpoint = data.substring(start + 1, end);
			}

			start = end + 1;
			end = data.length();

			messages.add(new SocketIOFrame(frameType,
                    frameType == FrameType.MESSAGE ? TEXT_MESSAGE_TYPE : JSON_MESSAGE_TYPE,
                    data.substring(start, end)));
			idx = end;
		}
		
		return messages;
	}
	
	public static String encode(FrameType type, String data) {
		StringBuilder str = new StringBuilder(data.length() + 16);
		str.append(Integer.toHexString(type.value()));
        str.append(SEPERATOR_CHAR);
        //str.append("1"); // message id
		str.append(SEPERATOR_CHAR);
		//str.append(""); // endpoint
		str.append(SEPERATOR_CHAR);
		str.append(data);
		return str.toString();
	}
	
	private final FrameType frameType;
	private final int messageType;
	private final String data;
	
	public SocketIOFrame(FrameType frameType, int messageType, String data) {
		this.frameType = frameType;
		this.messageType = messageType;
		this.data = data;
	}
	
	public FrameType getFrameType() {
		return frameType;
	}
	
	public int getMessageType() {
		return messageType;
	}
	
	public String getData() {
		return data;
	}
	
	public String encode() {
		return encode(frameType, data);
	}
}
