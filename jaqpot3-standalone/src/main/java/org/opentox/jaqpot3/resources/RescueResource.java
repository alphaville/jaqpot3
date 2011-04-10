package org.opentox.jaqpot3.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 *
 */
public class RescueResource extends JaqpotResource {

    /*
     * Template:
     * /rescue?id={id}
     */
    public static final URITemplate template = new URITemplate("rescue", null, null);
    private String id;

    @Override
    protected void doInit() {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML,
                MediaType.TEXT_PLAIN);
        id = getQuery().getFirstValue("id");
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        DisableComponent enabler = new DisableComponent(id);
        try {
            int count = enabler.enable();
            return new StringRepresentation(count + " components enabled\n", variant.getMediaType());
        } catch (DbException ex) {
            Logger.getLogger(RescueResource.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            try {
                enabler.close();
            } catch (DbException ex) {
                Logger.getLogger(RescueResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return super.get(variant);
    }
}
