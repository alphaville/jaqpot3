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
 * Copyright (C) 2009-2011 Pantelis Sopasakis & Charalampos Chomenides
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
package org.opentox.jaqpot3.www.guard;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.logging.Level;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.ErrorReport;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.aa.SSLConfiguration;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.security.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class OpenSSOAuthorizer extends Authorizer {

    private Logger logger = LoggerFactory.getLogger(OpenSSOAuthorizer.class);
    private boolean doAuthentication = true;
    private String acceptHeader;
    private MediaType acceptedMedia = MediaType.TEXT_HTML;
    private final boolean protectGet;
    private final boolean protectPost;

    private static boolean doAuthentication() {
        boolean doAuth = Boolean.parseBoolean(Configuration.getStringProperty("jaqpot.doAuthentication").toLowerCase());
        return doAuth;
    }

    public OpenSSOAuthorizer(boolean protectGet, boolean protectPost) {
        super();
        SSLConfiguration.initializeSSLConnection();
        this.protectGet = protectGet;
        this.protectPost = protectPost;
    }

    private synchronized void throwError(
            final Response response,
            String actor,
            String errorCode,
            String message,
            String details,
            int httpStatus) {
        ErrorReport er = new ErrorReport(httpStatus, actor, message, details, errorCode);
        Publisher p = new Publisher(acceptedMedia);
        response.setStatus(Status.valueOf(httpStatus));
        try {
            Representation rep = p.createRepresentation(er, false);
            rep.setMediaType(acceptedMedia);
            response.setEntity(rep);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private AuthenticationToken getToken(Request request, Response response) {
        AuthenticationToken userToken = null;
        Form headers = (Form) request.getAttributes().get("org.restlet.http.headers");
        String token = headers.getFirstValue("subjectid");
        if (token != null) {
            userToken = new AuthenticationToken(token);
        }
        if (token == null) {// no token in subjectid header
            /*
             * There might be a token in the URL (as URL encoded)
             */
            String tokenStringUrlEncoded = request.getResourceRef().getQueryAsForm().getFirstValue("subjectid");
            if (tokenStringUrlEncoded != null) {
                String tokenString = null;
                try {
                    tokenString = URLDecoder.decode(tokenStringUrlEncoded, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    java.util.logging.Logger.getLogger(OpenSSOAuthorizer.class.getName()).log(Level.SEVERE, null, ex);
                }
                AuthenticationToken tokenRetrieved = new AuthenticationToken(tokenString);
                try {
                    if (tokenRetrieved.validate()) {
                        userToken = tokenRetrieved;
                    }
                } catch (ServiceInvocationException ex) {
                    java.util.logging.Logger.getLogger(OpenSSOAuthorizer.class.getName()).log(Level.SEVERE, null, ex);
                }
                response.getCookieSettings().add("subjectid", tokenString);// set it as a cookie
            }

        }
        if (userToken == null) {
            /*
             * There might be a token as a cookie ;)
             */
            String tokenAsCookie = request.getCookies().getValues("subjectid"); // The value in the cookie is not URL-encoded
            AuthenticationToken tokenRetrieved = new AuthenticationToken(tokenAsCookie);
            try {
                if (tokenRetrieved.validate()) { // Has the user provided a valid token via the cookie?                   
                    userToken = tokenRetrieved;
                }
            } catch (ServiceInvocationException ex) {
                java.util.logging.Logger.getLogger(OpenSSOAuthorizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (userToken == null) {
            return null;
        }

        return userToken;
    }

    @Override
    protected boolean authorize(Request request, Response response) {

        String clientRequest = request.getMethod().getName();

        if ("GET".equals(clientRequest) && !protectGet) {
            return true;
        }

        if ("POST".equals(clientRequest) && !protectPost) {
            return true;
        }

        acceptHeader = request.getResourceRef().getQueryAsForm().getFirstValue("accept");
        if (acceptHeader != null) {
            this.acceptedMedia = MediaType.valueOf(acceptHeader);
        } else {
            // Now check the header of the request
            acceptHeader = ((Form) request.getAttributes().get("org.restlet.http.headers")).getFirstValue("Accept");
            System.out.println("Received accept header is : " + acceptHeader);
            if (acceptHeader == null) {
                acceptedMedia = MediaType.TEXT_HTML;
            } else {
                try {
                    this.acceptedMedia = MediaType.valueOf(acceptHeader);
                } catch (IllegalArgumentException iae) {
                    System.out.println("AA543");
                    acceptedMedia = MediaType.TEXT_HTML;
                }
            }
        }
        if (MediaType.ALL.equals(acceptedMedia)) {
            System.out.println("BB38423");
            acceptedMedia = MediaType.TEXT_HTML; // preferred mediatype
        }
        doAuthentication = doAuthentication();
        if (!doAuthentication) {
            return true;
        }
        String clientAddress = request.getClientInfo().getAddress();
        String actionUri = request.getResourceRef().toString();
        AuthenticationToken userToken = getToken(request, response);
        try {
            VRI actionVri = new VRI(actionUri);
            actionVri.getUrlParams().clear();
            actionUri = actionVri.toString();
        } catch (URISyntaxException ex) {
            java.util.logging.Logger.getLogger(OpenSSOAuthorizer.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (userToken == null) {
            throwError(response, "(Client) " + clientAddress, "Anonymous", "Anonymous use of this service is not allowed. "
                    + "Please provide your authentication token using the HTTP Header 'subjectid' as "
                    + "specified in section 14.8 of RFC 2616.", "Client attempted to apply a " + clientRequest + " method "
                    + "on " + actionUri, 403);
            return false;
        }
        try {
            boolean allowed = userToken.authorize("PATCH".equals(clientRequest) ? "POST" : clientRequest, actionUri);
            try {
                logger.info("Authorizing " + userToken.getUser().getName() + " on " + actionUri + " : " + allowed);
            } catch (ServiceInvocationException ex) {
                logger.warn("Cannot retrieve user information from the undelying token", ex);
            } catch (ToxOtisException ex) {
                logger.warn("Received bad user email", ex);
            }
            if (allowed) {
                return true; // proceed to the resource...
            } else {
                throwError(response, "(Client) " + clientAddress, "Unauthorized", "User is not authorized to perform a " + clientRequest
                        + " method on " + actionUri, "Client attempted to apply a " + clientRequest + " method "
                        + "on " + actionUri + " but authorization failed.", 401);
                return false;
            }
        } catch (ServiceInvocationException ex) {
            logger.error("Exception due to (probably) bad connection/communication with the "
                    + "remote SSO server [see details]", ex);
        }
        return false;

    }
}
