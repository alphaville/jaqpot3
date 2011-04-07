package org.opentox.jaqpot3.resources;

import java.net.URISyntaxException;
import java.util.List;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.pool.ExecutionPool;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.task.ListTasks;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.data.ReferenceList;
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


        ListTasks modelLister = new ListTasks();
        IDbIterator<String> list = null;
        try {
            list = modelLister.list();
        } catch (DbException ex) {

        }

        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setBaseUri(Configuration.getBaseUri().augment("task"));
        try {
            return publisher.process(list);
        } catch (JaqpotException ex) {

        }

        return new StringRepresentation("Under Construction");
    }

//    @Override
//    protected Representation delete(Variant variant) throws ResourceException {
//        int linesAffected = 0;
//        try {
//            User user = getUser();
//            if (user == null) {
//                toggleForbidden();
//                return errorReport("Forbidden", "No token is provided!",
//                        "Cannot apply a DELETE method on /task without providing your authentication token",
//                        variant.getMediaType(), false);
//            }
//
//            Query q = getNewSession().createQuery("SELECT uri from Task WHERE createdBy = :creator");
//            q.setString("creator", user.getUid());
//            List taskUris = q.list();
//            for (Object s : taskUris) {
//                try {
//                    ExecutionPool.POOL.cancel(new VRI(s.toString()).getId());
//                } catch (URISyntaxException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//
//            String hql = "DELETE FROM Task WHERE createdBy = :creatorUid";
//            Query query = getNewSession().createQuery(hql).setString("creatorUid", user.getUid());
//            linesAffected = query.executeUpdate();
//
//        } catch (ToxOtisException ex) {
//            logger.trace("Error trying to retrieve user data...", ex);
//        } catch (HibernateException ex) {
//            toggleServerError();
//            logger.warn("Hibernate exception caught while trying to DELETE all tasks for a user", ex);
//            return errorReport("DatabaseError", "Exceptional event related to task deletion from the database. "
//                    + "The issue is automatically logged and a team of jackals will tackle it asap.", null, variant.getMediaType(), true);
//        } finally {
//            try {
//                getNewSession().close();
//            } catch (HibernateException ex) {
//                toggleServerError();
//                logger.warn("Hibernate exception caught while trying to close a session", ex);
//                return errorReport("DatabaseError", "Exceptional event related to session management. "
//                        + "The issue is automatically logged and a team of jackals will tackle it asap.", null, variant.getMediaType(), true);
//            }
//        }
//        return new StringRepresentation(linesAffected + " tasks were deleted!" + NEWLINE, MediaType.TEXT_PLAIN);
//    }
}
