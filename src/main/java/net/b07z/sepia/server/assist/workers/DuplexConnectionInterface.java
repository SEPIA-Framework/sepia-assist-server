package net.b07z.sepia.server.assist.workers;

import java.util.function.Consumer;

import org.json.simple.JSONObject;

/**
 * Interface for duplex connections. A duplex connection maintains an open, permanent channel for two-way data transfer, e.g. WebSocket or MQTT.
 * 
 * @author Florian Quirin
 *
 */
public interface DuplexConnectionInterface {
	
	/**
	 * Start any setup process.
	 * @throws Exception 
	 */
	public void setup() throws Exception;
	
	/**
	 * Get connection status code. E.g.:<br>
	 * -1: error,<br>
	 *  0: connected,<br>
	 *  1: connecting,<br>
	 *  2: offline,<br>
	 *  3: disconnecting
	 */
	public int getStatus();
	
	/**
	 * Get a text description for the status code.
	 */
	public String getStatusDescription();
	
	/**
	 * An unique ID given at creation. 
	 */
	public String getName();
	
	/**
	 * Get timestamp of last activity (read, write, etc.)
	 */
	public long getLastActivity();
	
	/**
	 * Connect right away.
	 */
	public void connect();
	/**
	 * Send connect request after a custom delay time (handy if you don't want all connections to start at the same time).
	 * @param delay - wait this long before connect (-1 = default wait)
	 * @param onConnected - a method called when connection was successful
	 */
	public void connect(long delay, Runnable onConnected);
	
	/**
	 * Send a message.
	 * @param msg - JSONObject with message
	 * @param path - (optional, null) path info for message like MQTT topic or broadcast channel. Note: not all implementations might support this.
	 * @param receiver - (optional, null) receiver. Note: not all implementations might support this.
	 * @return true if message was sent (note: does not guarantee that someone received it)
	 */
	public boolean sendMessage(JSONObject msg, String path, String receiver);
	
	/**
	 * Add a message handler with optional path filter.<br>
	 * NOTE: usually each path can only have ONE handler!
	 * @param pathFilter - filter, e.g. topic or channel name (wildcards possible)
	 * @param handlerFun - function to handle JSON message data when received
	 * @return ID of this handler. Can be used to remove handler
	 */
	public boolean addMessageHandler(String pathFilter, Consumer<JSONObject> handlerFun);
	
	/**
	 * Remove handler previously registered for path.
	 * @param pathFilter - same path given when handler was added
	 */
	public boolean removeMessageHandler(String pathFilter);
	
	/**
	 * Send disconnect request then close the connection and release all resources. Usually after this the client cannot be reused and connect requests fail.
	 */
	public void disconnect();
	
	/**
	 * Use this if you want to make sure that you don't execute any action 
	 * while the connection is in transient state (state-code 1 or 3 usually).
	 * @param state - state to wait for
	 * @param maxWait - maximum time to wait before timeout. Use -1 for default.
	 * @return true if state changed before timeout
	 */
	public boolean waitForState(int state, long maxWait);
}
