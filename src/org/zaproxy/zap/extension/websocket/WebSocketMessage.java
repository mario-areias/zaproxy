/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.zaproxy.zap.extension.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;

/**
 * Represents a single WebSocket message, consisting out of at least one frame.
 * This class was created with sight on the WebSocketMessage class of the
 * Monsoon project (http://code.google.com/p/monsoon/).
 */
public abstract class WebSocketMessage {

	public enum Direction {
		INCOMING, OUTGOING
	}
	
	/**
	 * Consecutive number identifying a {@link WebSocketMessage}. Unique within
	 * a {@link WebSocketProxy}.
	 */
	private int messageId;
	
	/**
	 * This buffer will contain the whole payload, unmasked
	 */
	protected ByteBuffer payload;
	
	/**
	 * Indicates if this message already contains all of its frames.
	 */
	protected boolean isFinished;

	/**
	 * Indicating when this message was received.
	 */
	protected Timestamp timestamp;
	
	// WebSocket OpCodes - int's are used instead of a enum's for extensibility.

	public static final int OPCODE_CONTINUATION = 0x0;
	public static final int OPCODE_TEXT = 0x1;
	public static final int OPCODE_BINARY = 0x2;
	// non-control frames (0x3 - 0x7 are reserved for further non-control frames)

	public static final int OPCODE_CLOSE = 0x8;
	public static final int OPCODE_PING = 0x9;
	public static final int OPCODE_PONG = 0xA;
	// control frames (0xB - 0xF are reserved for further control frames)
	
	/**
	 * Indicates a normal closure, meaning that the purpose for
	 * which the connection was established has been fulfilled.
	 */
	public static final int STATUS_CODE_OK = 1000;
	
	/**
	 * Indicates that an endpoint is "going away", such as a server
     * going down or a browser having navigated away from a page.
	 */
	public static final int STATUS_CODE_GOING_AWAY = 1001;
	
	/**
	 * Indicates that an endpoint is terminating the connection due
     * to a protocol error.
	 */
	public static final int STATUS_CODE_PROTOCOL_ERROR = 1002;
	
	/**
	 * Indicates that an endpoint is terminating the connection
     * because it has received a type of data it cannot accept (e.g., an
     * endpoint that understands only text data MAY send this if it
     * receives a binary message).
	 */
	public static final int STATUS_CODE_INVALID_DATA_TYPE = 1003;
	
	// 1004 - 1006 are reserved values
	
	/**
	 * Indicates that an endpoint is terminating the connection
     * because it has received data within a message that was not
     * consistent with the type of the message (e.g., non-UTF-8 [RFC3629]
     * data within a text message).
	 */
	public static final int STATUS_CODE_INVALID_DATA = 1007;
	
	/**
	 * Indicates that an endpoint is terminating the connection
     * because it has received a message that violates its policy.  This
     * is a generic status code that can be returned when there is no
     * other more suitable status code (e.g., 1003 or 1009) or if there
     * is a need to hide specific details about the policy.
	 */
	public static final int STATUS_CODE_POLICY_VIOLATION = 1008;
	
	/**
	 * Indicates that an endpoint is terminating the connection
     * because it has received a message that is too big for it to
     * process.
	 */
	public static final int STATUS_CODE_MESSAGE_TOO_LARGE = 1009;
	
	/**
	 * Indicates that an endpoint (client) is terminating the
     * connection because it has expected the server to negotiate one or
     * more extension, but the server didn't return them in the response
     * message of the WebSocket handshake.  The list of extensions that
     * are needed SHOULD appear in the "reason" part of the Close frame.
     * Note that this status code is not used by the server, because it
     * can fail the WebSocket handshake instead.
	 */
	public static final int STATUS_CODE_EXTENSION_NEGOTIATION_FAILED = 1010;
	
	/**
	 * Indicates that a server is terminating the connection because
     * it encountered an unexpected condition that prevented it from
     * fulfilling the request.
	 */
	public static final int STATUS_CODE_SERVER_ERROR = 1011;

	// 1015 is another reserved status code
	
	public final static int[] OPCODES = {OPCODE_TEXT, OPCODE_BINARY, OPCODE_CLOSE, OPCODE_PING, OPCODE_PONG };

