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


package org.opentox.jaqpot3.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 *
 */
public class RescueResource extends JaqpotResource {

    /*
     * Template:
     * /rescue?id={id}
     */
    public static final URITemplate template = new URITemplate("rescue", null, null);
    private String id;

    @Override
    protected void doInit() {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML,
                MediaType.TEXT_PLAIN);
        id = getQuery().getFirstValue("id");
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        DisableComponent enabler = new DisableComponent(id);
        try {
            int count = enabler.enable();
            return new StringRepresentation(count + " components enabled\n", variant.getMediaType());
        } catch (DbException ex) {
            Logger.getLogger(RescueResource.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            try {
                enabler.close();
            } catch (DbException ex) {
                Logger.getLogger(RescueResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return super.get(variant);
    }
}
