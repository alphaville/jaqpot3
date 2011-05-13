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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.UriListPublishable;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ListInterfaces extends JaqpotResource {

    public static final URITemplate template = new URITemplate("iface", null, null);
    private static final List LIST = new ArrayList() {

        {
            add(Configuration.getBaseUri().augment("iface", "generic").toString());
            add(Configuration.getBaseUri().augment("iface", "policy").toString());
            add(Configuration.getBaseUri().augment("iface", "bibtex").toString());
        }
    };

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


        if (MediaType.TEXT_HTML.equals(mediatype) || MediaType.TEXT_URI_LIST.equals(mediatype)) {
            try {
                UriListPublishable listPublishable = new UriListPublishable(LIST);
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
}
