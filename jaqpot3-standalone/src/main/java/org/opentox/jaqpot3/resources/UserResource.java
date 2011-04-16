package org.opentox.jaqpot3.resources;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.user.FindUser;
import org.opentox.toxotis.database.exception.DbException;
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
public class UserResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("user", "user_id", null);

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
        super.doInit();
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        System.out.println(primaryId);
        FindUser findUser = new FindUser();
        findUser.setWhere("uid='" + primaryId + "'");
        try {
            IDbIterator<User> iterator = findUser.list();
            User u = null;
            if (iterator.hasNext()) {
                u = iterator.next();
            }
            u.setUri(getCurrentVRI());
            Publisher p = new Publisher(variant.getMediaType());
            return p.createRepresentation(u, true);
        } catch (JaqpotException ex) {
            ex.printStackTrace();
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbException ex) {
            ex.printStackTrace();
        } finally {
        }
        return super.get(variant);
    }
}
