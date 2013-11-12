package com.fotonauts.lackr;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.Interpolr;

public class InterpolrProxy extends BaseProxy {

    static Logger log = LoggerFactory.getLogger(InterpolrProxy.class);

    protected Interpolr interpolr;

    public InterpolrProxy() {
    }

    public Interpolr getInterpolr() {
        return interpolr;
    }

    public void setInterpolr(Interpolr interpolr) {
        this.interpolr = interpolr;
    }

    protected BaseFrontendRequest createLackrFrontendRequest(HttpServletRequest request) {
        return new InterpolrFrontendRequest(this, request);        
    }
    
    @Override
    protected void doStart() throws Exception {
        log.debug("Starting...");
        super.doStart();
        interpolr.start();
        log.debug("Started.");
    }
    
    @Override
    public void doStop() throws Exception {
        log.debug("Stopping...");
        interpolr.stop();
        super.doStop();
        log.debug("Stopped...");
    }
}
