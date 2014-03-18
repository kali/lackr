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
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.rope.Document;

/**
 * Extends {@link BaseProxy} with {@link Interpolr} support, the gist of it being ESI expansion.
 * 
 * @author kali
 *
 */
public class InterpolrProxy extends BaseProxy {

    static Logger log = LoggerFactory.getLogger(InterpolrProxy.class);

    protected Interpolr interpolr;

    public InterpolrProxy() {
    }

    /**
     * Get the interpolr processor.
     * 
     * @return the interpolr processor.
     */
    public Interpolr getInterpolr() {
        return interpolr;
    }

    /**
     * Set the interpolr processor.
     * 
     * @param interpolr the interpolr processor
     */
    public void setInterpolr(Interpolr interpolr) {
        this.interpolr = interpolr;
    }

    /**
     * Creates an {@link InterpolrFrontendRequest}, instead of a plain {@link BaseFrontendRequest}.
     * 
     * @return a {@link InterpolrFrontendRequest}
     */
    protected BaseFrontendRequest createFrontendRequest(HttpServletRequest request) {
        return new InterpolrFrontendRequest(this, request);
    }

    /**
     * Start interpolr processing of the main request.
     * 
     * <p>Replace the {@link BaseProxy} callback that was only triggering a new container dispatch.
     * 
     * <p>It is not called for the inner queries.
     */
    @Override
    public void onBackendRequestDone(BaseFrontendRequest baseFrontendRequest) {
        InterpolrFrontendRequest frontendRequest = (InterpolrFrontendRequest) baseFrontendRequest;
        log.debug("Request completion for root: {}", getPathAndQuery(frontendRequest.getIncomingServletRequest()));
        InterpolrBackendRequest scope = new InterpolrBackendRequest(frontendRequest);
        frontendRequest.setRootScope(scope);
        scope.setRequest(frontendRequest.getBackendRequest());
        getInterpolr().processResult(frontendRequest.getRootScope());
        frontendRequest.setRootRequestDone();
        log.debug("Interpolation done for root: {}", getPathAndQuery(baseFrontendRequest.getIncomingServletRequest()));
        if (frontendRequest.getPendingCount() == 0) {
            log.debug("No ESI found for {}.", getPathAndQuery(baseFrontendRequest.getIncomingServletRequest()));
            yieldRootRequestProcessing(frontendRequest);
        }
    }

    /**
     * Create a sub request for fragment processing.
     * 
     * <p>it is possible to override to set headers to the fragment request.
     * 
     * @param frontendRequest
     * @param dadRequest
     * @param url
     * @param format
     * @param listener
     * @return
     */
    protected LackrBackendRequest createSubRequest(InterpolrFrontendRequest frontendRequest, LackrBackendRequest dadRequest,
            String url, String format, CompletionListener listener) {
        HashMap<String, Object> attributes = new HashMap<>(1);
        attributes.put("PARENT", dadRequest);
        attributes.put("FORMAT", format);
        LackrBackendRequest req = new LackrBackendRequest(frontendRequest, "GET", url, null, buildHttpFields(frontendRequest),
                attributes, listener);
        return req;
    }

    void scheduleSubBackendRequest(LackrBackendRequest req) {
        scheduleBackendRequest(req);
    }

    /**
     * Called when all sub-fragments of the request have been processed.
     * 
     * <p>Just forward call to BaseProxy.onBackendRequestDone to come back to the 
     *    base cycle.
     * 
     * @param frontendRequest 
     */
    protected void yieldRootRequestProcessing(InterpolrFrontendRequest frontendRequest) {
        log.debug("Yield root incomingServletRequest.");
        super.onBackendRequestDone(frontendRequest);
    }

    /**
     * Run a preflight check on the query before yielding to the base implementation.
     */
    @Override
    protected void writeResponse(BaseFrontendRequest baseFrontendRequest, HttpServletResponse response) throws IOException {
        InterpolrFrontendRequest frontendRequest = (InterpolrFrontendRequest) baseFrontendRequest;
        if (frontendRequest.getPendingCount() > 0) {
            frontendRequest.addError(new LackrPresentableError("Unfinished business with backends: \n"
                    + frontendRequest.dumpCurrentState()));
        }

        if (frontendRequest.getErrors().isEmpty())
            preflightCheck(baseFrontendRequest);

        super.writeResponse(baseFrontendRequest, response);
    }

    /**
     * Performs the preflight check on the request.
     * 
     * <p>Called in the container thread, just before writing the response.
     * 
     * @param baseFrontendRequest
     */
    protected void preflightCheck(BaseFrontendRequest baseFrontendRequest) {
        InterpolrFrontendRequest frontendRequest = (InterpolrFrontendRequest) baseFrontendRequest;
        log.debug("Entering preflight check for {}", frontendRequest);
        try {
            getInterpolr().preflightCheck(frontendRequest);
        } catch (Throwable e) {
            frontendRequest.addError(LackrPresentableError.fromThrowable(e));
        }
    }

    /**
     * Instead of using the root request raw body, use the Interpolr-expanded one.
     */
    @Override
    protected void writeContentTo(BaseFrontendRequest req, OutputStream out) throws IOException {
        InterpolrScope scope = ((InterpolrFrontendRequest) req).getRootScope();
        Document doc = scope.getParsedDocument();
        log.debug("Doc for {}#{} is {}", scope, scope.hashCode(), doc);
        doc.writeTo(out);
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
