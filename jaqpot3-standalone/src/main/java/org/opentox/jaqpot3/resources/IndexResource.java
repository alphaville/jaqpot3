package org.opentox.jaqpot3.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.UriListPublishable;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.JaqpotWebApplication;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.ReferenceList;
import org.restlet.ext.wadl.WadlRepresentation;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class IndexResource extends JaqpotResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        initialize(MediaType.TEXT_HTML, MediaType.APPLICATION_WADL, MediaType.TEXT_URI_LIST);
        Set<Method> allowedMethods = getAllowedMethods();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.OPTIONS);
        setAllowedMethods(allowedMethods);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {

        final MediaType mediatype = variant.getMediaType();


        List list = new ArrayList();
        list.add(Configuration.getBaseUri().augment("algorithm").toString());
        list.add(Configuration.getBaseUri().augment("model").toString());
        list.add(Configuration.getBaseUri().augment("task").toString());
        list.add(Configuration.getBaseUri().augment("bibtex").toString());
        list.add(Configuration.getBaseUri().augment("dbstats").toString());
        list.add(Configuration.getBaseUri().augment("user").toString());

        Representation rep;
        if (MediaType.TEXT_HTML.equals(mediatype) || MediaType.TEXT_URI_LIST.equals(mediatype)) {
            try {
                UriListPublishable listPublishable = new UriListPublishable(list);
                listPublishable.setMediaType(mediatype);
                Representer representer = new Representer(true);
                return representer.process(listPublishable);
            } catch (JaqpotException ex) {
                throw new ResourceException(ex);
            }
        } else if (mediatype == MediaType.APPLICATION_WADL) {
            return options(variant);
        } else {
            return super.get(variant);
        }

    }

    @Override
    public Representation options(Variant variant) {
        if (variant.getMediaType().equals(MediaType.TEXT_HTML)) {
            return new WadlRepresentation(((JaqpotWebApplication) getApplication()).getAppInfo(getRequest(), getResponse())).getHtmlRepresentation();
        } else if (variant.getMediaType().equals(MediaType.APPLICATION_WADL)) {
            return new WadlRepresentation(((JaqpotWebApplication) getApplication()).getAppInfo(getRequest(), getResponse()));
        }
        return options();
    }
}
