package org.opentox.jaqpot3.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IClient;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.client.collection.Services;
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
 */
public class PolicyResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("policy", "policy_id", null);
    private String method;

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_WADL);

        Set<Method> allowedMethods = getAllowedMethods();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.OPTIONS);
        setAllowedMethods(allowedMethods);
        parseStandardParameters();
        updatePrimaryId(template);
        method = parseParameter("method");
        super.doInit();
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        if ("delete".equals(method)) {
            return delete(variant);
        }
        IClientInput input = new ClientInput(entity);
        String policy = input.getFirstValue("policy");
        boolean isOK = policy.contains("DOCTYPE Policies PUBLIC");
        BufferedReader reader = new BufferedReader(
                new StringReader(policy));
        StringBuilder sb = new StringBuilder();
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                if (str.length() > 0) {
                    sb.append(str.trim());
                    if (!isOK) {
                        if (str.startsWith("<?xml")) {
                            sb.append("\n<!DOCTYPE Policies PUBLIC \"-//Sun Java System Access Manager7.1 2006Q3 "
                                    + "Admin CLI DTD//EN\" \"jar://com/sun/identity/policy/policyAdmin.dtd\">");
                        }
                        isOK = true;
                    }
                    sb.append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        policy = sb.toString();
        try {
            PolicyManager.deleteRemotePolicy(null, primaryId, getUserToken());
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        IPostClient scp = ClientFactory.createPostClient(Services.SingleSignOn.ssoPolicyOld());
        scp.setContentType(Media.APPLICATION_XML);
        scp.setPostable(policy.trim(), true);
        scp.authorize(getUserToken());
        try {
            scp.post();
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                scp.close();
            } catch (IOException ex) {
                Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new StringRepresentation("<html><body>Policy Updated Successfully</body></html>",
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        String html = "<html>"
                + "<body>"
                + "<h2>Policy " + primaryId + "</h2>"
                + "<form method=\"POST\" action=\"\">"
                + "<p><textarea name=\"policy\" rows=\"10\" cols=\"90\">%s</textarea></p>"
                + "<input type=\"submit\" value=\"Update\">"
                + "</form>"
                + "<form method=\"POST\" action=\"?method=DELETE\">"
                + "<input type=\"submit\" value=\"Delete\">"
                + "</form>"
                + "</body>"
                + "</html>";
        IClient client = ClientFactory.createGetClient(Services.SingleSignOn.ssoPolicy());
        client.authorize(getUserToken());
        client.addHeaderParameter("id", primaryId);
        try {
            InputStream stream = client.getRemoteStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (IOException ex) {
                Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                try {
                    reader.close();
                    stream.close();
                    client.close();
                } catch (IOException ex) {
                    Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return new StringRepresentation(String.format(html, sb), variant.getMediaType());
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return super.get(variant);
    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {
        try {
            PolicyManager.deleteRemotePolicy(null, primaryId, getUserToken());
            return new StringRepresentation("Deleted!!!");
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(PolicyResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return super.delete(variant);
    }
}