	/**
	 * Indicates the opcode of this message.
	 * Initially it is set to -1, meaning that its type is unknown.
	 */
	protected int opcode = -1;

	/**
	 * Indicates the status code of this message if it is a close message.
	 * Initially it is set to -1, meaning that its close code is unknown.
	 */
	protected int closeCode = -1;
	
	/**
	 * Used for en- & decoding from bytes to String and vice versa.
	 */
	protected static final Charset UTF8_CHARSET;
	
	/**
	 * Use the static initializer for setting up one date formatter for all
	 * instances.
	 */
	static {		
		UTF8_CHARSET = Charset.forName("UTF-8");
	}
	
	public WebSocketMessage(int messageId) {
		this.messageId = messageId;
	}

	/**
	 * Returns a consecutive number unique within one WebSocket channel.
	 * 
	 * @return
	 */
	public int getMessageId() {
		return messageId;
	}

	/**
	 * Write all frames of this message to given channel if and only if message
	 * is finished.
	 * 
	 * @param out
	 * @throws IOException
	 */
	public abstract void forward(OutputStream out) throws IOException;

	/**
	 * Read further frame for non-control message.
	 * 
	 * @param in
	 * @param frameHeader
	 * @throws IOException
	 */
	public abstract void readContinuation(InputStream in, byte frameHeader) throws IOException;

	/**
	 * Returns the status code if the message's opcode is a
	 * {@link WebSocketMessage#OPCODE_CLOSE}.
	 * One of the possible close codes is
	 * {@link WebSocketMessage#STATUS_CODE_OK}.
	 * 
	 * @return
	 */
	public int getCloseCode() {
		return closeCode;
	}
	
	/**
	 * @return the opcode for this message.
	 */
	public final int getOpcode() {
		return opcode;
	}
	
	/**
	 * @see WebSocketMessage#isBinary(int)
	 */
	public final boolean isBinary() {
		return isBinary(opcode);
	}

	/**
	 * @param opcode
	 * @return Returns true if this message contains binary content.
	 */
	public static final boolean isBinary(int opcode) {
		return opcode == OPCODE_BINARY;
	}
	
	/**
	 * @see WebSocketMessage#isText(int)
	 */
	public final boolean isText() {
		return isText(opcode);
	}

	/**
	 * @param opcode
	 * @return Returns true if this message contains textual content.
	 */
	public static final boolean isText(int opcode) {
		return opcode == OPCODE_TEXT;
	}
	
	/**
	 * @see WebSocketMessage#isControl(int)
	 */
	public final boolean isControl() {
		return isControl(opcode);
	}

	/**
	 * @param opcode
	 * @return Returns true if this message represents a control message.
	 */
	public static final boolean isControl(int opcode) {
		if (opcode >= 0x8 && opcode <= 0xF) {
			return true;
		}
		
		return false;
	}

	/**
	 * Returns true if all frames for this message were already
	 * read (i.e.: if the frame with the FIN flag was read).
	 * 
	 * @return
	 */
	public boolean isFinished() {
		return isFinished;
	}

	/**
	 * Returns the readable representation of this messages opcode.
	 * @return
	 */
	public String getOpcodeString() {
		return opcode2string(opcode);
	}

	/**
	 * Returns a readable representation of the numeric opcode.
	 * 
	 * @param opcode
	 * @return
	 */
	public static String opcode2string(int opcode) {
		switch (opcode) {
		case OPCODE_BINARY:
			return "BINARY";
			
		case OPCODE_CLOSE:
			return "CLOSE";
			
		case OPCODE_CONTINUATION:
			return "CONTINUATION";
			
		case OPCODE_PING:
			return "PING";
			
		case OPCODE_PONG:
			return "PONG";
			
		case OPCODE_TEXT:
			return "TEXT";
			
		default:
			return "UNKNOWN";
		}
	}

	/**
	 * Helper method to encode payload into UTF-8 string.
	 * 
	 * @param utf8bytes 
	 * @return
	 * @throws WebSocketException 
	 */
	protected String encodePayloadToUtf8(byte[] utf8bytes) throws WebSocketException {
		return encodePayloadToUtf8(utf8bytes, 0, utf8bytes.length);
	}
	
