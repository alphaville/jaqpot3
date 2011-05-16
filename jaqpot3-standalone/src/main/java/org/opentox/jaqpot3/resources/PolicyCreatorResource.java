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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
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
public class PolicyCreatorResource extends JaqpotResource {

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
        super.doInit();
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        IClientInput input = new ClientInput(entity);
        String policy = input.getFirstValue("policy");
        if (policy == null) {
            toggleBadRequest();
            return errorReport("BadRequest", "You cannot post an empty policy",
                    "No policy was specified - Client provided an empty policy.", variant.getMediaType(), false);
        }
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
        IPostClient scp = ClientFactory.createPostClient(Services.SingleSignOn.ssoPolicyOld());
        scp.setContentType(Media.APPLICATION_XML);
        scp.setPostable(policy.trim(), true);
        scp.authorize(getUserToken());
        try {
            scp.post();
            int responseCode = scp.getResponseCode();
            if (responseCode == 400) {
                toggleBadRequest();
                return errorReport("PolicyCreationException", "The policy could not be created as the remote service at "
                        + scp.getUri() + " returned a status code 400. Please check the syntax of your xml and try again.",
                        "The remote service returned the following message : " + scp.getResponseText(), variant.getMediaType(), false);
            } else if (responseCode != 200) {
                toggleRemoteError();
                return errorReport("PolicyCreationException", "The policy could not be created as the remote service at "
                        + scp.getUri() + " returned a status code " + responseCode,
                        "The remote service returned the following message : " + scp.getResponseText(), variant.getMediaType(), false);
            }
        } catch (ServiceInvocationException ex) {
            toggleRemoteError();
            return errorReport(ex, ex.getClass().getSimpleName(), "Fatal exception while invoking the remote SSO service at " + scp.getUri(),
                    variant.getMediaType(), false);
        } finally {
            try {
                scp.close();
            } catch (IOException ex) {
                toggleServerError();
                return errorReport(ex, ex.getClass().getSimpleName(), "Fatal exception while closing an HTTP connection",
                    variant.getMediaType(), false);
            }
        }
        toggleSuccess();
        return new StringRepresentation("<html><body>"
                + "<h3>Policy Successfully Created</h3>"
                + "<p>Get a list of your policies <a href=\"/policy\"here</a>.</p>"
                + "</body></html>",
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        AuthenticationToken at = getUserToken();
        if (at == null) {
            redirectSeeOther("/login?redirect=" + getCurrentVRI());
            return new StringRepresentation("You have to login first");
        }
        String html = "<html>"
                + "<body>"
                + "<h2>Policy Creator</h2>"
                + "<p>Paste your policy XML below and click \"Create\".</p>"
                + "<form method=\"POST\" action=\"\">"
                + "<p><textarea name=\"policy\" rows=\"10\" cols=\"90\"></textarea></p>"
                + "<input type=\"submit\" value=\"Create\">"
                + "</form>"
                + "</body>"
                + "</html>";
        return new StringRepresentation(html, variant.getMediaType());
    }
}
