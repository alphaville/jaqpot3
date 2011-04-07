package org.opentox.jaqpot3.resources;

import java.util.Set;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.JaqpotWebApplication;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.ReferenceList;
import org.restlet.ext.wadl.WadlRepresentation;
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
        ReferenceList list = new ReferenceList();
        list.add(Configuration.BASE_URI + "/algorithm");
        list.add(Configuration.BASE_URI + "/model");
        list.add(Configuration.BASE_URI + "/task");
        list.add(Configuration.BASE_URI + "/bibtex");
        list.add(Configuration.BASE_URI + "/status");
        list.add(Configuration.BASE_URI + "/user");

        Representation rep;
        if (mediatype == MediaType.TEXT_HTML) {
            rep = list.getWebRepresentation();
            rep.setMediaType(mediatype);
            return rep;
        } else if (mediatype == MediaType.TEXT_URI_LIST) {
            rep = list.getTextRepresentation();
            rep.setMediaType(mediatype);
            return rep;
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
