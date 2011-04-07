package org.opentox.jaqpot3.resources.publish;

import java.util.HashSet;
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
        // TODO: Take care of all supported and unsupported mediatypes.
        Publishable p = null;
        if (ms_rdfRelated.contains(m_mediaType)) {
            p = new DataModelPublishable(data.asOntModel(), m_mediaType);
            p.setMediaType(m_mediaType);
            return p;
        } else if (MediaType.TEXT_URI_LIST.equals(m_mediaType)) {
            return new ComponentListPublishable(data, m_mediaType);
        } else if (MediaType.TEXT_HTML.equals(m_mediaType)) {
            if (data instanceof IHTMLSupport) {
                p = new HTMLPublishable(((IHTMLSupport) data).inHtml(), MediaType.TEXT_HTML);
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
