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


package org.opentox.jaqpot3.resources.publish;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class DatabaseIteratorPublisher extends AbstractPublishable {

    private final IDbIterator iterator;
    private final VRI uri;

    public DatabaseIteratorPublisher(final IDbIterator iterator, final VRI uri) {
        this.iterator = iterator;
        this.uri = uri;

    }

    @Override
    public void publish(final OutputStream stream) {

        OutputRepresentation output = new OutputRepresentation(MediaType.TEXT_TSV) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                try {
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
                    while (iterator.hasNext()) {
                        bw.write(uri.augment(iterator.next()).toString());
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();
                } catch (DbException ex) {
                    Logger.getLogger(DatabaseIteratorPublisher.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IOException(ex);
                }
            }
        };
        try {
            output.write(stream);
        } catch (IOException ex) {
            Logger.getLogger(DatabaseIteratorPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (iterator != null) {
            try {
                iterator.close();
            } catch (DbException ex) {
                throw new IOException(ex);
            }
        }
    }
}
