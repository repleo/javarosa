/**
 * 
 */
package org.javarosa.service.transport.securehttp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import org.javarosa.core.log.WrappedException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.BasicTransportMessage;
import org.javarosa.services.transport.impl.TransportMessageStatus;
import org.javarosa.services.transport.impl.simplehttp.HttpRequestProperties;

import de.enough.polish.util.StreamUtil;

/**
 * An AuthenticatedHttpTransportMessage is a transport message which is used to
 * either perform a GET or POST request to an HTTP server, which includes the 
 * capacity for authenticating with that server if a WWW-Authenticate challenge
 * is issued. 
 * 
 * AuthenticatedHttpTransportMessage are currently unable to cache themselves
 * natively with the transport service.
 * 
 * @author ctsims
 *
 */
public class AuthenticatedHttpTransportMessage extends BasicTransportMessage {
	String URL;
	HttpAuthenticator authenticator;

	int responseCode;
	InputStream response;
	String authentication;

	IDataPayload payload;
	
	HttpRequestProperties responseProperties;
	
	private AuthenticatedHttpTransportMessage(String URL, HttpAuthenticator authenticator) {
		this.setCreated(new Date());
		this.setStatus(TransportMessageStatus.QUEUED);
		this.URL = URL;
		this.authenticator = authenticator;
	}
	
	/**
	 * Creates a message which will perform an HTTP GET Request to the server referenced at
	 * the given URL. 
	 * 
	 * @param URL The requested server URL
	 * @param authenticator An authenticator which is capable of providing credentials upon
	 * request.
	 * @return A new authenticated HTTP message ready for sending.
	 */
	public static AuthenticatedHttpTransportMessage AuthenticatedHttpRequest(String URL, HttpAuthenticator authenticator) {
		return new AuthenticatedHttpTransportMessage(URL, authenticator);
	}
	
	/**
	 * Creates a message which will perform an HTTP POST Request to the server referenced at
	 * the given URL. 
	 * 
	 * @param URL The requested server URL
	 * @param authenticator An authenticator which is capable of providing credentials upon
	 * request.
	 * @param payload A data payload which will be posted to the remote server.
	 * @return A new authenticated HTTP message ready for sending.
	 */
	public static AuthenticatedHttpTransportMessage AuthenticatedHttpPOST(String URL, IDataPayload payload, HttpAuthenticator authenticator) {
		AuthenticatedHttpTransportMessage message = new AuthenticatedHttpTransportMessage(URL, authenticator);
		message.payload = payload;
		return message;
	}
	

	
	/**
	 * @return The HTTP request method (Either GET or POST) for
	 * this message.
	 */
	public String getMethod() {
		return (payload == null ? HttpConnection.GET : HttpConnection.POST);
	}
	
