/*
 *
 * Jaqpot - version 3
 *
 * The JAQPOT-3 web services are OpenTox API-1.2 compliant web services. Jaqpot
 * is a web application that supports model training and data preprocessing algorithms
 * such as multiple linear regression, support vector machines, neural networks
 * (an in-house implementation based on an efficient algorithm), an implementation
 * of the leverage algorithm for domain of applicability estimation and various
 * data preprocessing algorithms like PLS and data cleanup.
 *
 * Copyright (C) 2009-2012 Pantelis Sopasakis & Charalampos Chomenides
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * Pantelis Sopasakis
 * chvng@mail.ntua.gr
 * Address: Iroon Politechniou St. 9, Zografou, Athens Greece
 * tel. +30 210 7723236
 *
 */
package org.opentox.jaqpot3.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.core.html.GoogleAnalytics;
import org.opentox.toxotis.database.engine.user.AddUser;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
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
    private String redirect = null;

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
        redirect = parseParameter("redirect");
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
        formBuilder.append(token != null ? token : "You are not currently logged in");
        formBuilder.append("</td>");
        formBuilder.append("</tr>");
        formBuilder.append("</table>");
        formBuilder.append("<input type=\"submit\" value=\"Login\">");
        formBuilder.append("</form>");
        if (token != null && !token.isEmpty()) {
            formBuilder.append("<p>If you are already logged in and you need to logout click :</p>");
            formBuilder.append("<form method=\"POST\" actionUri=\"./\">");
            formBuilder.append("<input name=\"logout\" value=\"logout\" type=\"hidden\"");
            formBuilder.append("<input name=\"token\" value=\"").append(token).append("\" type=\"hidden\"");
            formBuilder.append("<input type=\"submit\" value=\"Logout\"/></form>");
            formBuilder.append("<br/>");
        }

        formBuilder.append("<p>Click <a href=\"..\">here</a> to go back to the main page</p>");
        if (token != null) {
            AuthenticationToken at = new AuthenticationToken(token);
            try {
                User u = at.getUser();
                String userid = u.getUid();
                formBuilder.append("<p>Check your profile <a href=\"/user/").
                        append(userid).append("\">here</a></p>");
            } catch (ServiceInvocationException ex) {
                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ToxOtisException ex) {
                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
                tokenAsCookie = null;
            } catch (ToxOtisException ex) {
                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new StringRepresentation(getLoginForm(userName, "123456", tokenAsCookie), MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        IClientInput clientInput = new ClientInput(entity);
        String purpose = clientInput.getFirstValue("logout");
        /*
         * LOG OUT
         */
        if ("logout".equals(purpose)) {
            String token = getRequest().getCookies().getValues("subjectid");
            if (token != null) {
                getCookieSettings().removeAll("subjectid");
                AuthenticationToken at = new AuthenticationToken(token);
                try {
                    at.invalidate();
                    toggleSeeOther(redirect);
                    return new StringRepresentation(getLoginForm("", "1x2x3x4x5x", ""),
                            MediaType.TEXT_HTML);
                } catch (ServiceInvocationException ex) {
                    getResponse().setStatus(Status.valueOf(ex.getHttpStatus()));
                    return errorReport(ex, ex.errorCode(), "The user cannot be logged out",
                            variant.getMediaType(), false);
                } finally {
                }
            }
        }

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
//            try {
//                User user = at.getUser();
//                user.setMaxBibTeX(2000);
//                user.setMaxParallelTasks(5);
//                user.setMaxModels(10000);
//                System.out.println(user);
//                AddUser adder = new AddUser(user);
//                adder.write();
//                adder.close();
//            } catch (ServiceInvocationException ex) {
//                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ToxOtisException ex) {
//                Logger.getLogger(LoginResource.class.getName()).log(Level.SEVERE, null, ex);
//            }
            ps = null;
            getCookieSettings().removeAll("subjectid");
            tok = at.stringValue();
            getCookieSettings().add("subjectid", tok);// set it as a cookie
        } catch (ServiceInvocationException ex) {
            getResponse().setStatus(Status.valueOf(ex.getHttpStatus()));
            return errorReport(ex, ex.errorCode(), "The user cannot be authorized",
                    variant.getMediaType(), false);
        }
        if (tok == null) {
            tok = "Invalid Credentials - Unauthorized";
            return new StringRepresentation(getLoginForm(un, "1x2x3x4x5x", tok), MediaType.TEXT_HTML);
        } else {
            toggleSeeOther(redirect);
            return new StringRepresentation(getLoginForm(un, "1x2x3x4x5x", tok), MediaType.TEXT_HTML);
        }
    }
}
