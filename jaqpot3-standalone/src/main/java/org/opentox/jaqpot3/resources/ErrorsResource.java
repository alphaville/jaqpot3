package org.opentox.jaqpot3.resources;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.error.ListError;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;


/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ErrorsResource extends JaqpotResource{

    public static final URITemplate template = new URITemplate("error", null, null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModelsResource.class);
    private String creator;
    private String dataset_uri;
    private String dependent_feature;
    private String algorithm_uri;
    private String algorithm;

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
        creator = parseParameter("creator");
        dataset_uri = parseParameter("dataset_uri");
        dependent_feature = parseParameter("dependent_feature");
        algorithm_uri = parseParameter("algorithm_uri");
        algorithm = parseParameter("algorithm");
    }


    @Override
    protected Representation get(Variant variant) throws ResourceException {

        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }

        ListError errorLister = new ListError();
        
        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setMedia(variant.getMediaType());
        publisher.setBaseUri(Configuration.getBaseUri().augment("error"));
        try {
            return publisher.process(errorLister);
        } catch (JaqpotException ex) {
            Logger.getLogger(ModelsResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new StringRepresentation("Under Construction");
    }



}