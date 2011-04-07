package org.opentox.jaqpot3.resources.publish;

import java.io.Closeable;
import java.io.OutputStream;
import org.restlet.data.MediaType;

public interface Publishable extends Closeable {

    void publish(OutputStream stream);

    void setMediaType(MediaType media);

    MediaType getMediaType();

}
