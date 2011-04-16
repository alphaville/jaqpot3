package org.opentox.jaqpot3.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.parameter.FindParameter;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ParameterResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("parameter", "parameter_id", null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModelResource.class);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_RDF_XML,
                MediaType.register("application/rdf+xml-abbrev", NEWLINE),
                MediaType.APPLICATION_RDF_TURTLE,
                MediaType.APPLICATION_PDF,
                MediaType.TEXT_URI_LIST,
                MediaType.TEXT_RDF_N3,
                MediaType.TEXT_RDF_NTRIPLES,
                MediaType.APPLICATION_XML);
        parseStandardParameters();
        updatePrimaryId(template);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        FindParameter finder = new FindParameter(Configuration.getBaseUri());
        IDbIterator parameterIterator = null;
        finder.setSearchById(primaryId);

        try {
            parameterIterator = finder.list();
            if (parameterIterator.hasNext()) {
                Parameter prm = (Parameter) parameterIterator.next();
                Publisher p = new Publisher(variant.getMediaType());
                try {
                    return p.createRepresentation(prm, true);
                } catch (JaqpotException ex) {
                    Logger.getLogger(ParameterResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (DbException ex) {
            Logger.getLogger(ParameterResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return super.get(variant);
    }
}
