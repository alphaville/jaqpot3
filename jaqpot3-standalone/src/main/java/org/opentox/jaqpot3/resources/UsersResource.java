package org.opentox.jaqpot3.resources;

import java.util.Set;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.DbReader;
import org.opentox.toxotis.database.engine.user.ListUsers;
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
public class UsersResource extends JaqpotResource{
    public static final URITemplate template = new URITemplate("user", null, null);

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
        super.doInit();
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        ListUsers lister = new ListUsers();
        lister.setMode(ListUsers.ListUsersMode.BY_UID);
        
        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setMedia(variant.getMediaType());
        publisher.setBaseUri(Configuration.getBaseUri().augment("user"));
        try {
            return publisher.process(lister);
        } catch (JaqpotException ex) {
            return errorReport(ex, "DbError", "Error while getting data from the DB",
                    variant.getMediaType(), false);
        }
    }


    

}