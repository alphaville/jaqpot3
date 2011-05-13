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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.BibTeX;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.engine.bibtex.AssociateBibTeX;
import org.opentox.toxotis.database.engine.bibtex.FindBibTeX;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class BibTexResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("bibtex", "bibtex_id", null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BibTexResource.class);

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
        acceptString = getRequest().getResourceRef().getQueryAsForm().getFirstValue("accept");
        updatePrimaryId(template);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }

        FindBibTeX fb = new FindBibTeX(Configuration.getBaseUri().augment("bibtex"));
        fb.setSearchById(primaryId);
        IDbIterator<BibTeX> bibtexFound = null;
        BibTeX bibtex = null;
        try {
            bibtexFound = fb.list();
            if (bibtexFound.hasNext()) {
                bibtex = bibtexFound.next();
            }
        } catch (DbException ex) {
            Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bibtexFound.close();
            } catch (DbException ex) {
                logger.error("DbIterator is uncloseable", ex);
            }
            try {
                fb.close();
            } catch (DbException ex) {
                logger.error("DB reader is uncloseable", ex);
            }
        }


        if (bibtex == null || (bibtex != null && !bibtex.isEnabled())) {
            toggleNotFound();
            return errorReport("BibTeXNotFound", "The bibtex you requested was not found in our database",
                    "The bibtex with id " + primaryId + " was not found in the database",
                    variant.getMediaType(), false);
        }

        Publisher pub = new Publisher(variant.getMediaType());
        try {
            return pub.createRepresentation(bibtex, true);
        } catch (JaqpotException ex) {
            toggleServerError();
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        }
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {

        IClientInput clientInput = new ClientInput(entity);
        String modelUri = clientInput.getFirstValue("resource");
        VRI modelVri = null;
        try {
            modelVri = new VRI(modelUri);
        } catch (URISyntaxException ex) {
        }
        String modelId = modelVri.getId();
        AssociateBibTeX associator = new AssociateBibTeX(modelId, primaryId);
        try {
            associator.write();
        } catch (DbException ex) {
            Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                associator.close();
            } catch (DbException ex) {
                Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return new StringRepresentation("...\n");
    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {
        DisableComponent disabler = new DisableComponent(primaryId);
        try {
            int count = disabler.disable();
            return new StringRepresentation(count + " components where disabled.\n", MediaType.TEXT_PLAIN);
        } catch (DbException ex) {
            Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                disabler.close();
            } catch (DbException ex) {
                Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
