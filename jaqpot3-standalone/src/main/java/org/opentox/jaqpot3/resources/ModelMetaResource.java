package org.opentox.jaqpot3.resources;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.engine.model.SimpleModelMetaFinder;
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
public class ModelMetaResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("model", "model_id", "meta");

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
        updateSecondaryId(template);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        SimpleModelMetaFinder finder = new SimpleModelMetaFinder(SimpleModelMetaFinder.SEARCH_MODE.valueOf(secondaryId.toUpperCase(Locale.ENGLISH)), primaryId);
        try {
            DbListStreamPublisher publisher = new DbListStreamPublisher();
            publisher.setBaseUri(null);
            publisher.setMedia(variant.getMediaType());
            return publisher.process(finder);
        } catch (JaqpotException ex) {
            return errorReport(ex, "DbError", "Error while getting data from the DB",
                    variant.getMediaType(), false);
        } finally {
            try {
                finder.close();
            } catch (DbException ex) {
                return errorReport(ex, "DbError", "Error while getting data from the DB - Finder uncloseable",
                        variant.getMediaType(), false);
            }
        }

    }
}
