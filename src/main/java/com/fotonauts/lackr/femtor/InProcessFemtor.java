package com.fotonauts.lackr.femtor;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

import org.springframework.util.StringUtils;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.Gateway;

public class InProcessFemtor implements Backend {

	Filter filter;
	private String femtorHandlerClass;
	private String femtorJar;
	private URLClassLoader loader;
    private Gateway[] upstreamServices;
	

	public InProcessFemtor() throws Exception {
	    this.upstreamServices = new Gateway[] { new Gateway() {

            @Override
            public String getMBeanName() {
                return "InProcessFemtor";
            }
	        
	    } };
	    this.upstreamServices[0].start();
	}

	@SuppressWarnings("deprecation")
    @PostConstruct
	public void init() throws Exception {
		Class<?> c = null;
		if(StringUtils.hasText(femtorJar)) {
			loader = URLClassLoader.newInstance(new URL[] { new File(femtorJar).toURL() }, getClass().getClassLoader());
			c = loader.loadClass(getFemtorHandlerClass());
		} else {
			c = ClassLoader.getSystemClassLoader().loadClass(getFemtorHandlerClass());
		}
		filter = (Filter) c.getConstructor().newInstance();
	}

	public void setfemtorJar(String femtorJar) {
		this.femtorJar = femtorJar;
	}

	@Override
	public LackrBackendExchange createExchange(BackendRequest request) {
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
	public void stop() throws Exception {
	}

    @Override
    public Gateway[] getGateways() {
        return upstreamServices;
    }

    @Override
    public String getName() {
        return "in-process-femtor";
    }
}
