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


package org.opentox.jaqpot3.resources.publish;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.toxotis.core.IHTMLSupport;
import org.opentox.toxotis.core.IOTComponent;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;

public class Publisher {

    private MediaType m_mediaType;
    static Set<MediaType> ms_rdfRelated;

    static {
        ms_rdfRelated = new HashSet<MediaType>();
        ms_rdfRelated.add(MediaType.APPLICATION_RDF_XML);
        ms_rdfRelated.add(MediaType.APPLICATION_RDF_TURTLE);
        ms_rdfRelated.add(MediaType.TEXT_RDF_N3);
        ms_rdfRelated.add(MediaType.TEXT_RDF_NTRIPLES);
    }

    public Publisher(MediaType m_mediaType) {
        this.m_mediaType = m_mediaType;
    }

    public Publishable process(IOTComponent data) throws JaqpotException {
        // TODO: RESTLET Take care of all supported and unsupported mediatypes.
        Publishable p = null;
        if (ms_rdfRelated.contains(m_mediaType)) {
            p = new DataModelPublishable(data.asOntModel(), m_mediaType);
            p.setMediaType(m_mediaType);
            return p;
        } else if (MediaType.TEXT_URI_LIST.equals(m_mediaType)) {
            List singleElementList = new ArrayList();
            singleElementList.add(data.getUri());
            return new UriListPublishable(singleElementList, m_mediaType);
        } else if (MediaType.TEXT_HTML.equals(m_mediaType)) {
            if (data instanceof IHTMLSupport) {
                p = new HTMLPublishable(((IHTMLSupport) data).inHtml(), MediaType.TEXT_HTML);
                return p;
            }
            return null;
        } else if (MediaType.MULTIPART_FORM_DATA.equals(m_mediaType)) {
            if (data instanceof IHTMLSupport) {
                p = new HTMLPublishable(((IHTMLSupport) data).inHtml(), MediaType.MULTIPART_FORM_DATA);
                return p;
            }
            return null;
        } else if (MediaType.TEXT_PLAIN.equals(m_mediaType)) {
            return new PlainTextPublishable(data.toString());
        }
        throw new JaqpotException("MediaType :" + m_mediaType + " is not supported!");
    }

    public Representation createRepresentation(IOTComponent data, boolean doClosePublishable) throws JaqpotException {
        Representer rest = new Representer(doClosePublishable);
        return rest.process(process(data));
    }
}
