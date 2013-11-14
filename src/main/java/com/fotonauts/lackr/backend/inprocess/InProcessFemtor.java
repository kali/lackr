package com.fotonauts.lackr.backend.inprocess;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.Filter;

import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;

// FIXME rename me (InProcessBackend)
// FIXME reference to Femtor all around
public class InProcessFemtor extends AbstractLifeCycle implements Backend {

    static Logger log = LoggerFactory.getLogger(InProcessFemtor.class);

	Filter filter;
	private String femtorHandlerClass;
	private String femtorJar;
	private URLClassLoader loader;
	
	@SuppressWarnings("deprecation")
	public void init() throws Exception {
	    log.debug("initing");
		Class<?> c = null;
		if(StringUtil.isNotBlank(femtorJar)) {
			loader = URLClassLoader.newInstance(new URL[] { new File(femtorJar).toURL() }, getClass().getClassLoader());
			c = loader.loadClass(getFemtorHandlerClass());
		} else {
			c = ClassLoader.getSystemClassLoader().loadClass(getFemtorHandlerClass());
		}
		filter = (Filter) c.getConstructor().newInstance();
        log.debug("inited");
	}

	public void setfemtorJar(String femtorJar) {
		this.femtorJar = femtorJar;
	}

	@Override
	public LackrBackendExchange createExchange(LackrBackendRequest request) {
		return new FemtorExchange(this, request);
	}

	@Override
	public void dumpStatus(PrintStream ps) {
		ps.format("Femtor HTTP Client\n\n");
	}

	public String getFemtorHandlerClass() {
		return femtorHandlerClass;
	}

	public void setFemtorHandlerClass(String femtorHandlerClass) {
		this.femtorHandlerClass = femtorHandlerClass;
	}

	@Override
    public String getName() {
        return "in-process-femtor";
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), getName());
    }

    @Override
    protected void doStart() throws Exception {
        init();
    }
}
