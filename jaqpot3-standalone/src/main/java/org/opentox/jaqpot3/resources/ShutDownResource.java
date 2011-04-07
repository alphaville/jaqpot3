package org.opentox.jaqpot3.resources;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.jaqpot3.www.WebApplecation;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ShutDownResource extends JaqpotResource {


    public static final URITemplate template = new URITemplate("shutdown", null, null);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_URI_LIST);
    }


    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        Thread t = new Thread(){

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    System.exit(0);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ShutDownResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        Executors.newFixedThreadPool(1).submit(t);
        
        return new StringRepresentation("bye!\n");
    }

    

}