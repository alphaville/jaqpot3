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
