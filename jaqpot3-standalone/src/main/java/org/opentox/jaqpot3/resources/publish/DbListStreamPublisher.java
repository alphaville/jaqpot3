package org.opentox.jaqpot3.resources.publish;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
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

    public Representation process(final IDbIterator<String> iterator) throws JaqpotException {
        Representation representation = new OutputRepresentation(media) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                try {
                    if (iterator != null) {
                        try {

                            if (MediaType.TEXT_HTML.equals(media)) {
                                writer.write("<p><ol>");
                            }
                            while (iterator.hasNext()) {
                                StringBuilder nextUri = new StringBuilder();
                                String id = iterator.next();
                                nextUri.append(getBaseUri().toString());
                                nextUri.append("/");
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
                } catch (IOException ex) {
                    throw ex;
                } finally {
                    writer.close();
                    if (outputStream != null) {// Always close the output stream!
                        outputStream.flush();
                        outputStream.close();
                    }
                    this.release();
                }
            }
        };

        representation.setMediaType(media);
        return representation;

    }
}