	/**
	 * Helper method to encode payload into UTF-8 string.
	 * 
	 * @param utf8bytes
	 * @param offset
	 * @param length
	 * @return
	 * @throws WebSocketException 
	 */
	protected String encodePayloadToUtf8(byte[] utf8bytes, int offset, int length) throws WebSocketException {
		try {
			Utf8StringBuilder builder = new Utf8StringBuilder(length);
			builder.append(utf8bytes, offset, length);

			return builder.toString();
		} catch (IllegalArgumentException e) {
			if (e.getMessage().equals("!utf8")) {
				throw new WebSocketException("Given bytes are no valid UTF-8!");
			} else {
				throw e;
			}
		} catch (IllegalStateException e) {
			if (e.getMessage().equals("!utf8")) {
				throw new WebSocketException("Given bytes are no valid UTF-8!");
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * Helper method that takes an UTF-8 string and returns its byte
	 * representation.
	 * 
	 * @param utf8string
	 * @return
	 */
	protected byte[] decodePayloadFromUtf8(String utf8string) {
		synchronized (UTF8_CHARSET) {
			return utf8string.getBytes(UTF8_CHARSET);
		}
	}
	
	/**
	 * Use this helper for concatenating payloads of different WebSocket frames.
	 * 
	 * @param bytes
	 */
	protected void appendPayload(byte[] bytes) {
		if (payload == null) {
			// initialize first
			payload = ByteBuffer.wrap(bytes);
		} else {
			// increase buffer
			payload = reallocate(payload, payload.capacity() + bytes.length);
			payload.put(bytes);
		}
	}
	
	/**
     * Resizes a given ByteBuffer to a new size.
     * 
     * @param src ByteBuffer to get resized
     * @param newSize size in bytes for the resized buffer
     */
    protected ByteBuffer reallocate(ByteBuffer src, int newSize) {
        int srcPos = src.position();
        if (srcPos > 0) {
            src.flip();
        }
        
        ByteBuffer dest = ByteBuffer.allocate(newSize);
        dest.put(src);
        dest.position(srcPos);
        
        return dest;
    }

	/**
	 * Returns date of receiving this message. Might also indicate the timestamp
	 * of the last frame received.
	 * 
	 * @return
	 */
	public Timestamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Returns the number of bytes used in the payload.
	 * 
	 * @return
	 */
	public abstract Integer getPayloadLength();
	
	/**
	 * Returns the 'original' payload as found in the WebSocket frame.
	 * 
	 * @return
	 */
	public abstract byte[] getPayload();

	/**
	 * Returns the payload from {@link WebSocketMessage#getPayload()} as
	 * readable string (i.e.: converted to UTF-8).
	 * 
	 * @return
	 */
	public abstract String getReadablePayload();

	/**
	 * Modifies the payload to given UTF-8 string. Converts that into bytes. 
	 * 
	 * @param newReadablePayload
	 * @throws WebSocketException 
	 */
	public abstract void setReadablePayload(String newReadablePayload) throws WebSocketException;
	
	/**
	 * Returns either {@link Direction#INCOMING} if this message originated from
	 * the browser, or {@link Direction#OUTGOING} if it came from server side.
	 * 
	 * @return
	 */
	public abstract Direction getDirection();

	/**
	 * Returns another presentation on this message object, used to decouple the
	 * user interface from this version specific implementation.
	 * 
	 * @return
	 */
	public WebSocketMessageDAO getDAO() {
		WebSocketMessageDAO dao = new WebSocketMessageDAO();
		
		Timestamp ts = getTimestamp();
		dao.setTime(ts);
		
		dao.opcode = getOpcode();
		dao.readableOpcode = getOpcodeString();

		if (isText()) {
			dao.payload = getReadablePayload();
		} else if (isBinary()) {
			// TODO: find binary websocket demo and set appropriate representation
			dao.payload = getReadablePayload();
//			dao.payload = byteArrayToHexString(getPayload());
		} else {
			dao.payload = getReadablePayload();
		}
		
		if (dao.payload == null) {
			// prevents NullPointerException
			dao.payload = "";
		}
		
		dao.isOutgoing = (getDirection() == Direction.OUTGOING) ? true : false;
		
		dao.payloadLength = getPayloadLength();
		
		return dao;
	}
	
	@Override
	public String toString() {
		return "WebSocketMessage#" + getMessageId();
	}
}