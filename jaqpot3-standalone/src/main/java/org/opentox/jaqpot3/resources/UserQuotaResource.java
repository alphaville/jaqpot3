package org.opentox.jaqpot3.resources;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.resources.publish.UriListPublishable;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.core.html.HTMLDivBuilder;
import org.opentox.toxotis.core.html.HTMLPage;
import org.opentox.toxotis.core.html.HTMLTable;
import org.opentox.toxotis.core.html.impl.HTMLPageImpl;
import org.opentox.toxotis.core.html.impl.HTMLTextImpl;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.bibtex.CountBibTeX;
import org.opentox.toxotis.database.engine.model.CountModel;
import org.opentox.toxotis.database.engine.task.CountTasks;
import org.opentox.toxotis.database.engine.user.FindUser;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.policy.Policy;
import org.opentox.toxotis.util.aa.policy.PolicyManager;
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
public class UserQuotaResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("user", "user_id", "meta");

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_WADL,
                MediaType.TEXT_URI_LIST);

        Set<Method> allowedMethods = getAllowedMethods();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.OPTIONS);
        setAllowedMethods(allowedMethods);
        parseStandardParameters();
        updatePrimaryId(template);
        updateSecondaryId(template);
        super.doInit();
    }

    private Representation get_quota(Variant variant, User u) throws ResourceException {
        CountModel modelCounter = new CountModel();
        CountTasks taskCounter = new CountTasks();
        CountTasks activeTaskCounter = new CountTasks();
        CountBibTeX bibtexCounter = new CountBibTeX();
        int countModels = -1;
        int countTasks = -1;
        int countActiveTasks = -1;
        int countBibTeX = -1;
        try {
            modelCounter.setWhere("createdBy='" + primaryId + "'");
            countModels = modelCounter.count();
            taskCounter.setWhere("createdBy='" + primaryId + "'");
            countTasks = taskCounter.count();
            activeTaskCounter.setWhere("createdBy='" + primaryId + "' AND status IN ('QUEUED','RUNNING')");
            countActiveTasks = activeTaskCounter.count();
            bibtexCounter.setWhere("createdBy='" + primaryId + "'");
            countBibTeX = bibtexCounter.count();
        } catch (DbException ex) {
        } finally {
            try {
                modelCounter.close();
            } catch (DbException ex) {
                Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                taskCounter.close();
            } catch (DbException ex) {
                Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                activeTaskCounter.close();
            } catch (DbException ex) {
                Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                bibtexCounter.close();
            } catch (DbException ex) {
                Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        HTMLDivBuilder builder = new HTMLDivBuilder();
        builder.addSubHeading("User Quota Report");
        builder.addSubSubHeading(getReference().toString());

        double percentage = 100 * (((double) countModels / u.getMaxModels()) * 0.8 + ((double) countBibTeX / u.getMaxBibTeX()) * 0.2);

        builder.addSubSubHeading("Quota Summary");
        HTMLTable table = builder.addTable(2);
        table.setAtCursor(new HTMLTextImpl("User ID").formatBold(true)).setTextAtCursor("<a href=\"" + Configuration.getBaseUri().augment("user", primaryId) + "\">" + primaryId + "</a>").
                setAtCursor(new HTMLTextImpl("Usage Index").formatBold(true)).setTextAtCursor(String.format("%3.2f", percentage) + "%").
                setAtCursor(new HTMLTextImpl("Nb. Models").formatBold(true)).setTextAtCursor(countModels + " / " + u.getMaxModels()).
                setAtCursor(new HTMLTextImpl("Nb. BibTeX").formatBold(true)).setTextAtCursor(countBibTeX + " / " + u.getMaxBibTeX()).
                setAtCursor(new HTMLTextImpl("Nb. Tasks").formatBold(true)).setTextAtCursor(countTasks + "").
                setAtCursor(new HTMLTextImpl("Nb. Active Tasks").formatBold(true)).setTextAtCursor(countActiveTasks + " / " + u.getMaxParallelTasks());


        table.setCellPadding(5).
                setCellSpacing(2).
                setTableBorder(1).
                setColWidth(1, 350).
                setColWidth(2, 350);
        HTMLPage document = new HTMLPageImpl();
        document.getHTMLHead().setTitle("User Quota").setAuthor("Jaqpot3");
        document.getHtmlBody().addComponent(builder.getDiv());
        return new StringRepresentation(document.toString(), MediaType.TEXT_HTML);
    }
    

    @Override
    protected Representation get(Variant variant) throws ResourceException {

        if (getUserToken() == null) {
            toggleForbidden();
            return errorReport("Anonymous", "Please login in order to see your quota", "Anonynous users are not allowed to see anyone's quota",
                    variant.getMediaType(), false);
        }

        User u = null;
        FindUser finder = new FindUser();
        finder.setWhere("uid='" + primaryId + "'");
        try {
            IDbIterator<User> iterator = finder.list();
            if (iterator.hasNext()) {
                u = iterator.next();
            }
            u.setUri(Configuration.getBaseUri().augment("user", primaryId));
        } catch (DbException ex) {
        }

        if (u == null) {
            // NOT FOUND!!!
        }
        try {
            if (!u.getUid().startsWith("guest@") && !u.getUid().equals(getUserToken().getUser().getUid())) {
                toggleUnauthorized();
                return errorReport("Unauthorized", "You are not authorized to see this user's private information",
                        "Registered Users are allowed to see private information for themselves and for the user 'guest' which "
                        + "corresponds to the public user (anyone). Users are not allowed to access each other's private aerea therefore "
                        + "see their quota on the server or SSO policies.", variant.getMediaType(), false);
            }
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ToxOtisException ex) {
            Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        if ("quota".equalsIgnoreCase(secondaryId)) {
            return get_quota(variant, u);
        } else {
            toggleNotFound();
            return get_quota(variant, u);
        }
    }
}
