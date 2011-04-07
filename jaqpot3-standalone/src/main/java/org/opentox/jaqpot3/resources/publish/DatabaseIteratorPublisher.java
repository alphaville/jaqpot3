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
