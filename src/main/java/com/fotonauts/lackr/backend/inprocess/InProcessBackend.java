package com.fotonauts.lackr.backend.inprocess;

import java.io.PrintStream;

import javax.servlet.Filter;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;

// FIXME reference to Femtor all around
public class InProcessBackend extends AbstractLifeCycle implements Backend {

    static Logger log = LoggerFactory.getLogger(InProcessBackend.class);

	private Filter filter;
	
	public InProcessBackend(Filter filter) {
	    this.filter = filter;
    }
	
    @Override
    protected void doStart() throws Exception {
	}

	@Override
	public LackrBackendExchange createExchange(LackrBackendRequest request) {
		return new InProcessExchange(this, request);
	}

	@Override
	public void dumpStatus(PrintStream ps) {
		ps.format("Femtor HTTP Client\n\n");
	}

	@Override
    public String getName() {
        return "in-process-femtor";
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), getName());
    }

    protected void setFilter(Filter filter) {
        this.filter = filter;
    }
    
    public Filter getFilter() {
        return filter;
    }

}
