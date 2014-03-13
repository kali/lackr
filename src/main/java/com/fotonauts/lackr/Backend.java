package com.fotonauts.lackr;

import org.eclipse.jetty.util.component.LifeCycle;

/**
 * Represents a backend server or any other upstream system lackr will try to delegate a {@link LackrBackendRequest} too. 
 *  
 * @author kali
 *
 */
public interface Backend extends LifeCycle {

    /**
     * The "interesting" method of the backend.
     * 
     * @param incomingServletRequest the incomingServletRequest specification to process
     * @return a {@link LackrBackendExchange} materialising the transaction with the Backend.
     */
    public LackrBackendExchange createExchange(LackrBackendRequest request);

    /**
     * For debugging and logging purposes.
     */
    public String getName();

    /**
     * Check the current status of the backend stack.
     * 
     * @return true if the backend is in a good state
     */
    public boolean probe();
}
