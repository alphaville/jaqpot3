package org.opentox.jaqpot3.resources;

import java.util.ArrayList;
import java.util.List;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.BibTeX;
import org.opentox.toxotis.database.engine.bibtex.FindBibTeX;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class BibTexResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("bibtex", "bibtex_id", null);
    private String bibTexId;

    public BibTexResource() {
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_RDF_XML,
                MediaType.register("application/rdf+xml-abbrev", NEWLINE),
                MediaType.APPLICATION_RDF_TURTLE,
                MediaType.APPLICATION_PDF,
                MediaType.TEXT_URI_LIST,
                MediaType.TEXT_RDF_N3,
                MediaType.TEXT_RDF_NTRIPLES,
                MediaType.TEXT_PLAIN);
        bibTexId = Reference.decode(getRequest().getAttributes().get(template.getPrimaryKey()).toString());
        acceptString = getRequest().getResourceRef().getQueryAsForm().getFirstValue("accept");
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }
        BibTeX myBibTex = null;

        FindBibTeX fb = new FindBibTeX();


        
        Publisher pub = new Publisher(variant.getMediaType());
        try {
            return pub.createRepresentation(myBibTex, true);
        } catch (JaqpotException ex) {
            toggleServerError();
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        }

    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {
        return null;
//        BibTeX prototype = new BibTeX();
//        prototype.setId(bibTexId);
//
//        try {
//            prototype.delete(YAQP.getDb());
//            return sendMessage("The bibtex " + Configuration.BASE_URI + "/" + bibTexId
//                    + " is successfully deleted from the database." + NEWLINE);
//        } catch (NoUniqueFieldException ex) {
//            if (prototype.search(YAQP.getDb()).size() == 0) {
//                toggleNotFound();
//                return errorReport(Cause.BibTexNotFoundInDatabase, "The BibTex you specified was not found in the database",
//                        "You can find a list of all available BibTex references on the server at " + Configuration.BASE_URI + "/bibtex",
//                        variant.getMediaType(), false);
//            } else {
//                return fatalException(Cause.UnknownCauseOfException, ex, null);
//            }
//        }
    }
}
