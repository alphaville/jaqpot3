package org.opentox.jaqpot3.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.core.html.GoogleAnalytics;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.restlet.data.Language;
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
public class LoginResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("login", null, null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoginResource.class);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    private String getLoginForm(String userName, String pass, String token) {
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html><head><title>Jaqpot Login Page</title>");
        formBuilder.append(GoogleAnalytics.getGAjs());
        formBuilder.append("</head>");
        formBuilder.append("<body>");
        formBuilder.append("<h1>Login</h1>");


        formBuilder.append("<form method=\"POST\" actionUri=\"./\">");
        formBuilder.append("<table>");
        formBuilder.append("<tr>");
        formBuilder.append("<td>Username</td> <td><input type=\"text\" size=\"15\" maxlength=\"40\" name=\"username\" ");
        if (userName != null) {
            formBuilder.append("value=\"");
            formBuilder.append(userName);
            formBuilder.append("\"");
        }
        formBuilder.append("> </td>");
        formBuilder.append("</tr><tr>");
        formBuilder.append("<td>Password</td> <td><input type=\"password\" size=\"15\" maxlength=\"40\" name=\"password\"></td>");
        formBuilder.append("</tr><tr>");
        formBuilder.append("<td>Token</td> <td>");
        formBuilder.append(token);
        formBuilder.append("</td>");
        formBuilder.append("</tr>");
        formBuilder.append("</table>");
        formBuilder.append("<input type=\"submit\" value=\"Login\">");

        formBuilder.append("</form><br/>");

        formBuilder.append("<p>Click <a href=\"..\">here</a> to go back to the main page</p>");
        formBuilder.append("</body></html>");
        return formBuilder.toString();
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {

        String tokenAsCookie = getRequest().getCookies().getValues("subjectid");
        String userName = null;
        if (tokenAsCookie != null) {
            AuthenticationToken at = new AuthenticationToken(tokenAsCookie);
            try {
                User u = at.getUser();
                userName = u.getUid().split("@")[0];
            } catch (ServiceInvocationException ex) {
                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ToxOtisException ex) {
                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new StringRepresentation(getLoginForm(userName, "123456", tokenAsCookie), MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        IClientInput clientInput = new ClientInput(entity);
        String un = clientInput.getFirstValue("username");
        String ps = clientInput.getFirstValue("password");
        String tok = null;

        try {
            String oldToken = getCookieSettings().getFirstValue("subjectid");
            if (oldToken != null) {
                new AuthenticationToken(oldToken).invalidate();
            }
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            AuthenticationToken at = new AuthenticationToken(un, ps);
            ps = null;

            getCookieSettings().removeAll("subjectid");
            tok = at.stringValue();
            getCookieSettings().add("subjectid", tok);// set it as a cookie
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (tok == null) {
            tok = "Invalid Credentials - Unauthorized";
        }
        return new StringRepresentation(getLoginForm(un, "1x2x3x4x5x", tok), MediaType.TEXT_HTML);
    }
}
