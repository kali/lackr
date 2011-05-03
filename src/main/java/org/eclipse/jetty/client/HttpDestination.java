// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================
package org.eclipse.jetty.client;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.client.HttpClient.Connector;
import org.eclipse.jetty.client.security.Authentication;
import org.eclipse.jetty.client.security.SecurityListener;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.log.Log;

/**
 * 
 * @version $Revision: 879 $ $Date: 2009-09-11 16:13:28 +0200 (Fri, 11 Sep 2009)
 *          $
 */
public class HttpDestination {
	private final ByteArrayBuffer _hostHeader;
	private final Address _address;
	private final List<HttpConnection> _all = Collections.synchronizedList(new LinkedList<HttpConnection>());
	private final Queue<HttpConnection> _idle = new ConcurrentLinkedQueue<HttpConnection>();
	private final HttpClient _client;
	private final int _maxConnections;
	private final AtomicInteger _openedConnectionCount = new AtomicInteger();
	private final Queue<Throwable> _connectionFailures = new ConcurrentLinkedQueue<Throwable>();

	/* The queue of exchanged for this destination if connections are limited */
	private ConcurrentLinkedQueue<HttpExchange> _queue = new ConcurrentLinkedQueue<HttpExchange>();

	/*
	private static List<HttpDestination> _allDestinations = Collections.synchronizedList(new LinkedList<HttpDestination>());	
	private static Runnable _destinationThread = new Thread() {
		public void run() {
			for(HttpDestination d: _allDestinations)
	            try {
	                d.makeThingsHappen();
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
		};
	};
	*/

	HttpDestination(HttpClient client, Address address, boolean ssl, int maxConnections) {
		_client = client;
		_address = address;
		_maxConnections = maxConnections;
		String addressString = address.getHost();
		if (address.getPort() != 80)
			addressString += ":" + address.getPort();
		_hostHeader = new ByteArrayBuffer(addressString);
	}

	public Address getAddress() {
		return _address;
	}

	public Buffer getHostHeader() {
		return _hostHeader;
	}

	public HttpClient getHttpClient() {
		return _client;
	}

	public boolean isSecure() {
		return false;
	}

	public void addAuthorization(String pathSpec, Authentication authorization) {
		throw new RuntimeException("Not implemented");
	}

	public void addCookie(HttpCookie cookie) {
		throw new RuntimeException("Not implemented");
	}

	private void makeThingsHappen() throws IOException {
		HttpExchange ex = _queue.poll();
		if (ex == null)
			return;
		Throwable throwable = _connectionFailures.poll();
		if (throwable != null) {
			ex.onConnectionFailed(throwable);
			return;
		}
		HttpConnection connection;
		if ((connection = _idle.poll()) == null) {
			if (_openedConnectionCount.get() < _maxConnections) {
				_openedConnectionCount.incrementAndGet();
				startNewConnection();
			}
			while((connection = _idle.poll()) == null)
				Thread.yield();
		}
		send(connection, ex);
	}

	protected void startNewConnection() {
		try {
			final Connector connector = _client._connector;
			if (connector != null)
				connector.startConnection(this);
		} catch (Exception e) {
			Log.debug(e);
			onConnectionFailed(e);
		}
	}

	public void onConnectionFailed(Throwable throwable) {
		_connectionFailures.add(throwable);
	}

	public void onException(Throwable throwable) {
		_connectionFailures.add(throwable);
	}

	public void onNewConnection(final HttpConnection connection) throws IOException {
		_all.add(connection);
		returnConnection(connection, false);
	}

	public void returnConnection(HttpConnection connection, boolean close) throws IOException {

		if (close || !connection.getEndPoint().isOpen()) {
			try {
				connection.close();
				if (_all.remove(connection))
					_openedConnectionCount.decrementAndGet();
			} catch (IOException e) {
				Log.ignore(e);
			}
		} else {
			if (!_client.isStarted())
				return;

			if (connection.isReserved())
				connection.setReserved(false);

			_idle.add(connection);
		}
	}

	@SuppressWarnings("unchecked")
	public void send(HttpExchange ex) throws IOException {
		LinkedList<String> listeners = _client.getRegisteredListeners();

		if (listeners != null) {
			// Add registered listeners, fail if we can't load them
			for (int i = listeners.size(); i > 0; --i) {
				String listenerClass = listeners.get(i - 1);

				try {
					Class listener = Class.forName(listenerClass);
					Constructor constructor = listener
					        .getDeclaredConstructor(HttpDestination.class, HttpExchange.class);
					HttpEventListener elistener = (HttpEventListener) constructor.newInstance(this, ex);
					ex.setEventListener(elistener);
				} catch (Exception e) {
					e.printStackTrace();
					throw new IOException("Unable to instantiate registered listener for destination: " + listenerClass);
				}
			}
		}
		doSend(ex);
	}

	public void resend(HttpExchange ex) throws IOException {
		ex.getEventListener().onRetry();
		ex.reset();
		doSend(ex);
	}

	protected void doSend(HttpExchange ex) throws IOException {
		_queue.add(ex);
		makeThingsHappen();
	}

	protected void send(HttpConnection connection, HttpExchange exchange) throws IOException {
		// System.err.println(toString() + " sending");
		boolean result;
		result = connection.send(exchange);	        
		if (!result) {
			// System.err.println(toString() + " failure sending");
			_queue.add(exchange);
			returnConnection(connection, false);
		}
	}

	@Override
	public synchronized String toString() {
		return "HttpDestination@" + hashCode() + "//" + _address.getHost() + ":" + _address.getPort() + "("
		        + _all.size() + "," + _idle.size() + "," + _queue.size() + ")";
	}

	public synchronized String toDetailString() {
		StringBuilder b = new StringBuilder();
		b.append(toString());
		b.append('\n');
		b.append("--");
		b.append('\n');

		return b.toString();
	}

	public void setProxy(Address proxy) {
		throw new RuntimeException("Not implemented");
	}

	public Address getProxy() {
		throw new RuntimeException("Not implemented");
	}

	public Authentication getProxyAuthentication() {
		throw new RuntimeException("Not implemented");
	}

	public void setProxyAuthentication(Authentication authentication) {
		throw new RuntimeException("Not implemented");
	}

	public boolean isProxied() {
		return false;
	}

	public void close() throws IOException {
	}

}
