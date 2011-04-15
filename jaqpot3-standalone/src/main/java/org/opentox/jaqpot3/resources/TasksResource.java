package org.opentox.jaqpot3.resources;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.DbReader;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.task.ListTasks;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class TasksResource extends JaqpotResource {

    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TasksResource.class);
    public static final URITemplate template = new URITemplate("task", null, null);
    private String status = null;

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
                MediaType.TEXT_RDF_NTRIPLES);
        parseStandardParameters();
        status = getRequest().getResourceRef().getQueryAsForm().getFirstValue("status");
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {

        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }


        ListTasks lister = new ListTasks();
        IDbIterator<String> list = null;
        try {
            list = lister.list();
        } catch (DbException ex) {
        }

        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setMedia(variant.getMediaType());
        publisher.setBaseUri(Configuration.getBaseUri().augment("task"));
        try {
            return publisher.process(lister);
        } catch (JaqpotException ex) {
            return errorReport(ex, "DbError", "Error while getting data from the DB",
                    variant.getMediaType(), false);
        }


    }
}
