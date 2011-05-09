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
public class BibTeXInterface extends JaqpotResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        IClientInput input = new ClientInput(entity);

        IPostClient client = ClientFactory.createPostClient(Configuration.getBaseUri().augment("bibtex"));
        try {
            client.setMediaType(Media.TEXT_URI_LIST);
            client.authorize(getUserToken());
            Iterator<Entry<String, String>> iterator = input.getValuesMap().entrySet().iterator();
            Entry<String, String> nextEntry = null;
            String tempVal = null;
            while (iterator.hasNext()) {
                nextEntry = iterator.next();
                tempVal = nextEntry.getValue();
                if (tempVal != null) {
                    client.addPostParameter(nextEntry.getKey(), tempVal);
                }
            }
            client.post();
            int response = client.getResponseCode();
            if (response == 200) {
                return new StringRepresentation("<html><body><h3>BibTeX created</h3><p>Check out your bibtex <a href=\"" + client.getResponseText()
                        + "\">here</a></p></body></html>", MediaType.TEXT_HTML);
            }

        } catch (ServiceInvocationException ex) {
            Logger.getLogger(BibTeXInterface.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    toggleServerError();
                    return errorReport(ex, "ClientUncloseable", "POST Client cannot close",
                            variant.getMediaType(), true);
                }
            }
        }
        return super.post(entity, variant);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html><head><title>Create a BibTeX entry</title>");
        //formBuilder.append(GoogleAnalytics.getGAjs());
        formBuilder.append("</head>");
        formBuilder.append("<body>");
        formBuilder.append("<h1>Create BibTeX</h1>");


        formBuilder.append("<form method=\"POST\" action=\"\">");
        formBuilder.append("<table>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>BibTeX Type</td><td><select name=\"bibtype\">");
        formBuilder.append("<option value=\"Entry\">General Entry</option>");
        formBuilder.append("<option value=\"Article\">Article in journal/magazine</option>");
        formBuilder.append("<option value=\"Book\">Book</option>");
        formBuilder.append("<option value=\"Booklet\">Booklet</option>");
        formBuilder.append("<option value=\"Inbook\">In Book</option>");
        formBuilder.append("<option value=\"Inproceedings\">In Proceedings</option>");
        formBuilder.append("<option value=\"Manual\">Manual / Technical Documentation</option>");
        formBuilder.append("<option value=\"Mastersthesis\">A Master's thesis</option>");
        formBuilder.append("<option value=\"Proceedings\">Proceedings of a conference</option>");
        formBuilder.append("<option value=\"TechReport\">Technical Report</option>");
        formBuilder.append("<option value=\"Unpublished\">Unpublished</option>");
        formBuilder.append("<option value=\"Misc\">Other/Miscelaneous</option>");
        formBuilder.append("</select></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Title</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"title\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Author(s)</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"author\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Address</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"address\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Annotation</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"annotation\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Booktitle</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"booktitle\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Chapter</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"chapter\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Copyright</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"copyright\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Edition</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"edition\"></td>");
        formBuilder.append("</tr>");


        formBuilder.append("<tr>");
        formBuilder.append("<td>Editor</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"editor\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Crossreference</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"crossref\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Year</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"year\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Pages</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"pages\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Volume</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"volume\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Number</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"number\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>ISBN</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"isbn\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>ISSN</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"issn\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Keywords</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"keywords\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Key</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"key\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Series</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"series\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Web Address (URL)</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"\" name=\"url\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Abstract</td><td><textarea rows=\"5\" cols=\"60\"></textarea></td>");
        formBuilder.append("</tr>");

        formBuilder.append("</table>");
        formBuilder.append("<input type=\"submit\" value=\"Create\">");

        formBuilder.append("</form><br/>");

        formBuilder.append("<p>Click <a href=\"..\">here</a> to go back to the main page</p>");
        formBuilder.append("</body></html>");
        return new StringRepresentation(formBuilder, MediaType.TEXT_HTML);
    }
}
