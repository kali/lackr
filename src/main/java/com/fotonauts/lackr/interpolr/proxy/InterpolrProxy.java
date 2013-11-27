package com.fotonauts.lackr.interpolr.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.BaseFrontendRequest;
import com.fotonauts.lackr.BaseProxy;
import com.fotonauts.lackr.CompletionListener;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.LackrPresentableError;
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

    protected BaseFrontendRequest createFrontendRequest(HttpServletRequest request) {
        return new InterpolrFrontendRequest(this, request);
    }

    // only called for the main request, not for esi sub-fragments.
    @Override
    public void onBackendRequestDone(BaseFrontendRequest baseFrontendRequest) {
        InterpolrFrontendRequest frontendRequest = (InterpolrFrontendRequest) baseFrontendRequest;
        log.debug("Request completion for root: {}", getPathAndQuery(frontendRequest.getRequest()));
        ProxyInterpolrScope scope = new ProxyInterpolrScope(frontendRequest);
        frontendRequest.setRootScope(scope);
        scope.setRequest(frontendRequest.getRootRequest());
        getInterpolr().processResult(frontendRequest.getRootScope());
        log.debug("Interpolation done for root: {}", getPathAndQuery(baseFrontendRequest.getRequest()));
        if (frontendRequest.getPendingCount() == 0) {
            log.debug("No ESI found for {}.", getPathAndQuery(baseFrontendRequest.getRequest()));
            yieldRootRequestProcessing(frontendRequest);
        }
    }

    LackrBackendRequest createSubRequest(InterpolrFrontendRequest frontendRequest, LackrBackendRequest dadRequest, String url,
            String format, CompletionListener listener) {
        HashMap<String, Object> attributes = new HashMap<>(1);
        attributes.put("PARENT", dadRequest);
        LackrBackendRequest req = new LackrBackendRequest(frontendRequest, "GET", url, null, dadRequest.getFields(), attributes, listener);
        return req;
    }

    void scheduleSubBackendRequest(LackrBackendRequest req) {
        scheduleBackendRequest(req);
    }

    @Override
    protected void preflightCheck(BaseFrontendRequest baseFrontendRequest) {
        InterpolrFrontendRequest frontendRequest = (InterpolrFrontendRequest) baseFrontendRequest;
        log.debug("Entering preflight check for {}", frontendRequest);
        try {
            getInterpolr().preflightCheck(frontendRequest);
        } catch (Throwable e) {
            frontendRequest.addBackendExceptions(LackrPresentableError.fromThrowable(e));
        }
    }

    protected void yieldRootRequestProcessing(InterpolrFrontendRequest frontendRequest) {
        log.debug("Yield root request.");
        super.onBackendRequestDone(frontendRequest);
    }

    @Override
    protected void writeResponse(BaseFrontendRequest baseFrontendRequest, HttpServletResponse response) throws IOException {
        InterpolrFrontendRequest frontendRequest = (InterpolrFrontendRequest) baseFrontendRequest;
        if (frontendRequest.getPendingCount() > 0)
            frontendRequest.addBackendExceptions(new LackrPresentableError("There is unfinished business with backends..."));

        super.writeResponse(baseFrontendRequest, response);
    }

    @Override
    protected void writeContentTo(BaseFrontendRequest req, OutputStream out) throws IOException {
        ((InterpolrFrontendRequest) req).getRootScope().getParsedDocument().writeTo(out);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        interpolr.start();
    }

    @Override
    public void doStop() throws Exception {
        interpolr.stop();
        super.doStop();
    }

}
