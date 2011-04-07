package org.opentox.jaqpot3.resources.publish;

import java.io.IOException;
import java.io.OutputStream;
import org.opentox.toxotis.core.html.HTMLContainer;
import org.opentox.toxotis.core.html.HTMLPage;
import org.opentox.toxotis.core.html.impl.HTMLPageImpl;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class HTMLPublishable extends AbstractPublishable {

    private HTMLPage document;
    private HTMLContainer div;

    private HTMLPublishable() {
        super();
    }

    public HTMLPublishable(HTMLContainer div) {
        this();
        this.div = div;
    }

    public HTMLPublishable(HTMLContainer div, MediaType media) {
        this();
        this.div = div;
        setMediaType(media);
    }
    

    public HTMLContainer getDiv() {
        return div;
    }

    public HTMLPage getDocument() {
        return document;
    }

    @Override
    public void publish(OutputStream stream) {
        document = new HTMLPageImpl();
        document.getHtmlBody().addComponent(div);
        try {
            new StringRepresentation(document.toString()).write(stream);
        } catch (IOException ex) {
            throw new RuntimeException("IOException : " + ex.getMessage());
        }

    }

    @Override
    public void close() throws IOException {
        
    }
}
