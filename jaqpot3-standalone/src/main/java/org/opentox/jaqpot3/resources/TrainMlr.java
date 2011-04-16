package org.opentox.jaqpot3.resources;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IClient;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.core.html.GoogleAnalytics;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
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
public class TrainMlr extends JaqpotResource {

    public static final URITemplate template = new URITemplate("train", "mlr", null);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html><head><title>Jaqpot Login Page</title>");
        //formBuilder.append(GoogleAnalytics.getGAjs());
        formBuilder.append("</head>");
        formBuilder.append("<body>");
        formBuilder.append("<h1>Train MLR model</h1>");

        formBuilder.append("<form method=\"POST\" action=\"/train/generic\">");
        formBuilder.append("<input type=\"hidden\""
                + "value=\"" + Configuration.getBaseUri().augment("algorithm", "mlr")
                + "\" name=\"algorithm_uri\">");
        formBuilder.append("<table>");
        formBuilder.append("<tr>");
        formBuilder.append("<td>Dataset URI</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/dataset/54\" name=\"dataset_uri\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Prediction Feature</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/feature/22202\" name=\"prediction_feature\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Feature Service</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"https://apps.ideaconsult.net:8080/ambit2/feature\" name=\"feature_service\"></td>");
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
        IClientInput input = new ClientInput(entity);
        String ds = input.getFirstValue("dataset_uri");
        String pf = input.getFirstValue("prediction_feature");
        IPostClient client = ClientFactory.createPostClient(Configuration.getBaseUri().augment("algorithm", "mlr"));
        client.setMediaType(Media.TEXT_URI_LIST);
        System.out.println(ds);
        System.out.println(pf);
        client.addPostParameter("dataset_uri", ds);
        client.addPostParameter("prediction_feature", pf);
        client.authorize(getUserToken());
        try {
            client.post();
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(TrainMlr.class.getName()).log(Level.SEVERE, null, ex);
        }
        String nextUri = null;
        try {
            nextUri = client.getResponseUriList().iterator().next().toString();
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(TrainMlr.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            client.close();
        } catch (IOException ex) {
            Logger.getLogger(TrainMlr.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new StringRepresentation(nextUri, MediaType.TEXT_HTML);
    }
}
