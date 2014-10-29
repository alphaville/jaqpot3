package org.opentox.jaqpot3.resources;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
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
public class ValidationInterfaceResource extends JaqpotResource {

    public ValidationInterfaceResource() {
        super();
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        IClientInput input = new ClientInput(entity);
        String testDataset = input.getFirstValue("test_dataset_uri");
        String testTargetDataset = input.getFirstValue("test_target_dataset_uri");
        String validationService = input.getFirstValue("validation_service");
        String modelUri = input.getFirstValue("model_uri");

        VRI validationVri = null;
        try {
            validationVri = new VRI(validationService);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ValidationInterfaceResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        IPostClient poster = ClientFactory.createPostClient(validationVri);
        poster.authorize(getUserToken());
        poster.addPostParameter("model_uri", modelUri);
        poster.addPostParameter("test_dataset_uri", testDataset);
        poster.addPostParameter("test_target_dataset_uri", testTargetDataset);
        poster.setMediaType(Media.TEXT_URI_LIST);
        try {
            poster.post();
            String responseText = poster.getResponseText();
            toggleCreated();
            return new StringRepresentation(String.format("<html><body><p><a href=\"%s\">Task Created</a></p></body></html>", responseText),
                    MediaType.TEXT_HTML);
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(ValidationInterfaceResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        return super.post(entity, variant);
    }
}

