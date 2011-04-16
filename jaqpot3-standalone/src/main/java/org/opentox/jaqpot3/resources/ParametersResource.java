package org.opentox.jaqpot3.resources;

import java.util.Set;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.engine.parameter.ListParameter;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ParametersResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("parameter", null, null);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_RDF_XML,
                MediaType.APPLICATION_WADL,
                MediaType.APPLICATION_RDF_TURTLE,
                MediaType.TEXT_URI_LIST);

        Set<Method> allowedMethods = getAllowedMethods();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.OPTIONS);
        setAllowedMethods(allowedMethods);
        parseStandardParameters();
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        ListParameter lister = new ListParameter();

        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setMedia(variant.getMediaType());
        publisher.setBaseUri(Configuration.getBaseUri().augment("parameter"));
        try {
            return publisher.process(lister);
        } catch (JaqpotException ex) {
            return errorReport(ex, "DbError", "Error while getting data from the DB",
                    variant.getMediaType(), false);
        }
    }
}
