package org.opentox.jaqpot3.resources.publish;

import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.restlet.data.MediaType;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class DataModelPublishable extends AbstractPublishable {

    private OntModel m_model;
    private String lang;
    private static Map<MediaType, String> ms_langMediaRelation;

    

    public DataModelPublishable(final OntModel model, final MediaType media) {
        setMediaType(media);
        this.m_model = model;
        lang = mediaTypeToLang(getMediaType());
    }

    private synchronized String mediaTypeToLang(MediaType media) {

        if (ms_langMediaRelation == null) {
            ms_langMediaRelation = new HashMap<MediaType, String>();
            ms_langMediaRelation.put(MediaType.APPLICATION_RDF_XML, "RDF/XML");
            ms_langMediaRelation.put(MediaType.APPLICATION_RDF_TURTLE, "TTL");
            ms_langMediaRelation.put(MediaType.TEXT_RDF_N3, "N3");
            ms_langMediaRelation.put(MediaType.TEXT_RDF_NTRIPLES, "N-TRIPLE");
            ms_langMediaRelation.put(MediaType.register("application/rdf+xml-abbrev", ""), "RDF/XML-ABBREV");
        }
        String language = ms_langMediaRelation.get(media);
        if (language == null){
            setMediaType(MediaType.APPLICATION_RDF_XML);
        }
        return ms_langMediaRelation.get(media);
    }

    @Override
    public void publish(OutputStream stream) {
        m_model.write(stream, lang);
    }


    @Override
    public void close() throws IOException {
        m_model.close();
    }

    public OntModel getOntModel() {
        return m_model;
    }
}
