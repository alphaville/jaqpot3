package org.opentox.jaqpot3.resources;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.resources.publish.UriListPublishable;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
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
public class PolicyListResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("policy", null, null);

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
    public Representation get(Variant variant) {
        if (getUserToken() == null) {
            toggleSeeOther(Configuration.getBaseUri().augment("login").
                    addUrlParameter("redirect", getCurrentVRI().toString()).toString());
            return new StringRepresentation("");
        }
        try {
            UriListPublishable publishable = new UriListPublishable(PolicyManager.listPolicyUris(null, getUserToken()));
            publishable.setBaseUri(getCurrentVRI());
            publishable.setHeading("Policies for user " + getUser().getUid());
            publishable.setMediaType(variant.getMediaType());
            Representer representer = new Representer();
            return representer.process(publishable);
        } catch (JaqpotException ex) {
            Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
