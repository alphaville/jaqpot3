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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 */
public class TrainGeneric extends JaqpotResource {

    

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        AuthenticationToken userToken = getUserToken();
        if (userToken == null) {
            toggleSeeOther("/login?redirect=" + getCurrentVRI());
        }
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html><head><title>Train a Model</title>");
        //formBuilder.append(GoogleAnalytics.getGAjs());
        formBuilder.append("</head>");
        formBuilder.append("<body>");
        formBuilder.append("<h1>Train Any model</h1>");


        formBuilder.append("<form method=\"POST\" action=\"#\">");
        formBuilder.append("<table>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Algorithm URI</td><td><select name=\"algorithm_uri\" style=\"width:505\" width=\"505\">");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "mlr") + "\">Multiple Linear Regression</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "svm") + "\">Support Vector Machines</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "fastRbfNn") + "\">RBF Neural Networks (Fast Impl)</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "leverages") + "\">Leverage Domain of Applicability</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "consensus") + "\">Consensus Modeling</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "scaling") + "\">Preprocessing: Scaling</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "mvh") + "\">Preprocessing: MVH</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "cleanup") + "\">Preprocessing: Data Cleanup</option>");
        formBuilder.append("<option value=\"" + Configuration.getBaseUri().augment("algorithm", "pls") + "\">Preprocessing: PLS fitler</option>");
        formBuilder.append("</select></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Dataset URI</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/dataset/R545\" name=\"dataset_uri\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Prediction Feature</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/feature/22200\" name=\"prediction_feature\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Feature Service</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/feature\" name=\"feature_service\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Parameters</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"params\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("</table>");
        formBuilder.append("<input type=\"submit\" value=\"Train\">");

        formBuilder.append("</form><br/>");

        formBuilder.append("<p>Click <a href=\"..\">here</a> to go back to the main page</p>");
        formBuilder.append("</body></html>");
        return new StringRepresentation(formBuilder, MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        AuthenticationToken userToken = getUserToken();
        if (userToken == null) {
            toggleSeeOther("/login?redirect=" + getCurrentVRI());
            return new StringRepresentation("You must login first!!!");
        }
        IClientInput input = new ClientInput(entity);
        String ds = input.getFirstValue("dataset_uri");
        String pf = input.getFirstValue("prediction_feature");
        String alg = input.getFirstValue("algorithm_uri");
        String params = input.getFirstValue("params");

        Map<String, String> map = new HashMap<String, String>();
        if (params != null && !params.isEmpty()) {
            String paramTokens[] = params.split(" ");
            for (String nvp : paramTokens) {
                String[] parts = nvp.split("=");
                map.put(parts[0], parts[1]);
            }
        }

        IPostClient client = null;
        try {
            client = ClientFactory.createPostClient(new VRI(alg));
        } catch (URISyntaxException ex) {
            Logger.getLogger(TrainGeneric.class.getName()).log(Level.SEVERE, null, ex);
        }
        client.setMediaType(Media.TEXT_URI_LIST);
        client.addPostParameter("dataset_uri", ds);
        client.addPostParameter("prediction_feature", pf);
        if (!map.isEmpty()) {
            Iterator<Entry<String, String>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, String> entry = iterator.next();
                client.addPostParameter(entry.getKey(), entry.getValue());
            }
        }        
        client.authorize(userToken);
        try {
            client.post();
        } catch (ServiceInvocationException ex) {
        }
        String nextUri = null;
        try {
            nextUri = client.getResponseUriList().iterator().next().toString();
        } catch (ServiceInvocationException ex) {
            //return ex.asErrorReport().;
        }
        try {
            client.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        toggleSeeOther(nextUri);
        String htmlResponse = "<html>"
                + "<head>"
                + "  <title>Task Created</title>"
                + "</head>"
                + "<body>"
                + "<p>Message : </p>\n"
                + "<p><a href=\"" + nextUri + "\">Task Created</a></p>"
                + "</body>"
                + "</html>";
        return new StringRepresentation(htmlResponse, MediaType.TEXT_HTML);
    }
}
