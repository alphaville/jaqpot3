package org.opentox.jaqpot3.resources.publish;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.html.GoogleAnalytics;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.ReferenceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class UriListPublishable extends AbstractPublishable {

    private Logger logger = LoggerFactory.getLogger(UriListPublishable.class);
    private List m_list;
    private VRI baseUri;
    private String heading;

    private UriListPublishable() {
        m_list = new ArrayList();
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public void setBaseUri(VRI baseUri) {
        this.baseUri = baseUri;
    }

    public UriListPublishable(List list) {
        m_list = list;
    }

    public UriListPublishable(List list, MediaType mediatype) {
        m_list = list;
        setMediaType(mediatype);
    }

    private ReferenceList getReferenceList() {
        ReferenceList refList = new ReferenceList();
        for (Object obj : m_list) {
            refList.add(obj.toString());
        }
        return refList;
    }

    private void serializeHtml(OutputStreamWriter writer, ReferenceList refList) throws IOException {
        writer.write("<html>\n"
                + "<head>\n");
        writer.write(GoogleAnalytics.getGAjs());
        writer.write("</head>\n"
                + "<body>");
        if (heading == null) {
            writer.write("<h1>List of URIs</h1>\n");
        } else {
            writer.write("<h1>");
            writer.write(heading);
            writer.write("</h1>\n");
        }
        writer.write("<p>");

        Iterator<Reference> iterator = refList.iterator();
        String currentUri = "";
        while (iterator.hasNext()) {
            currentUri = iterator.next().toString();
            writer.write("<a href=\"");
            if (baseUri != null) {
                writer.write(new VRI(baseUri).augment(currentUri).toString());
            } else {
                writer.write(currentUri);
            }
            writer.write("\">");
            writer.write(currentUri);
            writer.write("</a></br>\n");
        }

        writer.write("</p>\n"
                + "</boby>\n"
                + "</html>");


    }

    private void serializeText(OutputStreamWriter writer, ReferenceList refList) throws IOException {

        Iterator<Reference> iterator = refList.iterator();
        String currentUri = "";
        while (iterator.hasNext()) {
            currentUri = iterator.next().toString();
            writer.write(currentUri);
            writer.write("\n");
        }
    }

    @Override
    public void publish(OutputStream stream) {
        OutputStreamWriter writer = new OutputStreamWriter(stream);
        ReferenceList refList = getReferenceList();

        try {
            if (MediaType.TEXT_URI_LIST.equals(getMediaType())) { // URI list
                serializeText(writer, refList);
            } else if (MediaType.TEXT_HTML.equals(getMediaType())) { // HTML
                serializeHtml(writer, refList);
            }
        } catch (final IOException ex) {
            logger.error("Exception while writing a reference list to "
                    + "the output stream.", ex);
            throw new RuntimeException(ex);
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (final IOException ex) {
                logger.error("Exception while closing outputstream writer!!!", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void close() throws IOException {
        // ?
    }
}
