package com.fotonauts.lackr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.springframework.util.Log4jConfigurer;

import com.fotonauts.lackr.hashring.HashRing;
import com.fotonauts.lackr.hashring.RingHost;

public class TestRingHA extends TestCase {

	class StubServer extends Server {
		public AtomicInteger requestCount = new AtomicInteger(0);
		public AtomicBoolean up = new AtomicBoolean(true);
		public RingHost host;

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
		        throws IOException, ServletException {
			requestCount.incrementAndGet();
			if (up.get()) {
				response.setStatus(200);
				response.getWriter().write("okie");
				response.flushBuffer();
			} else {
				throw new ServletException("this is a 500");
			}
		}

		public StubServer() throws Exception {
			addConnector(new SelectChannelConnector());
			start();
			host = new RingHost("localhost:" + getConnectors()[0].getLocalPort());
			host.setProbe("/");
		}
	}

	@Override
	protected void setUp() throws Exception {
		Log4jConfigurer.initLogging("classpath:log4j.debug.properties");

	}

	public void testHostProbeNoConnection() throws MalformedURLException {
		RingHost h = new RingHost("localhost:29843");
		h.setProbe("/");
		h.probe();
		assertFalse("h is down", h.isUp());
	}

	public void testHostProbeWrongHostname() throws Exception {
		RingHost h = new RingHost("something.that.does.not.exists");
		h.setProbe("/");
		h.probe();
		assertFalse("h is down", h.isUp());
	}

	public void testHostProbe500() throws Exception {
		StubServer backend = new StubServer();
		backend.up.set(false);
		RingHost h = backend.host;
		h.probe();
		assertEquals("server has been probed", 1, backend.requestCount.get());
		assertFalse("h is down", h.isUp());
	}
	
	public void testHostProbe200() throws Exception {
		StubServer backend = new StubServer();
		RingHost h = backend.host;
		h.probe();
		assertEquals("server has been probed", 1, backend.requestCount.get());
		assertTrue("h is up", h.isUp());
	}

	public void testRingStatusDiscovery() throws Exception {
		StubServer backend1 = new StubServer();
		StubServer backend2 = new StubServer();
		HashRing ring = new HashRing(backend1.host, backend2.host);
		ring.init();
		Thread.sleep(100);
		assertTrue("server has been probed", backend1.requestCount.get() > 0);
		assertTrue("server has been probed", backend2.requestCount.get() > 0);
		assertTrue("ring is up", ring.up());
		backend1.up.set(false);
		Thread.sleep(1500);
		assertTrue("ring is still up", ring.up());
		assertTrue("backend1 is down", !backend1.host.isUp());
		assertTrue("backend2 is up", backend2.host.isUp());
		backend2.up.set(false);
		Thread.sleep(1500);
		assertTrue("ring is now down", !ring.up());
		assertTrue("backend1 is down", !backend1.host.isUp());
		assertTrue("backend2 is down", !backend2.host.isUp());
		backend1.up.set(true);
		Thread.sleep(1500);
		assertTrue("ring is back up", ring.up());
		assertTrue("backend1 is up", backend1.host.isUp());
		assertTrue("backend2 is down", !backend2.host.isUp());
	}

}