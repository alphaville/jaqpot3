package org.opentox.jaqpot3.resources.publish;

import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.opentox.toxotis.core.IOTComponent;
import org.opentox.toxotis.core.OTComponent;
import org.opentox.toxotis.ontology.impl.SimpleOntModelImpl;
import org.restlet.data.MediaType;
import org.restlet.data.ReferenceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ComponentListPublishable<T extends IOTComponent> extends AbstractPublishable {

    private Logger logger = LoggerFactory.getLogger(ComponentListPublishable.class);

    private Collection<T> m_list;

    private ComponentListPublishable() {
        m_list = new ArrayList<T>();
    }

    public ComponentListPublishable(Collection<T> list) {
        m_list = list;
    }

    public ComponentListPublishable(Collection<T> list, MediaType mediatype) {
        m_list = list;
        setMediaType(mediatype);
    }

    public ComponentListPublishable(T singleComponent, MediaType mediatype) {
        this();
        m_list.add(singleComponent);
        setMediaType(mediatype);
    }

    private ReferenceList getReferenceList() {
        ReferenceList refList = new ReferenceList();
        for (T yc : m_list) {
            refList.add(yc.getUri().toString());
        }
        return refList;
    }

    @Override
    public void publish(OutputStream stream) {
        if (MediaType.TEXT_URI_LIST.equals(getMediaType())) { // URI list
            ReferenceList refList = getReferenceList();
            try {
                refList.getTextRepresentation().write(stream);
            } catch (IOException ex) {
                logger.error("Exception while writing a reference list to "
                        + "the output stream.", ex);
                throw new RuntimeException();
            }
        } else if (MediaType.TEXT_HTML.equals(getMediaType())) { // HTML
            ReferenceList refList = getReferenceList();
            try {
                refList.getWebRepresentation().write(stream);
            } catch (IOException ex) {
                logger.error("Exception while writing a reference list to "
                        + "the output stream.", ex);
                throw new RuntimeException();
            }
        } else if (Publisher.ms_rdfRelated.contains(getMediaType())) { // RDF-related
            OntModel ontMod = new SimpleOntModelImpl();
            for (T yc : m_list) {
                yc.asIndividual(ontMod);
            }
            DataModelPublishable dataModelPublishable = new DataModelPublishable(ontMod, getMediaType());
            dataModelPublishable.publish(stream);
        }
    }

    @Override
    public void close() throws IOException {
        // ?
    }
}