	/**
	 * @return The HTTP URL of the server for this message
	 */
	public String getUrl() {
		return URL;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.services.transport.TransportMessage#isCacheable()
	 */
	public boolean isCacheable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.services.transport.TransportMessage#setCacheIdentifier(java.lang.String)
	 */
	public void setCacheIdentifier(String id) {
		Logger.log("transport", "warn: setting cache ID on non-cacheable message");
		//suppress; these messages are not cacheable
	}
	
	public void setSendingThreadDeadline(long queuingDeadline) {
		Logger.log("transport", "warn: setting cache expiry on non-cacheable message");
		//suppress; these messages are not cacheable
	}
	
	/**
	 * @param code The response code of the most recently attempted
	 * request.
	 */
	public void setResponseCode(int code) {
		this.responseCode = code;
	}
	
	/**
	 * @return code The response code of the most recently attempted
	 * request.
	 */
	public int getResponseCode() {
		return responseCode;
	}
	
	/**
	 * Sets the stream of the response from a delivery attempt
	 * @param response The stream provided from the http connection
	 * from a deliver attempt
	 */
	protected void setResponseStream(InputStream response) {
		this.response = response;
	}
	
	/**
	 * @return The stream provided from the http connection
	 * from the previous deliver attempt
	 */
	public InputStream getResponse() {
		return response;
	}
	
	public HttpRequestProperties getReponseProperties() {
		return responseProperties;
	}

	/**
	 * @return The properties for this http request (other than
	 * authorization headers).
	 */
	public HttpRequestProperties getRequestProperties() {
		//TODO: Possibly actually count content length here
		return new HttpRequestProperties(this.getMethod(), -1, "1.0");
	}

	public InputStream getContentStream() {
		if (payload == null) {
			return new ByteArrayInputStream("".getBytes());
		} else {
			return payload.getPayloadStream();
		}
	}
	
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.services.transport.Transporter#send()
	 */
	public void send() {
		try {
			
			//Open the connection assuming either cached credentials
			//or no Authentication
			HttpConnection connection = getConnection();
			int response = connection.getResponseCode();
			
			if (response == HttpConnection.HTTP_UNAUTHORIZED) {
				
				String challenge = getChallenge(connection);
				//If authentication is needed, issue the challenge
				if (this.issueChallenge(connection, challenge)) {
					
					// The challenge was handled, and authentication
					// is now provided, try the request again after
					//closing the current connection.
					connection.close();
					connection = getConnection();
					
					//Handle the new response as-is, if authentication failed,
					//the sending process can issue a new request.
					handleResponse(connection);
				} else {
					// The challenge couldn't be addressed. Set the message to
					// failure.
					handleResponse(connection);
				}
			} else {
				//The message did not fail due to authorization problems, so
				//handle the response.
				handleResponse(connection);
			}
		} catch (IOException e) {
			e.printStackTrace();
			this.setStatus(TransportMessageStatus.FAILED);
			this.setFailureReason(WrappedException.printException(e));
		}
	}
	
	private String getChallenge(HttpConnection connection ) throws IOException {
		final String AUTH_HEADER_HACK = "X-S60-Auth";
		
		//technically the standard
		
		String challenge = null;
		if (challenge == null) {
			challenge = connection.getHeaderField(AUTH_HEADER_HACK);			
		}
		if (challenge == null) {
			challenge = connection.getHeaderField(AUTH_HEADER_HACK.toLowerCase());			
		}
		if (challenge == null) {
			challenge = connection.getHeaderField("WWW-Authenticate");
		}
		if(challenge == null) {
			challenge = connection.getHeaderField("www-authenticate");
		}
		
		return challenge;
	}

	/**
	 * Issues an authentication challenge from the provided HttpConnection
	 * 
	 * @param connection The connection which issued the challenge
	 * @param challenge The WWW-Authenticate challenge issued.
	 * @return True if the challenge was addressed by the message's authenticator,
	 * and the request should be retried, False if the challenge could not be 
	 * addressed.
	 */
	public boolean issueChallenge(HttpConnection connection, String challenge) {
		authentication = this.authenticator.challenge(connection, challenge, this);
		if(authentication == null) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * @return the current best-guess authorization header for this message, 
	 * either produced as a response to a WWW-Authenticate challenge, or 
	 * provided by the authentication cache based on previous requests
	 * (if enabled and relevant in the message's authenticator). 
	 */
	public String getAuthString() {
		if(authentication == null) {
			//generally pre-challenge
			return authenticator.checkCache(this);
		}
		return authentication;
	}
	
	private void handleResponse(HttpConnection connection) throws IOException {
		int responseCode = connection.getResponseCode();
		responseProperties = HttpRequestProperties.HttpResponsePropertyFactory(connection);
		long responseLength = connection.getLength();

		if (responseLength > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD) {
			Logger.log("recv", "size " + responseLength);
		}
		
		if(responseCode >= 200 && responseCode < 300) {
			//It's all good, message was a success.
			this.setResponseCode(responseCode);
			this.setStatus(TransportMessageStatus.SENT);
			
			//Wire up the input stream from the connection to the message.
			this.setResponseStream(new InputStreamC(connection.openInputStream(), responseLength, this.getTag()));
		} else {
			this.setStatus(TransportMessageStatus.FAILED);
			this.setResponseCode(responseCode);
			
			//We'll assume that any failures come with a message which is sufficiently
			//small that they can be fit into memory.
			byte[] response = StreamUtil.readFully(connection.openInputStream());
			String reason = responseCode + ": " + new String(response);
			reason = PropertyUtils.trim(reason, 400);
			this.setFailureReason(reason);
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	private HttpConnection getConnection() throws IOException {
		HttpConnection conn = (HttpConnection) Connector.open(this.getUrl());
		if (conn == null)
			throw new RuntimeException("Null conn in getConnection()");

		HttpRequestProperties requestProps = this.getRequestProperties();
		if (requestProps == null) {
			throw new RuntimeException("Null message.getRequestProperties() in getConnection()");
		}
		requestProps.configureConnection(conn);
		
		//Retrieve either the response auth header, or the cached guess
		String authorization = this.getAuthString();
		if(authorization != null) {
			conn.setRequestProperty("Authorization", authorization);
		}

		return conn;
	}

	protected class InputStreamC extends InputStream {
		private InputStream is;
		private long total;
		private long read;
		private String tag;
		
		boolean logged = false;
		
		public InputStreamC (InputStream is, long totalLength, String tag) {
			this.is = is;
			this.total = totalLength;
			this.read = 0;
			this.tag = tag;
		}
		
		public int read() throws IOException {
			try {
				int c = is.read();
				read += 1;
				return c;
			} catch (IOException ioe) {
				log(true);
				throw ioe;
			}
		}
		
		public int read(byte[] b) throws IOException {
			try {
				int k = is.read(b);
				read += Math.max(k, 0);
				return k;
			} catch (IOException ioe) {
				log(true);
				throw ioe;
			}
		}
		
		public int read(byte[] b, int off, int len) throws IOException {
			try {
				int k = is.read(b, off, len);
				read += Math.max(k, 0);
				return k;
			} catch (IOException ioe) {
				log(true);
				throw ioe;
			}
		}
		
		public long skip(long n) throws IOException {
			try {
				long k = is.skip(n);
				read += k;
				return k;
			} catch (IOException ioe) {
				log(true);
				throw ioe;
			}
		}

		public void close() throws IOException {
			log(false);
			is.close();
		}

		private void log (boolean ex) {
			if (logged)
				return;
			logged = true;
			
			try {
				boolean hasLength = (total >= 0);	//whether we have total length
				boolean diff;						//whether bytes read differed from total length
				boolean logIt;						//whether to log stats
				
				if (hasLength) {
					diff = (total != read);
					logIt = diff;
				} else {
					logIt = (read > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD || ex);
					diff = false;
				}
		
				if (logIt) {
					Logger.log("recv", "<" + tag + "> " + read + (diff ? " of " + total : ""));
				}
			} catch (Exception e) {
				//extrasafe
				Logger.exception("InputStreamC.log", e);
			}
		}
		
		public int available() throws IOException {
			System.err.println("don't expect this to ever be called");
			return is.available();
		}
		
		public void mark(int rl) {
			System.err.println("don't expect this to ever be called");
			is.mark(rl);
		}
		
		public void reset() throws IOException {
			System.err.println("don't expect this to ever be called");
			is.reset();
		}
		
		public boolean markSupported() {
			System.err.println("don't expect this to ever be called");
			return is.markSupported();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		//doesn't cache;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		//doesn't cache;
	}
}
