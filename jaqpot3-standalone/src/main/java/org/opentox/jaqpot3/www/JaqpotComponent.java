package org.opentox.jaqpot3.www;

import java.io.IOException;
import org.opentox.jaqpot3.util.Configuration;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.ext.wadl.WadlComponent;
import org.restlet.representation.Representation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class JaqpotComponent extends WadlComponent {

    public JaqpotComponent() throws IOException {
        this(null);
    }

    public JaqpotComponent(Context context) throws IOException{
        this(context,new Application[]{new WebApplecation()});
    }


    public JaqpotComponent(Context context, Application[] applications) {
        super();
        this.getClients().add(Protocol.HTTP);

        for (Application application : applications) {
            application.setContext(context == null ? getContext().createChildContext() : context);
            getDefaultHost().attach(application);
        }
        getInternalRouter().attach("/", applications[0]);

    }
}
