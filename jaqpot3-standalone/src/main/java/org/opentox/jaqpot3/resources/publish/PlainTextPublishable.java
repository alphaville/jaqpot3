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

import java.io.IOException;
import java.io.OutputStream;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PlainTextPublishable extends AbstractPublishable {

    private String text;

    public void setText(String text) {
        this.text = text;
    }

    public PlainTextPublishable(String text) {
        super();
        this.text = text;
        setMediaType(MediaType.TEXT_PLAIN);
    }

    public PlainTextPublishable(String text, MediaType media) {
        super();
        this.text = text;
        setMediaType(media);
    }

    @Override
    public void publish(OutputStream stream) {
        try {
            new StringRepresentation(text, getMediaType() != null ? getMediaType() : MediaType.TEXT_PLAIN).write(stream);
        } catch (IOException ex) {
            throw new RuntimeException("IOException : " + ex.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
    }
}
