package org.opentox.jaqpot3.resources;

import org.opentox.jaqpot3.www.URITemplate;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class LoginResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("login", null, null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoginResource.class);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        StringBuilder formBuilder = new StringBuilder();
        
        return super.get(variant);
    }


}
