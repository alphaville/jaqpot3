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
