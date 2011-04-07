package org.opentox.jaqpot3.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.ErrorReport;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.error.FindError;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ErrorResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("error", "error_id", null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ErrorResource.class);

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
        FindError errorFinder = new FindError(Configuration.getBaseUri());
        errorFinder.setSearchById(primaryId);
        ErrorReport foundErrorReport = null;
        try {
            IDbIterator<ErrorReport> errorReportsFound = errorFinder.list();
            if (errorReportsFound.hasNext()) {
                foundErrorReport = errorReportsFound.next();
            }
            errorReportsFound.close();
        } catch (DbException ex) {
            Logger.getLogger(ErrorResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                errorFinder.close();
            } catch (DbException ex) {
                Logger.getLogger(ErrorResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (foundErrorReport == null || (foundErrorReport != null && !foundErrorReport.isEnabled())) {
            toggleNotFound();
            return errorReport("ModelNotFound", "The error report you requested was not found in our database",
                    "The model with id " + primaryId + " was not found in the database",
                    variant.getMediaType(), false);
        }
        Publisher p = new Publisher(variant.getMediaType());
        try {
            return p.createRepresentation(foundErrorReport, true);
        } catch (JaqpotException ex) {
            logger.error(null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }
        
    }
}
