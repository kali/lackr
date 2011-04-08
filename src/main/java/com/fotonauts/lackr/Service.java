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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BasicBSONObject;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class Service extends AbstractHandler {

	private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
	static Logger log = LoggerFactory.getLogger(Service.class);
	protected String backend = "http://localhost";
	protected String mongoLoggingPath = "127.0.0.1:27017/logs/logs";
	protected HttpClient client;
	private List<SubstitutionEngine> substituers = new ArrayList<SubstitutionEngine>();
	protected Mongo logConnection;
	protected DBCollection logCollection;
	private Executor executor;

	
	@Override
	protected void doStart() throws Exception {
		setExecutor(Executors.newFixedThreadPool(16));
	    super.doStart();
	}

	public static void addHeadersIfPresent(BasicBSONObject logLine, HttpServletRequest request, MongoLoggingKeys key,
	        String headerName) {
		String value = request.getHeader(headerName);
		if (value != null)
			logLine.put(key.getPrettyName(), value);
	}

	public static BasicDBObject standardLogLine(HttpServletRequest request, String facility) {
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
					logLine.put(LOGIN_SESSION.getPrettyName(), cookie.getValue());
				}
			}
		}

		return logLine;
	}

	public String getBackend() {
		return backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public HttpClient getClient() {
		return client;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	public List<SubstitutionEngine> getSubstituers() {
		return substituers;
	}

	public void setSubstituers(List<SubstitutionEngine> substituers) {
		this.substituers = substituers;
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

		String[] pathComponents = mongoLoggingPath.split("/");
		if (pathComponents.length != 3)
			throw new IllegalArgumentException("Mongo Logging Path not compliant with spec in \"" + mongoLoggingPath
			        + "\", format is host:port/database/collection.");

		String[] hostComponents = pathComponents[0].split(":");
		if (hostComponents.length != 2)
			throw new IllegalArgumentException(
			        "Mongo Logging Hostname not compliant with spec, should be host:port (is \"" + pathComponents[0]
			                + "\" ).");

		logConnection = new Mongo(hostComponents[0], Integer.parseInt(hostComponents[1]));
		setLogCollection(logConnection.getDB(pathComponents[1]).getCollection(pathComponents[2]));

	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
	        throws IOException, ServletException {
		LackrRequest state = (LackrRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
		if (state == null) {
			log.debug("starting processing for: " + request.getRequestURL());
			state = new LackrRequest(this, request);
			request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
			state.kick();
		} else {
			log.debug("resuming processing for: " + request.getRequestURL());
			state.writeResponse(response);
		}
	}

	/**
	 * @return the logCollection
	 */
	public DBCollection getLogCollection() {
		return logCollection;
	}

	/**
	 * @param logCollection
	 *            the logCollection to set
	 */
	public void setLogCollection(DBCollection logCollection) {
		this.logCollection = logCollection;
	}

	public void setExecutor(Executor executor) {
	    this.executor = executor;
    }

	public Executor getExecutor() {
	    return executor;
    }

}
