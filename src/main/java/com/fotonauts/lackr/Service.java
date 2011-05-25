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
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.codehaus.jackson.map.ObjectMapper;
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

public class Service extends AbstractHandler implements RapportrInterface {

	private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
	private final String hostname;
	static Logger log = LoggerFactory.getLogger(Service.class);

	protected BackendClient client;
	protected DBCollection accessLogCollection;
	protected DBCollection mongoRapportrQueue;

	private int timeout;

	protected Interpolr interpolr;

	public Service() {
		String hostname = "";
		try {
			hostname = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {

		}
		this.hostname = hostname;
	}

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
	private ObjectMapper objectMapper = new ObjectMapper();
	private String grid;
	private String ircErrorChannel;

	@Override
	protected void doStart() throws Exception {
		setExecutor(Executors.newFixedThreadPool(16));
		super.doStart();
	}

	public static void addHeadersIfPresent(BasicBSONObject logLine, HttpServletRequest request, String key,
	        String headerName) {
		String value = request.getHeader(headerName);
		if (value != null)
			logLine.put(key, value);
	}

	public static void addHeadersIfPresent(BasicBSONObject logLine, HttpServletRequest request, MongoLoggingKeys key,
	        String headerName) {
		addHeadersIfPresent(logLine, request, key.getPrettyName(), headerName);
	}

	public BasicDBObject createRapportrMessage() {
		BasicDBObject mongoDoc = new BasicDBObject();
		mongoDoc.put("locked_by", null);
		mongoDoc.put("locked_at", null);
		mongoDoc.put("last_error", null);
		mongoDoc.put("attempts", 0);
		mongoDoc.put("priority", 0);
		mongoDoc.put("no_meta_infos", false);
		mongoDoc.put("irc_channel", ircErrorChannel);
		mongoDoc.put("class", "Hash");
		mongoDoc.put("store", true);

		BasicDBObject obj = new BasicDBObject();
		mongoDoc.put("obj", obj);
		obj.put("facility", "lackr");
		obj.put("app", "lackr");
		obj.put("created_at", new Date());
		obj.put("level", "error");
		obj.put("type", "exception");
		obj.put("hostname", hostname);
		obj.put("grid", grid);

		BasicDBObject data = new BasicDBObject();
		obj.put("data", data);

		return mongoDoc;
	}

	public void rapportrException(HttpServletRequest request, String errorDescription) {
		if (mongoRapportrQueue == null)
			return;

		BasicDBObject mongoDoc = createRapportrMessage();
		BSONObject obj = (BSONObject) mongoDoc.get("obj");
		BSONObject data = (BSONObject) obj.get("data");

		data.put("description", errorDescription);

		BasicDBObject env = new BasicDBObject();
		data.put("env", env);
		env.put("root_url", request.getRequestURL().toString());
		addHeadersIfPresent(env, request, "HTTP_USER_AGENT", "User-Agent");
		addHeadersIfPresent(env, request, "REMOTE_ADDR", "X-Forwarded-For");
		addHeadersIfPresent(env, request, "HTTP_X_FTN_OPERATIONID", "X-Ftn-OperationId");
		addHeadersIfPresent(env, request, CLIENT_ID, "X-Ftn-User");
		addHeadersIfPresent(env, request, SESSION_ID, "X-Ftn-Session");

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String cname = cookie.getName();
				if (cname.equals("login_session")) {
					env.put(LOGIN_SESSION.getPrettyName(), cookie.getValue());
				}
			}
		}
		obj.put("message", errorDescription.subSequence(0, errorDescription.indexOf('\n')));
		mongoRapportrQueue.save(mongoDoc);
	}

	@Override
    public void warnMessage(String message, String description) {
		BasicDBObject mongoDoc = createRapportrMessage();
		BSONObject obj = (BSONObject) mongoDoc.get("obj");
		BSONObject data = (BSONObject) obj.get("data");

		if (description != null)
			data.put("description", description);
		obj.put("message", message);
		mongoRapportrQueue.save(mongoDoc);
	}

	public static BasicDBObject accessLogLineTemplate(HttpServletRequest request, String facility) {
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

	public BackendClient getClient() {
		return client;
	}

	public void setClient(BackendClient client) {
		this.client = client;
	}

	private DBCollection getCollection(String mongoPath) throws NumberFormatException, UnknownHostException,
	        MongoException {
		if (!StringUtils.hasText(mongoPath)) {
			return null;
		}
		String[] pathComponents = mongoPath.split("/");
		if (pathComponents.length != 3)
			throw new IllegalArgumentException("Mongo Logging Path not compliant with spec in \"" + mongoPath
			        + "\", format is host:port/database/collection.");

		String[] hostComponents = pathComponents[0].split(":");
		if (hostComponents.length != 2)
			throw new IllegalArgumentException(
			        "Mongo Logging Hostname not compliant with spec, should be host:port (is \"" + pathComponents[0]
			                + "\" ).");

		Mongo logConnection = new Mongo(hostComponents[0], Integer.parseInt(hostComponents[1]));
		return logConnection.getDB(pathComponents[1]).getCollection(pathComponents[2]);
	}

	public void setMongoAccessLogCollection(String mongoLoggingPath) throws NumberFormatException,
	        UnknownHostException, MongoException {
		accessLogCollection = getCollection(mongoLoggingPath);
	}

	public void setMongoRapportrQueue(String mongoLoggingPath) throws NumberFormatException, UnknownHostException,
	        MongoException {
		mongoRapportrQueue = getCollection(mongoLoggingPath);
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
				hosts[i] = new Host(this, hostnames[i], probeUrl);
			ring = new HashRing(this, hosts);
		}
	}

	public void setLogCollection(DBCollection logCollection) {
		this.accessLogCollection = logCollection;
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

	public void log(final BasicDBObject logLine) {
		if (accessLogCollection != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					accessLogCollection.save(logLine);
				}
			});
		}
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}

	public ObjectMapper getJacksonObjectMapper() {
		return objectMapper;
	}

	public void setGrid(String grid) {
		this.grid = grid;
	}

	public void setIrcErrorChannel(String ircErrorChannel) {
		this.ircErrorChannel = ircErrorChannel;
	}
}
