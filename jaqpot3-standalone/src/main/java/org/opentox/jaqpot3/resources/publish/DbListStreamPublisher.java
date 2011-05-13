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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.database.DbReader;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class DbListStreamPublisher {

    private MediaType media = MediaType.TEXT_URI_LIST;
    private VRI baseUri;

    public MediaType getMedia() {
        return media;
    }

    public void setMedia(MediaType media) {
        this.media = media;
    }

    public VRI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(VRI baseUri) {
        this.baseUri = baseUri;
    }

    public Representation process(final DbReader<String> reader) throws JaqpotException {

        Representation representation = new OutputRepresentation(media) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                try {
                    IDbIterator<String> iterator = reader.list();
                    if (iterator != null) {
                        try {

                            if (MediaType.TEXT_HTML.equals(media)) {
                                writer.write("<p><ol>");
                            }
                            while (iterator.hasNext()) {
                                StringBuilder nextUri = new StringBuilder();
                                String id = iterator.next();
                                if (getBaseUri() != null) {
                                    nextUri.append(getBaseUri().toString());
                                    nextUri.append("/");
                                }                                
                                nextUri.append(id);

                                if (MediaType.TEXT_HTML.equals(media)) {
                                    writer.write("<li><a href=\"");
                                    writer.write(nextUri.toString());
                                    writer.write("\">");
                                    writer.write(id);
                                    writer.write("</a></li>");
                                } else {
                                    writer.write(nextUri.toString());
                                }
                                writer.write("\n");
                            }
                            if (MediaType.TEXT_HTML.equals(media)) {
                                writer.write("</ol></p>");
                            }
                            iterator.close();
                        } catch (DbException ex) {
                            Logger.getLogger(DbListStreamPublisher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } catch (DbException ex) {
                    throw new IOException(ex);
                } catch (IOException ex) {
                    throw ex;
                } finally {
                    writer.close();
                    if (outputStream != null) {// Always close the output stream!
                        outputStream.flush();
                        outputStream.close();
                    }
                    try {
                        reader.close();
                    } catch (DbException ex) {
                        throw new IOException(ex);
                    }
                    this.release();
                }
            }
        };

        representation.setMediaType(media);
        return representation;

    }
}
