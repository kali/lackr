package com.fotonauts.lackr;

import static com.fotonauts.lackr.MongoLoggingKeys.CLIENT_ID;
import static com.fotonauts.lackr.MongoLoggingKeys.FACILITY;
import static com.fotonauts.lackr.MongoLoggingKeys.LOGIN_SESSION;
import static com.fotonauts.lackr.MongoLoggingKeys.OPERATION_ID;
import static com.fotonauts.lackr.MongoLoggingKeys.REMOTE_ADDR;
import static com.fotonauts.lackr.MongoLoggingKeys.SESSION_ID;
import static com.fotonauts.lackr.MongoLoggingKeys.SSL;
import static com.fotonauts.lackr.MongoLoggingKeys.USER_AGENT;
import static com.fotonauts.lackr.MongoLoggingKeys.USER_ID;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BasicBSONObject;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;

import com.fotonauts.lackr.hashring.HashRing;
import com.fotonauts.lackr.hashring.Host;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class Service extends AbstractHandler {

	private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
	static Logger log = LoggerFactory.getLogger(Service.class);

	protected String mongoLoggingPath;
	protected BackendClient client;
	protected DBCollection logCollection;

	protected Interpolr interpolr;

	public Interpolr getInterpolr() {
		return interpolr;
	}

	@Required
	public void setInterpolr(Interpolr interpolr) {
		this.interpolr = interpolr;
	}

	private Executor executor;
	private HashRing ring;
	private String backends;
	private String probeUrl;

	@Override
	protected void doStart() throws Exception {
		setExecutor(Executors.newFixedThreadPool(16));
		super.doStart();
	}

	public static void addHeadersIfPresent(BasicBSONObject logLine,
			HttpServletRequest request, MongoLoggingKeys key, String headerName) {
		String value = request.getHeader(headerName);
		if (value != null)
			logLine.put(key.getPrettyName(), value);
	}

	public static BasicDBObject standardLogLine(HttpServletRequest request,
			String facility) {
		/* Prepare the log line */
		BasicDBObject logLine = new BasicDBObject();
		logLine.put(FACILITY.getPrettyName(), facility);

		addHeadersIfPresent(logLine, request, USER_AGENT, "User-Agent");
		addHeadersIfPresent(logLine, request, OPERATION_ID, "X-Ftn-OperationId");
		addHeadersIfPresent(logLine, request, REMOTE_ADDR, "X-Forwarded-For");
		addHeadersIfPresent(logLine, request, CLIENT_ID, "X-Ftn-User");
		addHeadersIfPresent(logLine, request, SESSION_ID, "X-Ftn-Session");

		if ("true".equals(request.getHeader("X-Ftn-SSL")))
			logLine.put(SSL.getPrettyName(), true);

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String cname = cookie.getName();
				if (cname.equals("uid")) {
					logLine.put(USER_ID.getPrettyName(), cookie.getValue());
				} else if (cname.equals("login_session")) {
					logLine.put(LOGIN_SESSION.getPrettyName(),
							cookie.getValue());
				}
			}
		}

		return logLine;
	}

	public BackendClient getClient() {
		return client;
	}

	public void setClient(BackendClient client) {
		this.client = client;
	}

	/**
	 * @return the mongoPath
	 */
	public String getMongoLoggingPath() {
		return mongoLoggingPath;
	}

	/**
	 * @param mongoPath
	 *            the mongoPath to set
	 */
	public void setMongoLoggingPath(String mongoPath) {
		this.mongoLoggingPath = mongoPath;
	}

	@PostConstruct
	public void initLogger() throws MongoException, UnknownHostException {
		if (StringUtils.hasText(mongoLoggingPath)) {
			String[] pathComponents = mongoLoggingPath.split("/");
			if (pathComponents.length != 3)
				throw new IllegalArgumentException(
						"Mongo Logging Path not compliant with spec in \""
								+ mongoLoggingPath
								+ "\", format is host:port/database/collection.");

			String[] hostComponents = pathComponents[0].split(":");
			if (hostComponents.length != 2)
				throw new IllegalArgumentException(
						"Mongo Logging Hostname not compliant with spec, should be host:port (is \""
								+ pathComponents[0] + "\" ).");

			Mongo logConnection = new Mongo(hostComponents[0],
					Integer.parseInt(hostComponents[1]));
			setLogCollection(logConnection.getDB(pathComponents[1])
					.getCollection(pathComponents[2]));
		}
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
	        throws IOException, ServletException {
		LackrFrontendRequest state = (LackrFrontendRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
		if (state == null) {
			log.debug("starting processing for: " + request.getRequestURL());
			state = new LackrFrontendRequest(this, request);
			request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
			state.kick();
		} else {
			log.debug("resuming processing for: " + request.getRequestURL());
			state.writeResponse(response);
		}
	}

	public void setBackends(String backends) {
		this.backends = backends;
	}

	public void setProbeUrl(String probeUrl) {
		this.probeUrl = probeUrl;
	}

	@PostConstruct
	public void buildRing() {
		if (ring == null && backends != null && !backends.equals("")) {
			String[] hostnames = backends.split(",");
			Host[] hosts = new Host[hostnames.length];
			for (int i = 0; i < hostnames.length; i++)
				hosts[i] = new Host(hostnames[i], probeUrl);
			ring = new HashRing(hosts);
		}
	}

	public void setLogCollection(DBCollection logCollection) {
		this.logCollection = logCollection;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public Executor getExecutor() {
		return executor;
	}

	public HashRing getRing() {
		return ring;
	}

	public void setRing(HashRing ring) {
		this.ring = ring;
	}

	public void log(BasicDBObject logLine) {
		if (logCollection != null)
			logCollection.save(logLine);
	}
}
