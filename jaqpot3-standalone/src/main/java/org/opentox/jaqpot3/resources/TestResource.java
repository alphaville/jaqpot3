package org.opentox.jaqpot3.resources;

import java.io.IOException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class TestResource extends JaqpotResource {

    @Override
    protected void doInit() throws ResourceException {
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
        super.doInit();
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        toggleRemoteError();
        Throwable t1 = new NullPointerException("message 1");
        Throwable t2 = new IOException("message 2");
        Throwable t3 = new ArrayIndexOutOfBoundsException("message 3"); // ("message 3");
        t2.initCause(t1);
        t3.initCause(t2);
        return errorReport(t3, "Exception", "this is a message", MediaType.TEXT_HTML, false);
    }

    
}
