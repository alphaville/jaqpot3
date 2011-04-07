package org.opentox.jaqpot3.resources.publish;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publishable;
import org.restlet.data.Language;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class Representer {

    private boolean doClosePublishable = true;
    private Exception m_exception;

    public Representer() {
    }

    public Representer(boolean doClosePublishable) {
        this.doClosePublishable = doClosePublishable;
    }
    
    public Representation process(final Publishable publishable) throws JaqpotException {

        Representation representation = new OutputRepresentation(publishable.getMediaType()) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                try {
                    publishable.publish(outputStream);
                } catch (Exception ex) {
                    m_exception = ex;
                } finally {
                    if (doClosePublishable && publishable != null) {
                        publishable.close();
                    }
                    if (outputStream != null) {// Always close the output stream!
                        outputStream.flush();
                        outputStream.close();
                    }
                    this.release();
                }
            }
        };
        if (m_exception != null) {
            throw new JaqpotException("Data could not be publihed to output stream.", m_exception);
        }
        List<Language> langs = new ArrayList<Language>();
        langs.add(Language.ENGLISH);
        langs.add(Language.ENGLISH_US);
        representation.setLanguages(langs);
        representation.setMediaType(publishable.getMediaType());        
        return representation;
    }

    
}
