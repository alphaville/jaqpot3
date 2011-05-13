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


package org.opentox.jaqpot3.resources;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
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
public class ShutDownResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("shutdown", null, null);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_URI_LIST,
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html><head><title>Shutdown Resource</title>");
        formBuilder.append("</head>");
        formBuilder.append("<body>");
        formBuilder.append("<h1>Shutdown the server</h1>");
        formBuilder.append("<form method=\"POST\" action=\""+Configuration.getBaseUri().augment("shutdown")+"\">");
        formBuilder.append("<table>");
        formBuilder.append("<tr>");
        formBuilder.append("<td>Passphrase</td><td><input type=\"password\" value=\"pass\" name=\"pass\"></td>");
        formBuilder.append("</tr>");
        formBuilder.append("<tr>");
        formBuilder.append("<td>Shutdown</td><td><input type=\"submit\" value=\"OFF\"></td>");
        formBuilder.append("</tr>");
        formBuilder.append("</form><br/>");
        formBuilder.append("</body>");
        formBuilder.append("</html>");
        return new StringRepresentation(formBuilder, MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {

        IClientInput clientInput = new ClientInput(entity);
        String passwd = clientInput.getFirstValue("pass");
        if (!"s3cret".equals(passwd)) {
            toggleUnauthorized();
            return errorReport("Unauthorized", "", "", variant.getMediaType(), true);
        }
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    System.exit(0);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ShutDownResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        Executors.newFixedThreadPool(1).submit(t);
        return new StringRepresentation("bye!\n");
    }
}
